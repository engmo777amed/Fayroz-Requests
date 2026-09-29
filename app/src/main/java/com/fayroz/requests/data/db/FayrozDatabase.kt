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
        ItemEntity::class,
        SupplierEntity::class,
        RequestSheetEntity::class,
        RequestLineEntity::class,
        PriceListEntity::class,
        SupplierDiscountRuleEntity::class,
        SupplierPriceEntity::class,
    ],
    version = 2,
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

        fun create(context: Context): FayrozDatabase = Room.databaseBuilder(
            context.applicationContext,
            FayrozDatabase::class.java,
            "fayroz_requests.db",
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
