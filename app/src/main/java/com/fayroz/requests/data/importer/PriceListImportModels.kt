package com.fayroz.requests.data.importer

data class TabularPriceData(
    val sourceFileName: String,
    val headerRowNumber: Int = 1,
    val headers: List<String>,
    val rows: List<List<String>>,
)

data class ImportColumnMapping(
    val itemNameColumn: Int,
    val listPriceColumn: Int,
    val codeColumn: Int? = null,
    val unitColumn: Int? = null,
    val discountColumn: Int? = null,
    val brandColumn: Int? = null,
    val specificationColumn: Int? = null,
)

enum class ImportMatchKind {
    EXACT_CODE,
    EXACT_NAME,
    SUGGESTED,
    UNMATCHED,
    INVALID,
}

data class PriceImportCandidate(
    val sourceRowNumber: Int,
    val itemName: String,
    val itemCode: String,
    val unit: String,
    val brand: String,
    val specification: String,
    val listPrice: Double,
    val discountPercent: Double?,
    val matchedItemId: Long? = null,
    val matchedItemName: String? = null,
    val suggestedItemId: Long? = null,
    val suggestedItemName: String? = null,
    val matchKind: ImportMatchKind,
    val confidence: Double = 0.0,
    val validationMessage: String = "",
)

data class ResolvedPriceImportRow(
    val sourceRowNumber: Int,
    val itemName: String,
    val itemCode: String,
    val unit: String,
    val brand: String,
    val specification: String,
    val listPrice: Double,
    val discountPercent: Double?,
    val existingItemId: Long? = null,
    val createNewItem: Boolean = false,
)

data class PriceImportSummary(
    val importedRows: Int,
    val insertedPrices: Int,
    val updatedPrices: Int,
    val createdItems: Int,
    val skippedRows: Int,
)
