package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import kotlinx.serialization.json.buildJsonObject

class XrayConfigPolicyProcessor(
    private val ecContext: EngineConfigContext,
    private val config: XrayConfig
) {
    fun genPolicy() {
        config.policy = XrayConfig.PolicyBean(
            levels = mapOf(
                "8" to XrayConfig.PolicyBean.LevelBean(
                    connIdle = 300,
                    downlinkOnly = 1,
                    handshake = 4,
                    uplinkOnly = 1,
                )
            ),
            system = XrayConfig.PolicyBean.SystemBean(
                statsOutboundUplink = true,
                statsOutboundDownlink = true,
            )
        )
    }

    fun genStats() {
        config.stats = buildJsonObject { }
    }
}