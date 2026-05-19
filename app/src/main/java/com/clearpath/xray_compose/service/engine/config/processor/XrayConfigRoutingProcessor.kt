package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.ERuleType
import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext

class XrayConfigRoutingProcessor(
    private val ecContext: EngineConfigContext,
    private val config: XrayConfig
) {
    fun genRouting() {
        val ruleList = mutableListOf<XrayConfig.RoutingBean.RulesBean>()
        if (ecContext.isTunEnabled) {
            ruleList.add(
                XrayConfig.RoutingBean.RulesBean(
                    inboundTag = listOf(GlobalConst.tunInboundTag),
                    port = "53",
                    outboundTag = GlobalConst.dnsOutboundTag,
                )
            )
        }
        if (!ecContext.engineConfig.routing.enableAutoDetachment) {
            ecContext.engineConfig.routing.rules.forEach {
                if (it.ruleType == ERuleType.DNS || !it.enable) {
                    return@forEach
                }
                ruleList.add(
                    XrayConfig.RoutingBean.RulesBean(
                        ip = it.ip.split(",").map(String::trim).filter(String::isNotEmpty)
                            .ifEmpty { null },
                        domain = it.domain.split(",").map(String::trim).filter(String::isNotEmpty)
                            .ifEmpty { null },
                        outboundTag = it.outboundTag,
                    )
                )
            }
        } else {
            ecContext.engineConfig.routing.rules.forEach {
                if (it.ruleType == ERuleType.DNS || !it.enable) {
                    return@forEach
                }
                val rule = XrayConfig.RoutingBean.RulesBean(
                    ip = it.ip.split(",").map(String::trim).filter(String::isNotEmpty)
                        .ifEmpty { null },
                    domain = it.domain.split(",").map(String::trim).filter(String::isNotEmpty)
                        .ifEmpty { null },
                    outboundTag = it.outboundTag,
                )
                if (!rule.ip.isNullOrEmpty()) {
                    ruleList.add(rule.copy(domain = emptyList()))
                }
                if (!rule.domain.isNullOrEmpty()) {
                    ruleList.add(rule.copy(ip = emptyList()))
                }
            }
        }
        val routingBean = XrayConfig.RoutingBean(
            domainStrategy = ecContext.engineConfig.routing.domainStrategy.ifEmpty { GlobalConst.AsIs },
            rules = ruleList,
        )
        config.routing = routingBean
    }
}