package com.quickfind.core.matching

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumbersTest {
    @Test
    fun digitsOnly() {
        assertEquals("919876543210", PhoneNumbers.digitsOnly("+91 98765 43210"))
        assertEquals("5551234567", PhoneNumbers.digitsOnly("(555) 123-4567"))
    }

    @Test
    fun partialNumberMatchesContact() {
        val stored = PhoneNumbers.digitsOnly("+91 98765 43210")
        assertNotNull(PhoneNumbers.matchScore("98765", stored))
        assertNotNull(PhoneNumbers.matchScore(PhoneNumbers.digitsOnly("98765 432"), stored))
        assertNull(PhoneNumbers.matchScore("12345", stored))
        assertNull("too few digits", PhoneNumbers.matchScore("98", stored))
    }

    @Test
    fun longerMatchesScoreHigher() {
        val stored = "919876543210"
        assertTrue(PhoneNumbers.matchScore("9876543", stored)!! > PhoneNumbers.matchScore("987", stored)!!)
    }

    @Test
    fun numericQueryDetection() {
        assertTrue(NormalizedQuery.of("98765").looksNumeric)
        assertTrue(NormalizedQuery.of("+91 987").looksNumeric)
        assertEquals(false, NormalizedQuery.of("abc 987").looksNumeric)
    }
}
