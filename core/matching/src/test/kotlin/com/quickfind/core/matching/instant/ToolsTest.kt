package com.quickfind.core.matching.instant

import com.quickfind.core.model.SourceType
import com.quickfind.core.model.WebSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitConverterTest {
    private fun conv(q: String) = UnitConverter.parse(q)?.let { "${UnitConverter.format(it.result)} ${it.to.symbol}" }

    @Test
    fun converts() {
        val table = listOf(
            "5 ft in cm" to "152.4 cm",
            "5ft to cm" to "152.4 cm",
            "1 in to cm" to "2.54 cm",
            "1 in in cm" to "2.54 cm",
            "10 km" to "6.21 mi",
            "100 kg in lb" to "220.46 lb",
            "30 c to f" to "86 °F",
            "98.6 f" to "37 °C",
            "2 gb in mb" to "2,048 MB",
            "1 l" to "0.2642 gal",
            "60 mph" to "96.56 km/h",
            "2,5 kg in g" to "2,500 g",
        )
        for ((q, expected) in table) assertEquals(q, expected, conv(q))
    }

    @Test
    fun rejects() {
        for (q in listOf("5 ft in kg", "hello", "5", "10 apples", "camera", "2+3", "5 ft in")) assertNull(q, UnitConverter.parse(q))
    }
}

class CurrencyQueryTest {
    @Test
    fun parses() {
        assertEquals(CurrencyQuery.Request(100.0, "USD", "INR"), CurrencyQuery.parse("100 usd in inr"))
        assertEquals(CurrencyQuery.Request(50.0, "USD", null), CurrencyQuery.parse("$50"))
        assertEquals(CurrencyQuery.Request(20.0, "EUR", "INR"), CurrencyQuery.parse("€20 to inr"))
        assertEquals(CurrencyQuery.Request(50.0, "USD", null), CurrencyQuery.parse("50 dollars"))
        assertEquals(CurrencyQuery.Request(1500.0, "JPY", null), CurrencyQuery.parse("1.5k jpy"))
        assertEquals(CurrencyQuery.Request(500.0, "INR", "USD"), CurrencyQuery.parse("500 rs to usd"))
    }

    @Test
    fun rejects() {
        for (q in listOf("100 usd in usd", "100 abc", "usd", "10 m", "5 ft", "hello 100", "100 inr in xyz")) {
            assertNull(q, CurrencyQuery.parse(q))
        }
    }

    @Test
    fun formats() {
        assertEquals("₹8,350.12", CurrencyQuery.format(8350.123, "INR"))
        assertEquals("12.50 SEK", CurrencyQuery.format(12.5, "SEK"))
    }
}

class TimeCommandsTest {
    @Test
    fun timers() {
        val table = listOf(
            "timer 10 min" to 600, "10 min timer" to 600, "5 minute timer" to 300, "timer 1h 30m" to 5400,
            "set a timer for 45 seconds" to 45, "timer 2 hours" to 7200, "timer 3" to 180, "countdown 90 s" to 90,
        )
        for ((q, s) in table) assertEquals(q, TimeCommands.Command.Timer(s), TimeCommands.parse(q))
    }

    @Test
    fun alarms() {
        val table = listOf(
            "alarm 6:30" to (6 to 30), "alarm 7pm" to (19 to 0), "alarm at 7 am" to (7 to 0),
            "wake me at 6" to (6 to 0), "wake me up at 6.45 am" to (6 to 45), "set alarm for 12 am" to (0 to 0),
            "alarm 12pm" to (12 to 0), "alarm 21:15" to (21 to 15),
        )
        for ((q, hm) in table) assertEquals(q, TimeCommands.Command.Alarm(hm.first, hm.second), TimeCommands.parse(q))
    }

    @Test
    fun rejects() {
        for (q in listOf("timer pasta", "timer", "alarm", "alarm 25:00", "alarm 13pm", "alarm 6:75", "10 min", "timer 0", "calendar")) {
            assertNull(q, TimeCommands.parse(q))
        }
    }

    @Test
    fun describes() {
        assertEquals("1 h 30 min", TimeCommands.describe(5400))
        assertEquals("6:30 AM", TimeCommands.describeAlarm(6, 30, use24Hour = false))
        assertEquals("12:00 AM", TimeCommands.describeAlarm(0, 0, use24Hour = false))
        assertEquals("18:05", TimeCommands.describeAlarm(18, 5, use24Hour = true))
    }
}

class QueryPrefixesAndDetectorsTest {
    @Test
    fun prefixes() {
        assertEquals(QueryPrefixes.Parsed(QueryPrefixes.Prefix.Scope(setOf(SourceType.FILE, SourceType.FOLDER)), "report"), QueryPrefixes.parse("f report"))
        assertEquals(QueryPrefixes.Parsed(QueryPrefixes.Prefix.Web(WebSearchEngine.YOUTUBE), "lofi beats"), QueryPrefixes.parse("yt lofi beats"))
        assertEquals("", QueryPrefixes.parse("c ")!!.rest)
        assertNull(QueryPrefixes.parse("facebook"))
        assertNull(QueryPrefixes.parse("fb login"))
        assertNull(QueryPrefixes.parse("f"))
    }

    @Test
    fun emailAndAddress() {
        assertTrue(QueryDetectors.looksLikeEmail("rahul.k@example.co.in"))
        assertFalse(QueryDetectors.looksLikeEmail("rahul@"))
        assertTrue(QueryDetectors.looksLikeAddress("12, MG Road, Indiranagar, Bengaluru 560038"))
        assertTrue(QueryDetectors.looksLikeAddress("Flat 4, Shanti Society, near Station, Pune"))
        assertFalse(QueryDetectors.looksLikeAddress("rahul, call me"))
        assertFalse(QueryDetectors.looksLikeAddress("whatsapp"))
    }
}
