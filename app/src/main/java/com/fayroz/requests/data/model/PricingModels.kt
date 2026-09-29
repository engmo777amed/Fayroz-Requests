package com.fayroz.requests.data.model

data class PriceListSummary(
    val id: Long,
    val supplierId: Long,
    val name: String,
    val effectiveDate: Long,
    val sourceReference: String,
    val entryCount: Int,
)

data class PriceListEntryDetail(
    val priceId: Long,
    val priceListId: Long?,
    val supplierId: Long,
    val itemId: Long,
    val itemName: String,
    val itemCode: String,
    val unit: String,
    val listPrice: Double,
    val appliedDiscountPercent: Double,
    val netPrice: Double,
    val priceDate: Long,
    val source: PriceSource,
    val notes: String,
)

data class PricingOffer(
    val supplierPriceId: Long,
    val supplierId: Long,
    val supplierName: String,
    val itemId: Long,
    val listPrice: Double,
    val discountPercent: Double,
    val netUnitPrice: Double,
    val quantity: Double,
    val totalNet: Double,
    val priceDate: Long,
    val source: PriceSource,
)

data class PricingLineComparison(
    val lineId: Long,
    val itemId: Long,
    val itemName: String,
    val itemCode: String,
    val quantity: Double,
    val unit: String,
    val usage: String,
    val lineDescription: String,
    val offers: List<PricingOffer>,
)

data class SupplierPricingTotal(
    val supplierId: Long,
    val supplierName: String,
    val coveredLines: Int,
    val totalLines: Int,
    val totalNet: Double,
) {
    val complete: Boolean get() = totalLines > 0 && coveredLines == totalLines
}

data class SheetPricingComparison(
    val sheet: RequestSheetEntity,
    val project: ProjectEntity,
    val lines: List<PricingLineComparison>,
    val supplierTotals: List<SupplierPricingTotal>,
) {
    val pricedLineCount: Int get() = lines.count { it.offers.isNotEmpty() }
    val totalLineCount: Int get() = lines.size
    val bestMixTotal: Double? get() = if (lines.isNotEmpty() && lines.all { it.offers.isNotEmpty() }) {
        lines.sumOf { line -> line.offers.minOf { it.totalNet } }
    } else null
}

data class ItemPriceHistoryDetail(
    val priceId: Long,
    val itemId: Long,
    val supplierId: Long,
    val supplierName: String,
    val priceListName: String?,
    val listPrice: Double,
    val appliedDiscountPercent: Double,
    val netPrice: Double,
    val priceDate: Long,
    val source: PriceSource,
    val notes: String,
)


data class PricingCopySummary(
    val copyId: Long,
    val sheetId: Long,
    val placeName: String,
    val quoteDate: Long,
    val lineCount: Int,
    val pricedCount: Int,
    val total: Double,
)

data class PricingCopyLineDetail(
    val copyLineId: Long,
    val copyId: Long,
    val requestLineId: Long,
    val itemId: Long,
    val itemName: String,
    val categoryId: Long?,
    val quantity: Double,
    val unit: String,
    val usage: String,
    val brand: String,
    val unitPrice: Double?,
)

data class PricingCopyDetail(
    val copy: PricingCopyEntity,
    val sheet: RequestSheetEntity,
    val project: ProjectEntity,
    val lines: List<PricingCopyLineDetail>,
) {
    val total: Double
        get() = lines.sumOf { (it.unitPrice ?: 0.0) * it.quantity }
    val pricedCount: Int
        get() = lines.count { it.unitPrice != null }
}

data class PricingCopiesComparison(
    val sheet: RequestSheetEntity,
    val project: ProjectEntity,
    val copies: List<PricingCopyDetail>,
)
