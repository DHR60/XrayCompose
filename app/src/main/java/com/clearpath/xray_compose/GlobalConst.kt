@file:Suppress("ConstPropertyName")

package com.clearpath.xray_compose

import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.enums.ETransport

object GlobalConst {
    const val appName = "Xray-Compose"
    const val appId = BuildConfig.APPLICATION_ID

    const val broadcastActionService = "$appId.action.service"

    const val loopbackAddress = "127.0.0.1"
    const val publicAddress = "0.0.0.0"
    const val defaultSocksPort = 10808

    const val defaultRemoteDns =
        "https://cloudflare-dns.com/dns-query,https://dns.google/dns-query,8.8.8.8"
    const val defaultLocalDns = "119.29.29.29,223.5.5.5,https://doh.pub/dns-query"

    const val tunInboundTag = "tun"
    const val socksInboundTag = "socks"

    const val proxyTag = "proxy"
    const val directTag = "direct"
    const val blockTag = "block"
    const val dnsOutboundTag = "dns"
    const val dnsModuleTag = "dns-module"
    const val localDnsModuleTag = "domestic-dns"

    const val geositeCN = "geosite:cn"
    const val geoipCN = "geoip:cn"

    const val none = "none"
    const val AsIs = "AsIs"
    const val IPIfNonMatch = "IPIfNonMatch"
    const val IPOnDemand = "IPOnDemand"

    val domainStrategyList = listOf(
        AsIs,
        IPIfNonMatch,
        IPOnDemand,
    )

    const val defaultLevel = 8
    const val defaultPort = 443
    const val defaultTransportNetwork = "raw"
    const val rawTransportNetworkAlias = "tcp"

    val configTypeHumanFyMap = mapOf(
        "VLESS" to EConfigType.VLESS,
        "ShadowSocks" to EConfigType.SHADOWSOCKS,
        "Hysteria2" to EConfigType.HYSTERIA2,
        "WireGuard" to EConfigType.WIREGUARD,
        "Trojan" to EConfigType.TROJAN,
        "VMess" to EConfigType.VMESS,
        "SOCKS" to EConfigType.SOCKS5,
        "HTTP" to EConfigType.HTTP,

        "Custom" to EConfigType.CUSTOM,
        "Custom Outbound" to EConfigType.CUSTOMOUTBOUND,
        "Policy Group" to EConfigType.POLICYGROUP,
        "Proxy Chain" to EConfigType.PROXYCHAIN,
    )

    val configTypeHumanFyReverseMap = configTypeHumanFyMap.entries.associate { (k, v) -> v to k }

    val configTypeProtocolMap = mapOf(
        EConfigType.VLESS to "vless",
        EConfigType.SHADOWSOCKS to "shadowsocks",
        EConfigType.HYSTERIA2 to "hysteria",
        EConfigType.WIREGUARD to "wireguard",
        EConfigType.TROJAN to "trojan",
        EConfigType.VMESS to "vmess",
        EConfigType.SOCKS5 to "socks",
        EConfigType.HTTP to "http",
    )

    val configTypeProtocolReverseMap = configTypeProtocolMap.entries.associate { (k, v) -> v to k }

    val transportMap = mapOf(
        "raw" to ETransport.RAW,
        "xhttp" to ETransport.XHTTP,
        "ws" to ETransport.WS,
        "grpc" to ETransport.GRPC,
        "httpupgrade" to ETransport.HTTPUPGRADE,
        "kcp" to ETransport.KCP,
    )

    val transportReverseMap = transportMap.entries.associate { (k, v) -> v to k }

    val xhttpTypeList = listOf(
        "auto",
        "packet-up",
        "stream-up",
        "stream-one",
    )

    const val transportSecurityTls = "tls"
    const val transportSecurityReality = "reality"

    val transportSecurityList = listOf(
        "",
        transportSecurityTls,
        transportSecurityReality,
    )

    val transportSecurityTlsOnlyList = listOf(
        "",
        transportSecurityTls,
    )

    val alpnList = listOf(
        "",
        "h3",
        "h2",
        "http/1.1",
        "h3,h2",
        "h2,http/1.1",
        "h3,h2,http/1.1",
    )

    val allowInsecureList = listOf(
        "",
        "false",
        "true",
    )

