package com.example.calculator

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthState
import com.example.auth.GoogleAuthManager
import com.example.auth.GoogleUserAccount
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import com.example.integrity.DeviceIntegrityChecker
import com.example.integrity.DeviceSecurityReport
import com.example.report.ReportManager
import com.example.report.ReportSubmitState
import com.example.report.UserReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("calculator_settings", Context.MODE_PRIVATE)
    private val repository: HistoryRepository
    private val authManager = GoogleAuthManager(application)
    private val integrityChecker = DeviceIntegrityChecker(application)

    val authState: StateFlow<AuthState> = authManager.authState

    private val _deviceSecurityReport = MutableStateFlow<DeviceSecurityReport?>(null)
    val deviceSecurityReport: StateFlow<DeviceSecurityReport?> = _deviceSecurityReport.asStateFlow()

    private val _isCheckingIntegrity = MutableStateFlow(false)
    val isCheckingIntegrity: StateFlow<Boolean> = _isCheckingIntegrity.asStateFlow()

    private val _isScreenshotBlocked = MutableStateFlow(
        prefs.getBoolean("screenshot_blocked", true)
    )
    val isScreenshotBlocked: StateFlow<Boolean> = _isScreenshotBlocked.asStateFlow()

    fun toggleScreenshotBlock() {
        val newVal = !_isScreenshotBlocked.value
        _isScreenshotBlocked.value = newVal
        prefs.edit().putBoolean("screenshot_blocked", newVal).apply()
    }

    val reportManager = ReportManager(application)
    val reportSubmitState: StateFlow<ReportSubmitState> = reportManager.submitState

    fun resetReportSubmitState() = reportManager.resetSubmitState()

    fun submitReport(category: String, title: String, description: String, email: String) {
        viewModelScope.launch {
            val report = UserReport(
                category = category,
                title = title,
                description = description,
                email = email,
                isRadMode = _isRadMode.value
            )
            reportManager.submitReport(report)
        }
    }

    fun createEmailIntent(category: String, title: String, description: String, email: String): android.content.Intent {
        val report = UserReport(
            category = category,
            title = title,
            description = description,
            email = email,
            isRadMode = _isRadMode.value
        )
        return reportManager.createEmailIntent(report)
    }

    fun getFormattedReportText(category: String, title: String, description: String, email: String): String {
        val report = UserReport(
            category = category,
            title = title,
            description = description,
            email = email,
            isRadMode = _isRadMode.value
        )
        return reportManager.getFormattedReportText(report)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = HistoryRepository(database.historyDao())
        
        // Initial silent security and integrity check
        checkDeviceIntegrity()
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

    private val _resultState = MutableStateFlow("")
    val resultState: StateFlow<String> = _resultState.asStateFlow()

    private val _isRadMode = MutableStateFlow(false) // Standard default is Deg mode on many devices, but lets support toggle easily!
    val isRadMode: StateFlow<Boolean> = _isRadMode.asStateFlow()

    private val _isInvMode = MutableStateFlow(false)
    val isInvMode: StateFlow<Boolean> = _isInvMode.asStateFlow()

    private val _isScientificExpanded = MutableStateFlow(false)
    val isScientificExpanded: StateFlow<Boolean> = _isScientificExpanded.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        // Collect input values to make live calculation previews
        viewModelScope.launch {
            _inputState.collect { currentInput ->
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

                    val parser = ExpressionParser(_isRadMode.value)
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

    fun setInput(expression: String) {
        _inputState.value = expression
    }

    fun toggleScientific() {
        _isScientificExpanded.value = !_isScientificExpanded.value
    }

    fun toggleRadMode() {
        _isRadMode.value = !_isRadMode.value
    }

    fun toggleInvMode() {
        _isInvMode.value = !_isInvMode.value
    }

    fun toggleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
    }

    fun handleKeyPress(key: String) {
        when (key) {
            "AC" -> {
                _inputState.value = ""
                _resultState.value = ""
            }
            "⌫" -> {
                val current = _inputState.value
                if (current.isEmpty()) return

                // Slick multichar deletion for scientific functions
                val functions = listOf("sin⁻¹(", "cos⁻¹(", "tan⁻¹(", "sin(", "cos(", "tan(", "log(", "ln(", "√(")
                var deleted = false
                for (f in functions) {
                    if (current.endsWith(f)) {
                        _inputState.value = current.substring(0, current.length - f.length)
                        deleted = true
                        break
                    }
                }
                if (!deleted) {
                    _inputState.value = current.substring(0, current.length - 1)
                }
            }
            "(" -> {
                val current = _inputState.value
                val lastChar = if (current.isNotEmpty()) current.last() else null
                if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == '%' || lastChar == '!')) {
                    _inputState.value += "×("
                } else {
                    _inputState.value += "("
                }
            }
            ")" -> {
                val current = _inputState.value
                val openCount = current.count { it == '(' }
                val closeCount = current.count { it == ')' }
                if (openCount > closeCount && current.isNotEmpty() && !isOperator(current.last()) && current.last() != '(') {
                    _inputState.value += ")"
                }
            }
            "( )" -> {
                val current = _inputState.value
                val openCount = current.count { it == '(' }
                val closeCount = current.count { it == ')' }
                val lastChar = if (current.isNotEmpty()) current.last() else null

                if (openCount > closeCount && lastChar != null && lastChar != '(' && !isOperator(lastChar)) {
                    _inputState.value += ")"
                } else {
                    if (lastChar != null && (lastChar.isDigit() || lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == '%' || lastChar == '!')) {
                        _inputState.value += "×("
                    } else {
                        _inputState.value += "("
                    }
                }
            }
            "=" -> {
                val currentInput = _inputState.value
                if (currentInput.isEmpty()) return
                try {
                    val parser = ExpressionParser(_isRadMode.value)
                    val rawResult = parser.evaluate(currentInput)
                    val formattedResult = formatResult(rawResult)

                    // Insert to history database
                    viewModelScope.launch {
                        try {
                            repository.insert(
                                HistoryEntry(
                                    expression = currentInput,
                                    result = formattedResult
                                )
                            )
                        } catch (t: Throwable) {
                            t.printStackTrace()
                        }
                    }

                    _inputState.value = formattedResult
                    _resultState.value = ""
                } catch (e: Throwable) {
                    _resultState.value = e.message ?: "Error"
                }
            }
            "+", "-", "×", "÷", "^" -> {
                val current = _inputState.value
                if (current.isNotEmpty() && isOperator(current.last())) {
                    _inputState.value = current.substring(0, current.length - 1) + key
                } else if (current.isNotEmpty() || key == "-") { // Allow unary minus at beginning
                    _inputState.value += key
                }
            }
            "%" -> {
                val current = _inputState.value
                if (current.isNotEmpty() && (current.last().isDigit() || current.last() == ')' || current.last() == 'π' || current.last() == 'e' || current.last() == '!')) {
                    _inputState.value += "%"
                }
            }
            "!" -> {
                val current = _inputState.value
                if (current.isNotEmpty() && (current.last().isDigit() || current.last() == ')' || current.last() == 'π' || current.last() == 'e')) {
                    _inputState.value += "!"
                }
            }
            "π", "e" -> {
                appendConstant(key)
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
                appendFunction(finalFunc)
            }
            "." -> {
                val current = _inputState.value
                var hasDecimal = false
                for (i in current.length - 1 downTo 0) {
                    val ch = current[i]
                    if (ch == '.') {
                        hasDecimal = true
                        break
                    }
                    if (!ch.isDigit()) {
                        break
                    }
                }
                if (!hasDecimal) {
                    if (current.isEmpty() || !current.last().isDigit()) {
                        _inputState.value += "0."
                    } else {
                        _inputState.value += "."
                    }
                }
            }
            else -> { // Standard digit input 0-9
                val current = _inputState.value
                val lastChar = if (current.isNotEmpty()) current.last() else null
                
                // If last char is a constant or closed parenthesis or percent/factorial and we append a digit, insert implicit multiply
                if (lastChar != null && (lastChar == 'π' || lastChar == 'e' || lastChar == ')' || lastChar == '%' || lastChar == '!')) {
                    _inputState.value += "×$key"
                } else {
                    _inputState.value += key
                }
            }
        }
    }

    private fun appendConstant(constant: String) {
        val current = _inputState.value
        val prefix = if (current.isNotEmpty() && (current.last().isDigit() || current.last() == ')' || current.last() == 'π' || current.last() == 'e' || current.last() == '%' || current.last() == '!')) {
            "×$constant"
        } else {
            constant
        }
        _inputState.value += prefix
    }

    private fun appendFunction(func: String) {
        val current = _inputState.value
        val prefix = if (current.isNotEmpty() && (current.last().isDigit() || current.last() == ')' || current.last() == 'π' || current.last() == 'e' || current.last() == '%' || current.last() == '!')) {
            "×$func"
        } else {
            func
        }
        _inputState.value += prefix
    }

    private fun isOperator(char: Char): Boolean {
        return char == '+' || char == '-' || char == '×' || char == '÷' || char == '^'
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            authManager.signInWithGoogle(activityContext)
        }
    }

    fun signInWithCustomAccount(name: String, email: String) {
        viewModelScope.launch {
            authManager.signInWithCustomGoogle(name, email)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun syncHistoryCloud() {
        viewModelScope.launch {
            authManager.updateSyncTime()
        }
    }

    fun checkDeviceIntegrity() {
        viewModelScope.launch {
            _isCheckingIntegrity.value = true
            try {
                _deviceSecurityReport.value = integrityChecker.checkIntegrity()
            } catch (_: Exception) {
            } finally {
                _isCheckingIntegrity.value = false
            }
        }
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

    private fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value > 0) "Infinity" else "-Infinity"

        // Check closeness to a whole integer representation
        val longVal = Math.round(value)
        if (abs(value - longVal) < 1e-12) {
            return longVal.toString()
        }

        // Scientific representation if too big or too small
        if (abs(value) >= 1e12 || (abs(value) < 1e-4 && value != 0.0)) {
            return String.format(java.util.Locale.US, "%.6e", value)
                .replace("0+e", "e")
                .replace(".e", "e")
        }

        val formatted = String.format(java.util.Locale.US, "%.10f", value)
        if (formatted.contains(".")) {
            return formatted.trimEnd('0').trimEnd('.')
        }
        return formatted
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}
