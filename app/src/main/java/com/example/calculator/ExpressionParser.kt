package com.example.calculator

import kotlin.math.*

/**
 * High-performance, robust mathematical expression parser for both basic and scientific operations.
 * Supports:
 * - Basic arithmetic (+, -, ×, ÷)
 * - Exponentiation (^), percentage (%), factorial (!)
 * - Constants (π, e)
 * - Trigonometric & inverse functions (sin, cos, tan, sin⁻¹/asin, cos⁻¹/acos, tan⁻¹/atan)
 * - Natural & common logarithms (ln, log)
 * - Square root (√, sqrt)
 * - Implicit multiplication (e.g. 2π, 2(3), (2)(3), 2sin(30), 5!2)
 * - Angle mode selection (Degrees vs Radians)
 * - Full domain validation for edge cases (division by zero, tan(90°), negative roots, etc.)
 */
class ExpressionParser(private val isRadMode: Boolean) {

    private var pos = -1
    private var ch: Int = -1
    private var str: String = ""

    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0

        // Normalize expression operators and function names
        str = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace(" ", "")

        // Auto-balance missing trailing parentheses if user left them open
        val openCount = str.count { it == '(' }
        val closeCount = str.count { it == ')' }
        if (openCount > closeCount) {
            str += ")".repeat(openCount - closeCount)
        }

        pos = -1
        ch = -1
        nextChar()

        val result = parseExpression()

        if (pos < str.length) {
            throw IllegalArgumentException("Unexpected character inside expression: '${str[pos]}'")
        }

        if (result.isNaN()) {
            throw ArithmeticException("Calculation resulted in an undefined value (NaN)")
        }

