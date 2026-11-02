package com.quickfind.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransliteratorTest {
    @Test
    fun romanizes() {
        val table = listOf(
            "राहुल" to "rahul", "रमेश" to "ramesh", "कमला" to "kamala", "प्रिया" to "priya",
            "अमित" to "amit", "नमस्ते" to "namaste", "શાહ" to "shah", "નિકેતન" to "niketan",
            "ਸਿੰਘ" to "singh",
        )
        for ((native, latin) in table) assertEquals(native, latin, Transliterator.toLatin(native))
    }

    @Test
    fun latinTypingFindsNativeNames() {
        for ((q, name) in listOf("rahul" to "राहुल शर्मा", "sharma" to "राहुल शर्मा", "niketan" to "નિકેતન શાહ", "kamla" to "कमला")) {
            assertNotNull("$q → $name", FuzzyMatcher.match(NormalizedQuery.of(q), SearchableText(name)))
        }
        assertNull(FuzzyMatcher.match(NormalizedQuery.of("xyz"), SearchableText("राहुल")))
    }

    @Test
    fun latinTextIsUntouched() {
        val t = SearchableText("Rahul Sharma")
        assertNull(t.romanized)
        assertEquals(listOf(0..1), FuzzyMatcher.match(NormalizedQuery.of("ra"), t)!!.ranges)
    }
}

class OtpDetectorTest {
    @Test
    fun finds() {
        val table = listOf(
            "482913 is your OTP for login. Do not share." to "482913",
            "Your verification code is 7731" to "7731",
            "G-552310 is your Google verification code." to "552310",
            "Use OTP 019283 to pay Rs 2500.00 at Amazon" to "019283",
            "आपका ओटीपी 445566 है" to "445566",
        )
        for ((text, code) in table) assertEquals(text, code, OtpDetector.find(text))
    }

    @Test
    fun ignores() {
        for (text in listOf("Rs 2500 debited from A/c XX1234", "Meeting at 1530 tomorrow", "Your order 12345678 is shipped", "Code review in 2026")) {
            assertNull(text, OtpDetector.find(text))
        }
    }
}

class VectorsTest {
    @Test
    fun cosine() {
        assertEquals(1f, Vectors.cosine(floatArrayOf(1f, 2f), floatArrayOf(2f, 4f)), 1e-5f)
        assertEquals(0f, Vectors.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 3f)), 1e-5f)
        assertTrue(Vectors.cosine(floatArrayOf(1f, 1f), floatArrayOf(1f, 0.9f)) > 0.99f)
    }
}
