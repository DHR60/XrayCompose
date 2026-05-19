package com.clearpath.xray_compose.service.engine.config

import com.clearpath.xray_compose.service.engine.config.processor.XrayConfigDnsProcessor
import com.clearpath.xray_compose.service.engine.config.processor.XrayConfigInboundProcessor
import com.clearpath.xray_compose.service.engine.config.processor.XrayConfigLogProcessor
import com.clearpath.xray_compose.service.engine.config.processor.XrayConfigOutboundProcessor
import com.clearpath.xray_compose.service.engine.config.processor.XrayConfigPolicyProcessor
import com.clearpath.xray_compose.service.engine.config.processor.XrayConfigRoutingProcessor
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.utils.JsonUtil

class XrayConfigService(private val ecContext: EngineConfigContext) {
    private val xrayConfig: XrayConfig = XrayConfig()

    private val logProcessor = XrayConfigLogProcessor(ecContext, xrayConfig)
    private val policyProcessor = XrayConfigPolicyProcessor(ecContext, xrayConfig)
    private val inboundProcessor = XrayConfigInboundProcessor(ecContext, xrayConfig)
    private val outboundProcessor = XrayConfigOutboundProcessor(ecContext, xrayConfig)
    private val routingProcessor = XrayConfigRoutingProcessor(ecContext, xrayConfig)
    private val dnsProcessor = XrayConfigDnsProcessor(ecContext, xrayConfig)

    fun buildBaseConfig(): String {
        genNormalConfig()
        return JsonUtil.defaultJson.encodeToString(XrayConfig.serializer(), xrayConfig)
    }

    private fun genNormalConfig() {
        xrayConfig.remarks = ecContext.node.remark
        logProcessor.genLog()
        policyProcessor.genPolicy()
        policyProcessor.genStats()
        inboundProcessor.genInbounds()
        outboundProcessor.genOutbounds()
        routingProcessor.genRouting()
        dnsProcessor.genDns()
    }
}