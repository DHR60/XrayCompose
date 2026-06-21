package com.clearpath.xray_compose.data.db.converter

import androidx.room3.ColumnTypeConverter
import com.clearpath.xray_compose.enums.EConfigType

class EConfigTypeConverter {
    @ColumnTypeConverter
    fun fromEConfigType(value: EConfigType?): Int? {
        return value?.value
    }

    @ColumnTypeConverter
    fun toEConfigType(value: Int?): EConfigType? {
        return value?.let { EConfigType.fromValue(it) }
    }
}