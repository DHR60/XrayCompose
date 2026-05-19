package com.clearpath.xray_compose.data.db.converter

import androidx.room3.TypeConverter
import com.clearpath.xray_compose.enums.EConfigType

class EConfigTypeConverter {
    @TypeConverter
    fun fromEConfigType(value: EConfigType?): Int? {
        return value?.value
    }

    @TypeConverter
    fun toEConfigType(value: Int?): EConfigType? {
        return value?.let { EConfigType.fromValue(it) }
    }
}