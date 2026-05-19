package com.clearpath.xray_compose.service.formatter.model

import com.clearpath.xray_compose.service.formatter.model.serializer.LenientIntSerializer
import kotlinx.serialization.Serializable

@Serializable
data class VmessQRCode(
    @Serializable(with = LenientIntSerializer::class)
    val v: Int = 2,
    val ps: String = "",
    val add: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val port: Int = 0,
    val id: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val aid: Int = 0,
    val scy: String = "",
    val net: String = "",
    val type: String = "",
    val host: String = "",
    val path: String = "",
    val tls: String = "",
    val sni: String = "",
    val alpn: String = "",
    val fp: String = "",
    val insecure: String = "",
    val vcn: String = "",
    val pcs: String = "",
)
