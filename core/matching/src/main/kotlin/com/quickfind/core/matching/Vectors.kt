package com.quickfind.core.matching

import kotlin.math.sqrt

/** Small vector maths for the on-device embedding search. */
object Vectors {
    fun normalize(v: FloatArray): FloatArray {
        var sum = 0.0
        for (x in v) sum += x * x
        val norm = sqrt(sum).toFloat()
        if (norm == 0f) return v
        return FloatArray(v.size) { v[it] / norm }
    }

    /** Dot product; equals cosine similarity when both vectors are normalized. */
    fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in 0 until minOf(a.size, b.size)) s += a[i] * b[i]
        return s
    }

    fun cosine(a: FloatArray, b: FloatArray): Float = dot(normalize(a), normalize(b))
}
