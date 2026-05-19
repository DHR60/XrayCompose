package com.clearpath.xray_compose.enums

enum class ETransport(val value: String) {
    RAW("raw"),
    XHTTP("xhttp"),
    WS("ws"),
    GRPC("grpc"),
    HTTPUPGRADE("httpupgrade"),
    KCP("kcp");

    companion object {
        fun fromValue(value: String): ETransport {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid ETransport value: $value")
        }
    }
}