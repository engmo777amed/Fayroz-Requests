package com.fayroz.requests.data.repository

import androidx.room.withTransaction
import com.fayroz.requests.data.db.FayrozDatabase
import com.fayroz.requests.data.backup.DatabaseBackup
import com.fayroz.requests.data.importer.*
import com.fayroz.requests.data.model.*
import com.fayroz.requests.domain.PricingEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.UUID

class FayrozRepository(private val database: FayrozDatabase) {
    private val dao = database.dao()

    val projects: Flow<List<ProjectEntity>> = dao.observeProjects()
    val categories: Flow<List<CategoryEntity>> = dao.observeCategories()
    val items: Flow<List<ItemEntity>> = dao.observeItems()
    val categoryBrands: Flow<List<CategoryBrandEntity>> = dao.observeCategoryBrands()
    val requestBrands: Flow<List<String>> = dao.observeRequestBrands()
    val sheetSummaries: Flow<List<RequestSheetSummary>> = dao.observeSheetSummaries()
    val suppliers: Flow<List<SupplierEntity>> = dao.observeSuppliers()

    fun writeBackup(output: java.io.OutputStream) = DatabaseBackup.write(database, output)
    suspend fun restoreBackup(input: java.io.InputStream) = DatabaseBackup.restore(database, input)

    suspend fun addProject(name: String, clientName: String = "", location: String = ""): Long {
        return dao.insertProject(
            ProjectEntity(
                name = name.trim(),
                clientName = clientName.trim(),
                location = location.trim(),
            )
        )
    }

    suspend fun addProject(project: ProjectEntity): Long = dao.insertProject(
        project.copy(
            name = project.name.trim(),
            clientName = project.clientName.trim(),
            location = project.location.trim(),
            projectType = project.projectType.trim(),
            projectStatus = project.projectStatus.trim(),
            notes = project.notes.trim(),
        )
    )

    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun deleteProject(project: ProjectEntity) = dao.deleteProject(project)

    suspend fun hasAnyItems(): Boolean = dao.countItems() > 0

    suspend fun ensureStarterCatalog() = database.withTransaction {
        val categoryIds = mutableMapOf<String, Long>()
        val catalogCategories = (StarterCatalog.categories + MarketCatalogExpansion.categories).distinct()
        catalogCategories.forEachIndexed { index, categoryName ->
            val existing = dao.findCategoryByName(categoryName)
            val id = existing?.id ?: dao.insertCategory(
                CategoryEntity(
                    name = categoryName,
                    sortOrder = index,
                )
            )
            categoryIds[categoryName] = id
        }

        categoryIds["أخرى"]?.let { dao.assignUncategorizedItems(it) }

        val catalogItems = StarterCatalog.items + MarketCatalogExpansion.items
        catalogItems.forEach { starter ->
            val normalized = ImportText.normalizeItemName(starter.name)
            val existing = dao.findItemByNormalizedName(normalized)
            if (existing == null) {
                dao.insertItem(
                    ItemEntity(
                        code = generateItemCode(),
                        name = starter.name,
                        marketName = StarterCatalog.marketName(starter.name),
                        normalizedName = normalized,
                        categoryId = categoryIds[starter.category],
                        defaultUnit = starter.unit,
                        specification = starter.specification,
                    )
                )
            } else {
                val seededMarketName = StarterCatalog.marketName(starter.name)
                val shouldUpdateSpecification =
                    existing.specification.isBlank() && starter.specification.isNotBlank()
                val shouldUpdateMarketName =
                    existing.marketName.isBlank() && seededMarketName.isNotBlank() && seededMarketName != starter.name
                val shouldUpdateCategory =
                    existing.categoryId == null && categoryIds[starter.category] != null
                if (shouldUpdateSpecification || shouldUpdateMarketName || shouldUpdateCategory) {
                    dao.updateItem(
                        existing.copy(
                            marketName = if (shouldUpdateMarketName) seededMarketName else existing.marketName,
                            categoryId = existing.categoryId ?: categoryIds[starter.category],
                            specification = if (shouldUpdateSpecification) starter.specification else existing.specification,
                        )
                    )
                }
            }
        }
    }

    suspend fun ensureStarterBrands() = database.withTransaction {
        val catalogCategories = (StarterCatalog.categories + MarketCatalogExpansion.categories).distinct()
        catalogCategories.forEach { categoryName ->
            val category = dao.findCategoryByName(categoryName) ?: return@forEach
            BrandCatalog.byCategory[categoryName].orEmpty().forEach { brandName ->
                if (dao.findCategoryBrand(category.id, brandName) == null) {
                    dao.insertCategoryBrand(
                        CategoryBrandEntity(
                            categoryId = category.id,
                            name = brandName,
                        )
                    )
                }
            }
        }
    }

