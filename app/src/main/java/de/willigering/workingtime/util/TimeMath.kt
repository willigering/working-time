package de.willigering.workingtime.util

import java.util.Calendar
import java.util.Locale

/** All billing uses completed minutes; periods are [start, end). */
object TimeMath {
    fun minutes(start: Long, end: Long): Long = (end - start).coerceAtLeast(0L) / 60_000L

    fun dayRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val start = c.timeInMillis
        c.add(Calendar.DAY_OF_YEAR, 1)
        return start to c.timeInMillis
    }

    fun weekRange(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val c = Calendar.getInstance().apply {
            timeInMillis = dayRange(now).first
            val days = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return c.timeInMillis to now
    }

    /** Accept decimal input and correctly grouped thousands, reject ambiguity. */
    fun parseRate(input: String, locale: Locale = Locale.getDefault()): Double? {
        val s = input.trim()
        if (s.isEmpty()) return 0.0
        val comma = locale.language == "de"
        val decimal = if (comma) ',' else '.'
        val group = if (comma) '.' else ','
        val plain = Regex("[0-9]+([.,][0-9]{1,2})?")
        val grouped = Regex("[0-9]{1,3}(" + Regex.escape(group.toString()) + "[0-9]{3})+(" + Regex.escape(decimal.toString()) + "[0-9]{1,2})?")
        val normalized = when {
            grouped.matches(s) -> s.replace(group.toString(), "").replace(decimal, '.')
            plain.matches(s) -> s.replace(',', '.')
            else -> return null
        }
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 && it <= 1_000_000.0 }
    }
}
