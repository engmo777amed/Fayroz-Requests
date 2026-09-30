package com.fayroz.requests.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.fayroz.requests.data.model.*
import java.io.OutputStream
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FayrozReports {
    fun writeRequestSheetPdf(
        output: OutputStream,
        project: ProjectEntity,
        sheet: RequestSheetEntity,
        lines: List<RequestLineDetail>,
    ) {
        writePdf(
            output = output,
            title = "كشف طلبات ${sheet.sheetNumber}",
            subtitle = project.name,
            meta = buildList {
                if (sheet.workLocation.isNotBlank()) add("المكان: ${sheet.workLocation}")
                if (sheet.craftsmanName.isNotBlank()) add("الصنايعي: ${sheet.craftsmanName}")
                add("التاريخ: ${date(sheet.sheetDate)}")
            },
            rows = lines.mapIndexed { index, line ->
                "${index + 1}. ${line.itemName} — ${qty(line.quantity)} ${line.unit}"
            },
            footer = listOf("عدد البنود: ${lines.size}"),
        )
    }

    fun writePricingCopyPdf(output: OutputStream, data: PricingCopyDetail) {
        val completeText = if (data.complete) "مكتمل" else "غير مكتمل: ${data.pricedCount}/${data.lines.size}"
        writePdf(
            output = output,
            title = "تسعير ${data.copy.placeName}",
            subtitle = "${data.project.name} — كشف ${data.sheet.sheetNumber}",
            meta = buildList {
                if (data.copy.quoteNumber.isNotBlank()) add("رقم العرض: ${data.copy.quoteNumber}")
                add("تاريخ العرض: ${date(data.copy.quoteDate)}")
                add("الحالة: $completeText")
            },
            rows = data.lines.mapIndexed { index, line ->
                val price = line.unitPrice
                val total = price?.times(line.quantity)
                buildString {
                    append("${index + 1}. ${line.itemName} — ${qty(line.quantity)} ${line.unit}")
                    if (line.brand.isNotBlank()) append(" — ${line.brand}")
                    append(" — ")
                    append(price?.let(::money) ?: "غير مسعّر")
                    total?.let { append(" — إجمالي ${money(it)}") }
                }
            },
            footer = listOf(
                if (data.complete) "إجمالي العرض: ${money(data.total)}" else "الإجمالي الجزئي: ${money(data.total)}",
            ),
        )
    }

    fun writeComparisonPdf(output: OutputStream, data: PricingCopiesComparison) {
        val rows = mutableListOf<String>()
        data.copies.forEach { copy ->
            rows += buildString {
                append(copy.copy.placeName)
                append(" — ${copy.pricedCount}/${copy.lines.size} بند")
                if (copy.complete) append(" — ${money(copy.total)}") else append(" — تسعير غير مكتمل")
                append(" — الأقل في ${data.cheapestLineCount(copy.copy.id)} بند")
            }
        }
        rows += "----- مقارنة البنود -----"
        val base = data.copies.firstOrNull()?.lines.orEmpty()
        base.forEach { line ->
            val offers = data.copies.mapNotNull { copy ->
                copy.lines.firstOrNull { it.requestLineId == line.requestLineId }?.unitPrice?.let { price ->
                    "${copy.copy.placeName}: ${money(price)}"
                }
            }
            rows += "${line.itemName} — ${qty(line.quantity)} ${line.unit} — ${offers.joinToString(" | ")}"
        }
        val footer = buildList {
            data.lowestCompleteTotal?.let { add("أقل عرض كامل: ${money(it)}") }
            data.highestCompleteTotal?.let { add("أعلى عرض كامل: ${money(it)}") }
            data.completeRangeSaving?.let { add("الفرق بين العروض الكاملة: ${money(it)}") }
            data.bestMixTotal?.let { add("أقل تجميعة بند-بند: ${money(it)}") }
        }
        writePdf(
            output,
            "مقارنة تسعيرات كشف ${data.sheet.sheetNumber}",
            data.project.name,
            listOf("عدد النسخ: ${data.copies.size}"),
            rows,
            footer,
        )
    }

    fun writeComparisonXlsx(output: OutputStream, data: PricingCopiesComparison) {
        val rows = mutableListOf<List<Cell>>()
        rows += listOf(Cell("FAYROZ REQUESTS"), Cell("مقارنة تسعيرات"))
        rows += listOf(Cell("المشروع"), Cell(data.project.name))
        rows += listOf(Cell("رقم الكشف"), Cell(data.sheet.sheetNumber))
        rows += emptyList()
        rows += listOf(Cell("المحل / المورد"), Cell("التغطية"), Cell("الحالة"), Cell("الإجمالي"), Cell("الأقل في بنود"))
        data.copies.forEach { copy ->
            rows += listOf(
                Cell(copy.copy.placeName),
                Cell("${copy.pricedCount}/${copy.lines.size}"),
                Cell(if (copy.complete) "مكتمل" else "غير مكتمل"),
                Cell(if (copy.complete) copy.total else null),
                Cell(data.cheapestLineCount(copy.copy.id).toDouble()),
            )
        }
        rows += emptyList()
        val headers = mutableListOf(Cell("الصنف"), Cell("الكمية"), Cell("الوحدة"))
        data.copies.forEach { headers += Cell(it.copy.placeName) }
        rows += headers
        val base = data.copies.firstOrNull()?.lines.orEmpty()
        base.forEach { line ->
            val row = mutableListOf(Cell(line.itemName), Cell(line.quantity), Cell(line.unit))
            data.copies.forEach { copy ->
                row += Cell(copy.lines.firstOrNull { it.requestLineId == line.requestLineId }?.unitPrice)
            }
            rows += row
        }
        rows += emptyList()
        data.lowestCompleteTotal?.let { rows += listOf(Cell("أقل عرض كامل"), Cell(it)) }
        data.completeRangeSaving?.let { rows += listOf(Cell("الفرق بين العروض الكاملة"), Cell(it)) }
        data.bestMixTotal?.let { rows += listOf(Cell("أقل تجميعة بند-بند"), Cell(it)) }
        writeXlsx(output, rows)
    }

    private fun writePdf(
        output: OutputStream,
        title: String,
        subtitle: String,
        meta: List<String>,
        rows: List<String>,
        footer: List<String>,
    ) {
        val doc = PdfDocument()
        val width = 595
        val height = 842
        val right = 555f
        val left = 40f
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 11f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 18f
            isFakeBoldText = true
        }
        val strong = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            textSize = 12f
            isFakeBoldText = true
        }
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun start() {
            page?.let(doc::finishPage)
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
            y = 48f
            page!!.canvas.drawText("FAYROZ REQUESTS", right, y, titlePaint)
            y += 28f
            page!!.canvas.drawText(clip(title, 70), right, y, strong)
            y += 20f
            page!!.canvas.drawText(clip(subtitle, 70), right, y, body)
            y += 20f
            meta.forEach {
                page!!.canvas.drawText(clip(it, 85), right, y, body)
                y += 18f
            }
            y += 4f
            page!!.canvas.drawLine(left, y, right, y, body)
            y += 20f
        }
        fun ensure(lines: Int = 1) {
            if (page == null || y + lines * 19f > height - 55f) start()
        }
        start()
        rows.forEach {
            ensure()
            page!!.canvas.drawText(clip(it, 95), right, y, body)
            y += 19f
        }
        if (footer.isNotEmpty()) {
            ensure(footer.size + 2)
            y += 4f
            page!!.canvas.drawLine(left, y, right, y, body)
            y += 20f
            footer.forEach {
                page!!.canvas.drawText(clip(it, 85), right, y, strong)
                y += 20f
            }
        }
        page?.let(doc::finishPage)
        doc.writeTo(output)
        doc.close()
    }

    private fun writeXlsx(output: OutputStream, rows: List<List<Cell>>) {
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
            entry("xl/worksheets/sheet1.xml", sheet(rows))
        }
    }

    private data class Cell(val text: String? = null, val number: Double? = null) {
        constructor(number: Double?) : this(null, number)
    }

    private fun sheet(rows: List<List<Cell>>) = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetViews><sheetView workbookViewId=\"0\" rightToLeft=\"1\"/></sheetViews>")
        append("<sheetData>")
        rows.forEachIndexed { r, cells ->
            append("<row r=\"${r + 1}\">")
            cells.forEachIndexed { c, cell -> append(cellXml(c + 1, r + 1, cell)) }
            append("</row>")
        }
        append("</sheetData></worksheet>")
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
<bookViews><workbookView/></bookViews><sheets><sheet name="المقارنة" sheetId="1" r:id="rId1"/></sheets>
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
    private fun money(value: Double) = "${df.format(value)} ج"
    private fun qty(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else df.format(value)
    private fun date(timestamp: Long) = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
    private fun clip(value: String, max: Int) = if (value.length <= max) value else value.take(max - 1) + "…"
    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;")
        .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
