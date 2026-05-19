package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext

class XrayConfigInboundProcessor(
    private val ecContext: EngineConfigContext,
    private val config: XrayConfig
) {
    fun genInbounds() {
        val inbounds = mutableListOf<XrayConfig.InboundBean>()
        inbounds.add(buildSocksInbound())
        if (ecContext.isTunEnabled) {
            inbounds.add(buildTunInbound())
        }
        config.inbounds.addAll(inbounds)
    }

    private fun buildTunInbound(): XrayConfig.InboundBean {
        return XrayConfig.InboundBean(
            tag = GlobalConst.tunInboundTag,
            protocol = "tun",
            settings = XrayConfig.InboundBean.InSettingsBean(
                mtu = 1500,
                name = "xray0",
                userLevel = GlobalConst.defaultLevel,
            ),
            sniffing = buildSniffing(),
        )
    }

    private fun buildSocksInbound(): XrayConfig.InboundBean {
        return XrayConfig.InboundBean(
            tag = GlobalConst.socksInboundTag,
            protocol = "socks",
            port = ecContext.engineConfig.inbound.port,
            settings = XrayConfig.InboundBean.InSettingsBean(
                auth = "noauth",
                udp = true,
                userLevel = GlobalConst.defaultLevel,
            ),
            sniffing = buildSniffing(),
        )
    }

    private fun buildSniffing(): XrayConfig.InboundBean.SniffingBean {
        return XrayConfig.InboundBean.SniffingBean(
            enabled = true,
            destOverride = listOf("http", "tls"),
        )
    }
}