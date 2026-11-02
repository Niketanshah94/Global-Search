package com.quickfind.core.matching.instant

import java.util.Locale

/** "timer 10 min", "5 minute timer", "timer 1h 30m", "alarm 6:30", "alarm 7pm", "wake me at 6". */
object TimeCommands {
    sealed interface Command {
        data class Timer(val seconds: Int) : Command
        data class Alarm(val hour: Int, val minute: Int) : Command
    }

    private val TIMER_WORDS = setOf("timer", "countdown")
    private val FILLER = setOf("set", "a", "an", "for", "start", "me")
    private val PART = Regex("""(\d+(?:\.\d+)?)\s*(hours|hour|hrs|hr|h|minutes|minute|mins|min|m|seconds|second|secs|sec|s)?""")
    private val ALARM = Regex(
        """^(?:set\s+)?(?:an?\s+)?(?:alarm|wake\s+me(?:\s+up)?)\s*(?:for|at)?\s*(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)?\s*$""",
    )

    fun parse(query: String): Command? {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return null
        ALARM.matchEntire(q)?.let { return alarm(it) }
        return timer(q)
    }

    private fun alarm(m: MatchResult): Command? {
        var hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
        val meridiem = m.groupValues[3].replace(".", "")
        if (minute > 59) return null
        when (meridiem) {
            "am" -> { if (hour !in 1..12) return null; if (hour == 12) hour = 0 }
            "pm" -> { if (hour !in 1..12) return null; if (hour != 12) hour += 12 }
            else -> if (hour > 23) return null
        }
        return Command.Alarm(hour, minute)
    }

    private fun timer(q: String): Command? {
        val words = q.split(Regex("\\s+"))
        if (words.none { it in TIMER_WORDS }) return null
        val rest = words.filter { it !in TIMER_WORDS && it !in FILLER }.joinToString(" ")
        if (rest.isBlank()) return null
        var total = 0.0
        var consumed = 0
        for (m in PART.findAll(rest)) {
            if (m.value.isBlank()) continue
            val n = m.groupValues[1].toDouble()
            total += n * when (m.groupValues[2]) {
                "h", "hr", "hrs", "hour", "hours" -> 3600
                "s", "sec", "secs", "second", "seconds" -> 1
                else -> 60 // "m", "min", … and a bare number means minutes
            }
            consumed += m.value.replace(" ", "").length
        }
        // Everything left must have been a duration ("timer for pasta" is not a timer).
        if (consumed != rest.replace(" ", "").length) return null
        val seconds = total.toInt()
        return if (seconds in 1..(24 * 3600)) Command.Timer(seconds) else null
    }

    /** "1 h 30 min", "45 s". */
    fun describe(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildList {
            if (h > 0) add("$h h")
            if (m > 0) add("$m min")
            if (s > 0) add("$s s")
        }.joinToString(" ")
    }

    /** "6:30 AM" or "18:30" depending on the user's clock preference. */
    fun describeAlarm(hour: Int, minute: Int, use24Hour: Boolean): String {
        if (use24Hour) return String.format(Locale.US, "%02d:%02d", hour, minute)
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return String.format(Locale.US, "%d:%02d %s", h12, minute, if (hour < 12) "AM" else "PM")
    }
}
