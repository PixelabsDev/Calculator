package com.example

import android.app.Application
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorViewModel
import com.example.report.EmailValidator
import com.example.report.ReportSubmitState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive End-to-End Jetpack Compose UI & Accessibility Test Suite.
 * Executes on JVM via Robolectric without requiring slow Android emulators.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class CalculatorUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = CalculatorViewModel(application)
    }

    @Test
    fun testStandardKeypadAddition() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Click '7' + '8' = 15
        composeTestRule.onNodeWithTag("btn_7").performClick()
        composeTestRule.onNodeWithTag("btn_+").performClick()
        composeTestRule.onNodeWithTag("btn_8").performClick()
        composeTestRule.onNodeWithTag("btn_=").performClick()

        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("15")
        assertEquals("15", viewModel.inputState.value)
    }

    @Test
    fun testScientificKeypadAndTrigonometry() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Expand scientific keypad if collapsed
        if (!viewModel.isScientificExpanded.value) {
            composeTestRule.onNodeWithTag("scientific_toggle").performClick()
        }

        // Clear display
        composeTestRule.onNodeWithTag("btn_AC").performClick()

        // Calculate √9 = 3
        composeTestRule.onNodeWithTag("btn_√").performClick()
        composeTestRule.onNodeWithTag("btn_9").performClick()
        composeTestRule.onNodeWithTag("btn_=").performClick()

        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("3")
        assertEquals("3", viewModel.inputState.value)
    }

    @Test
    fun testDegRadModeToggle() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Test in DEG mode: sin(90) = 1
        if (viewModel.isRadMode.value) {
            viewModel.toggleRadMode()
        }

        viewModel.handleKeyPress("AC")
        viewModel.handleKeyPress("sin")
        viewModel.handleKeyPress("9")
        viewModel.handleKeyPress("0")
        viewModel.handleKeyPress("=")

        assertEquals("1", viewModel.inputState.value)

        // Switch to RAD mode: sin(0) = 0
        viewModel.toggleRadMode()
        assertTrue(viewModel.isRadMode.value)

        viewModel.handleKeyPress("AC")
        viewModel.handleKeyPress("sin")
        viewModel.handleKeyPress("0")
        viewModel.handleKeyPress("=")

        assertEquals("0", viewModel.inputState.value)
    }

    @Test
    fun testClearAndBackspaceInteractions() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Enter 123
        composeTestRule.onNodeWithTag("btn_1").performClick()
        composeTestRule.onNodeWithTag("btn_2").performClick()
        composeTestRule.onNodeWithTag("btn_3").performClick()
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("123")

        // Backspace once -> 12
        composeTestRule.onNodeWithTag("btn_backspace").performClick()
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("12")

        // AC -> empty (renders "0" when input is empty)
        composeTestRule.onNodeWithTag("btn_AC").performClick()
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("0")
    }

    @Test
    fun testDivisionByZeroGracefulError() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("btn_AC").performClick()
        composeTestRule.onNodeWithTag("btn_9").performClick()
        composeTestRule.onNodeWithTag("btn_÷").performClick()
        composeTestRule.onNodeWithTag("btn_0").performClick()
        composeTestRule.onNodeWithTag("btn_=").performClick()

        // Verify the app did not crash and displays a division by zero error message
        composeTestRule.onNodeWithTag("calculation_preview", useUnmergedTree = true).assertTextContains("Division by zero")
    }

    @Test
    fun testAccessibilityAuditForScreenReaders() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Verify Backspace has non-empty TalkBack description
        composeTestRule.onNodeWithTag("btn_backspace")
            .assert(hasContentDescription("Backspace"))

        // Verify History icon button has non-empty TalkBack description
        composeTestRule.onNodeWithTag("history_button")
            .assert(hasContentDescription("Calculation History"))

        // Verify Clear button has non-empty TalkBack description
        composeTestRule.onNodeWithTag("btn_AC")
            .assert(hasContentDescription("Clear all"))

        // Verify More Options menu has non-empty TalkBack description
        composeTestRule.onNodeWithTag("more_options_button")
            .assert(hasContentDescription("More Options"))
    }

    @Test
    fun testPrivacyMenuExternalCanonicalLink() {
        composeTestRule.setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }

        // Open 3-dot menu and verify Privacy Policy & Terms item exists
        composeTestRule.onNodeWithTag("more_options_button").performClick()
        composeTestRule.onNodeWithTag("menu_privacy_terms").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_privacy_terms").performClick()
    }

    @Test
    fun testFeedbackOfflineGuard() {
        // Verify initial state is Idle
        assertEquals(ReportSubmitState.Idle, viewModel.reportSubmitState.value)

        // Verify state reset functionality
        viewModel.resetReportSubmitState()
        assertEquals(ReportSubmitState.Idle, viewModel.reportSubmitState.value)

        // Verify fallback email intent creation for offline support
        val emailIntent = viewModel.createEmailIntent(
            category = "Feedback",
            title = "Offline Test",
            description = "Testing offline handling",
            email = "tester@pixelabs.in",
            includeDiagnostics = false
        )
        assertNotNull(emailIntent)
        assertEquals(android.content.Intent.ACTION_SENDTO, emailIntent.action)
    }

    @Test
    fun testFeedbackEmailValidation() {
        // 1. Valid custom domain emails are accepted across diverse providers
        assertTrue(EmailValidator.isValid("user@custom.xyz"))
        assertTrue(EmailValidator.isValid("admin@pixelabs.in"))
        assertTrue(EmailValidator.isValid("dev@company.org"))
        assertTrue(EmailValidator.isValid("test.user+tag@domain.co.uk"))

        // 2. Malformed emails and plain text are strictly rejected
        assertFalse(EmailValidator.isValid("plain-text-no-at"))
        assertFalse(EmailValidator.isValid("missing-tld@domain"))
        assertFalse(EmailValidator.isValid("user@.com"))
        assertFalse(EmailValidator.isValid("@domain.com"))
        assertFalse(EmailValidator.isValid("user@domain."))

        // 3. Optional field allows blank and whitespace
        assertTrue(EmailValidator.isValid(""))
        assertTrue(EmailValidator.isValid(null))
        assertTrue(EmailValidator.isValid("   "))

        // 4. Verify ViewModel email intent generation with valid custom domain email
        val intent = viewModel.createEmailIntent(
            category = "Feedback",
            title = "Validation Test",
            description = "Valid email test",
            email = "tester@custom.xyz",
            includeDiagnostics = false
        )
        assertNotNull(intent)
        assertEquals(android.content.Intent.ACTION_SENDTO, intent.action)
    }
}
