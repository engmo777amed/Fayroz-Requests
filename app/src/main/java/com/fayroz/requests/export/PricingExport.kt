package com.fayroz.requests.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.fayroz.requests.data.model.PriceSource
import com.fayroz.requests.data.model.SheetPricingComparison
import java.io.OutputStream
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PricingExport {
    fun writePdf(output: OutputStream, data: SheetPricingComparison) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val right = 555f
        val left = 40f
        val lineHeight = 20f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 11f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 18f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 12f
            isFakeBoldText = true
        }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun startPage() {
            page?.let(document::finishPage)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = 46f
            val canvas = page!!.canvas
            canvas.drawText("FAYROZ REQUESTS", right, y, titlePaint)
            y += 26f
            canvas.drawText("${data.project.name} — كشف ${data.sheet.sheetNumber}", right, y, subPaint)
            y += 20f
            canvas.drawText("تاريخ الكشف: ${date(data.sheet.sheetDate)}", right, y, paint)
            y += 24f
            canvas.drawLine(left, y, right, y, paint)
            y += 20f
        }

        fun ensureSpace(lines: Int = 1) {
            if (page == null || y + lineHeight * lines > pageHeight - 50f) startPage()
        }

        startPage()
        fun canvas() = page!!.canvas

        data.lines.forEachIndexed { index, line ->
            ensureSpace(5)
            val best = line.offers.minByOrNull { it.netUnitPrice }
            canvas().drawText("${index + 1}. ${line.itemName} — ${qty(line.quantity)} ${line.unit}", right, y, subPaint)
            y += lineHeight
            if (line.usage.isNotBlank()) {
                canvas().drawText("الاستخدام: ${clip(line.usage, 70)}", right, y, paint)
                y += lineHeight
            }
            if (best == null) {
                canvas().drawText("لا يوجد سعر محفوظ", right, y, paint)
                y += lineHeight
            } else {
                canvas().drawText("أفضل سعر: ${best.supplierName}", right, y, paint)
                y += lineHeight
                canvas().drawText(
                    "ليستة ${money(best.listPrice)} | خصم ${discount(best.discountPercent)} | صافي ${money(best.netUnitPrice)} | إجمالي ${money(best.totalNet)}",
                    right, y, paint,
                )
                y += lineHeight
                canvas().drawText("${source(best.source)} — ${date(best.priceDate)}", right, y, paint)
                y += lineHeight
            }
            y += 8f
        }

        ensureSpace(4 + data.supplierTotals.size)
        canvas().drawLine(left, y, right, y, paint)
        y += 22f
        canvas().drawText("ملخص الموردين", right, y, subPaint)
        y += lineHeight
        data.supplierTotals.forEach { total ->
            ensureSpace(1)
            val coverage = "${total.coveredLines}/${total.totalLines} بند"
            canvas().drawText("${total.supplierName}: ${money(total.totalNet)} — $coverage", right, y, paint)
            y += lineHeight
        }
        data.bestMixTotal?.let {
            ensureSpace(2)
            y += 5f
            canvas().drawText("أقل تجميعة: ${money(it)}", right, y, subPaint)
            y += lineHeight
        }

        page?.let(document::finishPage)
        document.writeTo(output)
        document.close()
    }

    fun writeXlsx(output: OutputStream, data: SheetPricingComparison) {
        ZipOutputStream(output).use { zip ->
            fun entry(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            entry("[Content_Types].xml", contentTypes())
            entry("_rels/.rels", rootRels())
            entry("xl/workbook.xml", workbook())
            entry("xl/_rels/workbook.xml.rels", workbookRels())
            entry("xl/styles.xml", styles())
            entry("xl/worksheets/sheet1.xml", pricingSheet(data))
        }
    }

    private fun pricingSheet(data: SheetPricingComparison): String {
        val rows = mutableListOf<List<Cell>>()
        rows += listOf(Cell("FAYROZ REQUESTS"), Cell("تسعير كشف طلبات"))
        rows += listOf(Cell("المشروع"), Cell(data.project.name))
        rows += listOf(Cell("رقم الكشف"), Cell(data.sheet.sheetNumber), Cell("التاريخ"), Cell(date(data.sheet.sheetDate)))
        rows.add(emptyList())
        rows += listOf(
            Cell("م"), Cell("الصنف"), Cell("الكمية"), Cell("الوحدة"), Cell("الاستخدام"),
            Cell("أفضل مورد"), Cell("سعر الليستة"), Cell("الخصم %"), Cell("الصافي/وحدة"),
            Cell("الإجمالي"), Cell("تاريخ السعر"), Cell("المصدر"),
        )
        data.lines.forEachIndexed { index, line ->
            val best = line.offers.minByOrNull { it.netUnitPrice }
            rows += listOf(
                Cell((index + 1).toDouble()), Cell(line.itemName), Cell(line.quantity), Cell(line.unit), Cell(line.usage),
                Cell(best?.supplierName.orEmpty()), Cell(best?.listPrice), Cell(best?.discountPercent), Cell(best?.netUnitPrice),
                Cell(best?.totalNet), Cell(best?.priceDate?.let(::date).orEmpty()), Cell(best?.source?.let(::source).orEmpty()),
            )
        }
        rows.add(emptyList())
        rows += listOf(Cell("ملخص الموردين"))
        rows += listOf(Cell("المورد"), Cell("التغطية"), Cell("الإجمالي"))
        data.supplierTotals.forEach {
            rows += listOf(Cell(it.supplierName), Cell("${it.coveredLines}/${it.totalLines}"), Cell(it.totalNet))
        }
        data.bestMixTotal?.let { rows += listOf(Cell("أقل تجميعة"), Cell(""), Cell(it)) }

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
            append("<sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"/></sheetViews>")
            append("<cols>")
            val widths = listOf(6, 32, 12, 12, 28, 24, 16, 12, 16, 16, 16, 16)
            widths.forEachIndexed { i, width -> append("<col min=\"${i+1}\" max=\"${i+1}\" width=\"$width\" customWidth=\"1\"/>") }
            append("</cols><sheetData>")
            rows.forEachIndexed { r, cells ->
                append("<row r=\"${r + 1}\">")
                cells.forEachIndexed { c, cell -> append(cellXml(c + 1, r + 1, cell)) }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
    }

    private data class Cell(val text: String? = null, val number: Double? = null) {
        constructor(number: Double?) : this(null, number)
    }

    private fun cellXml(column: Int, row: Int, cell: Cell): String {
        val ref = columnName(column) + row
        return if (cell.number != null) {
            "<c r=\"$ref\" s=\"1\"><v>${cell.number}</v></c>"
        } else {
            "<c r=\"$ref\" t=\"inlineStr\" s=\"1\"><is><t>${xml(cell.text.orEmpty())}</t></is></c>"
        }
    }

    private fun columnName(index: Int): String {
        var n = index
        val out = StringBuilder()
        while (n > 0) {
            n--
            out.append(('A'.code + n % 26).toChar())
            n /= 26
        }
        return out.reverse().toString()
    }

    private fun contentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbook() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<bookViews><workbookView/></bookViews><sheets><sheet name="التسعير" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun styles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="1"><font><sz val="11"/><name val="Arial"/></font></fonts>
<fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"><alignment horizontal="right" vertical="center" wrapText="1"/></xf></cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""

    private val df = DecimalFormat("#,##0.00")
    private fun money(value: Double): String = "${df.format(value)} ج"
    private fun qty(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else df.format(value)
    private fun discount(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()}%" else "${df.format(value)}%"
    private fun date(timestamp: Long): String = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
    private fun source(value: PriceSource): String = when (value) {
        PriceSource.PRICE_LIST -> "ليستة مورد"
        PriceSource.DIRECT_QUOTE -> "عرض مباشر"
        PriceSource.MANUAL -> "سعر يدوي"
    }
    private fun clip(value: String, max: Int): String = if (value.length <= max) value else value.take(max - 1) + "…"
    private fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
