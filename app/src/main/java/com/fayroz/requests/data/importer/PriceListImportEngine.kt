package com.fayroz.requests.data.importer

import com.fayroz.requests.data.model.ItemEntity
import kotlin.math.max

object PriceListImportEngine {
    private val nameHeaders = listOf("الصنف", "اسم الصنف", "البيان", "الوصف", "item", "item name", "name", "description", "product")
    private val codeHeaders = listOf("كود", "كود الصنف", "رقم الصنف", "code", "item code", "sku")
    private val unitHeaders = listOf("الوحده", "الوحدة", "unit", "uom")
    private val priceHeaders = listOf("السعر", "سعر", "سعر الليسته", "سعر القائمة", "price", "list price", "retail price", "unit price")
    private val discountHeaders = listOf("الخصم", "خصم", "discount", "disc", "discount %")
    private val brandHeaders = listOf("الماركه", "الماركة", "brand", "manufacturer", "make")
    private val specificationHeaders = listOf("المواصفه", "المواصفة", "تفاصيل", "spec", "specification", "details")

    fun detectMapping(headers: List<String>): ImportColumnMapping {
        fun find(candidates: List<String>): Int? {
            val normalizedCandidates = candidates.map(ImportText::normalizeItemName).toSet()
            return headers.indexOfFirst { ImportText.normalizeItemName(it) in normalizedCandidates }.takeIf { it >= 0 }
                ?: headers.indexOfFirst { header ->
                    val h = ImportText.normalizeItemName(header)
                    normalizedCandidates.any { candidate -> h.contains(candidate) || candidate.contains(h) }
                }.takeIf { it >= 0 }
        }

        val name = find(nameHeaders) ?: 0
        val price = find(priceHeaders) ?: headers.indices.firstOrNull { it != name } ?: name
        return ImportColumnMapping(
            itemNameColumn = name,
            listPriceColumn = price,
            codeColumn = find(codeHeaders),
            unitColumn = find(unitHeaders),
            discountColumn = find(discountHeaders),
            brandColumn = find(brandHeaders),
            specificationColumn = find(specificationHeaders),
        )
    }

