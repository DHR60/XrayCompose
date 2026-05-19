package com.clearpath.xray_compose.data

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

    val kcpMtu: String? = null,
)
