package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ThemeMode
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayStoreScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = CalculatorViewModel(application)
    }

    private fun getFastlanePath(deviceFolder: String, fileName: String): String {
        val dir = if (File("fastlane").exists()) {
            File("fastlane/metadata/android/en-US/images/$deviceFolder")
        } else {
            File("../fastlane/metadata/android/en-US/images/$deviceFolder")
        }
        dir.mkdirs()
        return File(dir, fileName).canonicalPath
    }

    private fun populateSampleHistory() = runBlocking {
        val database: AppDatabase = AppDatabase.getDatabase(application)
        val dao = database.historyDao()
        dao.deleteAllHistory()
        dao.insertHistory(HistoryEntry(expression = "1,250 × 8.5", result = "10,625", timestamp = System.currentTimeMillis() - 60000))
        dao.insertHistory(HistoryEntry(expression = "sin(45) + cos(30)", result = "1.5731", timestamp = System.currentTimeMillis() - 120000))
        dao.insertHistory(HistoryEntry(expression = "(450 + 275) × 12", result = "8,700", timestamp = System.currentTimeMillis() - 180000))
        dao.insertHistory(HistoryEntry(expression = "√144 + 5!", result = "132", timestamp = System.currentTimeMillis() - 240000))
    }

    // =========================================================================
    // 1. PHONE SCREENSHOTS (Pixel 8 - 1080x2400 High-Res Modern Aspect Ratio)
    // =========================================================================

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
    fun capturePhone_1_StandardKeypad_DarkMode() {
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setScientificExpanded(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Standard arithmetic calculation: 1250 * 8.5
        composeTestRule.onNodeWithTag("btn_1").performClick()
        composeTestRule.onNodeWithTag("btn_2").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        composeTestRule.onNodeWithTag("btn_×").performClick()
        composeTestRule.onNodeWithTag("btn_8").performClick()
        composeTestRule.onNodeWithTag("btn_.").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()

        composeTestRule.waitForIdle()
        val path = getFastlanePath("phoneScreenshots", "1_standard_keypad.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
    fun capturePhone_2_ScientificKeypad_LightMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setScientificExpanded(true)
        viewModel.setRadMode(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Trigonometric expression with DEG/RAD toggle: sin(45) + cos(30)
        composeTestRule.onNodeWithTag("btn_sin").performClick()
        composeTestRule.onNodeWithTag("btn_4").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        val closeParen = composeTestRule.onAllNodesWithTag("btn_)")
        if (closeParen.fetchSemanticsNodes().isNotEmpty()) {
            closeParen[0].performClick()
        } else {
            composeTestRule.onNodeWithTag("btn_( )").performClick()
        }
        composeTestRule.onNodeWithTag("btn_+").performClick()
        composeTestRule.onNodeWithTag("btn_cos").performClick()
        composeTestRule.onNodeWithTag("btn_3").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        val closeParen2 = composeTestRule.onAllNodesWithTag("btn_)")
        if (closeParen2.fetchSemanticsNodes().isNotEmpty()) {
            closeParen2[0].performClick()
        } else {
            composeTestRule.onNodeWithTag("btn_( )").performClick()
        }

        composeTestRule.waitForIdle()
        val path = getFastlanePath("phoneScreenshots", "2_scientific_expanded.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
    fun capturePhone_3_CalculationHistory_DarkMode() {
        populateSampleHistory()
        viewModel.setThemeMode(ThemeMode.DARK)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Open calculation history drawer
        composeTestRule.onNodeWithTag("history_button").performClick()
        composeTestRule.waitForIdle()

        val path = getFastlanePath("phoneScreenshots", "3_calculation_history.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
    fun capturePhone_4_PrivacySettings_LightMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Open options menu showing Privacy Policy & Terms (canonical policies.pixelabs.in link)
        composeTestRule.onNodeWithTag("more_options_button").performClick()
        composeTestRule.waitForIdle()

        val path = getFastlanePath("phoneScreenshots", "4_privacy_and_settings.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    // =========================================================================
    // 2. 7-INCH TABLET SCREENSHOTS (Nexus 7 - 1200x1920 Dedicated Clean Layout)
    // =========================================================================

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Nexus7, sdk = [34])
    fun captureSevenInch_1_StandardKeypad_DarkMode() {
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setScientificExpanded(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("btn_1").performClick()
        composeTestRule.onNodeWithTag("btn_2").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        composeTestRule.onNodeWithTag("btn_×").performClick()
        composeTestRule.onNodeWithTag("btn_8").performClick()
        composeTestRule.onNodeWithTag("btn_.").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()

        composeTestRule.waitForIdle()
        val path = getFastlanePath("sevenInchScreenshots", "1_standard_keypad.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Nexus7, sdk = [34])
    fun captureSevenInch_2_ScientificKeypad_LightMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setScientificExpanded(true)
        viewModel.setRadMode(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("btn_sin").performClick()
        composeTestRule.onNodeWithTag("btn_4").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        val closeParen = composeTestRule.onAllNodesWithTag("btn_)")
        if (closeParen.fetchSemanticsNodes().isNotEmpty()) {
            closeParen[0].performClick()
        } else {
            composeTestRule.onNodeWithTag("btn_( )").performClick()
        }
        composeTestRule.onNodeWithTag("btn_+").performClick()
        composeTestRule.onNodeWithTag("btn_cos").performClick()
        composeTestRule.onNodeWithTag("btn_3").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        val closeParen2 = composeTestRule.onAllNodesWithTag("btn_)")
        if (closeParen2.fetchSemanticsNodes().isNotEmpty()) {
            closeParen2[0].performClick()
        } else {
            composeTestRule.onNodeWithTag("btn_( )").performClick()
        }

        composeTestRule.waitForIdle()
        val path = getFastlanePath("sevenInchScreenshots", "2_scientific_expanded.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Nexus7, sdk = [34])
    fun captureSevenInch_3_CalculationHistory_DarkMode() {
        populateSampleHistory()
        viewModel.setThemeMode(ThemeMode.DARK)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("history_button").performClick()
        composeTestRule.waitForIdle()

        val path = getFastlanePath("sevenInchScreenshots", "3_calculation_history.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Nexus7, sdk = [34])
    fun captureSevenInch_4_PrivacySettings_LightMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("more_options_button").performClick()
        composeTestRule.waitForIdle()

        val path = getFastlanePath("sevenInchScreenshots", "4_privacy_and_settings.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    // =========================================================================
    // 3. 10-INCH TABLET SCREENSHOTS (Pixel Tablet - 1600x2560 Large-Screen Layout)
    // =========================================================================

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.PixelTablet, sdk = [34])
    fun captureTenInch_1_StandardKeypad_DarkMode() {
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setScientificExpanded(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("btn_1").performClick()
        composeTestRule.onNodeWithTag("btn_2").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        composeTestRule.onNodeWithTag("btn_×").performClick()
        composeTestRule.onNodeWithTag("btn_8").performClick()
        composeTestRule.onNodeWithTag("btn_.").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()

        composeTestRule.waitForIdle()
        val path = getFastlanePath("tenInchScreenshots", "1_standard_keypad.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.PixelTablet, sdk = [34])
    fun captureTenInch_2_ScientificKeypad_LightMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        viewModel.setScientificExpanded(true)
        viewModel.setRadMode(true)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("btn_sin").performClick()
        composeTestRule.onNodeWithTag("btn_4").performClick()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        val closeParen = composeTestRule.onAllNodesWithTag("btn_)")
        if (closeParen.fetchSemanticsNodes().isNotEmpty()) {
            closeParen[0].performClick()
        } else {
            composeTestRule.onNodeWithTag("btn_( )").performClick()
        }
        composeTestRule.onNodeWithTag("btn_+").performClick()
        composeTestRule.onNodeWithTag("btn_cos").performClick()
        composeTestRule.onNodeWithTag("btn_3").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        val closeParen2 = composeTestRule.onAllNodesWithTag("btn_)")
        if (closeParen2.fetchSemanticsNodes().isNotEmpty()) {
            closeParen2[0].performClick()
        } else {
            composeTestRule.onNodeWithTag("btn_( )").performClick()
        }

        composeTestRule.waitForIdle()
        val path = getFastlanePath("tenInchScreenshots", "2_scientific_expanded.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.PixelTablet, sdk = [34])
    fun captureTenInch_3_CalculationHistory_DarkMode() {
        populateSampleHistory()
        viewModel.setThemeMode(ThemeMode.DARK)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("history_button").performClick()
        composeTestRule.waitForIdle()

        val path = getFastlanePath("tenInchScreenshots", "3_calculation_history.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.PixelTablet, sdk = [34])
    fun captureTenInch_4_PrivacySettings_LightMode() {
        viewModel.setThemeMode(ThemeMode.LIGHT)

        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("more_options_button").performClick()
        composeTestRule.waitForIdle()

        val path = getFastlanePath("tenInchScreenshots", "4_privacy_and_settings.png")
        composeTestRule.onRoot().captureRoboImage(filePath = path)
    }
}
