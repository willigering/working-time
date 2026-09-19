package de.willigering.workingtime.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import de.willigering.workingtime.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object PdfExporter {

    private const val PAGE_W = 595 // A4 points
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    /** Display size of logo in PDF points (right side, next to name). */
    private const val LOGO_MAX_H = 56f
    private const val LOGO_MAX_W = 130f

    fun exportTimesheet(context: Context, summary: ExportSummary, outFile: File): File =
        writePdf(context, summary, outFile, monthly = false)

    fun exportMonthlyReport(context: Context, summary: ExportSummary, outFile: File): File =
        writePdf(context, summary, outFile, monthly = true)

    private fun writePdf(
        context: Context,
        summary: ExportSummary,
        outFile: File,
        monthly: Boolean,
    ): File {
        val doc = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 35, 126)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 33, 33)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(50, 50, 50)
            textSize = 11f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 100, 100)
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 1f
        }
        val accentLine = Paint().apply {
            color = Color.rgb(26, 35, 126)
            strokeWidth = 2f
        }
        // High-quality bitmap filter — keeps full-res logo sharp when scaled into a points rect
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_H - MARGIN - 24f) newPage()
        }

        // —— Header: name/company left, logo right (same vertical band) ——
        val logo = loadUserLogo(summary.logoPath)
        val headerTop = MARGIN
        var textBottom = headerTop
        var logoBottom = headerTop

        // Draw logo first on the right, using full-resolution bitmap into a points dest rect
        // (avoids pre-downscaling to ~50px which looked pixelated in PDF viewers)
        if (logo != null && logo.width > 0 && logo.height > 0) {
            val aspect = logo.width.toFloat() / logo.height.toFloat()
            var drawH = LOGO_MAX_H
            var drawW = drawH * aspect
            if (drawW > LOGO_MAX_W) {
                drawW = LOGO_MAX_W
                drawH = drawW / aspect
            }
            val left = PAGE_W - MARGIN - drawW
            val dest = RectF(left, headerTop, left + drawW, headerTop + drawH)
            canvas.drawBitmap(logo, null, dest, logoPaint)
            logoBottom = headerTop + drawH
        }

        // Name + company on the left, vertically aligned with logo top
        var textY = headerTop + titlePaint.textSize
        canvas.drawText(summary.title, MARGIN, textY, titlePaint)
        textBottom = textY + 4f
        if (summary.companyName.isNotBlank()) {
            textY = textBottom + bodyPaint.textSize + 4f
            // Keep company text clear of logo on the right
            val maxTextWidth = if (logo != null) {
                PAGE_W - 2 * MARGIN - LOGO_MAX_W - 16f
            } else {
                PAGE_W - 2 * MARGIN
            }
            val company = ellipsize(summary.companyName, bodyPaint, maxTextWidth)
            canvas.drawText(company, MARGIN, textY, bodyPaint)
            textBottom = textY + 4f
        }

        y = max(textBottom, logoBottom) + 14f

        val docTitle = if (monthly) {
            context.getString(R.string.export_pdf_monthly_title)
        } else {
            context.getString(R.string.export_pdf_timesheet_title)
        }
        canvas.drawText(docTitle, MARGIN, y, headingPaint)
        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, accentLine)
        y += 18f

        // Meta block
        canvas.drawText("${context.getString(R.string.export_label_project)}:", MARGIN, y, mutedPaint)
        canvas.drawText(summary.projectLabel, MARGIN + 90f, y, bodyPaint)
        y += 16f
        canvas.drawText("${context.getString(R.string.export_label_client)}:", MARGIN, y, mutedPaint)
        canvas.drawText(summary.clientLabel, MARGIN + 90f, y, bodyPaint)
        y += 16f
        canvas.drawText("${context.getString(R.string.export_label_period)}:", MARGIN, y, mutedPaint)
        canvas.drawText(summary.periodLabel, MARGIN + 90f, y, bodyPaint)
        y += 20f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 18f

        val colDate = MARGIN
        val colTime = MARGIN + 70f
        val colDur = MARGIN + 200f
        val colProject = MARGIN + 260f

        canvas.drawText(context.getString(R.string.export_col_date), colDate, y, headingPaint)
        canvas.drawText(
            "${context.getString(R.string.export_col_start)}–${context.getString(R.string.export_col_end)}",
            colTime,
            y,
            headingPaint,
        )
        canvas.drawText(context.getString(R.string.export_col_work), colDur, y, headingPaint)
        if (monthly) {
            canvas.drawText(context.getString(R.string.export_col_project), colProject, y, headingPaint)
        }
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 16f

        for (row in summary.rows) {
            ensureSpace(18f)
            val dateShow = if (row.date.length >= 6 && row.date[2] == '.') {
                row.date.substring(0, 6)
            } else {
                row.date
            }
            canvas.drawText(dateShow, colDate, y, bodyPaint)
            canvas.drawText("${row.start}-${row.end}", colTime, y, bodyPaint)
            canvas.drawText(ExportBuilder.durationHm(row.workMinutes), colDur, y, bodyPaint)
            if (monthly) {
                val proj = if (row.project.length > 28) row.project.take(27) + "…" else row.project
                canvas.drawText(proj, colProject, y, bodyPaint)
            } else if (row.notes.isNotBlank()) {
                val note = if (row.notes.length > 30) row.notes.take(29) + "…" else row.notes
                canvas.drawText(note, colProject, y, mutedPaint)
            }
            y += 16f
        }

        ensureSpace(90f)
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 20f

        canvas.drawText(
            "${context.getString(R.string.export_total_hours)}: ${ExportBuilder.durationHm(summary.totalMinutes)}",
            MARGIN,
            y,
            headingPaint,
        )
        y += 18f
        canvas.drawText(
            "${context.getString(R.string.export_hourly_rate)}: ${ExportBuilder.rateDe(summary.averageRate)}",
            MARGIN,
            y,
            bodyPaint,
        )
        y += 18f
        canvas.drawText(
            "${context.getString(R.string.export_total_amount)}: ${ExportBuilder.currencyDe(summary.totalEarnings)}",
            MARGIN,
            y,
            headingPaint,
        )

        if (monthly) {
            y += 28f
            ensureSpace(40f)
            canvas.drawText(
                context.getString(R.string.export_pdf_pre_invoice_note),
                MARGIN,
                y,
                mutedPaint,
            )
        }

        // Footer: page number only (no repeated name)
        canvas.drawText(
            "$pageNum",
            PAGE_W - MARGIN - 20f,
            PAGE_H - 28f,
            mutedPaint,
        )

        doc.finishPage(page)
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        logo?.recycle()
        return outFile
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) {
            t = t.dropLast(1)
        }
        return if (t.isEmpty()) "…" else "$t…"
    }

    private fun loadUserLogo(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            BitmapFactory.decodeFile(path, opts)
        } catch (_: Exception) {
            null
        }
    }
}
