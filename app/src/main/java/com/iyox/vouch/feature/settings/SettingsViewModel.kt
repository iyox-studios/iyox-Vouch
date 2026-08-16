package com.iyox.vouch.feature.settings

import android.app.Application
import android.app.LocaleManager
import android.app.UiModeManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.iyox.vouch.R
import com.iyox.vouch.core.currency.CurrencyManager
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchPersonCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.sql.Timestamp

sealed interface ToastEvent {
    data class Info(@field:StringRes val messageRes: Int) : ToastEvent
    data class Error(@field:StringRes val messageRes: Int) : ToastEvent
}

data class SettingsUiState(
    val theme: Int,
    val dynamicColors: Boolean,
    val languageTag: String,
    val currencyCode: String,
    val biometricEnabled: Boolean
)

data class ExportPersonAmount(
    val name: String,
    val amount: Long,
    val settled: Boolean = false
)

data class ExportVouch(
    val vouch: Vouch,
    val persons: List<ExportPersonAmount>
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _toastEvents = MutableSharedFlow<ToastEvent>()
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()

    val uiModeManager = application.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun currentLanguageTag(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return "system"
        }

        val localeManager =
            getApplication<Application>().getSystemService(LocaleManager::class.java)
        val current = localeManager.applicationLocales.toLanguageTags()
        return current.ifBlank { "system" }
    }

    private fun resolveInitialTheme(): Int {
        val storedTheme = prefs.getInt("theme", Int.MIN_VALUE)
        if (storedTheme != Int.MIN_VALUE) return storedTheme
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            UiModeManager.MODE_NIGHT_AUTO
        } else {
            val delegateMode = AppCompatDelegate.getDefaultNightMode()
            if (delegateMode != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                delegateMode
            } else {
                UiModeManager.MODE_NIGHT_AUTO
            }
        }
    }

    private val _theme = MutableStateFlow(resolveInitialTheme())
    private val _dynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors", true))
    private val _languageTag = MutableStateFlow(currentLanguageTag())
    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))

    val uiState: StateFlow<SettingsUiState> = combine(
        _theme, _dynamicColors, _languageTag, CurrencyManager.currencyFlow(), _biometricEnabled
    ) { theme, dynamicColors, languageTag, currency, biometricEnabled ->
        SettingsUiState(
            theme = theme,
            dynamicColors = dynamicColors,
            languageTag = languageTag,
            currencyCode = currency.currencyCode,
            biometricEnabled = biometricEnabled
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState(
            resolveInitialTheme(),
            prefs.getBoolean("dynamic_colors", true),
            currentLanguageTag(),
            CurrencyManager.getCurrency().currencyCode,
            prefs.getBoolean("biometric_enabled", false)
        )
    )

    fun setTheme(theme: Int) {
        _theme.value = theme
        prefs.edit { putInt("theme", theme) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        _dynamicColors.value = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _biometricEnabled.value = enabled
        prefs.edit { putBoolean("biometric_enabled", enabled) }
    }

    fun showBiometricError(@StringRes messageRes: Int) {
        viewModelScope.launch {
            _toastEvents.emit(ToastEvent.Error(messageRes))
        }
    }

    fun setLanguage(tag: String) {
        _languageTag.value = tag

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val appLocale = if (tag == "system") {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }

        val localeManager =
            getApplication<Application>().getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = appLocale
    }

    fun setCurrency(currencyCode: String) {
        CurrencyManager.setCurrency(currencyCode)
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Timestamp::class.java, object : JsonSerializer<Timestamp>,
            JsonDeserializer<Timestamp> {
            override fun serialize(
                src: Timestamp,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement {
                return JsonPrimitive(src.time)
            }

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Timestamp {
                return Timestamp(json.asLong)
            }
        })
        .create()

    private val database = AppDatabase.getDatabase(application)

    fun exportData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val exportVouches = database.vouchDataDao().getAll().map { vouch ->
                    val persons = database.vouchDataDao().getPersonsForVouch(vouch.id)
                        .map { personWithAmount ->
                            ExportPersonAmount(
                                name = personWithAmount.person.name,
                                amount = personWithAmount.amount,
                                settled = personWithAmount.settled
                            )
                        }
                    ExportVouch(vouch = vouch, persons = persons)
                }
                val json = gson.toJson(exportVouches)

                app.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                _toastEvents.emit(ToastEvent.Info(R.string.toast_export_success))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvents.emit(ToastEvent.Error(R.string.toast_export_failed))
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                app.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val itemType = object : TypeToken<List<ExportVouch>>() {}.type
                        val vouches: List<ExportVouch> = gson.fromJson(reader, itemType)

                        database.vouchDataDao().deleteAll()
                        database.personDao().deleteAll()

                        vouches.forEach { export ->
                            val newVouchId = database.vouchDataDao()
                                .insert(export.vouch.copy(id = 0))
                                .toInt()

                            val crossRefs = export.persons.mapNotNull { personAmount ->
                                val normalizedName = personAmount.name.trim()
                                if (normalizedName.isBlank()) return@mapNotNull null
                                database.personDao().insert(Person(name = normalizedName))
                                val storedPerson = database.personDao().getByName(normalizedName)
                                    ?: return@mapNotNull null
                                VouchPersonCrossRef(
                                    vouchId = newVouchId,
                                    personId = storedPerson.id,
                                    amount = personAmount.amount,
                                    settled = personAmount.settled
                                )
                            }

                            if (crossRefs.isNotEmpty()) {
                                database.vouchDataDao().insertCrossRefs(crossRefs)
                            }
                        }
                    }
                }
                _toastEvents.emit(ToastEvent.Info(R.string.toast_import_success))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvents.emit(ToastEvent.Error(R.string.toast_import_failed))
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.vouchDataDao().deleteAll()
                database.personDao().deleteAll()
                _toastEvents.emit(ToastEvent.Info(R.string.msg_all_data_deleted))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvents.emit(ToastEvent.Error(R.string.msg_delete_data_failed))
            }
        }
    }

    val appVersion: String
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(
                getApplication<Application>().packageName, 0
            )
            pInfo.versionName ?: getApplication<Application>().getString(R.string.label_unknown)
        } catch (_: Exception) {
            getApplication<Application>().getString(R.string.label_unknown)
        }
}