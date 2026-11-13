package com.quickfind.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProGateTest {
    @Test
    fun freeTierIsContactsAndCalling() {
        assertTrue(ProGate.isFree(SourceType.CONTACT))
        assertTrue(ProGate.isFree(SourceType.PHONE_NUMBER))
        SourceType.entries.filterNot { it in ProGate.FREE_TYPES }.forEach { assertFalse(it.name, ProGate.isFree(it)) }
    }

    @Test
    fun onlyCallingAndTheContactCardAreFreeActions() {
        assertTrue(ProGate.isFree(ResultAction.Call("123")))
        assertTrue(ProGate.isFree(ResultAction.OpenContact("content://contacts/1")))
        assertFalse(ProGate.isFree(ResultAction.WhatsApp("123")))
        assertFalse(ProGate.isFree(ResultAction.Sms("123")))
        assertFalse(ProGate.isFree(ResultAction.LaunchApp("a", "b", 0)))
        assertFalse(ProGate.isFree(ResultAction.CopyText("482913")))
    }

    @Test
    fun lockedThingsNameTheirFeature() {
        assertEquals(ProFeature.MESSAGING, ProGate.featureFor(ResultAction.WhatsApp("1")))
        assertEquals(ProFeature.APPS, ProGate.featureFor(listOf(SourceType.SETTING, SourceType.APP)))
        assertEquals(ProFeature.QUICK_ANSWERS, ProGate.featureFor(listOf(SourceType.CALCULATOR)))
        assertEquals(ProFeature.MORE_RESULTS, ProGate.featureFor(listOf(SourceType.CALENDAR)))
    }
}
