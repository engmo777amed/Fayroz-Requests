package com.fayroz.requests.data.model

data class RequestSheetSummary(
    val id: Long,
    val projectId: Long,
    val projectName: String,
    val sheetNumber: String,
    val sheetDate: Long,
    val trade: String,
    val craftsmanName: String,
    val workLocation: String,
    val notes: String,
    val lineCount: Int,
    val pricingCopyCount: Int,
    val completePricingCopyCount: Int,
) {
    val hasPricing: Boolean get() = pricingCopyCount > 0
    val hasCompletePricing: Boolean get() = completePricingCopyCount > 0
}

data class RequestLineDetail(
    val lineId: Long,
    val itemId: Long,
    val itemName: String,
    val itemCode: String,
    val quantity: Double,
    val unit: String,
    val brand: String,
    val usage: String,
    val lineDescription: String,
    val notes: String,
)

data class RequestLineDraft(
    val existingLineId: Long? = null,
    val existingItemId: Long? = null,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val brand: String = "",
    val usage: String = "",
    val lineDescription: String = "",
    val notes: String = "",
)

data class RequestSheetDraft(
    val id: Long = 0,
    val projectId: Long,
    val sheetNumber: String,
    val sheetDate: Long,
    val trade: String = "",
    val craftsmanName: String = "",
    val workLocation: String = "",
    val attachmentUri: String? = null,
    val notes: String = "",
    val lines: List<RequestLineDraft>,
)
