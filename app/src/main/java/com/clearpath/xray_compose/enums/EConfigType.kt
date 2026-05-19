package com.clearpath.xray_compose.enums

enum class EConfigType(val value: Int) {
    VLESS(0),
    SHADOWSOCKS(1),
    HYSTERIA2(2),
    WIREGUARD(3),
    TROJAN(4),
    VMESS(5),
    SOCKS5(6),
    HTTP(7),

    CUSTOM(100),
    CUSTOMOUTBOUND(101),
    POLICYGROUP(102),
    PROXYCHAIN(103);

    companion object {
        fun fromValue(value: Int): EConfigType {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid EConfigType value: $value")
        }
    }
}