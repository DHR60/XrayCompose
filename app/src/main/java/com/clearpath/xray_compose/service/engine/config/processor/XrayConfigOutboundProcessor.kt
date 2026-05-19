package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.EConfigType
import com.clearpath.xray_compose.enums.ETransport
import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.utils.JsonUtil
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
        var network = ecContext.node.getNetworkType()
        if (ecContext.node.configType == EConfigType.HYSTERIA2) {
            network = "hysteria"
        }
        streamSettings.network = network

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

        when (network) {
            GlobalConst.transportReverseMap[ETransport.RAW] -> {
                // do nothing
            }

            GlobalConst.transportReverseMap[ETransport.XHTTP] -> {
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
        }

        return streamSettings
    }
}