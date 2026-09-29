package com.fayroz.requests.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class PriceFreshnessEngineTest {
    private val now = 2_000_000_000_000L

    @Test
    fun freshPriceWithin30Days() {
        val result = PriceFreshnessEngine.evaluate(now - TimeUnit.DAYS.toMillis(10), now)
        assertEquals(10L, result.daysOld)
        assertEquals(PriceFreshnessStatus.FRESH, result.status)
    }

    @Test
    fun reviewPriceBetween31And90Days() {
        val result = PriceFreshnessEngine.evaluate(now - TimeUnit.DAYS.toMillis(45), now)
        assertEquals(PriceFreshnessStatus.REVIEW, result.status)
    }

    @Test
    fun oldPriceAfter90Days() {
        val result = PriceFreshnessEngine.evaluate(now - TimeUnit.DAYS.toMillis(120), now)
        assertEquals(PriceFreshnessStatus.OLD, result.status)
    }
}
