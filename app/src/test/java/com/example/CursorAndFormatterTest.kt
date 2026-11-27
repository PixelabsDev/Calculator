package com.example

import android.app.Application
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorViewModel
import com.example.calculator.DisplayTypographyScaler
import com.example.calculator.ExpressionFormatter
import com.example.calculator.ExpressionParser
import com.example.calculator.ThousandsOutputTransformation
import com.example.calculator.ThousandsSeparatorVisualTransformation
import com.example.ui.theme.DarkPrimaryContainer
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LightSurface
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testTypographyAutoScalingGracefullyStepsDownWithoutWrapping() {
        val portraitTiers = DisplayTypographyScaler.PORTRAIT_INPUT_SIZES_SP
        val previewTiers = DisplayTypographyScaler.PORTRAIT_PREVIEW_SIZES_SP
        val containerWidthPx = 800f

        // Short formula fits cleanly at largest 56sp tier
        val shortFont = DisplayTypographyScaler.selectFittingFontSizeSp(
            tiersSp = portraitTiers,
            maxWidthPx = containerWidthPx
        ) { sp -> 7 * sp * 0.6f } // 7 chars at 56sp -> ~235px <= 800px
        assertEquals(56f, shortFont)

        // Medium formula (~25 chars) overflows 56sp (840px) -> downscales cleanly to 44sp (660px)
        val mediumFont = DisplayTypographyScaler.selectFittingFontSizeSp(
            tiersSp = portraitTiers,
            maxWidthPx = containerWidthPx
        ) { sp -> 25 * sp * 0.6f }
        assertEquals(44f, mediumFont)

        // Longer formula (~34 chars) overflows 44sp (897px) -> downscales cleanly to 36sp (734px)
        val longFont = DisplayTypographyScaler.selectFittingFontSizeSp(
            tiersSp = portraitTiers,
            maxWidthPx = containerWidthPx
        ) { sp -> 34 * sp * 0.6f }
        assertEquals(36f, longFont)

        // Very long formula overflows 36sp -> floors cleanly at 28sp (preserving readability + scroll)
        val ultraLongFont = DisplayTypographyScaler.selectFittingFontSizeSp(
            tiersSp = portraitTiers,
            maxWidthPx = containerWidthPx
        ) { sp -> 60 * sp * 0.6f }
        assertEquals(28f, ultraLongFont)

        // Short preview starts at prominent 32sp
        val shortPreview = DisplayTypographyScaler.selectFittingFontSizeSp(
            tiersSp = previewTiers,
            maxWidthPx = containerWidthPx
        ) { sp -> 9 * sp * 0.6f }
        assertEquals(32f, shortPreview)
    }

    @Test
    fun testEdgeTapSnappingResolvesFarLeftRightWithoutFalseMiddleTrigger() {
        // Right-aligned short string "1234" occupying x = [650f .. 860f] in a 900px container
        val width = 900f
        val edgeZone = 45f
        val leftGlyph = 650f
        val rightGlyph = 860f

        // Far-left margin tap snaps to 0
        assertEquals(
            0,
            DisplayTypographyScaler.resolveEdgeTapCursorIndex(
                tapX = 15f,
                fieldWidthPx = width,
                edgeZonePx = edgeZone,
                lineLeftPx = leftGlyph,
                lineRightPx = rightGlyph,
                textLength = 4
            )
        )

        // Blank area left of short right-aligned text snaps cleanly to 0
        assertEquals(
            0,
            DisplayTypographyScaler.resolveEdgeTapCursorIndex(
                tapX = 320f,
                fieldWidthPx = width,
                edgeZonePx = edgeZone,
                lineLeftPx = leftGlyph,
                lineRightPx = rightGlyph,
                textLength = 4
            )
        )

        // Tap directly over middle digits returns null (places cursor cleanly between characters)
        assertEquals(
            null,
            DisplayTypographyScaler.resolveEdgeTapCursorIndex(
                tapX = 760f,
                fieldWidthPx = width,
                edgeZonePx = edgeZone,
                lineLeftPx = leftGlyph,
                lineRightPx = rightGlyph,
                textLength = 4
            )
        )

        // Tap on far-right edge margin snaps cleanly to text.length (4)
        assertEquals(
            4,
            DisplayTypographyScaler.resolveEdgeTapCursorIndex(
                tapX = 888f,
                fieldWidthPx = width,
                edgeZonePx = edgeZone,
                lineLeftPx = leftGlyph,
                lineRightPx = rightGlyph,
                textLength = 4
            )
        )
    }

    @Test
    fun testKeypadEventAndEdgeSnapIncrementHandleDismissalSignals() {
        val initialKeypad = viewModel.keypadEventCounter.value
        val initialDismiss = viewModel.cursorDismissSignal.value

        // Even a no-op keypad tap (e.g. Backspace when empty) triggers dismissal signal
        viewModel.handleKeyPress("⌫")
        assertEquals(initialKeypad + 1, viewModel.keypadEventCounter.value)

        viewModel.setInput("12+34")
        val afterSetDismiss = viewModel.cursorDismissSignal.value
        assertEquals(initialDismiss + 1, afterSetDismiss)

        // Moving cursor via edge tap updates selection and increments dismiss signal
        viewModel.moveCursorTo(0, dismissHandle = true)
        assertEquals(TextRange(0), viewModel.inputFieldState.value.selection)
        assertEquals(afterSetDismiss + 1, viewModel.cursorDismissSignal.value)

        // Typing any digit advances keypad counter cleanly
        viewModel.handleKeyPress("9")
        assertEquals("912+34", viewModel.inputState.value)
        assertEquals(initialKeypad + 2, viewModel.keypadEventCounter.value)
    }

    @Test
    fun testThousandsOutputTransformationMatchesFormatterPositions() {
        assertEquals(listOf(1, 6), ExpressionFormatter.findCommaInsertionPositions("1000×1000"))
        assertEquals(listOf(1), ExpressionFormatter.findCommaInsertionPositions("1234.5678"))
        assertEquals(emptyList<Int>(), ExpressionFormatter.findCommaInsertionPositions("123"))

        val state = TextFieldState(initialText = "1000×1000")
        composeTestRule.setContent {
            BasicTextField(
                state = state,
                outputTransformation = ThousandsOutputTransformation(),
                modifier = Modifier.testTag("test_output_transformation")
            )
        }

        // Underlying TextFieldState stays clean raw while rendered output contains thousands commas
        assertEquals("1000×1000", state.text.toString())
        composeTestRule.onNodeWithTag("test_output_transformation").assertTextEquals("1,000×1,000")
    }

    @Test
    fun testEdgeTapsAndSelectionCollapseOnKeypadInputInComposeUi() {
        composeTestRule.setContent {
            CalculatorAppScreen(viewModel = viewModel)
        }

        // Enter "1234" -> displays as "1,234"
        listOf("1", "2", "3", "4").forEach { key ->
            composeTestRule.onNodeWithTag("btn_$key").performClick()
        }
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("1,234")

        // Tap far-left edge -> snaps cursor to 0 without stuck handle, then typing '9' inserts at start
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).performTouchInput {
            click(Offset(x = left + 4f, y = centerY))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_9").performClick()
        assertEquals("91234", viewModel.inputState.value)
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("91,234")

        // Tap far-right edge -> snaps cursor to end, then typing '5' appends at end
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).performTouchInput {
            click(Offset(x = right - 2f, y = centerY))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_5").performClick()
        assertEquals("912345", viewModel.inputState.value)
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("912,345")

        // Select a range inside Compose TextFieldState via semantics, then tap '+6' -> replaces selection cleanly
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).performSemanticsAction(
            SemanticsActions.SetSelection
        ) { setSelection ->
            setSelection(2, 4, true)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_+").performClick()
        composeTestRule.onNodeWithTag("btn_6").performClick()
        composeTestRule.waitForIdle()

        assertEquals("91+645", viewModel.inputState.value)
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("91+645")
    }

    @Test
    fun testPostEvaluationDigitAndDecimalStartFresh() {
        // 50 + 50 = 100 -> sets isEvaluated = true
        listOf("5", "0", "+", "5", "0", "=").forEach { viewModel.handleKeyPress(it) }
        assertEquals("100", viewModel.inputState.value)
        assertTrue(viewModel.isEvaluated.value)

        // Pressing a digit immediately clears evaluated result and starts fresh ("7", not "1007")
        viewModel.handleKeyPress("7")
        assertEquals("7", viewModel.inputState.value)
        assertEquals(TextRange(1), viewModel.inputFieldState.value.selection)
        assertFalse(viewModel.isEvaluated.value)

        // Subsequent digits append normally
        viewModel.handleKeyPress("8")
        assertEquals("78", viewModel.inputState.value)

        // Decimal right after decimal evaluation (5.5 + 1.2 = 6.7) must still start fresh "0."
        viewModel.handleKeyPress("AC")
        listOf("5", ".", "5", "+", "1", ".", "2", "=").forEach { viewModel.handleKeyPress(it) }
        assertEquals("6.7", viewModel.inputState.value)
        assertTrue(viewModel.isEvaluated.value)

        viewModel.handleKeyPress(".")
        assertEquals("0.", viewModel.inputState.value)
        assertEquals(TextRange(2), viewModel.inputFieldState.value.selection)
        assertFalse(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("5")
        assertEquals("0.5", viewModel.inputState.value)
    }

    @Test
    fun testPostEvaluationOperatorsContinueFromResultAndScientificStartersReset() {
        // 50 + 50 = 100, then press × -> "100×" -> press 3 -> "100×3"
        listOf("5", "0", "+", "5", "0", "=").forEach { viewModel.handleKeyPress(it) }
        assertTrue(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("×")
        assertEquals("100×", viewModel.inputState.value)
        assertEquals(TextRange(4), viewModel.inputFieldState.value.selection)
        assertFalse(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("3")
        viewModel.handleKeyPress("=")
        assertEquals("300", viewModel.inputState.value)
        assertTrue(viewModel.isEvaluated.value)

        // Also test +, -, ÷, %, ! chaining directly after evaluation
        viewModel.handleKeyPress("+")
        assertEquals("300+", viewModel.inputState.value)
        assertFalse(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("5")
        viewModel.handleKeyPress("=")
        assertEquals("305", viewModel.inputState.value)
        assertTrue(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("%")
        assertEquals("305%", viewModel.inputState.value)
        assertFalse(viewModel.isEvaluated.value)

        // Scientific function / constant after evaluation starts fresh rather than multiplying by previous result
        viewModel.handleKeyPress("=")
        assertTrue(viewModel.isEvaluated.value)
        viewModel.handleKeyPress("√")
        assertEquals("√(", viewModel.inputState.value)
        assertFalse(viewModel.isEvaluated.value)
    }

    @Test
    fun testPostEvaluationClearBackspaceCursorEditAndComposeUiFlow() {
        // Backspace immediately after evaluation edits string and resets isEvaluated
        listOf("5", "0", "+", "5", "0", "=").forEach { viewModel.handleKeyPress(it) }
        assertEquals("100", viewModel.inputState.value)
        assertTrue(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("⌫")
        assertEquals("10", viewModel.inputState.value)
        assertFalse(viewModel.isEvaluated.value)

        viewModel.handleKeyPress("5")
        assertEquals("105", viewModel.inputState.value)

        // AC clears expression and resets isEvaluated
        viewModel.handleKeyPress("=")
        assertTrue(viewModel.isEvaluated.value)
        viewModel.handleKeyPress("AC")
        assertEquals("", viewModel.inputState.value)
        assertFalse(viewModel.isEvaluated.value)

        // Intentional cursor relocation inside an evaluated result preserves mid-number editing
        listOf("5", "0", "+", "5", "0", "=").forEach { viewModel.handleKeyPress(it) }
        assertTrue(viewModel.isEvaluated.value)
        viewModel.onInputFieldValueChange(TextFieldValue("100", selection = TextRange(1)))
        assertFalse(viewModel.isEvaluated.value)
        viewModel.handleKeyPress("5")
        assertEquals("1500", viewModel.inputState.value)

        // End-to-end Compose UI click verification: 50 + 50 = 100, then tap 7 -> "7"
        viewModel.handleKeyPress("AC")
        composeTestRule.setContent {
            CalculatorAppScreen(viewModel = viewModel)
        }
        listOf("5", "0", "+", "5", "0", "=").forEach { key ->
            composeTestRule.onNodeWithTag("btn_$key").performClick()
        }
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("100")

        composeTestRule.onNodeWithTag("btn_7").performClick()
        assertEquals("7", viewModel.inputState.value)
        composeTestRule.onNodeWithTag("calculation_input", useUnmergedTree = true).assertTextEquals("7")
    }
}
