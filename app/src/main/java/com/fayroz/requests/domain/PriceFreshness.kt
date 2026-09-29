package com.fayroz.requests.domain

import java.util.concurrent.TimeUnit

enum class PriceFreshnessStatus { FRESH, REVIEW, OLD }

data class PriceFreshness(
    val daysOld: Long,
    val status: PriceFreshnessStatus,
) {
    val label: String
        get() = when {
            daysOld <= 0 -> "اليوم"
            daysOld == 1L -> "منذ يوم"
            else -> "منذ $daysOld يوم"
        }

    val statusLabel: String
        get() = when (status) {
            PriceFreshnessStatus.FRESH -> "حديث"
            PriceFreshnessStatus.REVIEW -> "يحتاج مراجعة"
            PriceFreshnessStatus.OLD -> "قديم"
        }
}

object PriceFreshnessEngine {
    fun evaluate(priceDate: Long, now: Long = System.currentTimeMillis()): PriceFreshness {
        val ageMs = (now - priceDate).coerceAtLeast(0L)
        val days = TimeUnit.MILLISECONDS.toDays(ageMs)
        val status = when {
            days <= 30 -> PriceFreshnessStatus.FRESH
            days <= 90 -> PriceFreshnessStatus.REVIEW
            else -> PriceFreshnessStatus.OLD
        }
        return PriceFreshness(days, status)
    }
}
