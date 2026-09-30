package com.fayroz.requests.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingCopiesComparisonTest {
    private val project = ProjectEntity(id = 1, name = "مشروع")
    private val sheet = RequestSheetEntity(id = 1, projectId = 1, sheetNumber = "001")

    private fun line(
        id: Long,
        itemName: String,
        quantity: Double,
        price: Double?,
    ) = PricingCopyLineDetail(
        copyLineId = id,
        copyId = 1,
        requestLineId = id,
        itemId = id,
        itemName = itemName,
        categoryId = null,
        quantity = quantity,
        unit = "عدد",
        usage = "",
        brand = "",
        unitPrice = price,
    )

    private fun copy(
        id: Long,
        name: String,
        prices: List<Double?>,
    ) = PricingCopyDetail(
        copy = PricingCopyEntity(id = id, sheetId = 1, placeName = name),
        sheet = sheet,
        project = project,
        lines = prices.mapIndexed { index, price ->
            line(index.toLong() + 1, "صنف §{index + 1}", 1.0, price)
        },
    )

    @Test
    fun incompleteCopyIsExcludedFromCompleteTotals() {
        val incomplete = copy(1, "ناقص", listOf(10.0, null))
        val complete = copy(2, "كامل", listOf(20.0, 20.0))
        val comparison = PricingCopiesComparison(sheet, project, listOf(incomplete, complete))

        assertTrue(!incomplete.complete)
        assertTrue(complete.complete)
        assertEquals(1, comparison.completeCopies.size)
        assertEquals(40.0, comparison.lowestCompleteTotal!!, 0.001)
    }

    @Test
    fun bestMixUsesLowestAvailablePricePerLine() {
        val first = copy(1, "أ", listOf(10.0, 50.0))
        val second = copy(2, "ب", listOf(20.0, 30.0))
        val comparison = PricingCopiesComparison(sheet, project, listOf(first, second))

        assertEquals(40.0, comparison.bestMixTotal!!, 0.001)
        assertEquals(1, comparison.cheapestLineCount(first.copy.id))
        assertEquals(1, comparison.cheapestLineCount(second.copy.id))
    }
}
