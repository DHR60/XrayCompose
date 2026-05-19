package com.clearpath.xray_compose.data.repo

import com.clearpath.xray_compose.data.ConfigItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

object ConfigItemSerializer : androidx.datastore.core.Serializer<ConfigItem> {
    override val defaultValue: ConfigItem = ConfigItem()

    override suspend fun readFrom(input: InputStream): ConfigItem = withContext(Dispatchers.IO) {
        try {
            Json.decodeFromString(
                deserializer = ConfigItem.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: ConfigItem, output: OutputStream) =
        withContext(Dispatchers.IO) {
            output.write(Json.encodeToString(ConfigItem.serializer(), t).encodeToByteArray())
        }
}