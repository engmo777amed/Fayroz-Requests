package com.fayroz.requests.domain

import com.fayroz.requests.data.model.DiscountScope
import com.fayroz.requests.data.model.SupplierDiscountRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PricingEngineTest {
    @Test
    fun item_discount_has_priority_over_category_and_supplier() {
        val rules = listOf(
            SupplierDiscountRuleEntity(supplierId = 1, scope = DiscountScope.CATEGORY, categoryId = 3, discountPercent = 10.0),
            SupplierDiscountRuleEntity(supplierId = 1, scope = DiscountScope.ITEM, itemId = 9, discountPercent = 15.0),
        )
        val selected = PricingEngine.selectDiscountPercent(5.0, 3, 9, rules)
        assertEquals(15.0, selected, 0.001)
    }

    @Test
    fun category_discount_has_priority_over_supplier_default() {
        val rules = listOf(
            SupplierDiscountRuleEntity(supplierId = 1, scope = DiscountScope.CATEGORY, categoryId = 3, discountPercent = 11.0),
        )
        val selected = PricingEngine.selectDiscountPercent(7.0, 3, 9, rules)
        assertEquals(11.0, selected, 0.001)
    }

    @Test
    fun supplier_default_is_used_when_no_specific_rule_exists() {
        assertEquals(8.5, PricingEngine.selectDiscountPercent(8.5, null, 9, emptyList()), 0.001)
    }

    @Test
    fun net_price_is_calculated_from_list_price_and_discount() {
        assertEquals(2728.0, PricingEngine.netPrice(3100.0, 12.0), 0.001)
    }

    @Test
    fun discount_is_clamped_to_valid_range() {
        assertEquals(0.0, PricingEngine.netPrice(1000.0, 150.0), 0.001)
        assertEquals(1000.0, PricingEngine.netPrice(1000.0, -20.0), 0.001)
    }
}
