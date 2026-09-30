package com.fayroz.requests.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val clientName: String = "",
    val location: String = "",
    val projectType: String = "",
    val areaSqm: Double? = null,
    val floors: Int? = null,
    val units: Int? = null,
    val rooms: Int? = null,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val kitchens: Int? = null,
    val balconies: Int? = null,
    val projectStatus: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0,
)


@Entity(
    tableName = "category_brands",
    indices = [Index(value = ["categoryId", "name"], unique = true), Index("categoryId")],
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class CategoryBrandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
)


@Entity(
    tableName = "items",
    indices = [Index(value = ["code"], unique = true), Index(value = ["normalizedName"])],
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
    )],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val marketName: String = "",
    val normalizedName: String,
    val categoryId: Long? = null,
    val defaultUnit: String,
    val brand: String = "",
    val specification: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "suppliers", indices = [Index(value = ["name"])])
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val specialty: String = "",
    val approved: Boolean = true,
    val defaultDiscountPercent: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "request_sheets",
    indices = [Index("projectId"), Index(value = ["projectId", "sheetNumber"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = ProjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["projectId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class RequestSheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val sheetNumber: String,
    val sheetDate: Long = System.currentTimeMillis(),
    val trade: String = "",
    val craftsmanName: String = "",
    val workLocation: String = "",
    val attachmentUri: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "request_lines",
    indices = [Index("sheetId"), Index("itemId")],
    foreignKeys = [
        ForeignKey(
            entity = RequestSheetEntity::class,
            parentColumns = ["id"],
            childColumns = ["sheetId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class RequestLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val itemId: Long,
    val quantity: Double,
    val unit: String,
    val sortOrder: Int = 0,
    val brand: String = "",
    val usage: String = "",
    val lineDescription: String = "",
    val notes: String = "",
)


@Entity(
    tableName = "pricing_copies",
    indices = [Index("sheetId"), Index("supplierId")],
    foreignKeys = [ForeignKey(
        entity = RequestSheetEntity::class,
        parentColumns = ["id"],
        childColumns = ["sheetId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class PricingCopyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val placeName: String,
    val supplierId: Long? = null,
    val quoteNumber: String = "",
    val quoteDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "pricing_copy_lines",
    indices = [
        Index("copyId"),
        Index("requestLineId"),
        Index(value = ["copyId", "requestLineId"], unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = PricingCopyEntity::class,
            parentColumns = ["id"],
            childColumns = ["copyId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RequestLineEntity::class,
            parentColumns = ["id"],
            childColumns = ["requestLineId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PricingCopyLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val copyId: Long,
    val requestLineId: Long,
    val brand: String = "",
    val unitPrice: Double? = null,
)

@Entity(
    tableName = "price_lists",
    indices = [Index("supplierId")],
    foreignKeys = [ForeignKey(
        entity = SupplierEntity::class,
        parentColumns = ["id"],
        childColumns = ["supplierId"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class PriceListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val name: String,
    val effectiveDate: Long,
    val sourceReference: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class DiscountScope { CATEGORY, ITEM }

@Entity(
    tableName = "supplier_discount_rules",
    indices = [Index("supplierId"), Index("categoryId"), Index("itemId")],
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SupplierDiscountRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val scope: DiscountScope,
    val categoryId: Long? = null,
    val itemId: Long? = null,
    val discountPercent: Double,
    val active: Boolean = true,
)

enum class PriceSource { PRICE_LIST, DIRECT_QUOTE, MANUAL }

@Entity(
    tableName = "supplier_prices",
    indices = [Index("supplierId"), Index("itemId"), Index("priceListId"), Index("priceDate")],
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PriceListEntity::class,
            parentColumns = ["id"],
            childColumns = ["priceListId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class SupplierPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val itemId: Long,
    val priceListId: Long? = null,
    val listPrice: Double,
    val appliedDiscountPercent: Double,
    val netPrice: Double,
    val priceDate: Long,
    val source: PriceSource,
    val sourceReference: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
