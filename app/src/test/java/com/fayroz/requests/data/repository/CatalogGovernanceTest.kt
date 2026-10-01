package com.fayroz.requests.data.repository

import com.fayroz.requests.data.model.ItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogGovernanceTest {

    private fun item(
        id: Long,
        name: String,
        marketName: String = "",
    ) = ItemEntity(
        id = id,
        code = "ITM-$id",
        name = name,
        marketName = marketName,
        normalizedName = name,
        defaultUnit = "عدد",
    )

    @Test
    fun electricalWireTypesAreGroupedBeforeCablesAndOrderedBySection() {
        val items = listOf(
            item(1, "كابل نحاس 4×10 مم²"),
            item(2, "H07V-R 450/750V 1×10 مم²", "سلك نحاس مجدول 10 مم²"),
            item(3, "H07V-R 450/750V 1×2.5 مم²", "سلك نحاس مجدول 2.5 مم²"),
            item(4, "H07V-U 450/750V 1×2.5 مم²", "سلك نحاس مصمت 2.5 مم²"),
        ).sortedWith(CatalogGovernance.comparator("كهرباء - تأسيس"))

        assertEquals(4L, items[0].id)
        assertEquals(3L, items[1].id)
        assertEquals(2L, items[2].id)
        assertEquals(1L, items[3].id)
    }

    @Test
    fun dripEmitterIsCountNotMeter() {
        val override = CatalogGovernance.exactOverrides["نقاطة 4 لتر/ساعة"]
        assertEquals("عدد", override?.unit)
        assertEquals("شبكات مياه وري", override?.category)
    }

    @Test
    fun oldMixedCategoryRoutesPlasterItemsToPlaster() {
        assertEquals(
            "محارة وبياض",
            CatalogGovernance.targetForLegacyCategory("مباني ومحارة", "محارة أسمنتية جاهزة"),
        )
        assertEquals(
            "مواد بناء ومباني",
            CatalogGovernance.targetForLegacyCategory("مباني ومحارة", "طوب أحمر"),
        )
    }

    @Test
    fun ambiguousLegacyWireStaysArchivedInsteadOfChangingHistoricUnit() {
        assertTrue("سلك نحاس 25 مم²" in CatalogGovernance.archiveNames)
        assertTrue("سلك أرضي 50 مم²" in CatalogGovernance.archiveNames)
    }
}
