package de.willigering.workingtime.export

import android.content.Context
import de.willigering.workingtime.R
import java.io.File
import java.nio.charset.Charset
import java.util.Locale

object CsvExporter {

    fun export(context: Context, summary: ExportSummary, outFile: File): File {
        val locale = Locale.getDefault()
        val sep = ";"
        val sb = StringBuilder()
        // UTF-8 BOM for Excel
        sb.append('\uFEFF')
        sb.append(context.getString(R.string.export_csv_header)).append('\n')

        for (row in summary.rows) {
            sb.append(
                listOf(
                    row.date,
                    row.project,
                    row.start,
                    row.end,
                    ExportBuilder.pauseHm(row.pauseMinutes),
                    ExportBuilder.durationHm(row.workMinutes),
                    formatRate(row.hourlyRate, locale),
                    formatMoney(row.earnings, locale),
                    escape(row.notes),
                ).joinToString(sep),
            ).append('\n')
        }

        // Summary footer
        sb.append('\n')
        sb.append(
            listOf(
                "",
                context.getString(R.string.export_total),
                "",
                "",
                "",
                ExportBuilder.durationHm(summary.totalMinutes),
                "",
                formatMoney(summary.totalEarnings, locale),
                "",
            ).joinToString(sep),
        ).append('\n')

        outFile.parentFile?.mkdirs()
        outFile.writeText(sb.toString(), Charset.forName("UTF-8"))
        return outFile
    }

    private fun formatRate(rate: Double, locale: Locale): String =
        if (rate <= 0) "" else String.format(locale, "%.2f", rate)

    private fun formatMoney(amount: Double, locale: Locale): String =
        String.format(locale, "%.2f", amount)

    private fun escape(value: String): String {
        val cleaned = value.replace('\n', ' ').replace('\r', ' ').replace(';', ',')
        return if (cleaned.contains('"')) cleaned.replace("\"", "\"\"") else cleaned
    }
}
