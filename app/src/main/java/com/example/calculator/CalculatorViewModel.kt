package com.example.calculator

import android.app.Application
import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import com.example.report.ReportManager
import com.example.report.ReportSubmitState
import com.example.report.UserReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PREF_KEY_BLOCK_SCREENSHOTS = "block_screenshots_v2"
        private const val LEGACY_PREF_KEY_BLOCK_SCREENSHOTS = "screenshot_blocked"

        // Multi-character atomic tokens (ordered longest-first for unambiguous matching)
        private val FUNCTION_TOKENS = listOf(
            "sin⁻¹(", "cos⁻¹(", "tan⁻¹(",
            "sin(", "cos(", "tan(",
            "log(", "ln(", "√("
        )

        private val POSTFIX_MULTIPLY_TRIGGERS = setOf('π', 'e', ')', '%', '!')
    }

    private val prefs = application.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)
    private val repository: HistoryRepository
    private val _isScreenshotBlocked = MutableStateFlow(
        prefs.getBoolean(PREF_KEY_BLOCK_SCREENSHOTS, false)
    )
    val isScreenshotBlocked: StateFlow<Boolean> = _isScreenshotBlocked.asStateFlow()

    fun toggleScreenshotBlock() {
        val newVal = !_isScreenshotBlocked.value
        _isScreenshotBlocked.value = newVal
        prefs.edit().putBoolean(PREF_KEY_BLOCK_SCREENSHOTS, newVal).apply()
    }

    val reportManager = ReportManager(application)

    private val _reportSubmitState = MutableStateFlow<ReportSubmitState>(ReportSubmitState.Idle)
    val reportSubmitState: StateFlow<ReportSubmitState> = _reportSubmitState.asStateFlow()

    fun submitReportToSheet(
        category: String,
        title: String,
        description: String,
        email: String,
        includeDiagnostics: Boolean
    ) {
        viewModelScope.launch {
            _reportSubmitState.value = ReportSubmitState.Submitting
            val report = UserReport(
                category = category,
                title = title,
                description = description,
                email = email,
                includeDiagnostics = includeDiagnostics,
                isRadMode = _isRadMode.value
            )
            val result = reportManager.submitReportToSheet(report)
            _reportSubmitState.value = if (result.isSuccess) {
                ReportSubmitState.Success("Thank you for your feedback! Your response has been submitted successfully.")
            } else {
                ReportSubmitState.Error(result.exceptionOrNull()?.localizedMessage ?: "Failed to submit. Please check your internet connection or use Email.")
            }
        }
    }

    fun resetReportSubmitState() {
        _reportSubmitState.value = ReportSubmitState.Idle
    }

    fun createEmailIntent(category: String, title: String, description: String, email: String, includeDiagnostics: Boolean): android.content.Intent {
        val report = UserReport(
            category = category,
            title = title,
            description = description,
            email = email,
            includeDiagnostics = includeDiagnostics,
            isRadMode = _isRadMode.value
        )
        return reportManager.createEmailIntent(report)
    }

    fun getFormattedReportText(category: String, title: String, description: String, email: String, includeDiagnostics: Boolean): String {
        val report = UserReport(
            category = category,
            title = title,
            description = description,
            email = email,
            includeDiagnostics = includeDiagnostics,
            isRadMode = _isRadMode.value
        )
        return reportManager.getFormattedReportText(report)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())

        // Purge deprecated legacy preference key from previous versions so existing devices default to OFF
        if (prefs.contains(LEGACY_PREF_KEY_BLOCK_SCREENSHOTS)) {
            prefs.edit().remove(LEGACY_PREF_KEY_BLOCK_SCREENSHOTS).apply()
        }
    }

    // List of calculation histories
    val historyState: StateFlow<List<HistoryEntry>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _inputState = MutableStateFlow("")
    val inputState: StateFlow<String> = _inputState.asStateFlow()

    private val _inputFieldState = MutableStateFlow(TextFieldValue(""))
    val inputFieldState: StateFlow<TextFieldValue> = _inputFieldState.asStateFlow()

    private val _keypadEventCounter = MutableStateFlow(0)
    val keypadEventCounter: StateFlow<Int> = _keypadEventCounter.asStateFlow()

    private val _cursorDismissSignal = MutableStateFlow(0)
    val cursorDismissSignal: StateFlow<Int> = _cursorDismissSignal.asStateFlow()

    private val _resultState = MutableStateFlow("")
    val resultState: StateFlow<String> = _resultState.asStateFlow()

    private val _isRadMode = MutableStateFlow(false) // Default: Deg mode
    val isRadMode: StateFlow<Boolean> = _isRadMode.asStateFlow()

    private val _isInvMode = MutableStateFlow(false)
    val isInvMode: StateFlow<Boolean> = _isInvMode.asStateFlow()

    private val _isScientificExpanded = MutableStateFlow(false)
    val isScientificExpanded: StateFlow<Boolean> = _isScientificExpanded.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        // Collect both input values and angle mode to provide live, reactive calculation previews
        viewModelScope.launch {
            combine(_inputState, _isRadMode) { currentInput, isRad ->
                Pair(currentInput, isRad)
            }.collect { (currentInput, isRad) ->
                if (currentInput.isEmpty()) {
                    _resultState.value = ""
                    return@collect
                }
                try {
                    // Support live preview for open parentheses, auto-balance for partial strings
                    var parseInput = currentInput
                    val openParens = parseInput.count { it == '(' }
                    val closeParens = parseInput.count { it == ')' }
                    if (openParens > closeParens) {
                        parseInput += ")".repeat(openParens - closeParens)
                    }

                    // Avoid showing a preview if the input string is just a single number or constant
                    val doubleVal = parseInput.toDoubleOrNull()
                    val isSingleConstant = parseInput == "π" || parseInput == "e"
                    if (doubleVal != null || isSingleConstant) {
                        _resultState.value = ""
                        return@collect
                    }

                    val parser = ExpressionParser(isRad)
                    val rawResult = parser.evaluate(parseInput)
                    _resultState.value = formatResult(rawResult)
                } catch (e: Throwable) {
                    if (e is ArithmeticException) {
                        _resultState.value = e.message ?: "Arithmetic Error"
                    } else {
                        // Keep resultState empty for incomplete typed states
                        _resultState.value = ""
                    }
                }
            }
        }
    }

    private fun updateInputField(newText: String, newCursor: Int = newText.length) {
        val clamped = newCursor.coerceIn(0, newText.length)
        val snappedCursor = snapCursorOutOfFunctionTokens(newText, clamped)
        _inputFieldState.value = TextFieldValue(
            text = newText,
            selection = TextRange(snappedCursor)
        )
        _inputState.value = newText
    }

    /**
     * Prevents placing the cursor inside multi-character function tokens like "sin(" or "cos⁻¹(",
     * keeping function tokens atomic just like Google Calculator.
     */
    private fun snapCursorOutOfFunctionTokens(text: String, rawCursor: Int): Int {
        val cursor = rawCursor.coerceIn(0, text.length)
        var i = 0
        while (i < text.length) {
            val matched = FUNCTION_TOKENS.firstOrNull { token ->
                text.startsWith(token, startIndex = i)
            }
            if (matched != null) {
                val end = i + matched.length
                if (cursor in (i + 1) until end) {
                    return if (cursor - i < end - cursor) i else end
                }
                i = end
            } else {
                i++
            }
        }
        return cursor
    }

    /**
     * Handles interactive cursor movement, text selection, or physical keyboard input
     * from the Compose expression [androidx.compose.foundation.text.BasicTextField].
     */
    fun onInputFieldValueChange(newValue: TextFieldValue) {
        val current = _inputFieldState.value
        if (newValue.text == current.text) {
            val snappedStart = snapCursorOutOfFunctionTokens(newValue.text, newValue.selection.start)
            val snappedEnd = snapCursorOutOfFunctionTokens(newValue.text, newValue.selection.end)
            val updated = newValue.copy(selection = TextRange(snappedStart, snappedEnd))
            _inputFieldState.value = updated
            return
        }

        // Physical keyboard / paste edits: normalize operators and strip thousands commas
        val cleaned = newValue.text
            .replace(",", "")
            .replace("*", "×")
            .replace("/", "÷")
        val cursor = newValue.selection.end.coerceIn(0, cleaned.length)
        updateInputField(cleaned, cursor)
    }

    fun setInput(expression: String) {
        val cleaned = expression.replace(",", "")
        updateInputField(cleaned, cleaned.length)
        _cursorDismissSignal.value += 1
    }

    /**
     * Moves the cursor explicitly (e.g. far-left / far-right edge tap snapping)
     * and optionally requests dismissal of any active teardrop drag handle.
     */
    fun moveCursorTo(rawIndex: Int, dismissHandle: Boolean = true) {
        val current = _inputFieldState.value
        val clamped = rawIndex.coerceIn(0, current.text.length)
        val snapped = snapCursorOutOfFunctionTokens(current.text, clamped)
        if (current.selection != TextRange(snapped)) {
            _inputFieldState.value = current.copy(selection = TextRange(snapped))
        }
        if (dismissHandle) {
            _cursorDismissSignal.value += 1
        }
    }

    fun toggleScientific() {
        _isScientificExpanded.value = !_isScientificExpanded.value
    }

    fun setScientificExpanded(expanded: Boolean) {
        _isScientificExpanded.value = expanded
    }

    fun toggleRadMode() {
        _keypadEventCounter.value += 1
        _isRadMode.value = !_isRadMode.value
    }

    fun setRadMode(isRad: Boolean) {
        _isRadMode.value = isRad
    }

    fun toggleInvMode() {
        _keypadEventCounter.value += 1
        _isInvMode.value = !_isInvMode.value
    }

    fun toggleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun handleKeyPress(key: String) {
        _keypadEventCounter.value += 1
        val field = _inputFieldState.value
        val current = field.text
        val selStart = min(field.selection.start, field.selection.end).coerceIn(0, current.length)
        val selEnd = max(field.selection.start, field.selection.end).coerceIn(0, current.length)
        val hasSelection = selStart != selEnd

        when (key) {
            "AC" -> {
                updateInputField("", 0)
                _resultState.value = ""
                return
            }
            "⌫" -> {
                if (hasSelection) {
                    val newText = current.removeRange(selStart, selEnd)
                    updateInputField(newText, selStart)
                    return
                }
                if (selStart == 0) return

                val before = current.substring(0, selStart)
                val after = current.substring(selStart)

                // Slick multichar deletion for scientific functions left of cursor
                for (f in FUNCTION_TOKENS) {
                    if (before.endsWith(f)) {
                        val trimmedBefore = before.substring(0, before.length - f.length)
                        updateInputField(trimmedBefore + after, trimmedBefore.length)
                        return
                    }
                }
                val trimmedBefore = before.substring(0, before.length - 1)
                updateInputField(trimmedBefore + after, trimmedBefore.length)
                return
            }
            "=" -> {
                if (current.isEmpty()) return

                var evalInput = current
                val openCount = evalInput.count { it == '(' }
                val closeCount = evalInput.count { it == ')' }
                if (openCount > closeCount) {
                    evalInput += ")".repeat(openCount - closeCount)
                }

                try {
                    val parser = ExpressionParser(_isRadMode.value)
                    val rawResult = parser.evaluate(evalInput)
                    val formattedResult = formatResult(rawResult)

                    val isTrivial = current.toDoubleOrNull() != null || current == formattedResult
                    if (!isTrivial) {
                        viewModelScope.launch {
                            try {
                                repository.insert(
                                    HistoryEntry(
                                        expression = evalInput,
                                        result = formattedResult
                                    )
                                )
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
                        }
                    }

                    updateInputField(formattedResult, formattedResult.length)
                    _resultState.value = ""
                } catch (e: Throwable) {
                    _resultState.value = e.message ?: "Error"
                }
                return
            }
        }

        // For all insertion keys: replace any selected range first, then insert relative to (before | after)
        val before = current.substring(0, selStart)
        val after = current.substring(selEnd)
        val lastBefore = before.lastOrNull()

        when (key) {
            "(" -> {
                val insert = if (lastBefore != null && lastBefore in POSTFIX_MULTIPLY_TRIGGERS) "×(" else "("
                updateInputField(before + insert + after, before.length + insert.length)
            }
            ")" -> {
                val openCount = before.count { it == '(' }
                val closeCount = before.count { it == ')' }
                if (openCount > closeCount && before.isNotEmpty() && !isOperator(lastBefore!!) && lastBefore != '(') {
                    updateInputField("$before)$after", before.length + 1)
                } else if (hasSelection) {
                    updateInputField(before + after, before.length)
                }
            }
            "( )" -> {
                val openCount = before.count { it == '(' }
                val closeCount = before.count { it == ')' }
                if (openCount > closeCount && lastBefore != null && lastBefore != '(' && !isOperator(lastBefore)) {
                    updateInputField("$before)$after", before.length + 1)
                } else {
                    val insert = if (lastBefore != null && lastBefore in POSTFIX_MULTIPLY_TRIGGERS) "×(" else "("
                    updateInputField(before + insert + after, before.length + insert.length)
                }
            }
            "+", "-", "×", "÷", "^" -> {
                if (before.isEmpty()) {
                    if (key == "-" && !after.startsWith("-")) {
                        updateInputField("-$after", 1)
                    } else if (hasSelection) {
                        updateInputField(after, 0)
                    }
                    return
                }

                // Allow unary minus following multiply, divide, power, or open parenthesis
                if (key == "-" && (lastBefore == '×' || lastBefore == '÷' || lastBefore == '^' || lastBefore == '(')) {
                    updateInputField(before + key + after, before.length + 1)
                    return
                }

                if (lastBefore != null && isOperator(lastBefore)) {
                    val newBefore = if (before.length >= 2 && isOperator(before[before.length - 2])) {
                        before.substring(0, before.length - 2) + key
                    } else {
                        before.substring(0, before.length - 1) + key
                    }
                    updateInputField(newBefore + after, newBefore.length)
                } else if (lastBefore != '(') {
                    updateInputField(before + key + after, before.length + 1)
                } else if (hasSelection) {
                    updateInputField(before + after, before.length)
                }
            }
            "%" -> {
                if (lastBefore != null && (lastBefore.isDigit() || lastBefore == ')' || lastBefore == 'π' || lastBefore == 'e' || lastBefore == '!')) {
                    updateInputField("$before%$after", before.length + 1)
                } else if (hasSelection) {
                    updateInputField(before + after, before.length)
                }
            }
            "!" -> {
                if (lastBefore != null && (lastBefore.isDigit() || lastBefore == ')' || lastBefore == 'π' || lastBefore == 'e')) {
                    updateInputField("$before!$after", before.length + 1)
                } else if (hasSelection) {
                    updateInputField(before + after, before.length)
                }
            }
            "π", "e" -> {
                insertWithImplicitMultiply(before, after, key)
            }
            "sin", "cos", "tan", "ln", "log", "√" -> {
                val finalFunc = when (key) {
                    "sin" -> if (_isInvMode.value) "sin⁻¹(" else "sin("
                    "cos" -> if (_isInvMode.value) "cos⁻¹(" else "cos("
                    "tan" -> if (_isInvMode.value) "tan⁻¹(" else "tan("
                    "ln" -> "ln("
                    "log" -> "log("
                    "√" -> "√("
                    else -> key
                }
                insertWithImplicitMultiply(before, after, finalFunc)
            }
            "." -> {
                if (lastBefore in POSTFIX_MULTIPLY_TRIGGERS) {
                    val insert = "×0."
                    updateInputField(before + insert + after, before.length + insert.length)
                    return
                }

                if (numberTokenAtCursorHasDecimal(before, after)) {
                    if (hasSelection) {
                        updateInputField(before + after, before.length)
                    }
                    return
                }

                val insert = if (before.isEmpty() || !before.last().isDigit()) "0." else "."
                updateInputField(before + insert + after, before.length + insert.length)
            }
            else -> { // Standard digit input 0-9
                val insert = if (lastBefore != null && lastBefore in POSTFIX_MULTIPLY_TRIGGERS) {
                    "×$key"
                } else {
                    key
                }
                updateInputField(before + insert + after, before.length + insert.length)
            }
        }
    }

    private fun insertWithImplicitMultiply(before: String, after: String, token: String) {
        val lastBefore = before.lastOrNull()
        val insert = if (lastBefore != null && (lastBefore.isDigit() || lastBefore in POSTFIX_MULTIPLY_TRIGGERS)) {
            "×$token"
        } else {
            token
        }
        updateInputField(before + insert + after, before.length + insert.length)
    }

    private fun numberTokenAtCursorHasDecimal(before: String, after: String): Boolean {
        for (i in before.length - 1 downTo 0) {
            val ch = before[i]
            if (ch == '.') return true
            if (!ch.isDigit()) break
        }
        for (i in after.indices) {
            val ch = after[i]
            if (ch == '.') return true
            if (!ch.isDigit()) break
        }
        return false
    }

    private fun isOperator(char: Char): Boolean {
        return char == '+' || char == '-' || char == '×' || char == '÷' || char == '^'
    }

    fun clearHistory() {
        viewModelScope.launch {
            try {
                repository.clearAll()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteById(id)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    /**
     * Formats calculation results with high mathematical precision.
     * Prevents Long.MAX_VALUE saturation, floating-point noise, and awkward trailing zeros.
     */
    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"
        if (abs(value) < 1e-15) return "0"

        // Scientific notation for very large or very small non-zero numbers
        if (abs(value) >= 1e12 || (abs(value) < 1e-4 && value != 0.0)) {
            val sci = String.format(Locale.US, "%.8e", value)
            val parts = sci.split("e")
            val mantissa = parts[0].trimEnd('0').trimEnd('.')
            val exp = parts[1].replace("+", "").toInt()
            return "${mantissa}e$exp"
        }

        // Check closeness to a whole integer representation in safe range
        if (abs(value) < 1e12) {
            val rounded = Math.round(value)
            if (abs(value - rounded.toDouble()) < 1e-11) {
                return rounded.toString()
            }
        }

        // Clean high-precision decimal representation
        return try {
            val bd = BigDecimal(value, MathContext(12, RoundingMode.HALF_UP))
            val plain = bd.stripTrailingZeros().toPlainString()
            plain
        } catch (_: Exception) {
            val formatted = String.format(Locale.US, "%.10f", value)
            if (formatted.contains(".")) formatted.trimEnd('0').trimEnd('.') else formatted
        }
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
