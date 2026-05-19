package com.clearpath.xray_compose.data.db.converter

import androidx.room3.TypeConverter
import java.nio.ByteBuffer
import java.util.UUID

class UuidConverter {
    @TypeConverter
    fun fromUuid(uuid: UUID?): ByteArray? {
        if (uuid == null) return null
        val buffer = ByteBuffer.wrap(ByteArray(16))
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
        return buffer.array()
    }

    @TypeConverter
    fun toUuid(bytes: ByteArray?): UUID? {
        if (bytes == null || bytes.size != 16) return null
        val buffer = ByteBuffer.wrap(bytes)
        val firstLong = buffer.long
        val secondLong = buffer.long
        return UUID(firstLong, secondLong)
    }
}