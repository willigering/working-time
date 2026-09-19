package de.willigering.workingtime.export

import android.content.Context
import de.willigering.workingtime.R
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Lightweight XLSX writer (OOXML) without Apache POI — safe for Android.
 * Produces a formatted sheet with auto-filter, header colors and a totals row.
 */
object XlsxExporter {

    fun export(context: Context, summary: ExportSummary, outFile: File): File {
        val headers = listOf(
            context.getString(R.string.export_col_date),
            context.getString(R.string.export_col_project),
            context.getString(R.string.export_col_client),
            context.getString(R.string.export_col_start),
            context.getString(R.string.export_col_end),
            context.getString(R.string.export_col_pause),
            context.getString(R.string.export_col_work),
            context.getString(R.string.export_col_rate),
            context.getString(R.string.export_col_earnings),
            context.getString(R.string.export_col_note),
        )

        // Issuer block above the table (user branding, not app name)
        val metaRows = mutableListOf<List<String>>()
        if (summary.title.isNotBlank()) {
            metaRows += listOf(summary.title) + List(headers.size - 1) { "" }
        }
        if (summary.companyName.isNotBlank()) {
            metaRows += listOf(summary.companyName) + List(headers.size - 1) { "" }
        }
        if (metaRows.isNotEmpty()) {
            metaRows += List(headers.size) { "" }
        }

        val dataRows = summary.rows.map { row ->
            listOf(
                row.date,
                row.project,
                row.client,
                row.start,
                row.end,
                ExportBuilder.pauseHm(row.pauseMinutes),
                ExportBuilder.durationHm(row.workMinutes),
                if (row.hourlyRate > 0) String.format(Locale.GERMANY, "%.2f", row.hourlyRate) else "",
                String.format(Locale.GERMANY, "%.2f", row.earnings),
                row.notes,
            )
        }

        val totalRow = listOf(
            context.getString(R.string.export_total),
            "",
            "",
            "",
            "",
            "",
            ExportBuilder.durationHm(summary.totalMinutes),
            "",
            String.format(Locale.GERMANY, "%.2f", summary.totalEarnings),
            "",
        )

        val headerRowIndex = metaRows.size // 0-based
        val allRows = metaRows + listOf(headers) + dataRows + listOf(totalRow)
        val lastCol = colName(headers.size - 1)
        val tableHeaderExcelRow = headerRowIndex + 1 // 1-based
        val lastDataExcelRow = tableHeaderExcelRow + dataRows.size
        val filterRef = "A$tableHeaderExcelRow:$lastCol$lastDataExcelRow"

        // Single shared-string table used by both SST and sheet cells
        val sharedIndex = LinkedHashMap<String, Int>()
        for (row in allRows) {
            for (cell in row) {
                if (!sharedIndex.containsKey(cell)) {
                    sharedIndex[cell] = sharedIndex.size
                }
            }
        }

        outFile.parentFile?.mkdirs()
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            write(zip, "[Content_Types].xml", CONTENT_TYPES)
            write(zip, "_rels/.rels", RELS_ROOT)
            write(zip, "xl/workbook.xml", workbookXml(summary.title))
            write(zip, "xl/_rels/workbook.xml.rels", RELS_WORKBOOK)
            write(zip, "xl/styles.xml", STYLES)
            write(zip, "xl/sharedStrings.xml", buildSharedStrings(sharedIndex, allRows.size * headers.size))
            write(
                zip,
                "xl/worksheets/sheet1.xml",
                buildSheet(
                    rows = allRows,
                    index = sharedIndex,
                    filterRef = filterRef,
                    hasFilter = dataRows.isNotEmpty(),
                    tableHeaderIndex = headerRowIndex,
                ),
            )
        }
        return outFile
    }

    private fun workbookXml(issuerName: String): String {
        val sheetName = sanitizeSheetName(
            issuerName.ifBlank { "Export" }.take(31),
        )
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="${xml(sheetName)}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""
    }

    private fun sanitizeSheetName(name: String): String =
        name.replace(Regex("[\\\\/*?:\\[\\]]"), " ").trim().ifBlank { "Export" }

    private fun buildSharedStrings(index: Map<String, Int>, totalCount: Int): String {
        val ordered = Array(index.size) { "" }
        for ((s, i) in index) ordered[i] = s
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="$totalCount" uniqueCount="${ordered.size}">""")
        for (s in ordered) {
            sb.append("<si><t xml:space=\"preserve\">${xml(s)}</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun buildSheet(
        rows: List<List<String>>,
        index: Map<String, Int>,
        filterRef: String,
        hasFilter: Boolean,
        tableHeaderIndex: Int,
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<cols>""")
        val widths = listOf(12, 22, 18, 8, 8, 8, 12, 12, 12, 28)
        widths.forEachIndexed { i, w ->
            sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""")
        }
        sb.append("""</cols>""")
        sb.append("""<sheetData>""")

        rows.forEachIndexed { r, row ->
            val rowNum = r + 1
            val isTableHeader = r == tableHeaderIndex
            val isIssuer = r < tableHeaderIndex && row.firstOrNull().orEmpty().isNotBlank()
            val isTotal = r == rows.lastIndex
            sb.append("""<row r="$rowNum">""")
            row.forEachIndexed { c, value ->
                val ref = "${colName(c)}$rowNum"
                val style = when {
                    isTableHeader -> 1
                    isTotal -> 2
                    isIssuer && c == 0 -> 2
                    else -> 0
                }
                val si = index[value] ?: 0
                sb.append("""<c r="$ref" t="s" s="$style"><v>$si</v></c>""")
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        if (hasFilter) {
            sb.append("""<autoFilter ref="$filterRef"/>""")
        }
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun colName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A'.code + n % 26).toChar())
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    private fun write(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun xml(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
</Types>"""

    private const val RELS_ROOT = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val RELS_WORKBOOK = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

    private const val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1A237E"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1"><border/></borders>
  <cellStyleXfs count="1"><xf/></cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
}