    val utlsFingerprintList = listOf(
        "",
        "chrome",
        "firefox",
        "safari",
        "edge",
        "ios",
        "android",
        "360",
        "qq",
        "random",
        "randomized",
    )

    const val vlessVisionFlow = "xtls-rprx-vision"
    const val vlessVisionAllowQuicFlow = "xtls-rprx-vision-udp443"
    val vlessFlowList = listOf(
        "",
        vlessVisionFlow,
        vlessVisionAllowQuicFlow,
    )

    val protocolSchemeMap = mapOf(
        EConfigType.VLESS to listOf("vless://"),
        EConfigType.SHADOWSOCKS to listOf("ss://"),
        EConfigType.HYSTERIA2 to listOf("hysteria2://", "hy2://"),
        EConfigType.TROJAN to listOf("trojan://"),
        EConfigType.VMESS to listOf("vmess://"),
        EConfigType.SOCKS5 to listOf("socks://"),
        EConfigType.HTTP to listOf("http://"),
    )

    val LocalDNSList = listOf(
        "119.29.29.29,223.5.5.5,https://doh.pub/dns-query",
        "119.29.29.29",
        "223.5.5.5",
        "https://doh.pub/dns-query",
        "https://dns.alidns.com/dns-query",
        "https://doh.pub/dns-query,https://dns.alidns.com/dns-query",
        "localhost"
    )

    val remoteDnsList = listOf(
        "https://cloudflare-dns.com/dns-query,https://dns.google/dns-query,8.8.8.8",
        "https://cloudflare-dns.com/dns-query",
        "https://dns.google/dns-query",
        "https://dns.cloudflare.com/dns-query",
        "https://doh.dns.sb/dns-query",
        "https://doh.opendns.com/dns-query",
        "https://common.dot.dns.yandex.net/dns-query",
        "8.8.8.8",
        "1.1.1.1",
        "185.222.222.222",
        "208.67.222.222",
        "77.88.8.8"
    )

    val commonHostPredefinedMap = mapOf(
        "dns.alidns.com" to listOf("223.5.5.5", "223.6.6.6", "2400:3200::1", "2400:3200:baba::1"),
        "doh.pub" to listOf("1.12.12.12", "120.53.53.53"),
        "dns.google" to listOf(
            "8.8.8.8",
            "8.8.4.4",
            "2001:4860:4860::8888",
            "2001:4860:4860::8844"
        ),
        "one.one.one.one" to listOf(
            "1.1.1.1",
            "1.0.0.1",
            "2606:4700:4700::1111",
            "2606:4700:4700::1001"
        ),
        "1dot1dot1dot1.cloudflare-dns.com" to listOf(
            "1.1.1.1",
            "1.0.0.1",
            "2606:4700:4700::1111",
            "2606:4700:4700::1001"
        ),
        "cloudflare-dns.com" to listOf(
            "104.16.249.249",
            "104.16.248.249",
            "2606:4700::6810:f8f9",
            "2606:4700::6810:f9f9"
        ),
        "dns.cloudflare.com" to listOf(
            "162.159.61.8",
            "172.64.41.8",
            "2a06:98c1:52::8",
            "2803:f800:53::8"
        ),
        "dns.quad9.net" to listOf("9.9.9.9", "149.112.112.112", "2620:fe::fe", "2620:fe::9"),
        "dns.yandex.net" to listOf(
            "77.88.8.8",
            "77.88.8.1",
            "2a02:6b8::feed:0ff",
            "2a02:6b8:0:1::feed:0ff"
        ),
        "dns.sb" to listOf("45.11.45.11", "185.222.222.222", "2a09::", "2a11::"),
        "dns.umbrella.com" to listOf(
            "208.67.220.220",
            "208.67.222.222",
            "2620:119:35::35",
            "2620:119:53::53"
        ),
        "dns.sse.cisco.com" to listOf(
            "208.67.220.220",
            "208.67.222.222",
            "2620:119:35::35",
            "2620:119:53::53"
        ),
        "engage.cloudflareclient.com" to listOf("162.159.192.1"),
    )

    val commonIpv4HostPredefinedMap = commonHostPredefinedMap.mapValues { entry ->
        entry.value.filter { it.indexOf(':') == -1 }
    }
}