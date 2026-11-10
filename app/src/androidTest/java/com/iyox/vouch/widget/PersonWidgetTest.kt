package com.iyox.vouch.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.widget.TextView
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iyox.vouch.R
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersonWidgetTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun configurationRenameSettlementAndDeletionRefreshTheCorrectWidget() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "appwidget grantbind --package ${context.packageName} --user 0"
        )).use { it.readBytes() }
        val database = AppDatabase.getDatabase(context)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val repository = VouchRepository(database, database.vouchDao(), database.personDao(), scope)
        val name = "Widget test ${System.nanoTime()}"
        val vouch = repository.insert(VouchWithPerson(Vouch(amount = 5000), listOf(
            PersonWithAmount(Person(name = name), 2000),
            PersonWithAmount(Person(name = "$name other"), 3000)
        )))
        try {
            ActivityScenario.launch<WidgetHostTestActivity>(Intent(context, WidgetHostTestActivity::class.java)
                .putExtra("kind", "person")).use { host ->
                var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                host.onActivity { widgetId = it.widgetId }
                ActivityScenario.launchActivityForResult<PersonWidgetConfigurationActivity>(
                    Intent(context, PersonWidgetConfigurationActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                ).use { config ->
                    compose.waitUntil(5000) {
                        compose.onAllNodes(androidx.compose.ui.test.hasText(name)).fetchSemanticsNodes().isNotEmpty()
                    }
                    compose.onNodeWithText(name).performClick()
                    assertEquals(android.app.Activity.RESULT_OK, config.result.resultCode)
                }
                val person = vouch.persons.first { it.person.name == name }.person
                assertEquals(person.id, context.getSharedPreferences(VouchWidgetProvider.WIDGET_PREFERENCES, Context.MODE_PRIVATE)
                    .getLong(VouchWidgetProvider.personKey(widgetId), -1))
                waitForTitle(host, name)
                host.onActivity {
                    assertEquals(com.iyox.vouch.core.currency.CurrencyManager.formatAmountWithSymbol(2000),
                        it.widgetView.findViewById<TextView>(R.id.widget_amount).text.toString())
                }
                // Stable database identity keeps the widget attached through renames.
                database.personDao().updateName(person.id, "$name renamed")
                waitForTitle(host, "$name renamed")
                repository.setPersonSettlement(vouch.vouch.id, person.id, true)
                compose.waitUntil(5000) {
                    var count = ""
                    host.onActivity { count = it.widgetView.findViewById<TextView>(R.id.widget_count)?.text.toString() }
                    count.startsWith("0 ")
                }
                repository.delete(vouch.vouch.id)
                waitForTitle(host, context.getString(R.string.widget_person_missing))
            }
        } finally {
            repository.delete(vouch.vouch.id)
            scope.cancel()
        }
    }

    private fun waitForTitle(host: ActivityScenario<WidgetHostTestActivity>, expected: String) {
        compose.waitUntil(5000) {
            var title = ""
            host.onActivity { title = it.widgetView.findViewById<TextView>(R.id.widget_title)?.text.toString() }
            title == expected
        }
    }
}
