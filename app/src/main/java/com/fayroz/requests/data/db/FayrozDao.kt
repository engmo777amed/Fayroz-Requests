package com.fayroz.requests.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fayroz.requests.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FayrozDao {
    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProject(projectId: Long): ProjectEntity?

    @Insert suspend fun insertProject(project: ProjectEntity): Long
    @Update suspend fun updateProject(project: ProjectEntity)
    @Delete suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findCategoryByName(name: String): CategoryEntity?

    @Insert suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY name COLLATE NOCASE")
    suspend fun getItems(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE normalizedName LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getItem(itemId: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findItemByNormalizedName(normalizedName: String): ItemEntity?

    @Query("SELECT * FROM items WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun findItemByCode(code: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: ItemEntity): Long

    @Update suspend fun updateItem(item: ItemEntity)
    @Delete suspend fun deleteItem(item: ItemEntity)

    @Query("SELECT COUNT(*) FROM request_lines WHERE itemId = :itemId")
    suspend fun countRequestLinesForItem(itemId: Long): Int

    @Query("SELECT COUNT(*) FROM supplier_prices WHERE itemId = :itemId")
    suspend fun countPricesForItem(itemId: Long): Int

    @Query("SELECT * FROM suppliers ORDER BY approved DESC, name COLLATE NOCASE")
    fun observeSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY approved DESC, name COLLATE NOCASE")
    suspend fun getSuppliers(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :supplierId LIMIT 1")
    suspend fun getSupplier(supplierId: Long): SupplierEntity?

    @Insert suspend fun insertSupplier(supplier: SupplierEntity): Long
    @Update suspend fun updateSupplier(supplier: SupplierEntity)
    @Delete suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM request_sheets ORDER BY sheetDate DESC, id DESC")
    fun observeSheets(): Flow<List<RequestSheetEntity>>

    @Query(
        """
        SELECT rs.id AS id,
               rs.projectId AS projectId,
               p.name AS projectName,
               rs.sheetNumber AS sheetNumber,
               rs.sheetDate AS sheetDate,
               rs.trade AS trade,
               rs.craftsmanName AS craftsmanName,
               rs.notes AS notes,
               COUNT(rl.id) AS lineCount
        FROM request_sheets rs
        INNER JOIN projects p ON p.id = rs.projectId
        LEFT JOIN request_lines rl ON rl.sheetId = rs.id
        GROUP BY rs.id
        ORDER BY rs.sheetDate DESC, rs.id DESC
        """
    )
    fun observeSheetSummaries(): Flow<List<RequestSheetSummary>>

    @Query("SELECT * FROM request_sheets WHERE id = :sheetId LIMIT 1")
    suspend fun getSheet(sheetId: Long): RequestSheetEntity?

    @Query(
        """
        SELECT rl.id AS lineId,
               rl.itemId AS itemId,
               i.name AS itemName,
               i.code AS itemCode,
               rl.quantity AS quantity,
               rl.unit AS unit,
               rl.usage AS usage,
               rl.lineDescription AS lineDescription,
               rl.notes AS notes
        FROM request_lines rl
        INNER JOIN items i ON i.id = rl.itemId
        WHERE rl.sheetId = :sheetId
        ORDER BY rl.id
        """
    )
    suspend fun getRequestLineDetails(sheetId: Long): List<RequestLineDetail>

    @Query("SELECT COALESCE(MAX(CAST(sheetNumber AS INTEGER)), 0) FROM request_sheets WHERE projectId = :projectId")
    suspend fun maxNumericSheetNumber(projectId: Long): Int

    @Insert suspend fun insertSheet(sheet: RequestSheetEntity): Long
    @Update suspend fun updateSheet(sheet: RequestSheetEntity)
    @Delete suspend fun deleteSheet(sheet: RequestSheetEntity)

    @Insert suspend fun insertRequestLines(lines: List<RequestLineEntity>)

    @Query("DELETE FROM request_lines WHERE sheetId = :sheetId")
    suspend fun deleteLinesForSheet(sheetId: Long)

    @Transaction
    suspend fun insertSheetWithLines(sheet: RequestSheetEntity, lines: List<RequestLineEntity>): Long {
        val sheetId = insertSheet(sheet)
        insertRequestLines(lines.map { it.copy(sheetId = sheetId) })
        return sheetId
    }

    @Insert suspend fun insertPriceList(priceList: PriceListEntity): Long
    @Update suspend fun updatePriceList(priceList: PriceListEntity)
    @Delete suspend fun deletePriceList(priceList: PriceListEntity)

    @Query("DELETE FROM supplier_prices WHERE priceListId = :priceListId")
    suspend fun deletePricesForPriceList(priceListId: Long)

    @Query("SELECT * FROM price_lists WHERE id = :priceListId LIMIT 1")
    suspend fun getPriceList(priceListId: Long): PriceListEntity?

    @Query(
        """
        SELECT pl.id AS id,
               pl.supplierId AS supplierId,
               pl.name AS name,
               pl.effectiveDate AS effectiveDate,
               pl.sourceReference AS sourceReference,
               COUNT(sp.id) AS entryCount
        FROM price_lists pl
        LEFT JOIN supplier_prices sp ON sp.priceListId = pl.id
        WHERE pl.supplierId = :supplierId
        GROUP BY pl.id
        ORDER BY pl.effectiveDate DESC, pl.id DESC
        """
    )
    fun observePriceListSummaries(supplierId: Long): Flow<List<PriceListSummary>>

    @Insert suspend fun insertPrice(price: SupplierPriceEntity): Long
    @Update suspend fun updatePrice(price: SupplierPriceEntity)
    @Delete suspend fun deletePrice(price: SupplierPriceEntity)

    @Query("SELECT * FROM supplier_prices WHERE id = :priceId LIMIT 1")
    suspend fun getPrice(priceId: Long): SupplierPriceEntity?

    @Query("SELECT * FROM supplier_prices WHERE priceListId = :priceListId AND itemId = :itemId LIMIT 1")
    suspend fun getPriceInListForItem(priceListId: Long, itemId: Long): SupplierPriceEntity?

    @Query(
        """
        SELECT sp.id AS priceId,
               sp.priceListId AS priceListId,
               sp.supplierId AS supplierId,
               sp.itemId AS itemId,
               i.name AS itemName,
               i.code AS itemCode,
               i.defaultUnit AS unit,
               sp.listPrice AS listPrice,
               sp.appliedDiscountPercent AS appliedDiscountPercent,
               sp.netPrice AS netPrice,
               sp.priceDate AS priceDate,
               sp.source AS source,
               sp.notes AS notes
        FROM supplier_prices sp
        INNER JOIN items i ON i.id = sp.itemId
        WHERE sp.priceListId = :priceListId
        ORDER BY i.name COLLATE NOCASE
        """
    )
    fun observePriceListEntries(priceListId: Long): Flow<List<PriceListEntryDetail>>

    @Query("SELECT * FROM supplier_prices WHERE itemId = :itemId ORDER BY priceDate DESC, id DESC")
    fun observePriceHistory(itemId: Long): Flow<List<SupplierPriceEntity>>

    @Query(
        """
        SELECT sp.id AS priceId,
               sp.itemId AS itemId,
               sp.supplierId AS supplierId,
               s.name AS supplierName,
               pl.name AS priceListName,
               sp.listPrice AS listPrice,
               sp.appliedDiscountPercent AS appliedDiscountPercent,
               sp.netPrice AS netPrice,
               sp.priceDate AS priceDate,
               sp.source AS source,
               sp.notes AS notes
        FROM supplier_prices sp
        INNER JOIN suppliers s ON s.id = sp.supplierId
        LEFT JOIN price_lists pl ON pl.id = sp.priceListId
        WHERE sp.itemId = :itemId
        ORDER BY sp.priceDate DESC, sp.id DESC
        """
    )
    fun observeItemPriceHistory(itemId: Long): Flow<List<ItemPriceHistoryDetail>>

    @Query(
        """
        SELECT sp.*
        FROM supplier_prices sp
        WHERE sp.itemId IN (:itemIds)
          AND NOT EXISTS (
              SELECT 1
              FROM supplier_prices newer
              WHERE newer.supplierId = sp.supplierId
                AND newer.itemId = sp.itemId
                AND (newer.priceDate > sp.priceDate OR (newer.priceDate = sp.priceDate AND newer.id > sp.id))
          )
        ORDER BY sp.itemId, sp.netPrice
        """
    )
    suspend fun getLatestPricesForItems(itemIds: List<Long>): List<SupplierPriceEntity>

    @Insert suspend fun insertDiscountRule(rule: SupplierDiscountRuleEntity): Long

    @Query("UPDATE supplier_discount_rules SET active = 0 WHERE supplierId = :supplierId AND scope = 'ITEM' AND itemId = :itemId")
    suspend fun deactivateItemDiscountRules(supplierId: Long, itemId: Long)

    @Query("SELECT * FROM supplier_discount_rules WHERE supplierId = :supplierId AND active = 1 ORDER BY id DESC")
    suspend fun getActiveDiscountRules(supplierId: Long): List<SupplierDiscountRuleEntity>
}
