package com.quickfind.core.matching

/** Optimal-string-alignment Damerau-Levenshtein distance, with an early exit above [max]. */
internal object EditDistance {
    fun withinOne(a: String, b: String): Boolean = distance(a, b, max = 1) <= 1

    fun distance(a: String, b: String, max: Int = Int.MAX_VALUE): Int {
        if (kotlin.math.abs(a.length - b.length) > max) return max + 1
        val n = a.length
        val m = b.length
        var prevPrev = IntArray(m + 1)
        var prev = IntArray(m + 1) { it }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    v = minOf(v, prevPrev[j - 2] + 1)
                }
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val t = prevPrev
            prevPrev = prev
            prev = cur
            cur = t
        }
        return prev[m]
    }
}
