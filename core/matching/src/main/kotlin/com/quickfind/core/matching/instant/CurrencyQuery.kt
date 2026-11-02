package com.quickfind.core.matching.instant

import java.util.Locale

/**
 * Parses currency conversions: "100 usd in inr", "$50", "€20 to inr", "50 dollars", "1.5k jpy".
 * Rates come from elsewhere; this only understands the text.
 */
object CurrencyQuery {
    data class Request(val amount: Double, val from: String, val to: String?)

    /** ISO codes covered by the European Central Bank reference rates, plus EUR. */
    val SUPPORTED = setOf(
        "EUR", "USD", "JPY", "BGN", "CZK", "DKK", "GBP", "HUF", "PLN", "RON", "SEK", "CHF", "ISK", "NOK",
        "TRY", "AUD", "BRL", "CAD", "CNY", "HKD", "IDR", "ILS", "INR", "KRW", "MXN", "MYR", "NZD", "PHP",
        "SGD", "THB", "ZAR",
    )

    private val ALIASES = mapOf(
        "$" to "USD", "dollar" to "USD", "dollars" to "USD", "usd" to "USD", "bucks" to "USD",
        "€" to "EUR", "euro" to "EUR", "euros" to "EUR",
        "£" to "GBP", "pound sterling" to "GBP", "pounds sterling" to "GBP", "quid" to "GBP",
        "₹" to "INR", "rs" to "INR", "rs." to "INR", "rupee" to "INR", "rupees" to "INR",
        "¥" to "JPY", "yen" to "JPY", "yuan" to "CNY", "rmb" to "CNY",
        "aud" to "AUD", "cad" to "CAD", "sgd" to "SGD", "chf" to "CHF", "franc" to "CHF", "francs" to "CHF",
    )

    private val SYMBOL = mapOf("USD" to "$", "EUR" to "€", "GBP" to "£", "INR" to "₹", "JPY" to "¥", "CNY" to "¥")

    private const val NUM = """(\d+(?:[.,]\d+)?)\s*(k|m)?"""
    private val PREFIX_FORM = Regex("""^\s*([$€£₹¥])\s*$NUM\s*(?:(?:in|to|into|=)\s*([a-z$€£₹¥.]+))?\s*$""")
    private val SUFFIX_FORM = Regex("""^\s*$NUM\s*([a-z$€£₹¥.]+(?: sterling)?)\s*(?:(?:in|to|into|=)\s*([a-z$€£₹¥.]+))?\s*$""")

    fun parse(query: String): Request? {
        val q = query.lowercase(Locale.ROOT)
        PREFIX_FORM.matchEntire(q)?.let { m ->
            val from = code(m.groupValues[1]) ?: return null
            val amount = amount(m.groupValues[2], m.groupValues[3]) ?: return null
            val to = m.groupValues[4].takeIf { it.isNotBlank() }?.let { code(it) ?: return null }
            return Request(amount, from, to).takeIf { to != from }
        }
        SUFFIX_FORM.matchEntire(q)?.let { m ->
            val from = code(m.groupValues[3]) ?: return null
            val amount = amount(m.groupValues[1], m.groupValues[2]) ?: return null
            val to = m.groupValues[4].takeIf { it.isNotBlank() }?.let { code(it) ?: return null }
            return Request(amount, from, to).takeIf { to != from }
        }
        return null
    }

    private fun code(token: String): String? {
        val t = token.trim()
        ALIASES[t]?.let { return it }
        val upper = t.uppercase(Locale.ROOT)
        return upper.takeIf { it.length == 3 && it in SUPPORTED }
    }

    private fun amount(number: String, suffix: String): Double? {
        val n = number.replace(',', '.').toDoubleOrNull() ?: return null
        return when (suffix) {
            "k" -> n * 1_000
            "m" -> n * 1_000_000
            else -> n
        }
    }

    /** "₹8,350.12" or "8,350.12 SEK" when there is no common symbol. */
    fun format(amount: Double, code: String): String {
        val number = String.format(Locale.US, "%,.2f", amount)
        val symbol = SYMBOL[code]
        return if (symbol != null) "$symbol$number" else "$number $code"
    }

    fun symbolOrCode(code: String): String = SYMBOL[code] ?: code
}