        return result
    }

    private fun nextChar() {
        pos++
        ch = if (pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    /**
     * Checks if the next character indicates the start of a factor for implicit multiplication.
     * Factors can start with: '(', digit, '.', 'π', 'e', '√', or an alphabetic function name.
     */
    private fun isNextFactorStart(): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == -1) return false
        val c = ch.toChar()
        if (c == '(' || c.isDigit() || c == '.' || c == 'π' || c == 'e' || c == '√') {
            return true
        }
        if (c in 'a'..'z') {
            return true
        }
        return false
    }

    // Grammar rule: Expression -> Term ( ('+' | '-') Term )*
    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            when {
                eat('+'.code) -> x += parseTerm()
                eat('-'.code) -> x -= parseTerm()
                else -> return x
            }
        }
    }

    // Grammar rule: Term -> Factor ( ('*' | '/') Factor | [implicit] Factor )*
    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            when {
                eat('*'.code) -> x *= parseFactor()
                eat('/'.code) -> {
                    val divisor = parseFactor()
                    if (abs(divisor) == 0.0) {
                        throw ArithmeticException("Division by zero")
                    }
                    x /= divisor
                }
                isNextFactorStart() -> {
                    // Implicit multiplication (e.g. 2π, 2(3), (2)(3), 2sin(30), etc.)
                    x *= parseFactor()
                }
                else -> return x
            }
        }
    }

    // Grammar rule: Factor -> ('+' | '-')? ( Number | Constant | '(' Expression ')' | Function '(' Expression ')' ) PostOps*
    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor() // Unary plus
        if (eat('-'.code)) return -parseFactor() // Unary minus

        var x: Double
        val startPos = this.pos

        when {
            eat('('.code) -> {
                x = parseExpression()
                eat(')'.code)
                x = parsePercentageAndFactorialAndPower(x)
            }
            ch == 'π'.code -> {
                eat('π'.code)
                x = Math.PI
                x = parsePercentageAndFactorialAndPower(x)
            }
            ch == 'e'.code -> {
                eat('e'.code)
                x = Math.E
                x = parsePercentageAndFactorialAndPower(x)
            }
            ch == '√'.code -> {
                eat('√'.code)
                val arg = if (eat('('.code)) {
                    val v = parseExpression()
                    eat(')'.code)
                    v
                } else {
                    parseFactor()
                }
                if (arg < 0.0) {
                    throw ArithmeticException("Cannot calculate square root of a negative number")
                }
                x = sqrt(arg)
                x = parsePercentageAndFactorialAndPower(x)
            }
            (ch >= '0'.code && ch <= '9'.code) || ch == '.'.code -> {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    nextChar()
                }
                val numStr = str.substring(startPos, this.pos)
                x = try {
                    numStr.toDouble()
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Invalid number format: '$numStr'")
                }
                x = parsePercentageAndFactorialAndPower(x)
            }
            ch >= 'a'.code && ch <= 'z'.code -> {
                while (ch >= 'a'.code && ch <= 'z'.code) {
                    nextChar()
                }
                val func = str.substring(startPos, this.pos)
                val arg = if (eat('('.code)) {
                    val inner = parseExpression()
                    eat(')'.code)
                    inner
                } else {
                    parseFactor()
                }
                x = evaluateFunction(func, arg)
                x = parsePercentageAndFactorialAndPower(x)
            }
            else -> {
                val symbol = if (ch != -1) "'${ch.toChar()}'" else "End of Input"
                throw IllegalArgumentException("Unexpected symbol: $symbol")
            }
        }

        return x
    }

    private fun evaluateFunction(func: String, arg: Double): Double {
        return when (func) {
            "sqrt" -> {
                if (arg < 0.0) throw ArithmeticException("Cannot calculate square root of a negative number")
                sqrt(arg)
            }
            "sin" -> {
                if (isRadMode) {
                    sin(arg)
                } else {
                    val norm = (abs(arg) % 360.0)
                    if (abs(norm - 0.0) < 1e-9 || abs(norm - 180.0) < 1e-9 || abs(norm - 360.0) < 1e-9) {
                        0.0
                    } else if (abs(norm - 90.0) < 1e-9) {
                        if (arg >= 0) 1.0 else -1.0
                    } else if (abs(norm - 270.0) < 1e-9) {
                        if (arg >= 0) -1.0 else 1.0
                    } else {
                        sin(Math.toRadians(arg))
                    }
                }
            }
            "cos" -> {
                if (isRadMode) {
                    cos(arg)
                } else {
                    val norm = (abs(arg) % 360.0)
                    if (abs(norm - 90.0) < 1e-9 || abs(norm - 270.0) < 1e-9) {
                        0.0
                    } else if (abs(norm - 0.0) < 1e-9 || abs(norm - 360.0) < 1e-9) {
                        1.0
                    } else if (abs(norm - 180.0) < 1e-9) {
                        -1.0
                    } else {
                        cos(Math.toRadians(arg))
                    }
                }
            }
            "tan" -> {
                if (isRadMode) {
                    tan(arg)
                } else {
                    val norm = (abs(arg) % 180.0)
                    if (abs(norm - 90.0) < 1e-9) {
                        throw ArithmeticException("tan(90°) is undefined")
                    }
                    if (abs(norm - 0.0) < 1e-9 || abs(norm - 180.0) < 1e-9) {
                        0.0
                    } else if (abs(norm - 45.0) < 1e-9) {
                        if (arg >= 0) 1.0 else -1.0
                    } else if (abs(norm - 135.0) < 1e-9) {
                        if (arg >= 0) -1.0 else 1.0
                    } else {
                        tan(Math.toRadians(arg))
                    }
                }
            }
            "asin" -> {
                var clamped = arg
                if (clamped > 1.0 && clamped <= 1.0 + 1e-12) clamped = 1.0
                if (clamped < -1.0 && clamped >= -1.0 - 1e-12) clamped = -1.0
                if (clamped < -1.0 || clamped > 1.0) {
                    throw ArithmeticException("asin domain error: input must be between -1 and 1")
                }
                val res = asin(clamped)
                if (isRadMode) res else Math.toDegrees(res)
            }
            "acos" -> {
                var clamped = arg
                if (clamped > 1.0 && clamped <= 1.0 + 1e-12) clamped = 1.0
                if (clamped < -1.0 && clamped >= -1.0 - 1e-12) clamped = -1.0
                if (clamped < -1.0 || clamped > 1.0) {
                    throw ArithmeticException("acos domain error: input must be between -1 and 1")
                }
                val res = acos(clamped)
                if (isRadMode) res else Math.toDegrees(res)
            }
            "atan" -> {
                val res = atan(arg)
                if (isRadMode) res else Math.toDegrees(res)
            }
            "ln" -> {
                if (arg <= 0.0) throw ArithmeticException("ln of non-positive number is undefined")
                ln(arg)
            }
            "log" -> {
                if (arg <= 0.0) throw ArithmeticException("log of non-positive number is undefined")
                log10(arg)
            }
            else -> throw IllegalArgumentException("Unknown mathematical function: '$func'")
        }
    }

    private fun parsePercentageAndFactorialAndPower(initial: Double): Double {
        var x = initial
        while (true) {
            when {
                eat('%'.code) -> {
                    x *= 0.01
                }
                eat('!'.code) -> {
                    x = factorial(x)
                }
                eat('^'.code) -> {
                    val exponent = parseFactor()
                    if (x < 0.0 && exponent != exponent.toLong().toDouble()) {
                        throw ArithmeticException("Negative base with non-integer exponent is undefined")
                    }
                    if (x == 0.0 && exponent < 0.0) {
                        throw ArithmeticException("Division by zero")
                    }
                    x = x.pow(exponent)
                }
                else -> return x
            }
        }
    }

    private fun factorial(n: Double): Double {
        if (n < 0.0) throw ArithmeticException("Factorial of negative number is undefined")
        if (n > 170.0) throw ArithmeticException("Factorial limit exceeded (maximum 170!)")

        val integerPart = n.toLong()
        if (n == integerPart.toDouble()) {
            if (integerPart == 0L || integerPart == 1L) return 1.0
            var result = 1.0
            for (i in 2..integerPart) {
                result *= i.toDouble()
            }
            return result
        }

        // Lanczos approximation for Gamma(n + 1)
        return exp(lgamma(n + 1.0))
    }

    private fun lgamma(x: Double): Double {
        val p = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        var y = x
        if (y < 0.5) {
            val sinVal = sin(PI * y)
            if (abs(sinVal) == 0.0) throw ArithmeticException("Factorial undefined for non-positive integers")
            return ln(PI / (sinVal * exp(lgamma(1.0 - y))))
        }
        y -= 1.0
        var a = p[0]
        val t = y + 7.5
        for (i in 1..8) {
            a += p[i] / (y + i)
        }
        return ln(sqrt(2 * PI) * a) + (y + 0.5) * ln(t) - t
    }
}
