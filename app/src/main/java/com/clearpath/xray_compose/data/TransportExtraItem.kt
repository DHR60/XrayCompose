package com.clearpath.xray_compose.data

import com.clearpath.xray_compose.utils.JsonUtil
import kotlinx.serialization.Serializable

@Serializable
data class TransportExtraItem(
    val host: String? = null,
    val path: String? = null,

    val rawHeaderType: String? = null,

    val xhttpMode: String? = null,
    val xhttpExtra: String? = null,

    val grpcAuthority: String? = null,
    val grpcServiceName: String? = null,
    val grpcMode: String? = null,

    val kcpHeaderType: String? = null,
    val kcpSeed: String? = null,
    val kcpMtu: String? = null,
) {
    companion object {
        fun fromJsonString(jsonString: String): TransportExtraItem {
            return JsonUtil.innerJson.decodeFromString(
                serializer(),
                jsonString
            )
        }
    }

    fun toJsonString(): String {
        return JsonUtil.innerJson.encodeToString(
            serializer(),
            this
        )
    }
}
