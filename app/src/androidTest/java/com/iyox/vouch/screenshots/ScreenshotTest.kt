package com.iyox.vouch.screenshots

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.iyox.vouch.MainActivity
import com.iyox.vouch.R
import com.iyox.vouch.core.backup.BackupPreview
import com.iyox.vouch.core.backup.BackupRepository
import com.iyox.vouch.core.backup.IyoxEnvelope
import com.iyox.vouch.core.data.AppDatabase
import com.iyox.vouch.core.data.VouchRepository
import com.iyox.vouch.core.utils.PreferenceKeys
import kotlinx.coroutines.runBlocking
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.util.Locale

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
        val instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        val isGerman = Locale.getDefault().language == "de"
        val currencyCode = if (isGerman) "EUR" else "USD"

        // 1. Initialize preferences: mark onboarding complete, disable biometrics, dark mode, and set currency
        val prefs = context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PreferenceKeys.KEY_ONBOARDING_COMPLETED, true)
            .putBoolean(PreferenceKeys.KEY_BIOMETRIC_ENABLED, false)
            .putString(PreferenceKeys.KEY_CURRENCY_CODE, currencyCode)
            .putInt(PreferenceKeys.KEY_THEME, UiModeManager.MODE_NIGHT_YES)
            .commit()

        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cmd uimode night yes")
        } catch (_: Exception) {
        }

        // 2. Pre-populate database from localized .iyxv backup files
        val database = AppDatabase.getDatabase(context)
        val vouchRepository = VouchRepository(database, database.vouchDao(), database.personDao())
        val backupRepository = BackupRepository(context, database, vouchRepository)

        val assetName = if (isGerman) "screenshot_vouches_ger.iyxv" else "screenshot_vouches.iyxv"
        val jsonText = instrumentationContext.assets.open(assetName).bufferedReader().use { it.readText() }

        runBlocking {
            val preview = BackupPreview(
                uri = "assets://$assetName",
                type = IyoxEnvelope.TYPE_BACKUP,
                version = IyoxEnvelope.VERSION_1_0,
                vouchCount = 4,
                personCount = 3,
                cachedPayload = jsonText
            )
            val result = backupRepository.importFromPreview(preview)
            if (result.isFailure) {
                throw result.exceptionOrNull() ?: IllegalStateException("Failed to import screenshot backup")
            }
        }

        // 3. Configure Screengrab
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

        val backDesc = context.getString(R.string.cd_back)

        // 4. Launch MainActivity and capture all 7 screens
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()
            Thread.sleep(1000)

            // Screen 1: Vouches Overview (Hero card + active vouches list)
            Screengrab.screenshot("01_vouches_overview")

            // Screen 2: Settled Space (Archive of settled vouches)
            val settledSpaceDesc = context.getString(R.string.screen_settled_space)
            composeTestRule.onNodeWithContentDescription(settledSpaceDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)
            Screengrab.screenshot("02_settled_space")

            // Navigate back to Vouches
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Screen 3: People Overview (Tab 2)
            val tabMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
            val peopleLabel = context.getString(R.string.screen_people)
            composeTestRule.onNode(tabMatcher.and(hasText(peopleLabel))).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)
            Screengrab.screenshot("03_persons_overview")

            // Screen 4: Person Detail Overview (John Doe / Max Mustermann)
            val firstPersonName = if (isGerman) "Max Mustermann" else "John Doe"
            composeTestRule.onNodeWithTag("person_card_$firstPersonName").performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)
            Screengrab.screenshot("04_person_overview")

            // Navigate back to People
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Screen 5: Add Vouch Form (Tab 1 -> FAB menu -> Add Vouch)
            val vouchesLabel = context.getString(R.string.screen_vouches)
            composeTestRule.onNode(tabMatcher.and(hasText(vouchesLabel))).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(400)

            val addVouchDesc = context.getString(R.string.action_add_vouch)
            composeTestRule.onNode(hasContentDescription(addVouchDesc).and(hasClickAction())).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(400)
            composeTestRule.onNode(hasText(addVouchDesc).and(hasClickAction())).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(600)

            // Fill form to showcase realistic creation matching add_vouch.png
            val placeholderAmount = context.getString(R.string.placeholder_amount)
            val sampleAmount = if (isGerman) "12,50" else "12.50"
            composeTestRule.onNodeWithText(placeholderAmount).performTextInput(sampleAmount)
            composeTestRule.waitForIdle()

            val receivedLabel = context.getString(R.string.label_received)
            composeTestRule.onNodeWithText(receivedLabel).performClick()
            composeTestRule.waitForIdle()

            val personLabel = context.getString(R.string.label_person)
            composeTestRule.onNodeWithText(personLabel).performTextInput(firstPersonName)
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(personLabel).performImeAction()
            composeTestRule.waitForIdle()

            val descLabel = context.getString(R.string.label_description)
            val sampleDesc = if (isGerman) "Schlaraffenland" else "Food Paradise"
            composeTestRule.onNodeWithText(descLabel).performTextInput(sampleDesc)
            composeTestRule.waitForIdle()
            Espresso.closeSoftKeyboard()
            Thread.sleep(700)
            Screengrab.screenshot("05_add_vouch")

            // Navigate back to Vouches
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Screen 6: Group Vouch Detail & Split Editing
            val groupVouchTitle = if (isGerman) "Platzbuchung" else "Court booking"
            composeTestRule.onNode(hasText(groupVouchTitle).and(hasClickAction())).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)

            val editVouchDesc = context.getString(R.string.cd_edit_vouch)
            composeTestRule.onNodeWithContentDescription(editVouchDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)

            // Advance to the participants split page matching add_group_vouch.png
            val continueLabel = context.getString(R.string.action_continue)
            composeTestRule.onNodeWithText(continueLabel).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)
            Screengrab.screenshot("06_add_group_vouch")

            // Navigate back to Vouches (step 2 -> step 1 -> VouchView -> VouchesScreen)
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(300)
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(300)
            composeTestRule.onNodeWithContentDescription(backDesc).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Screen 7: Settings Overview (Tab 3)
            val settingsLabel = context.getString(R.string.screen_settings)
            composeTestRule.onNode(tabMatcher.and(hasText(settingsLabel))).performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(700)
            Screengrab.screenshot("07_settings")
        }
    }
}
