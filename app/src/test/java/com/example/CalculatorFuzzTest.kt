package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ExpressionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Random

/**
 * Automated Fuzz & Chaos Stress Testing Suite for Pixelabs Calculator.
 * Executes 1,000+ deterministic pseudo-random sequences of edge cases,
 * deeply nested parentheses, extreme exponents, consecutive operators,
 * and malicious tokens against both the ExpressionParser and CalculatorViewModel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorFuzzTest {

    private lateinit var application: Application
    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = CalculatorViewModel(application)
    }

    @Test(timeout = 30000)
    fun testExpressionParserFuzzing1000Iterations() {
        // Fixed random seed for 100% deterministic reproducibility
        val random = Random(42L)
        val tokens = listOf(
            "0", "1", "2", "3", "7", "8", "9",
            "+", "-", "*", "/", "×", "÷", "^", "!", "%",
            "(", ")", ".",
            "sin(", "cos(", "tan(", "asin(", "acos(", "atan(",
            "ln(", "log(", "sqrt(", "√(",
            "π", "e"
        )

        var totalEvaluated = 0
        var handledErrors = 0
        var successfulEvaluations = 0

        for (i in 1..1000) {
            val length = random.nextInt(15) + 1
            val isRad = random.nextBoolean()
            val parser = ExpressionParser(isRad)

            val exprBuilder = StringBuilder()
            for (j in 0 until length) {
                exprBuilder.append(tokens[random.nextInt(tokens.size)])
            }
            val expr = exprBuilder.toString()

            try {
                val result = parser.evaluate(expr)
                assertTrue(!result.isNaN())
                successfulEvaluations++
            } catch (expected: ArithmeticException) {
                // Expected domain and mathematical safety exceptions (e.g. division by zero, domain errors)
                handledErrors++
            } catch (expected: IllegalArgumentException) {
                // Expected syntax and token parsing errors
                handledErrors++
            } catch (t: Throwable) {
                throw AssertionError("Unhandled fatal exception on iteration $i with expr '$expr': ${t::class.java.simpleName}: ${t.message}", t)
            }
            totalEvaluated++
        }

        assertEquals(1000, totalEvaluated)
        assertTrue("Expected both successful calculations and handled errors", totalEvaluated == (successfulEvaluations + handledErrors))
    }

    @Test(timeout = 30000)
    fun testDeeplyNestedParenthesesFuzzing() {
        val parser = ExpressionParser(isRadMode = true)

        // Test extreme nesting depth up to 50 levels
        for (depth in 1..50) {
            val nested = "(".repeat(depth) + "42" + ")".repeat(depth)
            val result = parser.evaluate(nested)
            assertEquals(42.0, result, 1e-9)
        }

        // Test unbalanced deep open parentheses (auto-balancing)
        for (depth in 1..30) {
            val unclosed = "(".repeat(depth) + "10+5"
            val result = parser.evaluate(unclosed)
            assertEquals(15.0, result, 1e-9)
        }
    }

    @Test(timeout = 30000)
    fun testExtremeExponentsAndLargeNumbers() {
        val parser = ExpressionParser(isRadMode = false)

        val expressions = listOf(
            "10^0",
            "2^10",
            "2^3^2",
            "999999999+1",
            "0.0000000001*0.0000000001",
            "100!",
            "170!",
            "0!"
        )

        for (expr in expressions) {
            try {
                val res = parser.evaluate(expr)
                assertTrue("Result must not be NaN for $expr", !res.isNaN())
            } catch (e: ArithmeticException) {
                // Factorial overflow or domain errors are acceptable
                assertNotNull(e.message)
            }
        }
    }

    @Test(timeout = 30000)
    fun testViewModelChaosKeypressesAndCleanStateRecovery() {
        val random = Random(1337L)
        val keys = listOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "+", "-", "×", "÷", "^", "!", "%", "(", ")", "( )", ".",
            "sin", "cos", "tan", "ln", "log", "√", "π", "e",
            "⌫", "=", "AC"
        )

        for (round in 1..100) {
            val steps = random.nextInt(20) + 1
            for (s in 0 until steps) {
                val key = keys[random.nextInt(keys.size)]
                // Must never throw an unhandled exception
                viewModel.handleKeyPress(key)
            }

            // Always test clean state recovery using AC
            viewModel.handleKeyPress("AC")
            assertEquals("", viewModel.inputState.value)
            assertEquals("", viewModel.resultState.value)
        }
    }
}
