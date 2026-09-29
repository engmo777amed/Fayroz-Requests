package com.fayroz.requests.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FayrozDatabase : RoomDatabase() {
    abstract fun dao(): FayrozDao

    companion object {
        fun create(context: Context): FayrozDatabase = Room.databaseBuilder(
            context.applicationContext,
            FayrozDatabase::class.java,
            "fayroz_requests.db",
        ).build()
    }
}
