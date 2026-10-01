package de.willigering.workingtime.util

import java.util.Calendar

object Periods {
    fun dayBounds(anchor: Long): Pair<Long, Long> = TimeMath.dayRange(anchor)

    fun weekDays(anchor: Long): List<Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = TimeMath.dayRange(anchor).first
            val offset = (get(Calendar.DAY_OF_WEEK) + 5) % 7
            add(Calendar.DAY_OF_YEAR, -offset)
        }
        return List(7) {
            val day = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            day
        }
    }

    fun weekBounds(anchor: Long): Pair<Long, Long> {
        val start = weekDays(anchor).first()
        val cal = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, 7)
        }
        return start to cal.timeInMillis
    }

    fun monthBounds(anchor: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = anchor
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    fun sameDay(a: Long, b: Long): Boolean {
        val left = Calendar.getInstance().apply { timeInMillis = a }
        val right = Calendar.getInstance().apply { timeInMillis = b }
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }
}
