package com.quickfind.core.matching.instant

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.pow

/**
 * A small calculator built on the shunting-yard algorithm (spec 5.5).
 *
 * Supports `+ - * / ^ %`, the typographic `− × ÷`, parentheses, decimals (`.` or `,`) and
 * unary minus. `%` is postfix percent: `50%` = 0.5, `200*10%` = 20.
 */
object Calculator {
    private val OPERATOR_CHARS = setOf('+', '-', '*', '/', '^', '%', '−', '×', '÷', 'x', 'X')
    private val ALLOWED = Regex("""^[0-9+\-*/^%().,\s−×÷xX]+$""")

    /** True when the query has at least one digit and one operator and nothing else. */
    fun looksLikeExpression(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty() || !ALLOWED.matches(q)) return false
        if (q.none { it.isDigit() }) return false
        // An operator after the first character (a leading "-" alone is just a negative number).
        val hasOperator = q.drop(1).any { it in OPERATOR_CHARS } || q.contains('(')
        return hasOperator
    }

    /** Evaluates [expression]; null if it is not a complete, finite expression. */
    fun evaluate(expression: String): Double? {
        if (!looksLikeExpression(expression)) return null
        return try {
            val rpn = toRpn(tokenize(expression)) ?: return null
            evalRpn(rpn)?.takeIf { it.isFinite() }
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    /** Formats a result: integers without ".0", others to 12 significant digits. */
    fun format(value: Double): String {
        if (value == 0.0) return "0"
        val bd = BigDecimal(value).round(MathContext(12)).stripTrailingZeros()
        val plain = bd.toPlainString()
        return if (plain.length > 20) bd.toString() else plain
    }

    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Op(val symbol: Char) : Token
        data object LParen : Token
        data object RParen : Token
    }

    private const val UNARY_MINUS = '~'

    private fun tokenize(input: String): List<Token> {
        val tokens = ArrayList<Token>()
        var i = 0
        val s = input.trim()
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' || c == ',' -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
                    val text = s.substring(start, i).replace(',', '.')
                    if (text.count { it == '.' } > 1) throw NumberFormatException(text)
                    tokens += Token.Num(text.toDouble())
                }
                c == '(' -> { tokens += Token.LParen; i++ }
                c == ')' -> { tokens += Token.RParen; i++ }
                else -> {
                    val op = when (c) {
                        '−' -> '-'
                        '×', 'x', 'X' -> '*'
                        '÷' -> '/'
                        else -> c
                    }
                    val prev = tokens.lastOrNull()
                    val isUnary = op == '-' && (prev == null || prev is Token.LParen || (prev is Token.Op && prev.symbol != '%'))
                    if (op == '+' && (prev == null || prev is Token.LParen || (prev is Token.Op && prev.symbol != '%'))) {
                        // Unary plus: ignore.
                    } else {
                        tokens += Token.Op(if (isUnary) UNARY_MINUS else op)
                    }
                    i++
                }
            }
        }
        return tokens
    }

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        UNARY_MINUS -> 3
        '^' -> 4
        '%' -> 5
        else -> 0
    }

    private fun rightAssociative(op: Char) = op == '^' || op == UNARY_MINUS

    private fun toRpn(tokens: List<Token>): List<Token>? {
        val output = ArrayList<Token>()
        val stack = ArrayDeque<Token>()
        for (t in tokens) {
            when (t) {
                is Token.Num -> output += t
                is Token.Op -> {
                    if (t.symbol == '%') {
                        // Postfix: applies immediately to the value on its left.
                        output += t
                        continue
                    }
                    while (true) {
                        val top = stack.lastOrNull() as? Token.Op ?: break
                        val pTop = precedence(top.symbol)
                        val pCur = precedence(t.symbol)
                        if (pTop > pCur || (pTop == pCur && !rightAssociative(t.symbol))) {
                            output += stack.removeLast()
                        } else {
                            break
                        }
                    }
                    stack.addLast(t)
                }
                Token.LParen -> stack.addLast(t)
                Token.RParen -> {
                    while (stack.isNotEmpty() && stack.last() != Token.LParen) output += stack.removeLast()
                    if (stack.isEmpty()) return null
                    stack.removeLast()
                }
            }
        }
        while (stack.isNotEmpty()) {
            val t = stack.removeLast()
            if (t == Token.LParen) {
                // Forgive a missing ")" at the end while the user is still typing.
                continue
            }
            output += t
        }
        return output
    }

    private fun evalRpn(rpn: List<Token>): Double? {
        val stack = ArrayDeque<Double>()
        for (t in rpn) {
            when (t) {
                is Token.Num -> stack.addLast(t.value)
                is Token.Op -> when (t.symbol) {
                    UNARY_MINUS -> stack.addLast(-(stack.removeLastOrNull() ?: return null))
                    '%' -> stack.addLast((stack.removeLastOrNull() ?: return null) / 100.0)
                    else -> {
                        val b = stack.removeLastOrNull() ?: return null
                        val a = stack.removeLastOrNull() ?: return null
                        stack.addLast(
                            when (t.symbol) {
                                '+' -> a + b
                                '-' -> a - b
                                '*' -> a * b
                                '/' -> if (b == 0.0) return null else a / b
                                '^' -> a.pow(b)
                                else -> return null
                            },
                        )
                    }
                }
                else -> return null
            }
        }
        return if (stack.size == 1) stack.single() else null
    }
}
