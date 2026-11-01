package com.quickfind.core.matching.instant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {
    private val table = listOf(
        "1+2" to "3",
        "2+3*4" to "14",
        "(2+3)*4" to "20",
        "10/4" to "2.5",
        "2^10" to "1024",
        "2^3^2" to "512",
        "-5+3" to "-2",
        "-(2+3)" to "-5",
        "-2^2" to "-4",
        "50%" to "0.5",
        "200*10%" to "20",
        "12 × 3.5" to "42",
        "9 ÷ 3 − 1" to "2",
        "0.1+0.2" to "0.3",
        "3,5*2" to "7",
        "(1+2" to "3",
        "1/3" to "0.333333333333",
    )

    @Test
    fun evaluates() {
        for ((expr, expected) in table) {
            val v = Calculator.evaluate(expr)
            assertEquals(expr, expected, v?.let(Calculator::format))
        }
    }

    @Test
    fun rejects() {
        for (expr in listOf("1/0", "1+", "*2", "hello", "42", "2..3+1", "()", "1+2)")) {
            assertNull(expr, Calculator.evaluate(expr))
        }
    }

    @Test
    fun trigger() {
        assertTrue(Calculator.looksLikeExpression("2+2"))
        assertTrue(Calculator.looksLikeExpression("(4)"))
        assertFalse(Calculator.looksLikeExpression("whatsapp"))
        assertFalse(Calculator.looksLikeExpression("-5"))
        assertFalse(Calculator.looksLikeExpression("+-*"))
    }
}
