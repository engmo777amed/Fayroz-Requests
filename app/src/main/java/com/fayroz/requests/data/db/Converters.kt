package com.fayroz.requests.data.db

import androidx.room.TypeConverter
import com.fayroz.requests.data.model.DiscountScope
import com.fayroz.requests.data.model.PriceSource

class Converters {
    @TypeConverter fun discountScopeToString(value: DiscountScope): String = value.name
    @TypeConverter fun stringToDiscountScope(value: String): DiscountScope = DiscountScope.valueOf(value)
    @TypeConverter fun priceSourceToString(value: PriceSource): String = value.name
    @TypeConverter fun stringToPriceSource(value: String): PriceSource = PriceSource.valueOf(value)
}
