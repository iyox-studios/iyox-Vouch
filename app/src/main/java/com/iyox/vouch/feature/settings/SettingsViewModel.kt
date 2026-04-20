package com.iyox.vouch.feature.settings

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.icu.util.Currency
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
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
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.Vouch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.reflect.Type
import java.math.BigDecimal
import java.sql.Timestamp

data class SettingsUiState(
    val theme: Int,
    val dynamicColors: Boolean
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val uiModeManager = application.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(uiModeManager.nightMode)
    private val _dynamicColors = MutableStateFlow(prefs.getBoolean("dynamic_colors", true))

    val uiState: StateFlow<SettingsUiState> = combine(
        _theme, _dynamicColors
    ) { theme, dynamicColors ->
        SettingsUiState(
            theme = theme,
            dynamicColors = dynamicColors
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState(uiModeManager.nightMode, prefs.getBoolean("dynamic_colors", true))
    )


    fun setTheme(theme: Int) {
        _theme.value = theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiModeManager.setApplicationNightMode(theme)
        } else {
            AppCompatDelegate.setDefaultNightMode(theme)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        _dynamicColors.value = enabled
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Currency::class.java, object : JsonSerializer<Currency>,
            JsonDeserializer<Currency> {
            override fun serialize(
                src: Currency,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement {
                return JsonPrimitive(src.currencyCode)
            }

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Currency {
                return try {
                    Currency.getInstance(json.asString)
                } catch (e: Exception) {
                    Currency.getInstance("USD")
                }
            }
        })
        .registerTypeAdapter(BigDecimal::class.java, object : JsonSerializer<BigDecimal>,
            JsonDeserializer<BigDecimal> {
            override fun serialize(
                src: BigDecimal,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement {
                return JsonPrimitive(src.toString())
            }

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): BigDecimal {
                return BigDecimal(json.asString)
            }
        })
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
                val vouches = database.vouchDataDao().getAll()
                val json = gson.toJson(vouches)

                app.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        "Export successful",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Export failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                app.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        val itemType = object : TypeToken<List<Vouch>>() {}.type
                        val vouches: List<Vouch> = gson.fromJson(reader, itemType)

                        database.personDao().deleteAll()
                        database.vouchDataDao().deleteAll()

                        val newPersons = mutableSetOf<String>()
                        vouches.forEach { vouch ->
                            val newVouch = vouch.copy(id = 0)
                            database.vouchDataDao().insert(newVouch)
                            if (!newVouch.personName.isNullOrBlank()) {
                                newPersons.add(newVouch.personName.trim())
                            }
                        }

                        newPersons.forEach {
                            database.personDao().insert(Person(name = it))
                        }
                    }
                }
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        "Import successful",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                launch(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Import failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val appVersion: String
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(
                getApplication<Application>().packageName, 0
            )
            pInfo.versionName ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
}