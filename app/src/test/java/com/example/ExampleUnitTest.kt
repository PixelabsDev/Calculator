package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun expressionParser_isCorrect() {
    val parserRad = com.example.calculator.ExpressionParser(isRadMode = true)
    assertEquals(14.0, parserRad.evaluate("2+3×4"), 1e-9)
    assertEquals(1.0, parserRad.evaluate("sin(π ÷ 2)"), 1e-4)
    assertEquals(8.0, parserRad.evaluate("2^3"), 1e-9)
    assertEquals(120.0, parserRad.evaluate("5!"), 1e-9)
    assertEquals(5.0, parserRad.evaluate("√(25)"), 1e-9)
  }
}
