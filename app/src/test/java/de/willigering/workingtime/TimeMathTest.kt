package de.willigering.workingtime

import de.willigering.workingtime.util.TimeMath
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TimeMathTest {
    @Test fun ratesAreValidatedAndLocalized() {
        assertEquals(1000.50, TimeMath.parseRate("1.000,50", Locale.GERMANY)!!, 0.0001)
        assertEquals(1000.50, TimeMath.parseRate("1,000.50", Locale.US)!!, 0.0001)
        assertEquals(25.50, TimeMath.parseRate("25,50", Locale.GERMANY)!!, 0.0001)
        assertEquals(25.50, TimeMath.parseRate("25.50", Locale.GERMANY)!!, 0.0001)
        for (invalid in listOf("25..5", "1.00,50", "-5", "NaN", "Infinity", "9999999999"))
            assertNull(invalid, TimeMath.parseRate(invalid, Locale.GERMANY))
        assertEquals(0.0, TimeMath.parseRate("", Locale.GERMANY)!!, 0.0)
    }

    @Test fun minutesNeverBillPartialOrNegativeTime() {
        assertEquals(0L, TimeMath.minutes(0, 59_999))
        assertEquals(1L, TimeMath.minutes(0, 60_000))
        assertEquals(0L, TimeMath.minutes(60_000, 0))
    }

    @Test fun dayBoundariesFollowBothDstTransitions() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            for ((month, day, hours) in listOf(Triple(Calendar.MARCH, 29, 23), Triple(Calendar.OCTOBER, 25, 25))) {
                val now = Calendar.getInstance().apply { set(2026, month, day, 12, 0, 0) }.timeInMillis
                val range = TimeMath.dayRange(now)
                assertEquals(hours * 3_600_000L, range.second - range.first)
            }
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun weekStartsOnMondayEvenOnSunday() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 4, 12, 0, 0) }.timeInMillis
        val start = Calendar.getInstance().apply { timeInMillis = TimeMath.weekRange(now).first }
        assertEquals(Calendar.MONDAY, start.get(Calendar.DAY_OF_WEEK))
        assertEquals(28, start.get(Calendar.DAY_OF_MONTH))
    }
}
