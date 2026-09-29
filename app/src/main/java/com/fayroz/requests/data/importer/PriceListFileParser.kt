package com.fayroz.requests.data.importer

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object PriceListFileParser {
    private const val MAX_CSV_BYTES = 12 * 1024 * 1024
    private const val MAX_XLSX_ENTRY_BYTES = 20 * 1024 * 1024

    fun parse(fileName: String, input: InputStream): TabularPriceData {
        val lower = fileName.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".csv") || lower.endsWith(".txt") -> parseCsv(fileName, input)
            lower.endsWith(".xlsx") -> parseXlsx(fileName, input)
            lower.endsWith(".xls") -> error("صيغة XLS القديمة غير مدعومة. احفظ الملف بصيغة XLSX أو CSV ثم أعد الاستيراد.")
            else -> error("نوع الملف غير مدعوم. اختر ملف XLSX أو CSV.")
        }
    }

    private fun parseCsv(fileName: String, input: InputStream): TabularPriceData {
        val bytes = input.readLimited(MAX_CSV_BYTES)
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val firstMeaningful = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val delimiter = detectDelimiter(firstMeaningful)
        val parsed = parseDelimited(text, delimiter).filter { row -> row.any { it.isNotBlank() } }
        return toTabularData(fileName, parsed)
    }

    private fun parseXlsx(fileName: String, input: InputStream): TabularPriceData {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && (entry.name == "xl/sharedStrings.xml" || entry.name.startsWith("xl/worksheets/sheet"))) {
                    entries[entry.name] = zip.readLimited(MAX_XLSX_ENTRY_BYTES)
                }
                zip.closeEntry()
            }
        }
        val sheetName = entries.keys.filter { it.startsWith("xl/worksheets/sheet") }.sorted().firstOrNull()
            ?: error("لم يتم العثور على ورقة بيانات داخل ملف Excel.")
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::readSharedStrings).orEmpty()
        val matrix = readWorksheet(entries.getValue(sheetName), sharedStrings)
            .filter { row -> row.any { it.isNotBlank() } }
        return toTabularData(fileName, matrix)
    }


    private fun toTabularData(fileName: String, matrix: List<List<String>>): TabularPriceData {
        require(matrix.isNotEmpty()) { "الملف لا يحتوي على بيانات." }
        val headerIndex = detectHeaderIndex(matrix)
        val headerRow = matrix[headerIndex]
        val headers = headerRow.mapIndexed { index, value -> value.trim().ifBlank { "عمود ${index + 1}" } }
        val rows = matrix.drop(headerIndex + 1).map { it.padTo(headers.size) }
        return TabularPriceData(
            sourceFileName = fileName,
            headerRowNumber = headerIndex + 1,
            headers = headers,
            rows = rows,
        )
    }

    private fun detectHeaderIndex(matrix: List<List<String>>): Int {
        val hints = listOf(
            "الصنف", "اسم الصنف", "البيان", "الوصف", "item", "name", "description",
            "السعر", "price", "list price", "كود", "code", "sku", "الوحده", "unit", "خصم", "discount",
        ).map(ImportText::normalizeItemName)
        val scored: List<Pair<Int, Int>> = matrix.take(10).mapIndexed { index, row ->
            val score = row.count { cell ->
                val normalized = ImportText.normalizeItemName(cell)
                normalized.isNotBlank() && hints.any { hint -> normalized == hint || normalized.contains(hint) }
            }
            index to score
        }
        val best = scored.maxByOrNull { it.second } ?: return 0
        return if (best.second >= 2) best.first else 0
    }

    private fun readSharedStrings(bytes: ByteArray): List<String> {
        val doc = newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val nodes = doc.getElementsByTagNameNS("*", "si")
        return (0 until nodes.length).map { index ->
            val si = nodes.item(index) as Element
            val tNodes = si.getElementsByTagNameNS("*", "t")
            buildString {
                for (i in 0 until tNodes.length) append(tNodes.item(i).textContent.orEmpty())
            }
        }
    }

    private fun readWorksheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val doc = newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val rowNodes = doc.getElementsByTagNameNS("*", "row")
        val result = mutableListOf<List<String>>()
        for (rowIndex in 0 until rowNodes.length) {
            val row = rowNodes.item(rowIndex) as Element
            val cellNodes = row.getElementsByTagNameNS("*", "c")
            val values = sortedMapOf<Int, String>()
            for (i in 0 until cellNodes.length) {
                val cell = cellNodes.item(i) as Element
                val ref = cell.getAttribute("r")
                val col = excelColumnIndex(ref)
                val type = cell.getAttribute("t")
                val v = cell.getElementsByTagNameNS("*", "v").item(0)?.textContent.orEmpty()
                val value = when (type) {
                    "s" -> v.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
                    "inlineStr" -> cell.getElementsByTagNameNS("*", "t").item(0)?.textContent.orEmpty()
                    "b" -> if (v == "1") "TRUE" else "FALSE"
                    else -> v
                }
                values[col] = value.trim()
            }
            if (values.isNotEmpty()) {
                val max = values.lastKey()
                result += (0..max).map { values[it].orEmpty() }
            }
        }
        return result
    }

    private fun newDocumentBuilder() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        try { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } catch (_: Exception) {}
        try { setFeature("http://xml.org/sax/features/external-general-entities", false) } catch (_: Exception) {}
        try { setFeature("http://xml.org/sax/features/external-parameter-entities", false) } catch (_: Exception) {}
    }.newDocumentBuilder()

    private fun excelColumnIndex(reference: String): Int {
        var value = 0
        for (ch in reference) {
            if (!ch.isLetter()) break
            value = value * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return (value - 1).coerceAtLeast(0)
    }

    private fun detectDelimiter(line: String): Char {
        val candidates = listOf(',', ';', '\t')
        return candidates.maxByOrNull { delimiter -> countOutsideQuotes(line, delimiter) } ?: ','
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var quoted = false
        var count = 0
        var i = 0
        while (i < line.length) {
            when (line[i]) {
                '"' -> if (quoted && i + 1 < line.length && line[i + 1] == '"') i++ else quoted = !quoted
                delimiter -> if (!quoted) count++
            }
            i++
        }
        return count
    }

    private fun parseDelimited(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' && quoted && i + 1 < text.length && text[i + 1] == '"' -> {
                    cell.append('"')
                    i++
                }
                ch == '"' -> quoted = !quoted
                ch == delimiter && !quoted -> {
                    row.add(cell.toString().trim())
                    cell.clear()
                }
                (ch == '\n' || ch == '\r') && !quoted -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString().trim())
                    cell.clear()
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString().trim())
            rows.add(row)
        }
        return rows
    }

    private fun InputStream.readLimited(maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            require(total <= maxBytes) { "حجم الملف أكبر من الحد المسموح للاستيراد." }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun List<String>.padTo(size: Int): List<String> = if (this.size >= size) take(size) else this + List(size - this.size) { "" }
}
