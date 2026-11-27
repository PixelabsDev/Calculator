package com.example

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ExpressionFormatter
import com.example.calculator.ExpressionParser
import com.example.calculator.ThousandsSeparatorVisualTransformation
import com.example.ui.theme.DarkPrimaryContainer
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightSurface
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies Google Calculator-style cursor editing, thousands comma display formatting,
 * OffsetMapping invariants, and Material You dynamic/fallback theming without regressions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class CursorAndFormatterTest {

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
    fun testThousandsCommaDisplayFormattingAndOffsetInvariants() {
        val cases = listOf(
            "" to "",
            "0" to "0",
            "123" to "123",
            "1000" to "1,000",
            "1000×1000" to "1,000×1,000",
            "1234567.8901+5000" to "1,234,567.8901+5,000",
            "sin⁻¹(12500)+√(1000000)" to "sin⁻¹(12,500)+√(1,000,000)",
            "1.234567" to "1.234567"
        )

        val transformation = ThousandsSeparatorVisualTransformation()
        for ((raw, expected) in cases) {
            val transformed = transformation.filter(AnnotatedString(raw))
            assertEquals("Formatted text mismatch for '$raw'", expected, transformed.text.text)

            val mapping = transformed.offsetMapping
            assertEquals(0, mapping.originalToTransformed(0))
            assertEquals(expected.length, mapping.originalToTransformed(raw.length))
            assertEquals(0, mapping.transformedToOriginal(0))
            assertEquals(raw.length, mapping.transformedToOriginal(expected.length))

            var prevTrans = 0
            for (i in 0..raw.length) {
                val mapped = mapping.originalToTransformed(i)
                assertTrue("origToTrans must be in bounds", mapped in 0..expected.length)
                assertTrue("origToTrans must be monotonic", mapped >= prevTrans)
                prevTrans = mapped
            }

            var prevOrig = 0
            for (j in 0..expected.length) {
                val mapped = mapping.transformedToOriginal(j)
                assertTrue("transToOrig must be in bounds", mapped in 0..raw.length)
                assertTrue("transToOrig must be monotonic", mapped >= prevOrig)
                prevOrig = mapped
            }
        }
    }

    @Test
    fun testPreviewFormattingPreservesErrorsAndFormatsNumbers() {
        assertEquals("1,000,000", ExpressionFormatter.formatResultForDisplay("1000000"))
        assertEquals("12,345.6789", ExpressionFormatter.formatResultForDisplay("12345.6789"))
        assertEquals("Division by zero", ExpressionFormatter.formatResultForDisplay("Division by zero"))
        assertEquals("tan(90°) is undefined", ExpressionFormatter.formatResultForDisplay("tan(90°) is undefined"))
        assertEquals("1e12", ExpressionFormatter.formatResultForDisplay("1e12"))
    }

    @Test
    fun testParserHandlesFormattedCommaStrings() {
        val parser = ExpressionParser(isRadMode = false)
        assertEquals(1_000_000.0, parser.evaluate("1,000 × 1,000"), 1e-9)
        assertEquals(10_625.0, parser.evaluate("1,250 × 8.5"), 1e-9)
    }

    @Test
    fun testViewModelCursorInsertionAndDeletionAtArbitraryIndex() {
        // Enter 12+34
        listOf("1", "2", "+", "3", "4").forEach { viewModel.handleKeyPress(it) }
        assertEquals("12+34", viewModel.inputState.value)
        assertEquals(TextRange(5), viewModel.inputFieldState.value.selection)

        // Move cursor between '12' and '+' (index 2), then insert '9' -> "129+34"
        viewModel.onInputFieldValueChange(TextFieldValue("12+34", selection = TextRange(2)))
        viewModel.handleKeyPress("9")
        assertEquals("129+34", viewModel.inputState.value)
        assertEquals(TextRange(3), viewModel.inputFieldState.value.selection)

        // Backspace at cursor index 3 removes '9' -> "12+34"
        viewModel.handleKeyPress("⌫")
        assertEquals("12+34", viewModel.inputState.value)
        assertEquals(TextRange(2), viewModel.inputFieldState.value.selection)

        // Backspace at start index 0 is a safe no-op
        viewModel.onInputFieldValueChange(TextFieldValue("12+34", selection = TextRange(0)))
        viewModel.handleKeyPress("⌫")
        assertEquals("12+34", viewModel.inputState.value)
        assertEquals(TextRange(0), viewModel.inputFieldState.value.selection)
    }

    @Test
    fun testViewModelSelectionReplacementAndDecimalGuard() {
        viewModel.setInput("12.34")
        // Attempt inserting a second decimal before existing '.' at index 1 ("1|2.34") -> ignored
        viewModel.onInputFieldValueChange(TextFieldValue("12.34", selection = TextRange(1)))
        viewModel.handleKeyPress(".")
        assertEquals("12.34", viewModel.inputState.value)

        // Select "2.3" [1, 4) and replace with "9" -> "194"
        viewModel.onInputFieldValueChange(TextFieldValue("12.34", selection = TextRange(1, 4)))
        viewModel.handleKeyPress("9")
        assertEquals("194", viewModel.inputState.value)
        assertEquals(TextRange(2), viewModel.inputFieldState.value.selection)
    }

    @Test
    fun testFunctionTokenAtomicCursorSnapAndDeletion() {
        viewModel.setInput("2+sin(30)")
        // Tap inside "sin(" at index 3 -> snaps to token boundary index 2 (or 6 if closer)
        viewModel.onInputFieldValueChange(TextFieldValue("2+sin(30)", selection = TextRange(3)))
        assertEquals(TextRange(2), viewModel.inputFieldState.value.selection)

        // Tap near end of "sin(" at index 5 -> snaps to after '(' at index 6
        viewModel.onInputFieldValueChange(TextFieldValue("2+sin(30)", selection = TextRange(5)))
        assertEquals(TextRange(6), viewModel.inputFieldState.value.selection)

        // Backspace at index 6 deletes entire "sin(" token cleanly
        viewModel.handleKeyPress("⌫")
        assertEquals("2+30)", viewModel.inputState.value)
        assertEquals(TextRange(2), viewModel.inputFieldState.value.selection)
    }

    @Test
    fun testFallbackDarkAndLightPalettesWithoutDynamicColor() {
        var resolvedDarkSurface = DarkSurface
        var resolvedDarkPrimaryContainer = DarkPrimaryContainer
        var resolvedLightSurface = LightSurface

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                resolvedDarkSurface = MaterialTheme.colorScheme.surface
                resolvedDarkPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
            }
            MyApplicationTheme(darkTheme = false, dynamicColor = false) {
                resolvedLightSurface = MaterialTheme.colorScheme.surface
            }
        }

        assertEquals(DarkSurface, resolvedDarkSurface)
        assertEquals(DarkPrimaryContainer, resolvedDarkPrimaryContainer)
        assertEquals(LightSurface, resolvedLightSurface)
    }

    @Test
    fun testInteractiveDisplayRendersAndEvaluatesInCompose() {
        composeTestRule.setContent {
            CalculatorAppScreen(viewModel = viewModel)
        }

        listOf("1", "0", "0", "0", "×", "1", "0", "0", "0").forEach { key ->
            composeTestRule.onNodeWithTag("btn_$key").performClick()
        }

        // Underlying raw expression stays clean while display & preview render thousands commas
        assertEquals("1000×1000", viewModel.inputState.value)
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("1,000×1,000")
        composeTestRule.onNodeWithTag("calculation_preview", useUnmergedTree = true).assertTextEquals("1,000,000")

        composeTestRule.onNodeWithTag("btn_=").performClick()
        assertEquals("1000000", viewModel.inputState.value)
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("1,000,000")
    }
}
