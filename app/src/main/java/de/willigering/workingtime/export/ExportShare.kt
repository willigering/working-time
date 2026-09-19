package de.willigering.workingtime.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import de.willigering.workingtime.R
import java.io.File

object ExportShare {

    fun exportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        subject: String? = null,
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_SUBJECT,
                subject ?: context.getString(R.string.export_share_subject),
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun mimeFor(format: ExportFormat): String = when (format) {
        ExportFormat.CSV -> "text/csv"
        ExportFormat.XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ExportFormat.PDF_TIMESHEET, ExportFormat.PDF_MONTHLY -> "application/pdf"
    }

    fun extensionFor(format: ExportFormat): String = when (format) {
        ExportFormat.CSV -> "csv"
        ExportFormat.XLSX -> "xlsx"
        ExportFormat.PDF_TIMESHEET, ExportFormat.PDF_MONTHLY -> "pdf"
    }

    fun fileNameFor(format: ExportFormat, stamp: String, issuerName: String = ""): String {
        val safeIssuer = issuerName
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .trim()
            .take(40)
            .ifBlank { null }
        val base = when (format) {
            ExportFormat.CSV -> listOfNotNull(safeIssuer, "Export").joinToString("-")
            ExportFormat.XLSX -> listOfNotNull(safeIssuer, "Export").joinToString("-")
            ExportFormat.PDF_TIMESHEET -> listOfNotNull(safeIssuer, "Stundennachweis").joinToString("-")
            ExportFormat.PDF_MONTHLY -> listOfNotNull(safeIssuer, "Monatsbericht").joinToString("-")
        }
        return "$base-$stamp.${extensionFor(format)}"
    }
}