    suspend fun reorderCategories(orderedIds: List<Long>) = database.withTransaction {
        val byId = dao.getCategories().associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            byId[id]?.let { category ->
                if (category.sortOrder != index) {
                    dao.updateCategory(category.copy(sortOrder = index))
                }
            }
        }
    }

    suspend fun addCategoryBrand(categoryId: Long, name: String): Long {
        val clean = name.trim()
        if (clean.isBlank()) return 0L
        val existing = dao.findCategoryBrand(categoryId, clean)
        if (existing != null) return existing.id
        return dao.insertCategoryBrand(CategoryBrandEntity(categoryId = categoryId, name = clean))
    }

    suspend fun addItem(
        name: String,
        unit: String,
        code: String = "",
        brand: String = "",
        specification: String = "",
        categoryId: Long? = null,
        marketName: String = "",
    ): Long {
        val cleanName = name.trim()
        val normalized = ImportText.normalizeItemName(cleanName)
        val existing = dao.findItemByNormalizedName(normalized)
        if (existing != null) return existing.id

        val finalCode = code.trim().ifBlank { generateItemCode() }
        return dao.insertItem(
            ItemEntity(
                code = finalCode,
                name = cleanName,
                marketName = marketName.trim(),
                normalizedName = normalized,
                categoryId = categoryId,
                defaultUnit = unit.trim(),
                brand = brand.trim(),
                specification = specification.trim(),
            )
        )
    }

    suspend fun updateItemDetails(
        itemId: Long,
        name: String,
        unit: String,
        brand: String = "",
        specification: String = "",
        categoryId: Long? = null,
        marketName: String = "",
    ): String? {
        val current = dao.getItem(itemId) ?: return "الصنف غير موجود."
        val cleanName = name.trim()
        val cleanUnit = unit.trim()
        if (cleanName.isBlank()) return "اسم الصنف مطلوب."
        if (cleanUnit.isBlank()) return "الوحدة مطلوبة."

        val normalized = ImportText.normalizeItemName(cleanName)
        val duplicate = dao.findItemByNormalizedName(normalized)
        if (duplicate != null && duplicate.id != itemId) {
            return "يوجد صنف آخر بنفس الاسم في دليل الأصناف."
        }

        dao.updateItem(
            current.copy(
                name = cleanName,
                marketName = marketName.trim(),
                normalizedName = normalized,
                categoryId = categoryId,
                defaultUnit = cleanUnit,
                brand = brand.trim(),
                specification = specification.trim(),
            )
        )
        return null
    }

    suspend fun deleteItem(itemId: Long): String? {
        val item = dao.getItem(itemId) ?: return "الصنف غير موجود."
        val requestCount = dao.countRequestLinesForItem(itemId)
        val priceCount = dao.countPricesForItem(itemId)
        if (requestCount > 0 || priceCount > 0) {
            val linked = buildList {
                if (requestCount > 0) add("$requestCount بند كشف")
                if (priceCount > 0) add("$priceCount سعر محفوظ")
            }.joinToString(" و ")
            return "لا يمكن حذف الصنف لأنه مرتبط بـ $linked. يمكنك تعديل بياناته بدل الحذف."
        }
        dao.deleteItem(item)
        return null
    }

    suspend fun addSupplier(
        name: String,
        phone: String = "",
        address: String = "",
        specialty: String = "",
        defaultDiscountPercent: Double = 0.0,
        approved: Boolean = true,
        notes: String = "",
    ): Long = dao.insertSupplier(
        SupplierEntity(
            name = name.trim(),
            phone = phone.trim(),
            address = address.trim(),
            specialty = specialty.trim(),
            approved = approved,
            defaultDiscountPercent = defaultDiscountPercent.coerceIn(0.0, 100.0),
            notes = notes.trim(),
        )
    )

    suspend fun updateSupplier(supplier: SupplierEntity) = dao.updateSupplier(
        supplier.copy(defaultDiscountPercent = supplier.defaultDiscountPercent.coerceIn(0.0, 100.0))
    )

    suspend fun deleteSupplier(supplier: SupplierEntity) = dao.deleteSupplier(supplier)
    suspend fun getSupplier(supplierId: Long): SupplierEntity? = dao.getSupplier(supplierId)

    fun priceLists(supplierId: Long): Flow<List<PriceListSummary>> = dao.observePriceListSummaries(supplierId)
    fun priceListEntries(priceListId: Long): Flow<List<PriceListEntryDetail>> = dao.observePriceListEntries(priceListId)
    fun itemPriceHistory(itemId: Long): Flow<List<ItemPriceHistoryDetail>> = dao.observeItemPriceHistory(itemId)

    suspend fun getItem(itemId: Long): ItemEntity? = dao.getItem(itemId)
    suspend fun getPriceList(priceListId: Long): PriceListEntity? = dao.getPriceList(priceListId)
    suspend fun getSheet(sheetId: Long): RequestSheetEntity? = dao.getSheet(sheetId)
    suspend fun getProject(projectId: Long): ProjectEntity? = dao.getProject(projectId)
    suspend fun getRequestLines(sheetId: Long): List<RequestLineDetail> = dao.getRequestLineDetails(sheetId)

    fun pricingCopies(sheetId: Long): Flow<List<PricingCopySummary>> = dao.observePricingCopies(sheetId)

    suspend fun createPricingCopy(
        sheetId: Long,
        placeName: String,
        supplierId: Long? = null,
        createSupplier: Boolean = false,
        quoteNumber: String = "",
        quoteDate: Long = System.currentTimeMillis(),
        notes: String = "",
    ): Long = database.withTransaction {
        val cleanPlace = placeName.trim()
        require(cleanPlace.isNotBlank()) { "اسم المكان أو المورد مطلوب" }
        val sheet = dao.getSheet(sheetId) ?: error("الكشف غير موجود")
        val finalSupplierId = when {
            supplierId != null -> supplierId
            createSupplier -> dao.insertSupplier(SupplierEntity(name = cleanPlace))
            else -> null
        }
        val copyId = dao.insertPricingCopy(
            PricingCopyEntity(
                sheetId = sheet.id,
                placeName = cleanPlace,
                supplierId = finalSupplierId,
                quoteNumber = quoteNumber.trim(),
                quoteDate = quoteDate,
                notes = notes.trim(),
            )
        )
        val lines = dao.getRequestLineDetails(sheetId)
        dao.insertPricingCopyLines(
            lines.map { line ->
                PricingCopyLineEntity(
                    copyId = copyId,
                    requestLineId = line.lineId,
                )
            }
        )
        copyId
    }

    suspend fun updatePricingCopyMetadata(
        copyId: Long,
        placeName: String,
        supplierId: Long?,
        createSupplier: Boolean = false,
        quoteNumber: String = "",
        quoteDate: Long,
        notes: String = "",
    ) = database.withTransaction {
        val current = dao.getPricingCopy(copyId) ?: error("نسخة التسعير غير موجودة")
        val cleanPlace = placeName.trim()
        require(cleanPlace.isNotBlank()) { "اسم المكان أو المورد مطلوب" }
        val finalSupplierId = when {
            supplierId != null -> supplierId
            createSupplier -> dao.insertSupplier(SupplierEntity(name = cleanPlace))
            else -> null
        }
        val updated = current.copy(
            placeName = cleanPlace,
            supplierId = finalSupplierId,
            quoteNumber = quoteNumber.trim(),
            quoteDate = quoteDate,
            notes = notes.trim(),
        )
        dao.updatePricingCopy(updated)
        if (finalSupplierId == null) {
            dao.deletePricesBySourceReferencePrefix("pricing-copy:$copyId:")
        } else {
            dao.getPricingCopyLineDetails(copyId).forEach { line ->
                syncPricingCopyLineToHistory(updated, line.requestLineId, line.unitPrice)
            }
        }
    }

    suspend fun deletePricingCopy(copyId: Long) = database.withTransaction {
        dao.deletePricesBySourceReferencePrefix("pricing-copy:$copyId:")
        dao.getPricingCopy(copyId)?.let { dao.deletePricingCopy(it) }
    }

    suspend fun loadPricingCopyDetail(copyId: Long): PricingCopyDetail? = database.withTransaction {
        val copy = dao.getPricingCopy(copyId) ?: return@withTransaction null
        val sheet = dao.getSheet(copy.sheetId) ?: return@withTransaction null
        val project = dao.getProject(sheet.projectId) ?: return@withTransaction null

        val currentRequestLines = dao.getRequestLineDetails(sheet.id)
        val currentCopyLines = dao.getPricingCopyLineDetails(copyId)
        val existingRequestLineIds = currentCopyLines.map { it.requestLineId }.toSet()
        val missing = currentRequestLines.filter { it.lineId !in existingRequestLineIds }
        if (missing.isNotEmpty()) {
            dao.insertPricingCopyLines(
                missing.map { requestLine ->
                    PricingCopyLineEntity(
                        copyId = copyId,
                        requestLineId = requestLine.lineId,
                    )
                }
            )
        }

        PricingCopyDetail(
            copy = copy,
            sheet = sheet,
            project = project,
            lines = dao.getPricingCopyLineDetails(copyId),
        )
    }

    suspend fun updatePricingCopyLine(
        copyId: Long,
        requestLineId: Long,
        brand: String,
        unitPrice: Double?,
    ) = database.withTransaction {
        val current = dao.getPricingCopyLine(copyId, requestLineId)
        val safePrice = unitPrice?.coerceAtLeast(0.0)
        if (current == null) {
            dao.insertPricingCopyLines(
                listOf(
                    PricingCopyLineEntity(
                        copyId = copyId,
                        requestLineId = requestLineId,
                        brand = brand.trim(),
                        unitPrice = safePrice,
                    )
                )
            )
        } else {
            dao.updatePricingCopyLine(
                current.copy(
                    brand = brand.trim(),
                    unitPrice = safePrice,
                )
            )
        }
        dao.getPricingCopy(copyId)?.let { copy ->
            syncPricingCopyLineToHistory(copy, requestLineId, safePrice)
        }
    }

    private suspend fun syncPricingCopyLineToHistory(
        copy: PricingCopyEntity,
        requestLineId: Long,
        unitPrice: Double?,
    ) {
        val sourceReference = "pricing-copy:${copy.id}:$requestLineId"
        val existing = dao.getPriceBySourceReference(sourceReference)
        val supplierId = copy.supplierId?.takeIf { dao.getSupplier(it) != null }
        val requestLine = dao.getRequestLine(requestLineId)
        if (supplierId == null || requestLine == null || unitPrice == null) {
            existing?.let { dao.deletePrice(it) }
            return
        }

        val note = buildString {
            append("عرض سعر: ${copy.placeName}")
            if (copy.quoteNumber.isNotBlank()) append(" • رقم ${copy.quoteNumber}")
        }
        val value = unitPrice.coerceAtLeast(0.0)
        if (existing == null) {
            dao.insertPrice(
                SupplierPriceEntity(
                    supplierId = supplierId,
                    itemId = requestLine.itemId,
                    listPrice = value,
                    appliedDiscountPercent = 0.0,
                    netPrice = value,
                    priceDate = copy.quoteDate,
                    source = PriceSource.DIRECT_QUOTE,
                    sourceReference = sourceReference,
                    notes = note,
                )
            )
        } else {
            dao.updatePrice(
                existing.copy(
                    supplierId = supplierId,
                    itemId = requestLine.itemId,
                    listPrice = value,
                    appliedDiscountPercent = 0.0,
                    netPrice = value,
                    priceDate = copy.quoteDate,
                    source = PriceSource.DIRECT_QUOTE,
                    notes = note,
                )
            )
        }
    }

    suspend fun loadPricingCopiesComparison(sheetId: Long): PricingCopiesComparison? {
        val sheet = dao.getSheet(sheetId) ?: return null
        val project = dao.getProject(sheet.projectId) ?: return null
        val summaries = dao.observePricingCopies(sheetId).first()
        val copies = summaries.mapNotNull { loadPricingCopyDetail(it.copyId) }
        return PricingCopiesComparison(sheet, project, copies)
    }

    suspend fun addPriceList(
        supplierId: Long,
        name: String,
        effectiveDate: Long = System.currentTimeMillis(),
        sourceReference: String = "",
    ): Long = dao.insertPriceList(
        PriceListEntity(
            supplierId = supplierId,
            name = name.trim(),
            effectiveDate = effectiveDate,
            sourceReference = sourceReference.trim(),
        )
    )

    suspend fun deletePriceList(priceListId: Long) = database.withTransaction {
        val priceList = dao.getPriceList(priceListId) ?: return@withTransaction
        dao.deletePricesForPriceList(priceListId)
        dao.deletePriceList(priceList)
    }

    suspend fun suggestedDiscountPercent(supplierId: Long, itemId: Long): Double {
        val supplier = dao.getSupplier(supplierId) ?: return 0.0
        val item = dao.getItem(itemId) ?: return supplier.defaultDiscountPercent.coerceIn(0.0, 100.0)
        return PricingEngine.selectDiscountPercent(
            supplierDefault = supplier.defaultDiscountPercent,
            categoryId = item.categoryId,
            itemId = itemId,
            rules = dao.getActiveDiscountRules(supplierId),
        )
    }

    suspend fun upsertPriceListEntry(
        priceListId: Long,
        itemId: Long,
        listPrice: Double,
        discountPercent: Double? = null,
        rememberAsItemDiscount: Boolean = false,
        notes: String = "",
    ): Long = database.withTransaction {
        val priceList = dao.getPriceList(priceListId) ?: error("قائمة الأسعار غير موجودة")
        val supplier = dao.getSupplier(priceList.supplierId) ?: error("المورد غير موجود")
        val item = dao.getItem(itemId) ?: error("الصنف غير موجود")

        val automaticDiscount = PricingEngine.selectDiscountPercent(
            supplierDefault = supplier.defaultDiscountPercent,
            categoryId = item.categoryId,
            itemId = item.id,
            rules = dao.getActiveDiscountRules(supplier.id),
        )
        val appliedDiscount = (discountPercent ?: automaticDiscount).coerceIn(0.0, 100.0)
        val safeListPrice = listPrice.coerceAtLeast(0.0)
        val netPrice = PricingEngine.netPrice(safeListPrice, appliedDiscount)

        if (rememberAsItemDiscount) {
            dao.deactivateItemDiscountRules(supplier.id, item.id)
            dao.insertDiscountRule(
                SupplierDiscountRuleEntity(
                    supplierId = supplier.id,
                    scope = DiscountScope.ITEM,
                    itemId = item.id,
                    discountPercent = appliedDiscount,
                )
            )
        }

        val existing = dao.getPriceInListForItem(priceListId, item.id)
        if (existing == null) {
            dao.insertPrice(
                SupplierPriceEntity(
                    supplierId = supplier.id,
                    itemId = item.id,
                    priceListId = priceList.id,
                    listPrice = safeListPrice,
                    appliedDiscountPercent = appliedDiscount,
                    netPrice = netPrice,
                    priceDate = priceList.effectiveDate,
                    source = PriceSource.PRICE_LIST,
                    notes = notes.trim(),
                )
            )
        } else {
            dao.updatePrice(
                existing.copy(
                    listPrice = safeListPrice,
                    appliedDiscountPercent = appliedDiscount,
                    netPrice = netPrice,
                    priceDate = priceList.effectiveDate,
                    notes = notes.trim(),
                )
            )
            existing.id
        }
    }

    suspend fun deletePrice(priceId: Long) {
        dao.getPrice(priceId)?.let { dao.deletePrice(it) }
    }

    suspend fun addDirectQuote(
        supplierId: Long,
        itemId: Long,
        quotedPrice: Double,
        discountPercent: Double? = null,
        rememberAsItemDiscount: Boolean = false,
        notes: String = "",
    ): Long = database.withTransaction {
        val supplier = dao.getSupplier(supplierId) ?: error("المورد غير موجود")
        val item = dao.getItem(itemId) ?: error("الصنف غير موجود")
        val automaticDiscount = PricingEngine.selectDiscountPercent(
            supplierDefault = supplier.defaultDiscountPercent,
            categoryId = item.categoryId,
            itemId = item.id,
            rules = dao.getActiveDiscountRules(supplier.id),
        )
        val appliedDiscount = (discountPercent ?: automaticDiscount).coerceIn(0.0, 100.0)
        val safePrice = quotedPrice.coerceAtLeast(0.0)

        if (rememberAsItemDiscount) {
            dao.deactivateItemDiscountRules(supplier.id, item.id)
            dao.insertDiscountRule(
                SupplierDiscountRuleEntity(
                    supplierId = supplier.id,
                    scope = DiscountScope.ITEM,
                    itemId = item.id,
                    discountPercent = appliedDiscount,
                )
            )
        }

        dao.insertPrice(
            SupplierPriceEntity(
                supplierId = supplier.id,
                itemId = item.id,
                priceListId = null,
                listPrice = safePrice,
                appliedDiscountPercent = appliedDiscount,
                netPrice = PricingEngine.netPrice(safePrice, appliedDiscount),
                priceDate = System.currentTimeMillis(),
                source = PriceSource.DIRECT_QUOTE,
                notes = notes.trim(),
            )
        )
    }

    suspend fun loadPricingComparison(sheetId: Long): SheetPricingComparison? {
        val sheet = dao.getSheet(sheetId) ?: return null
        val project = dao.getProject(sheet.projectId) ?: return null
        val lines = dao.getRequestLineDetails(sheetId)
        if (lines.isEmpty()) {
            return SheetPricingComparison(sheet, project, emptyList(), emptyList())
        }

        val suppliers = dao.getSuppliers().filter { it.approved }.associateBy { it.id }
        val latestPrices = dao.getLatestPricesForItems(lines.map { it.itemId }.distinct())
        val pricesByItem = latestPrices.groupBy { it.itemId }

        val lineComparisons = lines.map { line ->
            val offers = pricesByItem[line.itemId].orEmpty().mapNotNull { price ->
                val supplier = suppliers[price.supplierId] ?: return@mapNotNull null
                PricingOffer(
                    supplierPriceId = price.id,
                    supplierId = supplier.id,
                    supplierName = supplier.name,
                    itemId = line.itemId,
                    listPrice = price.listPrice,
                    discountPercent = price.appliedDiscountPercent,
                    netUnitPrice = price.netPrice,
                    quantity = line.quantity,
                    totalNet = price.netPrice * line.quantity,
                    priceDate = price.priceDate,
                    source = price.source,
                )
            }.sortedBy { it.netUnitPrice }

            PricingLineComparison(
                lineId = line.lineId,
                itemId = line.itemId,
                itemName = line.itemName,
                itemCode = line.itemCode,
                quantity = line.quantity,
                unit = line.unit,
                usage = line.usage,
                lineDescription = line.lineDescription,
                offers = offers,
            )
        }

        val totals = suppliers.values.filter { it.approved }.map { supplier ->
            val matching = lineComparisons.mapNotNull { line -> line.offers.firstOrNull { it.supplierId == supplier.id } }
            SupplierPricingTotal(
                supplierId = supplier.id,
                supplierName = supplier.name,
                coveredLines = matching.size,
                totalLines = lineComparisons.size,
                totalNet = matching.sumOf { it.totalNet },
            )
        }.filter { it.coveredLines > 0 }
            .sortedWith(compareByDescending<SupplierPricingTotal> { it.complete }.thenBy { it.totalNet })

        return SheetPricingComparison(
            sheet = sheet,
            project = project,
            lines = lineComparisons,
            supplierTotals = totals,
        )
    }

    suspend fun buildPriceImportCandidates(
        data: TabularPriceData,
        mapping: ImportColumnMapping,
    ): List<PriceImportCandidate> = PriceListImportEngine.buildCandidates(
        data = data,
        mapping = mapping,
        existingItems = dao.getItems(),
    )

    suspend fun importPriceListRows(
        priceListId: Long,
        rows: List<ResolvedPriceImportRow>,
        skippedRows: Int = 0,
        sourceReference: String = "",
    ): PriceImportSummary = database.withTransaction {
        val priceList = dao.getPriceList(priceListId) ?: error("قائمة الأسعار غير موجودة")
        val supplier = dao.getSupplier(priceList.supplierId) ?: error("المورد غير موجود")
        if (sourceReference.isNotBlank() && sourceReference != priceList.sourceReference) {
            dao.updatePriceList(priceList.copy(sourceReference = sourceReference.trim()))
        }
        val existingItems = dao.getItems().toMutableList()
        val byNormalizedName = existingItems.associateBy { ImportText.normalizeItemName(it.name) }.toMutableMap()
        val byNormalizedCode = existingItems.filter { it.code.isNotBlank() }
            .associateBy { ImportText.normalizeCode(it.code) }
            .toMutableMap()

        var insertedPrices = 0
        var updatedPrices = 0
        var createdItems = 0
        var importedRows = 0

        rows.forEach { row ->
            if (row.listPrice < 0.0 || row.itemName.isBlank()) return@forEach

            var item = row.existingItemId?.let { id -> existingItems.firstOrNull { it.id == id } }
            if (item == null && row.createNewItem) {
                val normalizedCode = ImportText.normalizeCode(row.itemCode)
                val normalizedName = ImportText.normalizeItemName(row.itemName)
                item = normalizedCode.takeIf { it.isNotBlank() }?.let(byNormalizedCode::get)
                    ?: byNormalizedName[normalizedName]

                if (item == null) {
                    val requestedCode = row.itemCode.trim()
                    val safeCode = if (requestedCode.isNotBlank() && ImportText.normalizeCode(requestedCode) !in byNormalizedCode) {
                        requestedCode
                    } else {
                        generateItemCode()
                    }
                    val newItem = ItemEntity(
                        code = safeCode,
                        name = row.itemName.trim(),
                        normalizedName = normalizedName,
                        defaultUnit = row.unit.trim().ifBlank { "وحدة" },
                        brand = row.brand.trim(),
                        specification = row.specification.trim(),
                    )
                    val id = dao.insertItem(newItem)
                    item = newItem.copy(id = id)
                    existingItems += item!!
                    byNormalizedName[normalizedName] = item!!
                    byNormalizedCode[ImportText.normalizeCode(item!!.code)] = item!!
                    createdItems++
                }
            }

            val resolvedItem = item ?: return@forEach
            val automaticDiscount = PricingEngine.selectDiscountPercent(
                supplierDefault = supplier.defaultDiscountPercent,
                categoryId = resolvedItem.categoryId,
                itemId = resolvedItem.id,
                rules = dao.getActiveDiscountRules(supplier.id),
            )
            val appliedDiscount = (row.discountPercent ?: automaticDiscount).coerceIn(0.0, 100.0)
            val safePrice = row.listPrice.coerceAtLeast(0.0)
            val netPrice = PricingEngine.netPrice(safePrice, appliedDiscount)
            val existingPrice = dao.getPriceInListForItem(priceListId, resolvedItem.id)

            if (existingPrice == null) {
                dao.insertPrice(
                    SupplierPriceEntity(
                        supplierId = supplier.id,
                        itemId = resolvedItem.id,
                        priceListId = priceList.id,
                        listPrice = safePrice,
                        appliedDiscountPercent = appliedDiscount,
                        netPrice = netPrice,
                        priceDate = priceList.effectiveDate,
                        source = PriceSource.PRICE_LIST,
                        notes = "مستورد من القائمة",
                    )
                )
                insertedPrices++
            } else {
                dao.updatePrice(
                    existingPrice.copy(
                        listPrice = safePrice,
                        appliedDiscountPercent = appliedDiscount,
                        netPrice = netPrice,
                        priceDate = priceList.effectiveDate,
                        source = PriceSource.PRICE_LIST,
                        notes = "تم تحديثه من الاستيراد",
                    )
                )
                updatedPrices++
            }
            importedRows++
        }

        PriceImportSummary(
            importedRows = importedRows,
            insertedPrices = insertedPrices,
            updatedPrices = updatedPrices,
            createdItems = createdItems,
            skippedRows = skippedRows + (rows.size - importedRows),
        )
    }

    suspend fun suggestedSheetNumber(projectId: Long): String {
        val maxNumber = dao.maxNumericSheetNumber(projectId)
        return (maxNumber + 1).toString().padStart(3, '0')
    }

    suspend fun loadSheetDraft(sheetId: Long): RequestSheetDraft? {
        val sheet = dao.getSheet(sheetId) ?: return null
        val lines = dao.getRequestLineDetails(sheetId).map {
            RequestLineDraft(
                existingLineId = it.lineId,
                existingItemId = it.itemId,
                itemName = it.itemName,
                quantity = it.quantity,
                unit = it.unit,
                brand = it.brand,
                usage = it.usage,
                lineDescription = it.lineDescription,
                notes = it.notes,
            )
        }
        return RequestSheetDraft(
            id = sheet.id,
            projectId = sheet.projectId,
            sheetNumber = sheet.sheetNumber,
            sheetDate = sheet.sheetDate,
            trade = sheet.trade,
            craftsmanName = sheet.craftsmanName,
            workLocation = sheet.workLocation,
            attachmentUri = sheet.attachmentUri,
            notes = sheet.notes,
            lines = lines,
        )
    }

    suspend fun saveSheet(draft: RequestSheetDraft): Long = database.withTransaction {
        if (draft.id == 0L) {
            val finalSheetNumber = (dao.maxNumericSheetNumber(draft.projectId) + 1)
                .toString()
                .padStart(3, '0')

            val newId = dao.insertSheet(
                RequestSheetEntity(
                    projectId = draft.projectId,
                    sheetNumber = finalSheetNumber,
                    sheetDate = draft.sheetDate,
                    trade = draft.trade.trim(),
                    craftsmanName = draft.craftsmanName.trim(),
                    workLocation = draft.workLocation.trim(),
                    attachmentUri = draft.attachmentUri,
                    notes = draft.notes.trim(),
                )
            )

            val newLines = draft.lines.mapIndexed { index, draftLine ->
                RequestLineEntity(
                    sheetId = newId,
                    itemId = resolveItemId(draftLine),
                    quantity = draftLine.quantity,
                    unit = draftLine.unit.trim(),
                    sortOrder = index,
                    brand = draftLine.brand.trim(),
                    usage = "",
                    lineDescription = draftLine.lineDescription.trim(),
                    notes = draftLine.notes.trim(),
                )
            }
            dao.insertRequestLines(newLines)
            newId
        } else {
            val existing = dao.getSheet(draft.id) ?: error("الكشف غير موجود")
            val finalSheetNumber = if (existing.projectId == draft.projectId) {
                existing.sheetNumber
            } else {
                (dao.maxNumericSheetNumber(draft.projectId) + 1)
                    .toString()
                    .padStart(3, '0')
            }

            dao.updateSheet(
                existing.copy(
                    projectId = draft.projectId,
                    sheetNumber = finalSheetNumber,
                    sheetDate = draft.sheetDate,
                    trade = draft.trade.trim(),
                    craftsmanName = draft.craftsmanName.trim(),
                    workLocation = draft.workLocation.trim(),
                    attachmentUri = draft.attachmentUri,
                    notes = draft.notes.trim(),
                )
            )

            val oldIds = dao.getRequestLineDetails(draft.id).map { it.lineId }.toSet()
            val retainedIds = mutableSetOf<Long>()
            val newLineIds = mutableListOf<Long>()

            draft.lines.forEachIndexed { index, draftLine ->
                val itemId = resolveItemId(draftLine)
                val existingLineId = draftLine.existingLineId?.takeIf { it in oldIds }
                val entity = RequestLineEntity(
                    id = existingLineId ?: 0L,
                    sheetId = draft.id,
                    itemId = itemId,
                    quantity = draftLine.quantity,
                    unit = draftLine.unit.trim(),
                    sortOrder = index,
                    brand = draftLine.brand.trim(),
                    usage = "",
                    lineDescription = draftLine.lineDescription.trim(),
                    notes = draftLine.notes.trim(),
                )

                if (existingLineId != null) {
                    dao.updateRequestLine(entity)
                    retainedIds += existingLineId
                } else {
                    newLineIds += dao.insertRequestLine(entity.copy(id = 0L))
                }
            }

            val pricingCopies = dao.getPricingCopies(draft.id)
            val removedIds = oldIds - retainedIds
            if (removedIds.isNotEmpty()) {
                pricingCopies.forEach { copy ->
                    removedIds.forEach { lineId ->
                        dao.deletePriceBySourceReference("pricing-copy:${copy.id}:$lineId")
                    }
                }
                dao.deleteRequestLinesByIds(removedIds.toList())
            }

            if (newLineIds.isNotEmpty()) {
                pricingCopies.forEach { copy ->
                    dao.insertPricingCopyLines(
                        newLineIds.map { lineId ->
                            PricingCopyLineEntity(
                                copyId = copy.id,
                                requestLineId = lineId,
                            )
                        }
                    )
                }
            }

            pricingCopies.forEach { copy ->
                dao.getPricingCopyLineDetails(copy.id).forEach { line ->
                    syncPricingCopyLineToHistory(copy, line.requestLineId, line.unitPrice)
                }
            }

            draft.id
        }
    }

    suspend fun deleteSheet(sheetId: Long) = database.withTransaction {
        dao.getPricingCopies(sheetId).forEach { copy ->
            dao.deletePricesBySourceReferencePrefix("pricing-copy:${copy.id}:")
        }
        dao.getSheet(sheetId)?.let { dao.deleteSheet(it) }
    }

    suspend fun duplicateSheet(sheetId: Long): Long = database.withTransaction {
        val source = dao.getSheet(sheetId) ?: error("الكشف غير موجود")
        val lines = dao.getRequestLineDetails(sheetId)
        val newNumber = (dao.maxNumericSheetNumber(source.projectId) + 1).toString().padStart(3, '0')
        val newId = dao.insertSheet(
            source.copy(
                id = 0,
                sheetNumber = newNumber,
                sheetDate = System.currentTimeMillis(),
                attachmentUri = null,
                createdAt = System.currentTimeMillis(),
            )
        )
        dao.insertRequestLines(
            lines.mapIndexed { index, line ->
                RequestLineEntity(
                    sheetId = newId,
                    itemId = line.itemId,
                    quantity = line.quantity,
                    unit = line.unit,
                    sortOrder = index,
                    brand = "",
                    usage = "",
                    lineDescription = line.lineDescription,
                    notes = line.notes,
                )
            }
        )
        newId
    }

    suspend fun updateSheetAttachment(sheetId: Long, attachmentUri: String?) {
        val sheet = dao.getSheet(sheetId) ?: return
        dao.updateSheet(sheet.copy(attachmentUri = attachmentUri))
    }

    private suspend fun resolveItemId(line: RequestLineDraft): Long {
        val existingId = line.existingItemId
        if (existingId != null && dao.getItem(existingId) != null) return existingId

        val normalized = ImportText.normalizeItemName(line.itemName)
        val existing = dao.findItemByNormalizedName(normalized)
        if (existing != null) return existing.id

        val fallbackCategoryId = dao.findCategoryByName("أخرى")?.id
            ?: dao.insertCategory(CategoryEntity(name = "أخرى"))
        return dao.insertItem(
            ItemEntity(
                code = generateItemCode(),
                name = line.itemName.trim(),
                normalizedName = normalized,
                categoryId = fallbackCategoryId,
                defaultUnit = line.unit.trim(),
            )
        )
    }

    private fun generateItemCode(): String = "ITM-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}"
}
