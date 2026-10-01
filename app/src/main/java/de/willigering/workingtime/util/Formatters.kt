package de.willigering.workingtime.util

import android.content.Context
import de.willigering.workingtime.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Formatters {
    private fun locale(): Locale = Locale.getDefault()

    fun date(millis: Long): String {
        val pattern = if (locale().language == "de") "dd.MM.yyyy" else "MM/dd/yyyy"
        return SimpleDateFormat(pattern, locale()).format(millis)
    }

    fun time(millis: Long): String =
        SimpleDateFormat("HH:mm", locale()).format(millis)

    fun duration(context: Context, totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (hours > 0) {
            context.getString(R.string.duration_hours_mins, hours, mins)
        } else {
            context.getString(R.string.duration_mins, mins)
        }
    }

    fun timer(elapsedMs: Long): String {
        val seconds = (elapsedMs / 1000).toInt()
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(locale(), "%02d:%02d:%02d", h, m, s)
    }

    fun timerParts(elapsedMs: Long): Triple<String, String, String> {
        val seconds = (elapsedMs / 1000).toInt()
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        val loc = locale()
        return Triple(
            String.format(loc, "%02d", h),
            String.format(loc, "%02d", m),
            String.format(loc, "%02d", s),
        )
    }

    fun compactHours(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(locale(), "%02d:%02d", hours, mins)
    }

    fun currency(context: Context, amount: Double): String =
        if (amount <= 0) {
            context.getString(R.string.currency_empty)
        } else {
            context.getString(R.string.currency_value, amount)
        }

    fun dateLong(millis: Long): String {
        val pattern = if (locale().language == "de") "EEEE, d. MMMM yyyy" else "EEEE, MMMM d, yyyy"
        return SimpleDateFormat(pattern, locale()).format(millis)
    }

    /** Mockup style: 6:42 h, 32:17 h. */
    fun hoursLabel(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(locale(), "%d:%02d h", hours, mins)
    }

    fun money(amount: Double): String {
        val fmt = NumberFormat.getNumberInstance(locale()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val number = fmt.format(amount)
        return if (locale().language == "de") "$number €" else "€$number"
    }

    fun weekdayShort(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val names = if (locale().language == "de") {
            arrayOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa")
        } else {
            arrayOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        }
        return names[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    fun dayNumber(millis: Long): String =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH).toString()
}