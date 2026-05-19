package com.clearpath.xray_compose.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object JsonUtil {
    // JsonElement
    private val prettyJsonConfig = Json { prettyPrint = true }

    fun prettyJson(jsonText: String): String {
        return try {
            val jsonElement = Json.parseToJsonElement(jsonText)
            prettyJsonConfig.encodeToString(JsonElement.serializer(), jsonElement)
        } catch (e: Exception) {
            LogUtil.e("Failed to pretty print JSON", e)
            jsonText
        }
    }

    private val compressJsonConfig = Json { prettyPrint = false }

    fun compressJson(jsonText: String): String {
        return try {
            val jsonElement = Json.parseToJsonElement(jsonText)
            compressJsonConfig.encodeToString(JsonElement.serializer(), jsonElement)
        } catch (e: Exception) {
            LogUtil.e("Failed to compress JSON", e)
            jsonText
        }
    }

    // Serialization
    val innerJson = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        isLenient = false
    }
    val defaultJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = false
    }
    val defaultIndentedJson = Json(defaultJson) {
        prettyPrint = true
    }
    val defaultNullValuesJson = Json(defaultJson) {
        explicitNulls = true
    }
    val defaultIndentedNullValuesJson = Json(defaultJson) {
        prettyPrint = true
        explicitNulls = true
    }
    val lenientJson = Json(defaultJson) {
        isLenient = true
        allowTrailingComma = true
        allowComments = true
        coerceInputValues = true
    }
}