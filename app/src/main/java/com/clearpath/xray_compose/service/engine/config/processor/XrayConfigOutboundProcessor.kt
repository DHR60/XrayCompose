package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.enums.ETransport
import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.utils.JsonUtil
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

class XrayConfigOutboundProcessor(
    private val ecContext: EngineConfigContext,
    private val config: XrayConfig
) {
    fun genOutbounds() {
        val outbounds = mutableListOf<XrayConfig.OutboundBean>()
        outbounds.addAll(buildAllProxyOutbounds())
        outbounds.add(buildDirectOutbound())
        outbounds.add(buildBlockOutbound())
        if (ecContext.isTunEnabled) {
            outbounds.add(buildDnsOutbound())
        }
        config.outbounds.addAll(outbounds)
    }

    private fun buildAllProxyOutbounds(): List<XrayConfig.OutboundBean> {
        return listOf(buildProxyOutbound())
    }

    private fun buildDirectOutbound(): XrayConfig.OutboundBean {
        return XrayConfig.OutboundBean(
            protocol = "freedom",
            tag = GlobalConst.directTag,
            streamSettings = XrayConfig.OutboundBean.StreamSettingsBean(
                sockopt = XrayConfig.OutboundBean.StreamSettingsBean.SockoptBean(
                    domainStrategy = "UseIP"
                )
            )
        )
    }

    private fun buildBlockOutbound(): XrayConfig.OutboundBean {
        return XrayConfig.OutboundBean(
            protocol = "blackhole",
            tag = GlobalConst.blockTag,
        )
    }

    private fun buildDnsOutbound(): XrayConfig.OutboundBean {
        return XrayConfig.OutboundBean(
            protocol = "dns",
            tag = GlobalConst.dnsOutboundTag,
        )
    }

    private fun buildProxyOutbound(baseTagName: String = GlobalConst.proxyTag): XrayConfig.OutboundBean {
        val outboundBean = XrayConfig.OutboundBean(
            protocol = GlobalConst.configTypeProtocolMap[ecContext.node.configType]
                ?: GlobalConst.configTypeProtocolMap[EConfigType.VLESS]!!,
            tag = baseTagName,
        )
        val muxEnabled = ecContext.node.muxEnabled == GlobalConst.trueStr

        val protocolExtra = ecContext.node.protocolExtra
        if (ecContext.node.configType == EConfigType.VLESS) {
            val vnextItem = XrayConfig.OutboundBean.OutSettingsBean.VnextBean(
                address = ecContext.node.address,
                port = ecContext.node.port,
                users = listOf(
                    XrayConfig.OutboundBean.OutSettingsBean.VnextBean.UsersBean(
                        id = ecContext.node.password,
                        flow = if (protocolExtra.flow.isNullOrEmpty()) null else protocolExtra.flow,
                        level = GlobalConst.defaultLevel,
                        encryption = if (protocolExtra.vlessEncryption.isNullOrEmpty()) GlobalConst.none else protocolExtra.vlessEncryption,
                    )
                )
            )
            outboundBean.settings = XrayConfig.OutboundBean.OutSettingsBean(
                vnext = listOf(vnextItem)
            )
            if (protocolExtra.flow.isNullOrEmpty()) {
                outboundBean.mux = buildMuxBean(muxEnabled, muxEnabled)
            } else {
                outboundBean.mux = buildMuxBean(tcpMux = false, udpMux = muxEnabled)
            }
        } else if (ecContext.node.configType == EConfigType.HYSTERIA2) {
            outboundBean.settings = XrayConfig.OutboundBean.OutSettingsBean(
                version = 2,
                address = JsonPrimitive(ecContext.node.address),
                port = ecContext.node.port,
                vnext = null,
                servers = null,
            )
        } else if (ecContext.node.configType == EConfigType.TROJAN) {
            val serverItem = XrayConfig.OutboundBean.OutSettingsBean.ServersBean(
                address = ecContext.node.address,
                port = ecContext.node.port,
                password = ecContext.node.password,
                level = GlobalConst.defaultLevel,
            )
            outboundBean.settings = XrayConfig.OutboundBean.OutSettingsBean(
                servers = listOf(serverItem)
            )
            if (muxEnabled) {
                outboundBean.mux = buildMuxBean(tcpMux = true, udpMux = false)
            }
        }

        outboundBean.streamSettings = buildBoundStreamSettings()

        return outboundBean
    }

    private fun buildMuxBean(
        tcpMux: Boolean = false,
        udpMux: Boolean = false
    ): XrayConfig.OutboundBean.MuxBean {
        val muxBean = XrayConfig.OutboundBean.MuxBean(
            enabled = tcpMux || udpMux,
            concurrency = -1,
        )

        if (tcpMux) {
            muxBean.concurrency = 8
        } else if (udpMux) {
            muxBean.xudpConcurrency = 8
            muxBean.xudpProxyUDP443 = "reject"
        }

        return muxBean
    }

    private fun buildBoundStreamSettings(): XrayConfig.OutboundBean.StreamSettingsBean {
        val streamSettings = XrayConfig.OutboundBean.StreamSettingsBean()

        val transportExtra = ecContext.node.transportExtra
        var networkName = ecContext.node.getNetworkName()
        if (ecContext.node.configType == EConfigType.HYSTERIA2) {
            networkName = "hysteria"
        }
        streamSettings.network = networkName

        // tls
        if (ecContext.node.streamSecurity == GlobalConst.transportSecurityTls) {
            streamSettings.security = GlobalConst.transportSecurityTls
            streamSettings.tlsSettings = XrayConfig.OutboundBean.StreamSettingsBean.TlsSettingsBean(
                serverName = ecContext.node.sni.ifEmpty {
                    transportExtra.host?.split(",")?.firstOrNull()
                },
                allowInsecure = ecContext.node.allowInsecure == GlobalConst.trueStr && ecContext.node.certSha.isEmpty(),
                alpn = if (ecContext.node.alpn.isEmpty()) null else ecContext.node.alpn.split(","),
                fingerprint = ecContext.node.utlsFingerprint.ifEmpty { null },
                echConfigList = ecContext.node.echConfigList.ifEmpty { null },
                pinnedPeerCertSha256 = ecContext.node.certSha.ifEmpty { null },
            )
        } else if (ecContext.node.streamSecurity == GlobalConst.transportSecurityReality) {
            streamSettings.security = GlobalConst.transportSecurityReality
            streamSettings.realitySettings =
                XrayConfig.OutboundBean.StreamSettingsBean.TlsSettingsBean(
                    serverName = ecContext.node.sni.ifEmpty {
                        transportExtra.host?.split(",")?.firstOrNull()
                    },
                    fingerprint = ecContext.node.utlsFingerprint.ifEmpty { null },
                    publicKey = ecContext.node.realityPublicKey.ifEmpty { null },
                    shortId = ecContext.node.realityShortId.ifEmpty { null },
                    spiderX = ecContext.node.realitySpiderX.ifEmpty { null },
                    mldsa65Verify = ecContext.node.realityMldsa65Verify.ifEmpty { null },
                )
        }

        when (networkName) {
            ETransport.RAW.value -> {
                // do nothing
            }

            ETransport.XHTTP.value -> {
                streamSettings.xhttpSettings =
                    XrayConfig.OutboundBean.StreamSettingsBean.XhttpSettingsBean(
                        host = transportExtra.host,
                        path = transportExtra.path,
                        mode = if (transportExtra.xhttpMode.isNullOrEmpty()) "auto" else transportExtra.xhttpMode,
                        extra = if (transportExtra.xhttpExtra.isNullOrEmpty()) null else JsonUtil.defaultJson.parseToJsonElement(
                            transportExtra.xhttpExtra
                        ).jsonObject,
                    )
            }

            ETransport.WS.value -> {
                streamSettings.wsSettings =
                    XrayConfig.OutboundBean.StreamSettingsBean.WsSettingsBean(
                        host = transportExtra.host,
                        path = transportExtra.path,
                    )
            }

            ETransport.HTTPUPGRADE.value -> {
                streamSettings.httpupgradeSettings =
                    XrayConfig.OutboundBean.StreamSettingsBean.HttpupgradeSettingsBean(
                        host = transportExtra.host,
                        path = transportExtra.path,
                    )
            }

            ETransport.GRPC.value -> {
                streamSettings.grpcSettings =
                    XrayConfig.OutboundBean.StreamSettingsBean.GrpcSettingsBean(
                        authority = transportExtra.grpcAuthority,
                        serviceName = transportExtra.grpcServiceName ?: "",
                        multiMode = transportExtra.grpcMode == GlobalConst.multiGrpcMode,
                    )
            }

            ETransport.KCP.value -> {
                streamSettings.kcpSettings =
                    XrayConfig.OutboundBean.StreamSettingsBean.KcpSettingsBean(
                        mtu = transportExtra.kcpMtu?.toIntOrNull() ?: 1350,
                    )
                val udpMaskList =
                    mutableListOf<XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean>()
                val headerTypeValue = GlobalConst.kcpHeaderMap[transportExtra.kcpHeaderType]
                if (headerTypeValue != null) {
                    udpMaskList.add(
                        XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean(
                            type = "mkcp-legacy",
                            settings = XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean.MaskSettingsBean(
                                header = headerTypeValue
                            )
                        )
                    )
                }
                if (transportExtra.kcpSeed.isNullOrEmpty()) {
                    udpMaskList.add(
                        XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean(
                            type = "mkcp-legacy"
                        )
                    )
                } else {
                    udpMaskList.add(
                        XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean(
                            type = "mkcp-legacy",
                            settings = XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean.MaskSettingsBean(
                                value = transportExtra.kcpSeed
                            )
                        )
                    )
                }
                streamSettings.finalmask = JsonUtil.defaultJson.encodeToJsonElement(
                    XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean(
                        udp = udpMaskList
                    )
                )
            }

            "hysteria" -> {
                val protocolExtra = ecContext.node.protocolExtra
                val ports = protocolExtra.ports ?: ""
                val hopInterval = protocolExtra.hopInterval ?: ""
                val upMbps = protocolExtra.upMbps?.toIntOrNull() ?: 0
                val downMbps = protocolExtra.downMbps?.toIntOrNull() ?: 0
                // quicParams
                val quicParams =
                    XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.QuicParamsBean()
                if (listOf(":", "-", ",").any { ports.contains(it) }) {
                    quicParams.udpHop =
                        XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.QuicParamsBean.UdpHopBean(
                            ports = ports.replace(":", "-"),
                            interval = hopInterval.ifEmpty { "30" }
                        )
                }
                if (upMbps > 0 || downMbps > 0) {
                    quicParams.congestion = "brutal"
                    quicParams.brutalUp = if (upMbps > 0) "${upMbps}mbps" else null
                    quicParams.brutalDown = if (downMbps > 0) "${downMbps}mbps" else null
                } else {
                    quicParams.congestion = "bbr"
                }
                streamSettings.finalmask = JsonUtil.defaultJson.encodeToJsonElement(
                    XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean(
                        udp = if (!protocolExtra.salamanderPass.isNullOrEmpty()) {
                            // salamander
                            listOf(
                                XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean(
                                    type = "salamander",
                                    settings = XrayConfig.OutboundBean.StreamSettingsBean.FinalMaskBean.MaskBean.MaskSettingsBean(
                                        value = protocolExtra.salamanderPass
                                    )
                                )
                            )
                        } else {
                            null
                        },
                        quicParams = quicParams
                    )
                )
            }
        }

        if (ecContext.node.finalmask.isNotEmpty()) {
            streamSettings.finalmask = JsonUtil.defaultJson.parseToJsonElement(
                ecContext.node.finalmask
            )
        }

        return streamSettings
    }
}