    fun buildCandidates(
        data: TabularPriceData,
        mapping: ImportColumnMapping,
        existingItems: List<ItemEntity>,
    ): List<PriceImportCandidate> {
        val byCode = existingItems.filter { it.code.isNotBlank() }.associateBy { ImportText.normalizeCode(it.code) }
        val byName = buildMap<String, ItemEntity> {
            existingItems.forEach { item ->
                put(ImportText.normalizeItemName(item.name), item)
                if (item.marketName.isNotBlank()) {
                    put(ImportText.normalizeItemName(item.marketName), item)
                }
            }
        }

        return data.rows.mapIndexedNotNull { index, row ->
            if (row.all { it.isBlank() }) return@mapIndexedNotNull null
            val name = row.cell(mapping.itemNameColumn).trim()
            val code = row.cell(mapping.codeColumn).trim()
            val unit = row.cell(mapping.unitColumn).trim()
            val brand = row.cell(mapping.brandColumn).trim()
            val specification = row.cell(mapping.specificationColumn).trim()
            val price = ImportText.parseNumber(row.cell(mapping.listPriceColumn))
            val discount = mapping.discountColumn?.let { ImportText.parseNumber(row.cell(it)) }

            if (name.isBlank() || price == null || price < 0.0 || (discount != null && discount !in 0.0..100.0)) {
                return@mapIndexedNotNull PriceImportCandidate(
                    sourceRowNumber = data.headerRowNumber + index + 1,
                    itemName = name,
                    itemCode = code,
                    unit = unit,
                    brand = brand,
                    specification = specification,
                    listPrice = price ?: 0.0,
                    discountPercent = discount,
                    matchKind = ImportMatchKind.INVALID,
                    validationMessage = when {
                        name.isBlank() -> "اسم الصنف فارغ"
                        price == null -> "السعر غير صالح"
                        price < 0.0 -> "السعر لا يمكن أن يكون سالبًا"
                        else -> "نسبة الخصم يجب أن تكون بين 0 و100"
                    },
                )
            }

            val exactByCode = code.takeIf { it.isNotBlank() }?.let { byCode[ImportText.normalizeCode(it)] }
            if (exactByCode != null) {
                return@mapIndexedNotNull candidate(data.headerRowNumber + index + 1, name, code, unit, brand, specification, price, discount, exactByCode, ImportMatchKind.EXACT_CODE, 1.0)
            }

            val normalizedName = ImportText.normalizeItemName(name)
            val exactByName = byName[normalizedName]
            if (exactByName != null) {
                return@mapIndexedNotNull candidate(data.headerRowNumber + index + 1, name, code, unit, brand, specification, price, discount, exactByName, ImportMatchKind.EXACT_NAME, 1.0)
            }

            val ranked = existingItems.asSequence()
                .map { item ->
                    val technicalScore = similarity(normalizedName, ImportText.normalizeItemName(item.name))
                    val marketScore = if (item.marketName.isBlank()) 0.0
                    else similarity(normalizedName, ImportText.normalizeItemName(item.marketName))
                    item to maxOf(technicalScore, marketScore)
                }
                .filter { it.second >= 0.58 }
                .sortedByDescending { it.second }
                .take(2)
                .toList()
            val top = ranked.firstOrNull()
            val second = ranked.getOrNull(1)
            val clearSuggestion = top != null && top.second >= 0.72 && (second == null || top.second - second.second >= 0.06)

            if (clearSuggestion) {
                PriceImportCandidate(
                    sourceRowNumber = data.headerRowNumber + index + 1,
                    itemName = name,
                    itemCode = code,
                    unit = unit,
                    brand = brand,
                    specification = specification,
                    listPrice = price,
                    discountPercent = discount,
                    suggestedItemId = top!!.first.id,
                    suggestedItemName = top.first.marketName.ifBlank { top.first.name },
                    matchKind = ImportMatchKind.SUGGESTED,
                    confidence = top.second,
                )
            } else {
                PriceImportCandidate(
                    sourceRowNumber = data.headerRowNumber + index + 1,
                    itemName = name,
                    itemCode = code,
                    unit = unit,
                    brand = brand,
                    specification = specification,
                    listPrice = price,
                    discountPercent = discount,
                    matchKind = ImportMatchKind.UNMATCHED,
                    confidence = top?.second ?: 0.0,
                )
            }
        }
    }

    private fun candidate(
        sourceRowNumber: Int,
        name: String,
        code: String,
        unit: String,
        brand: String,
        specification: String,
        price: Double,
        discount: Double?,
        item: ItemEntity,
        kind: ImportMatchKind,
        confidence: Double,
    ) = PriceImportCandidate(
        sourceRowNumber = sourceRowNumber,
        itemName = name,
        itemCode = code,
        unit = unit,
        brand = brand,
        specification = specification,
        listPrice = price,
        discountPercent = discount,
        matchedItemId = item.id,
        matchedItemName = item.marketName.ifBlank { item.name },
        matchKind = kind,
        confidence = confidence,
    )

    private fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0
        val aTokens = a.split(' ').filter { it.isNotBlank() }.toSet()
        val bTokens = b.split(' ').filter { it.isNotBlank() }.toSet()
        val intersection = aTokens.intersect(bTokens).size.toDouble()
        val union = aTokens.union(bTokens).size.toDouble().coerceAtLeast(1.0)
        val jaccard = intersection / union
        val edit = 1.0 - levenshtein(a, b).toDouble() / max(a.length, b.length).coerceAtLeast(1)
        return jaccard * 0.62 + edit * 0.38
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            val current = IntArray(b.length + 1)
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }

    private fun List<String>.cell(index: Int?): String = index?.let { getOrNull(it) }.orEmpty()
}
