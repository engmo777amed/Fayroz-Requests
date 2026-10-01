package com.fayroz.requests.domain

import com.fayroz.requests.data.importer.ImportColumnMapping
import com.fayroz.requests.data.importer.ImportMatchKind
import com.fayroz.requests.data.importer.PriceListImportEngine
import com.fayroz.requests.data.importer.PriceListFileParser
import com.fayroz.requests.data.importer.TabularPriceData
import com.fayroz.requests.data.model.ItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class PriceListImportEngineTest {
    private val items = listOf(
        ItemEntity(
            id = 1,
            code = "SW25",
            name = "H07V-K 450/750V 1×2.5 مم²",
            marketName = "سلك نحاس شعر 2.5 مم²",
            normalizedName = "h07v-k 450/750v 1×2.5 مم²",
            defaultUnit = "لفة",
        ),
        ItemEntity(id = 2, code = "P20", name = "خرطوم 20 مم", normalizedName = "خرطوم 20 مم", defaultUnit = "لفة"),
    )

    @Test
    fun matchesExistingItemByCodeBeforeName() {
        val data = TabularPriceData(
            sourceFileName = "test.csv",
            headers = listOf("code", "item", "price"),
            rows = listOf(listOf("SW25", "اسم مختلف", "3100")),
        )
        val candidates = PriceListImportEngine.buildCandidates(
            data,
            ImportColumnMapping(itemNameColumn = 1, listPriceColumn = 2, codeColumn = 0),
            items,
        )
        assertEquals(ImportMatchKind.EXACT_CODE, candidates.single().matchKind)
        assertEquals(1L, candidates.single().matchedItemId)
    }

    @Test
    fun normalizesArabicDigitsForNameMatching() {
        val data = TabularPriceData(
            sourceFileName = "test.csv",
            headers = listOf("item", "price"),
            rows = listOf(listOf("سلك نحاس شعر ٢٫٥ مم²", "3100")),
        )
        val candidates = PriceListImportEngine.buildCandidates(
            data,
            ImportColumnMapping(itemNameColumn = 0, listPriceColumn = 1),
            items,
        )
        assertEquals(ImportMatchKind.EXACT_NAME, candidates.single().matchKind)
    }


    @Test
    fun matchesExistingItemByMarketName() {
        val data = TabularPriceData(
            sourceFileName = "market.csv",
            headers = listOf("item", "price"),
            rows = listOf(listOf("سلك نحاس شعر 2.5 مم²", "3100")),
        )
        val candidate = PriceListImportEngine.buildCandidates(
            data,
            ImportColumnMapping(itemNameColumn = 0, listPriceColumn = 1),
            items,
        ).single()

        assertEquals(ImportMatchKind.EXACT_NAME, candidate.matchKind)
        assertEquals(1L, candidate.matchedItemId)
    }

    @Test
    fun warnsWhenSupplierPriceUnitDiffersFromCatalogUnit() {
        val data = TabularPriceData(
            sourceFileName = "units.csv",
            headers = listOf("item", "unit", "price"),
            rows = listOf(listOf("سلك نحاس شعر 2.5 مم²", "م", "35")),
        )
        val candidate = PriceListImportEngine.buildCandidates(
            data,
            ImportColumnMapping(itemNameColumn = 0, listPriceColumn = 2, unitColumn = 1),
            items,
        ).single()

        assertEquals(ImportMatchKind.EXACT_NAME, candidate.matchKind)
        assertTrue(candidate.validationMessage.contains("مختلفة"))
    }

    @Test
    fun csvParserKeepsQuotedThousandsSeparatorInOneCell() {
        val csv = "code,item,price\nSW25,سلك 2.5 مم,\"3,100\"\n"
        val data = PriceListFileParser.parse("prices.csv", ByteArrayInputStream(csv.toByteArray()))
        assertEquals("3,100", data.rows.single()[2])
        assertTrue(data.headers.contains("price"))
    }
}
