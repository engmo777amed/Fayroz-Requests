package com.fayroz.requests.domain

import com.fayroz.requests.data.model.DiscountScope
import com.fayroz.requests.data.model.SupplierDiscountRuleEntity
import kotlin.math.max
import kotlin.math.min

object PricingEngine {
    fun selectDiscountPercent(
        supplierDefault: Double,
        categoryId: Long?,
        itemId: Long,
        rules: List<SupplierDiscountRuleEntity>,
    ): Double {
        val itemRule = rules.firstOrNull {
            it.active && it.scope == DiscountScope.ITEM && it.itemId == itemId
        }
        if (itemRule != null) return clamp(itemRule.discountPercent)

        val categoryRule = rules.firstOrNull {
            it.active && it.scope == DiscountScope.CATEGORY && it.categoryId == categoryId
        }
        if (categoryRule != null) return clamp(categoryRule.discountPercent)

        return clamp(supplierDefault)
    }

    fun netPrice(listPrice: Double, discountPercent: Double): Double {
        val safePrice = max(0.0, listPrice)
        return safePrice * (1.0 - clamp(discountPercent) / 100.0)
    }

    private fun clamp(percent: Double): Double = min(100.0, max(0.0, percent))
}
