package com.iyox.vouch.screenshots

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyox.vouch.MainActivity
import com.iyox.vouch.R
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.model.Person
import com.iyox.vouch.core.model.PersonWithAmount
import com.iyox.vouch.core.model.Vouch
import com.iyox.vouch.core.model.VouchWithPerson
import com.iyox.vouch.core.utils.PreferenceKeys
import kotlinx.coroutines.runBlocking
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

/**
 * Instrumentation test used by Fastlane Screengrab to automatically capture localized screenshots.
 * Run via `fastlane screenshots` or `./gradlew connectedAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    companion object {
        @get:ClassRule
        @JvmStatic
        val localeTestRule = LocaleTestRule()
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun captureAppScreenshots() {
        val context: Application = ApplicationProvider.getApplicationContext()

        // 1. Initialize preferences: mark onboarding complete and disable biometrics for screenshots
        val prefs = context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PreferenceKeys.KEY_ONBOARDING_COMPLETED, true)
            .putBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false)
            .putString(PreferenceKeys.KEY_CURRENCY_CODE, "EUR")
            .commit()

        // 2. Pre-populate database with appealing sample data
        val database = AppDatabase.getDatabase(context)
        val repository = VouchRepository(database, database.vouchDao(), database.personDao())

        runBlocking {
            database.vouchDao().deleteAll()
            database.personDao().deleteAll()

            // Given 45.00 EUR to Alice for Dinner
            repository.insert(
                VouchWithPerson(
                    vouch = Vouch(amount = 4500L, description = "Sushi Dinner", given = true, timestamp = System.currentTimeMillis()),
                    persons = listOf(PersonWithAmount(Person(name = "Alice"), 4500L))
                )
            )

            // Received 25.00 EUR from Bob for Concert
            repository.insert(
                VouchWithPerson(
                    vouch = Vouch(amount = 2500L, description = "Concert Ticket", given = false, timestamp = System.currentTimeMillis() - 86400000L),
                    persons = listOf(PersonWithAmount(Person(name = "Bob"), 2500L))
                )
            )

            // Group: 90.00 EUR split with Charlie and Dave
            repository.insert(
                VouchWithPerson(
                    vouch = Vouch(amount = 9000L, description = "Roadtrip Fuel", given = true, timestamp = System.currentTimeMillis() - 172800000L),
                    persons = listOf(
                        PersonWithAmount(Person(name = "Charlie"), 4500L),
                        PersonWithAmount(Person(name = "Dave"), 4500L)
                    )
                )
            )
        }

        // 3. Configure Screengrab
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        // 4. Launch MainActivity and capture key screens
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()
            Thread.sleep(1000)

            // Screen 1: Main Vouches / Balance Hero Overview
            Screengrab.screenshot("01_vouches_overview")

            // Screen 2: People Overview
            val peopleLabel = context.getString(R.string.screen_people)
            composeTestRule.onNodeWithText(peopleLabel).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(600)
            Screengrab.screenshot("02_people_overview")

            // Screen 3: Settings Overview
            val settingsLabel = context.getString(R.string.screen_settings)
            composeTestRule.onNodeWithText(settingsLabel).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(600)
            Screengrab.screenshot("03_settings_overview")
        }
    }
}
