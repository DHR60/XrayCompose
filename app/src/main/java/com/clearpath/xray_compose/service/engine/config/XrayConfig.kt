package com.clearpath.xray_compose.service.engine.config

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.EConfigType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class XrayConfig(
    var remarks: String? = null,
    var stats: JsonElement? = null,
    val log: LogBean = LogBean(),
    var policy: PolicyBean? = null,
    val inbounds: MutableList<InboundBean> = mutableListOf(),
    val outbounds: MutableList<OutboundBean> = mutableListOf(),
    var dns: DnsBean? = null,
    var routing: RoutingBean? = null,
    val api: JsonElement? = null,
    val reverse: JsonElement? = null,
    var fakedns: FakednsBean? = null,
    var observatory: JsonElement? = null,
    var burstObservatory: JsonElement? = null
) {

    @Serializable
    data class LogBean(
        val access: String? = null,
        val error: String? = null,
        var loglevel: String? = null,
        val dnsLog: Boolean? = null
    )

    @Serializable
    data class InboundBean(
        var tag: String,
        var port: Int? = null,
        var protocol: String,
        var listen: String? = null,
        var settings: InSettingsBean? = null,
        var sniffing: SniffingBean? = null,
        val streamSettings: JsonElement? = null,
        val allocate: JsonElement? = null
    ) {
        @Serializable
        data class InSettingsBean(
            var auth: String? = null,
            var udp: Boolean? = null,
            var userLevel: Int? = null,
            var accounts: List<SocksAccountBean>? = null,
            var name: String? = null,

            var mtu: Int? = null
        ) {
            @Serializable
            data class SocksAccountBean(
                var user: String = "", var pass: String = ""
            )
        }

        @Serializable
        data class SniffingBean(
            var enabled: Boolean,
            val destOverride: List<String>,
            val metadataOnly: Boolean? = null,
            var routeOnly: Boolean? = null
        )
    }

    @Serializable
    data class OutboundBean(
        var tag: String = GlobalConst.proxyTag,
        var protocol: String,
        var settings: OutSettingsBean? = null,
        var streamSettings: StreamSettingsBean? = null,
        val sendThrough: String? = null,
        var mux: MuxBean? = MuxBean(false)
    ) {
        @Serializable
        data class OutSettingsBean(
            var vnext: List<VnextBean>? = null,
            var servers: List<ServersBean>? = null,
            /*Blackhole*/
            var response: Response? = null,
            /*DNS*/
            val network: String? = null,
            var address: JsonElement? = null,
            var port: Int? = null,
            /*Freedom*/
            val redirect: String? = null,
            val userLevel: Int? = null,
            /*Loopback*/
            val inboundTag: String? = null,
            /*Wireguard*/
            var secretKey: String? = null,
            val peers: List<WireGuardBean>? = null,
            var reserved: List<Int>? = null,
            var mtu: Int? = null,
            var obfsPassword: String? = null,
            var version: Int? = null,
        ) {
            @Serializable
            data class VnextBean(
                var address: String = "",
                var port: Int = GlobalConst.defaultSocksPort,
                var users: List<UsersBean>
            ) {

                @Serializable
                data class UsersBean(
                    var id: String = "",
                    var alterId: Int? = null,
                    var security: String? = null,
                    var level: Int = GlobalConst.defaultLevel,
                    var encryption: String? = null,
                    var flow: String? = null
                )
            }

            @Serializable
            data class ServersBean(
                var address: String = "",
                var method: String? = null,
                var ota: Boolean = false,
                var password: String? = null,
                var port: Int = GlobalConst.defaultPort,
                var level: Int = GlobalConst.defaultLevel,
                val email: String? = null,
                var flow: String? = null,
                val ivCheck: Boolean? = null,
                var users: List<SocksUsersBean>? = null
            ) {
                @Serializable
                data class SocksUsersBean(
                    var user: String = "",
                    var pass: String = "",
                    var level: Int = GlobalConst.defaultLevel
                )
            }

            @Serializable
            data class Response(var type: String)

            @Serializable
            data class WireGuardBean(
                var publicKey: String = "",
                var preSharedKey: String? = null,
                var endpoint: String = ""
            )
        }

        @Serializable
        data class StreamSettingsBean(
            var network: String = GlobalConst.defaultTransportNetwork,
            var security: String? = null,
            var rawSettings: RawSettingsBean? = null,
            var kcpSettings: KcpSettingsBean? = null,
            var wsSettings: WsSettingsBean? = null,
            var httpupgradeSettings: HttpupgradeSettingsBean? = null,
            var xhttpSettings: XhttpSettingsBean? = null,
            var httpSettings: HttpSettingsBean? = null,
            var tlsSettings: TlsSettingsBean? = null,
            var quicSettings: QuicSettingBean? = null,
            var realitySettings: TlsSettingsBean? = null,
            var grpcSettings: GrpcSettingsBean? = null,
            var hysteriaSettings: HysteriaSettingsBean? = null,
            var finalmask: JsonElement? = null,
            val dsSettings: JsonElement? = null,
            var sockopt: SockoptBean? = null
        ) {

            @Serializable
            data class RawSettingsBean(
                var header: HeaderBean = HeaderBean(), val acceptProxyProtocol: Boolean? = null
            ) {
                @Serializable
                data class HeaderBean(
                    var type: String = "none",
                    var request: RequestBean? = null,
                    var response: JsonElement? = null
                ) {
                    @Serializable
                    data class RequestBean(
                        var path: List<String> = emptyList(),
                        var headers: HeadersBean = HeadersBean(),
                        val version: String? = null,
                        val method: String? = null
                    ) {
                        @Serializable
                        data class HeadersBean(
                            @SerialName("Host") var host: List<String>? = emptyList(),
                            @SerialName("User-Agent") val userAgent: List<String>? = null,
                            @SerialName("Accept-Encoding") val acceptEncoding: List<String>? = null,
                            @SerialName("Connection") val connection: List<String>? = null,
                            @SerialName("Pragma") val pragma: String? = null
                        )
                    }
                }
            }

            @Serializable
            data class KcpSettingsBean(
                var mtu: Int = 1350,
                var tti: Int = 50,
                var uplinkCapacity: Int = 12,
                var downlinkCapacity: Int = 100,
                var congestion: Boolean = false,
                var readBufferSize: Int = 1,
                var writeBufferSize: Int = 1
            )

            @Serializable
            data class WsSettingsBean(
                var path: String? = null,
                var host: String? = null,
                var headers: Map<String, String>? = null,
                val maxEarlyData: Int? = null,
                val useBrowserForwarding: Boolean? = null,
                val acceptProxyProtocol: Boolean? = null
            )

            @Serializable
            data class HttpupgradeSettingsBean(
                var path: String? = null,
                var host: String? = null,
                val acceptProxyProtocol: Boolean? = null
            )

            @Serializable
            data class XhttpSettingsBean(
                var path: String? = null,
                var host: String? = null,
                var mode: String? = null,
                var extra: JsonObject? = null,
            )

            @Serializable
            data class HttpSettingsBean(
                var host: List<String> = emptyList(), var path: String? = null
            )

            @Serializable
            data class SockoptBean(
                var tcpKeepAliveIdle: Int? = null,
                var tcpFastOpen: Boolean? = null,
                var tproxy: String? = null,
                var mark: Int? = null,
                var dialerProxy: String? = null,
                var domainStrategy: String? = null,
                var happyEyeballs: HappyEyeballsBean? = null,
            )

            @Serializable
            data class HappyEyeballsBean(
                var prioritizeIPv6: Boolean? = null,
                var maxConcurrentTry: Int? = 4,
                var tryDelayMs: Int? = 250, // ms
                var interleave: Int? = null,
            )

            @Serializable
            data class TlsSettingsBean(
                var allowInsecure: Boolean = false,
                var serverName: String? = null,
                val alpn: List<String>? = null,
                val minVersion: String? = null,
                val maxVersion: String? = null,
                val preferServerCipherSuites: Boolean? = null,
                val cipherSuites: String? = null,
                val fingerprint: String? = null,
                val certificates: List<JsonElement>? = null,
                val disableSystemRoot: Boolean? = null,
                val enableSessionResumption: Boolean? = null,
                var echConfigList: String? = null,
                var pinnedPeerCertSha256: String? = null,
                // REALITY settings
                val show: Boolean = false,
                var publicKey: String? = null,
                var shortId: String? = null,
                var spiderX: String? = null,
                var mldsa65Verify: String? = null
            )

            @Serializable
            data class QuicSettingBean(
                var security: String = "none",
                var key: String = "",
                var header: HeaderBean = HeaderBean()
            ) {
                @Serializable
                data class HeaderBean(var type: String = "none")
            }

            @Serializable
            data class GrpcSettingsBean(
                var serviceName: String = "",
                var authority: String? = null,
                var multiMode: Boolean? = null,
                @SerialName("idle_timeout") var idleTimeout: Int? = null,
                @SerialName("health_check_timeout") var healthCheckTimeout: Int? = null
            )

            @Serializable
            data class HysteriaSettingsBean(
                var version: Int, var auth: String? = null
            )

            @Serializable
            // https://xtls.github.io/config/transport.html#finalmaskobject
            data class FinalMaskBean(
                var tcp: List<MaskBean>? = null,
                var udp: List<MaskBean>? = null,
                var quicParams: QuicParamsBean? = null
            ) {
                @Serializable
                data class MaskBean(
                    var type: String, var settings: MaskSettingsBean? = null
                ) {
                    @Serializable
                    data class MaskSettingsBean(
                        val password: String? = null,
                        val domain: String? = null,
                        val header: String? = null,
                        val value: String? = null,
                        // fragment
                        val packets: String? = null,
                        val length: String? = null,
                        val delay: String? = null,
                        // val maxSplit: String? = null,
                        // noise
                        val reset: Int? = null,
                        val noise: List<NoiseMaskBean>? = null
                    ) {
                        @Serializable
                        data class NoiseMaskBean(
                            val rand: String? = null,
                            // val randRange: String? = null,
                            // val type: String? = null,
                            // val packet: String? = null,
                            val delay: String? = null,
                        )
                    }
                }

                @Serializable
                data class QuicParamsBean(
                    var congestion: String? = null,
                    var brutalUp: String? = null,
                    var brutalDown: String? = null,
                    var udpHop: UdpHopBean? = null,
                ) {
                    // Nested data class for the udpHop JSON object
                    @Serializable
                    data class UdpHopBean(
                        var ports: String? = null, var interval: String? = null
                    )
                }
            }
        }

        @Serializable
        data class MuxBean(
            var enabled: Boolean,
            var concurrency: Int? = null,
            var xudpConcurrency: Int? = null,
            var xudpProxyUDP443: String? = null,
        )

        fun getServerAddress(): String? {
            if (arrayOf(
                    EConfigType.VMESS, EConfigType.VLESS
                ).any { protocol.equals(GlobalConst.configTypeProtocolMap[it], ignoreCase = true) }
            ) {
                val address = settings?.vnext?.firstOrNull()?.address
                if (!address.isNullOrEmpty()) {
                    return address
                }
            } else if (arrayOf(
                    EConfigType.SHADOWSOCKS,
                    EConfigType.SOCKS5,
                    EConfigType.HTTP,
                    EConfigType.TROJAN
                ).any { protocol.equals(GlobalConst.configTypeProtocolMap[it], ignoreCase = true) }
            ) {
                val address = settings?.servers?.firstOrNull()?.address
                if (!address.isNullOrEmpty()) {
                    return address
                }
            } else if (protocol.equals(
                    GlobalConst.configTypeProtocolMap[EConfigType.WIREGUARD], ignoreCase = true
                )
            ) {
                return settings?.peers?.firstOrNull()?.endpoint?.substringBeforeLast(":")
            }
            val primitive = settings?.address as? JsonPrimitive ?: return null
            return if (primitive.isString) primitive.content else null
        }

        fun getServerPort(): Int? {
            if (arrayOf(
                    EConfigType.VMESS, EConfigType.VLESS
                ).any { protocol.equals(GlobalConst.configTypeProtocolMap[it], ignoreCase = true) }
            ) {
                return settings?.vnext?.firstOrNull()?.port ?: settings?.port
            } else if (arrayOf(
                    EConfigType.SHADOWSOCKS,
                    EConfigType.SOCKS5,
                    EConfigType.HTTP,
                    EConfigType.TROJAN
                ).any { protocol.equals(GlobalConst.configTypeProtocolMap[it], ignoreCase = true) }
            ) {
                return settings?.servers?.firstOrNull()?.port ?: settings?.port
            } else if (protocol.equals(
                    GlobalConst.configTypeProtocolMap[EConfigType.WIREGUARD], ignoreCase = true
                )
            ) {
                return settings?.peers?.firstOrNull()?.endpoint?.substringAfterLast(":")?.toInt()
            } else if (protocol.equals(
                    GlobalConst.configTypeProtocolMap[EConfigType.HYSTERIA2], ignoreCase = true
                )
            ) {
                return settings?.port
            }
            return null
        }

        fun ensureSockopt(): StreamSettingsBean.SockoptBean {
            val stream = streamSettings ?: StreamSettingsBean().also {
                streamSettings = it
            }

            val sockopt = stream.sockopt ?: StreamSettingsBean.SockoptBean().also {
                stream.sockopt = it
            }

            return sockopt
        }
    }

    @Serializable
    data class DnsBean(
        var servers: List<JsonElement>? = null,
        var hosts: Map<String, List<String>>? = null,
        val clientIp: String? = null,
        val disableCache: Boolean? = null,
        val queryStrategy: String? = null,
        var enableParallelQuery: Boolean? = null,
        var serveStale: Boolean? = null,
        val tag: String? = null
    ) {
        @Serializable
        data class ServersBean(
            var address: String = "",
            var port: Int? = null,
            var domains: List<String>? = null,
            var expectIPs: List<String>? = null,
            val clientIp: String? = null,
            val skipFallback: Boolean? = null,
            val tag: String? = null,
        )
    }

    @Serializable
    data class RoutingBean(
        var domainStrategy: String? = null,
        var domainMatcher: String? = null,
        var rules: MutableList<RulesBean>,
        var balancers: List<BalancerBean>? = null
    ) {
        @Serializable
        data class RulesBean(
            var type: String = "field",
            var ip: List<String>? = null,
            var domain: List<String>? = null,
            var process: List<String>? = null,
            var outboundTag: String? = null,
            var balancerTag: String? = null,
            var port: String? = null,
            val sourcePort: String? = null,
            val network: String? = null,
            val source: List<String>? = null,
            val user: List<String>? = null,
            var inboundTag: List<String>? = null,
            val protocol: List<String>? = null,
            val attrs: String? = null,
            val domainMatcher: String? = null
        )

        @Serializable
        data class BalancerBean(
            val tag: String,
            val selector: List<String>,
            val fallbackTag: String? = null,
            val strategy: StrategyObject? = null
        ) {

            @Serializable
            data class StrategyObject(
                val type: String = "random", // "random" | "roundRobin" | "leastPing" | "leastLoad"
                val settings: StrategySettingsObject? = null
            ) {
                @Serializable
                data class StrategySettingsObject(
                    val expected: Int? = null,
                    val maxRTT: String? = null,
                    val tolerance: Double? = null,
                    val baselines: List<String>? = null,
                    val costs: List<CostObject>? = null
                ) {
                    @Serializable
                    data class CostObject(
                        val regexp: Boolean = false, val match: String, val value: Double
                    )
                }
            }
        }
    }

    @Serializable
    data class PolicyBean(
        var levels: Map<String, LevelBean>, var system: SystemBean? = null
    ) {
        @Serializable
        data class LevelBean(
            var handshake: Int? = null,
            var connIdle: Int? = null,
            var uplinkOnly: Int? = null,
            var downlinkOnly: Int? = null,
            val statsUserUplink: Boolean? = null,
            val statsUserDownlink: Boolean? = null,
            var bufferSize: Int? = null
        )

        @Serializable
        data class SystemBean(
            var statsInboundUplink: Boolean? = null,
            var statsInboundDownlink: Boolean? = null,
            var statsOutboundUplink: Boolean? = null,
            var statsOutboundDownlink: Boolean? = null
        )
    }

    @Serializable
    data class ObservatoryObject(
        val subjectSelector: List<String>,
        val probeUrl: String,
        val probeInterval: String,
        val enableConcurrency: Boolean = false
    )

    @Serializable
    data class BurstObservatoryObject(
        val subjectSelector: List<String>, val pingConfig: PingConfigObject
    ) {
        @Serializable
        data class PingConfigObject(
            val destination: String,
            val connectivity: String? = null,
            val interval: String,
            val sampling: Int,
            val timeout: String? = null
        )
    }

    @Serializable
    data class FakednsBean(
        var ipPool: String = "198.18.0.0/15", var poolSize: Int = 10000
    )

    fun getProxyOutbound(): OutboundBean? {
        return outbounds.firstOrNull { outbound ->
            GlobalConst.configTypeProtocolReverseMap.any {
                it.key.equals(
                    outbound.protocol, ignoreCase = true
                )
            }
        }
    }

    fun getAllProxyOutbound(): List<OutboundBean> {
        return outbounds.filter { outbound ->
            GlobalConst.configTypeProtocolReverseMap.any {
                it.key.equals(
                    outbound.protocol, ignoreCase = true
                )
            }
        }
    }
}
