package com.example

import com.example.calculator.ExpressionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.PI
import kotlin.math.E

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun expressionParser_basicAndScientific_isCorrect() {
        val parserRad = ExpressionParser(isRadMode = true)
        assertEquals(14.0, parserRad.evaluate("2+3×4"), 1e-9)
        assertEquals(1.0, parserRad.evaluate("sin(π ÷ 2)"), 1e-4)
        assertEquals(8.0, parserRad.evaluate("2^3"), 1e-9)
        assertEquals(120.0, parserRad.evaluate("5!"), 1e-9)
        assertEquals(5.0, parserRad.evaluate("√(25)"), 1e-9)
    }

    @Test
    fun expressionParser_implicitMultiplication_isCorrect() {
        val parserDeg = ExpressionParser(isRadMode = false)
        assertEquals(6.0, parserDeg.evaluate("2(3)"), 1e-9)
        assertEquals(6.0, parserDeg.evaluate("(2)(3)"), 1e-9)
        assertEquals(2 * PI, parserDeg.evaluate("2π"), 1e-9)
        assertEquals(2 * E, parserDeg.evaluate("2e"), 1e-9)
        assertEquals(6.0, parserDeg.evaluate("2√(9)"), 1e-9)
        assertEquals(1.0, parserDeg.evaluate("2sin(30)"), 1e-6)
        assertEquals(240.0, parserDeg.evaluate("5!2"), 1e-9)
        assertEquals(5.0, parserDeg.evaluate("10%50"), 1e-9)
    }

    @Test
    fun expressionParser_parenthesesAutobalance_isCorrect() {
        val parserDeg = ExpressionParser(isRadMode = false)
        assertEquals(0.5, parserDeg.evaluate("sin(30"), 1e-6)
        assertEquals(5.0, parserDeg.evaluate("(2+3"), 1e-9)
        assertEquals(20.0, parserDeg.evaluate("((2+3)×4"), 1e-9)
    }

    @Test
    fun expressionParser_trigonometryDegrees_isCorrect() {
        val parserDeg = ExpressionParser(isRadMode = false)
        assertEquals(0.0, parserDeg.evaluate("cos(90)"), 1e-9)
        assertEquals(0.0, parserDeg.evaluate("sin(180)"), 1e-9)
        assertEquals(1.0, parserDeg.evaluate("tan(45)"), 1e-6)
        assertEquals(90.0, parserDeg.evaluate("asin(1)"), 1e-6)
        assertEquals(0.0, parserDeg.evaluate("acos(1)"), 1e-6)
    }

    @Test
    fun expressionParser_edgeCases_throwsAppropriateExceptions() {
        val parserDeg = ExpressionParser(isRadMode = false)

        // Division by zero
        assertThrows(ArithmeticException::class.java) {
            parserDeg.evaluate("5 ÷ 0")
        }

        // tan(90°) undefined
        assertThrows(ArithmeticException::class.java) {
            parserDeg.evaluate("tan(90)")
        }

        // Square root of negative number
        assertThrows(ArithmeticException::class.java) {
            parserDeg.evaluate("√(-16)")
        }

        // Factorial of negative number
        assertThrows(ArithmeticException::class.java) {
            parserDeg.evaluate("(-5)!")
        }

        // asin out of range
        assertThrows(ArithmeticException::class.java) {
            parserDeg.evaluate("asin(2)")
        }
    }
}
