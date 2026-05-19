package com.clearpath.xray_compose.data.repo

import com.clearpath.xray_compose.data.ConfigItem
import com.clearpath.xray_compose.utils.JsonUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

object ConfigItemSerializer : androidx.datastore.core.Serializer<ConfigItem> {
    override val defaultValue: ConfigItem = ConfigItem()

    override suspend fun readFrom(input: InputStream): ConfigItem = withContext(Dispatchers.IO) {
        try {
            JsonUtil.innerJson.decodeFromString(
                deserializer = ConfigItem.serializer(),
                string = input.readBytes().decodeToString()
            )
        } catch (_: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: ConfigItem, output: OutputStream) =
        withContext(Dispatchers.IO) {
            output.write(
                JsonUtil.innerJson.encodeToString(
                    ConfigItem.serializer(), t
                ).encodeToByteArray()
            )
        }
}