package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.core.content.edit
import com.iyox.vouch.MainActivity
import com.iyox.vouch.R
import com.iyox.vouch.core.data.PersonDao
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.utils.authenticateForApp
import com.iyox.vouch.ui.theme.AppColorTheme
import com.iyox.vouch.core.utils.PreferenceKeys
import android.app.UiModeManager
import com.iyox.vouch.ui.theme.IyoxVouchTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PersonWidgetConfigurationActivity : AppCompatActivity() {
    @Inject lateinit var personDao: PersonDao
    @Inject lateinit var updater: VouchWidgetUpdater
    private var people by mutableStateOf<List<Person>?>(null)
    private var saving by mutableStateOf(false)
    private var isAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || info?.provider?.className != PersonWidgetProvider::class.java.name) {
            finish()
            return
        }
        enableEdgeToEdge()
        isAuthenticated = savedInstanceState?.getBoolean(KEY_IS_AUTHENTICATED, false) == true
        if (isAuthenticated) {
            showPicker(id)
        } else {
            authenticateForApp {
                isAuthenticated = true
                showPicker(id)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_AUTHENTICATED, isAuthenticated)
    }

    private fun showPicker(id: Int) {
        val prefs = getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, MODE_PRIVATE)
        val color = AppColorTheme.fromKey(prefs.getString(PreferenceKeys.KEY_COLOR_THEME, null)
            ?: if (prefs.getBoolean(PreferenceKeys.KEY_DYNAMIC_COLORS, true)) "dynamic" else "purple")
        val night = when (prefs.getInt(PreferenceKeys.KEY_THEME, UiModeManager.MODE_NIGHT_AUTO)) {
            UiModeManager.MODE_NIGHT_YES -> true
            UiModeManager.MODE_NIGHT_NO -> false
            else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        lifecycleScope.launch {
            people = personDao.getPersonsTotalAll().map { it.person }.sortedBy { it.name.lowercase() }
        }
        setContent {
            IyoxVouchTheme(darkTheme = night, colorTheme = color) {
                Scaffold { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)) {
                        Icon(painterResource(R.drawable.ic_person), null,
                            Modifier.padding(top = 24.dp, bottom = 20.dp).size(40.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.widget_choose_person), style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.widget_choose_person_hint),
                            Modifier.padding(top = 8.dp, bottom = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val list = people
                        when {
                            list == null || saving -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                            list.isEmpty() -> {
                                Text(stringResource(R.string.widget_no_people))
                                OutlinedButton(onClick = {
                                    startActivity(Intent(this@PersonWidgetConfigurationActivity, MainActivity::class.java))
                                    finish()
                                }, modifier = Modifier.padding(top = 16.dp)) {
                                    Text(stringResource(R.string.widget_open_app))
                                }
                            }
                            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                                items(list, key = { it.id }) { person ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                        shape = MaterialTheme.shapes.large,
                                        modifier = Modifier.fillMaxWidth().clickable(enabled = !saving) { selectPerson(id, person.id) }) {
                                        Text(person.name, Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun selectPerson(widgetId: Int, personId: Long) {
        saving = true
        lifecycleScope.launch {
            getSharedPreferences(VouchWidgetProvider.WIDGET_PREFERENCES, MODE_PRIVATE).edit {
                putLong(VouchWidgetProvider.personKey(widgetId), personId)
            }
            updater.updateAll()
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }

    companion object {
        private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    }
}
