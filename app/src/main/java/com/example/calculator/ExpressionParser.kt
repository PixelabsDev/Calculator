package com.example.calculator

import kotlin.math.*

class ExpressionParser(private val isRadMode: Boolean) {

    private var pos = -1
    private var ch = 0
    private var str = ""

    fun evaluate(expression: String): Double {
        // Sanitize string
        str = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "3.141592653589793")
            .replace("e", "2.718281828459045")
            .replace("√", "sqrt")
            .replace("sin⁻¹", "asin")
            .replace("cos⁻¹", "acos")
            .replace("tan⁻¹", "atan")
            .replace(" ", "") // Remove spaces

        pos = -1
        ch = 0
        nextChar()
        val x = parseExpression()
        if (pos < str.length) throw RuntimeException("Unexpected character inside expression: " + str[pos].toChar())
        return x
    }

    private fun nextChar() {
        ch = if (++pos < str.length) str[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.code)) x += parseTerm() // addition
            else if (eat('-'.code)) x -= parseTerm() // subtraction
            else break
        }
        return x
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.code)) x *= parseFactor() // multiplication
            else if (eat('/'.code)) {
                val divisor = parseFactor()
                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                x /= divisor // division
            } else break
        }
        return x
    }

    private fun parseFactor(): Double {
        if (eat('+'.code)) return parseFactor() // unary plus
        if (eat('-'.code)) return -parseFactor() // unary minus

        var x: Double
        val startPos = this.pos
        if (eat('('.code)) { // parentheses
            x = parseExpression()
            eat(')'.code)
            x = parsePercentageAndFactorialAndPower(x)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
            x = str.substring(startPos, this.pos).toDouble()
            x = parsePercentageAndFactorialAndPower(x)
        } else if (ch >= 'a'.code && ch <= 'z'.code || ch == 's'.code || ch == 'c'.code || ch == 't'.code || ch == 'l'.code) { // functions
            // Read function name
            val func: String
            while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
            func = str.substring(startPos, this.pos)
            
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else {
                x = parseFactor()
            }
            
            x = when (func) {
                "sqrt" -> {
                    if (x < 0.0) throw ArithmeticException("Square root of negative number")
                    sqrt(x)
                }
                "sin" -> if (isRadMode) sin(x) else sin(Math.toRadians(x))
                "cos" -> if (isRadMode) cos(x) else cos(Math.toRadians(x))
                "tan" -> if (isRadMode) tan(x) else tan(Math.toRadians(x))
                "asin" -> {
                    if (x < -1.0 || x > 1.0) throw ArithmeticException("Inverse sine range limit exceeded")
                    if (isRadMode) asin(x) else Math.toDegrees(asin(x))
                }
                "acos" -> {
                    if (x < -1.0 || x > 1.0) throw ArithmeticException("Inverse cosine range limit exceeded")
                    if (isRadMode) acos(x) else Math.toDegrees(acos(x))
                }
                "atan" -> if (isRadMode) atan(x) else Math.toDegrees(atan(x))
                "ln" -> {
                    if (x <= 0.0) throw ArithmeticException("Log of non-positive number")
                    ln(x)
                }
                "log" -> {
                    if (x <= 0.0) throw ArithmeticException("Log of non-positive number")
                    log10(x)
                }
                else -> throw RuntimeException("Unknown function: $func")
            }
            x = parsePercentageAndFactorialAndPower(x)
        } else {
            throw RuntimeException("Unexpected symbol: " + (if (ch != -1) ch.toChar() else "EOF"))
        }

        return x
    }

    private fun parsePercentageAndFactorialAndPower(initial: Double): Double {
        var x = initial
        while (true) {
            if (eat('%'.code)) {
                x *= 0.01
            } else if (eat('!'.code)) {
                x = factorial(x)
            } else if (eat('^'.code)) {
                x = x.pow(parseFactor())
            } else {
                break
            }
        }
        return x
    }

    private fun factorial(n: Double): Double {
        if (n < 0.0) throw ArithmeticException("Factorial of negative number")
        // Check if numeric value is reasonably integer
        val integerPart = n.toLong()
        if (n == integerPart.toDouble()) {
            if (integerPart > 150) throw ArithmeticException("Factorial limit exceeded") // Prevent overflow
            var result = 1.0
            for (i in 1..integerPart) {
                result *= i
            }
            return result
        }
        // Fallback approximation (Lanczos)
        if (n > 150.0) throw ArithmeticException("Factorial limit exceeded")
        return exp(lgamma(n + 1))
    }

    private fun lgamma(x: Double): Double {
        // Lanczos Gamma function approximation
        val p = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        var y = x
        if (y < 0.5) return ln(PI / (sin(PI * y) * exp(lgamma(1.0 - y))))
        y -= 1.0
        var a = p[0]
        val t = y + 7.5
        for (i in 1..8) {
            a += p[i] / (y + i)
        }
        return ln(sqrt(2 * PI) * a) + (y + 0.5) * ln(t) - t
    }
}
