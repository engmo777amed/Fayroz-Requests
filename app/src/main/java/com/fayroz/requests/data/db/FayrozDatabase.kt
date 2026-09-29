package com.fayroz.requests.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.fayroz.requests.data.model.*

@Database(
    entities = [
        ProjectEntity::class,
        CategoryEntity::class,
        CategoryBrandEntity::class,
        ItemEntity::class,
        SupplierEntity::class,
        RequestSheetEntity::class,
        RequestLineEntity::class,
        PricingCopyEntity::class,
        PricingCopyLineEntity::class,
        PriceListEntity::class,
        SupplierDiscountRuleEntity::class,
        SupplierPriceEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FayrozDatabase : RoomDatabase() {
    abstract fun dao(): FayrozDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE request_lines ADD COLUMN brand TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN projectType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN areaSqm REAL")
                db.execSQL("ALTER TABLE projects ADD COLUMN floors INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN units INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN rooms INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN bedrooms INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN bathrooms INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN kitchens INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN balconies INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN projectStatus TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN notes TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS category_brands (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        categoryId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_category_brands_categoryId_name ON category_brands(categoryId, name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_category_brands_categoryId ON category_brands(categoryId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pricing_copies (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sheetId INTEGER NOT NULL,
                        placeName TEXT NOT NULL,
                        quoteDate INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(sheetId) REFERENCES request_sheets(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pricing_copies_sheetId ON pricing_copies(sheetId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pricing_copy_lines (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        copyId INTEGER NOT NULL,
                        requestLineId INTEGER NOT NULL,
                        brand TEXT NOT NULL,
                        unitPrice REAL,
                        FOREIGN KEY(copyId) REFERENCES pricing_copies(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(requestLineId) REFERENCES request_lines(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pricing_copy_lines_copyId ON pricing_copy_lines(copyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pricing_copy_lines_requestLineId ON pricing_copy_lines(requestLineId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pricing_copy_lines_copyId_requestLineId ON pricing_copy_lines(copyId, requestLineId)")
            }
        }

        fun create(context: Context): FayrozDatabase = Room.databaseBuilder(
            context.applicationContext,
            FayrozDatabase::class.java,
            "fayroz_requests.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
}
