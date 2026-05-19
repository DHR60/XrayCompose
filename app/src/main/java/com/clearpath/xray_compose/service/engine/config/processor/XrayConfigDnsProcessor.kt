package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.ERuleType
import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.utils.JsonUtil
import com.clearpath.xray_compose.utils.Utils
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

class XrayConfigDnsProcessor(
    private val ecContext: EngineConfigContext,
    private val config: XrayConfig
) {
    fun genDns() {
        val proxyDomains = mutableListOf<String>()
        val directDomains = mutableListOf<String>()

        ecContext.engineConfig.routing.rules.forEach {
            if (it.ruleType != ERuleType.DNS) return@forEach
            if (it.outboundTag == GlobalConst.directTag) {
                directDomains.add(it.domain)
            } else if (it.outboundTag == GlobalConst.blockTag
                || it.outboundTag.isEmpty()
            ) {
                return@forEach
            } else {
                proxyDomains.add(it.domain)
            }
        }

        val remoteDnsList =
            ecContext.engineConfig.dns.remoteDns.split(",").filter { Utils.isCoreDNSAddress(it) }
                .ifEmpty {
                    GlobalConst.defaultRemoteDns.split(",").filter { Utils.isCoreDNSAddress(it) }
                }
        val localDnsList =
            ecContext.engineConfig.dns.localDns.split(",").filter { Utils.isCoreDNSAddress(it) }
                .ifEmpty {
                    GlobalConst.defaultLocalDns.split(",").filter { Utils.isCoreDNSAddress(it) }
                }

        val servers = mutableListOf<JsonElement>()

        val fakeDnsDomains = (proxyDomains + directDomains).distinct()
        if (ecContext.engineConfig.dns.enableFakeDns
            && fakeDnsDomains.isNotEmpty()
        ) {
            genFakeDns()
            servers.add(
                JsonUtil.defaultJson.encodeToJsonElement(
                    XrayConfig.DnsBean
                        .ServersBean(
                            address = "fakedns",
                            domains = fakeDnsDomains,
                        )
                )
            )
        }

        if (proxyDomains.isNotEmpty()) {
            servers.addAll(
                remoteDnsList.map {
                    XrayConfig.DnsBean.ServersBean(
                        address = it,
                        domains = proxyDomains.distinct(),
                        skipFallback = true,
                    )
                }.map { JsonUtil.defaultJson.encodeToJsonElement(it) }
            )
        }

        val localDnsTags = mutableListOf<String>()
        if (directDomains.isNotEmpty()) {
            val isCnRoutingMode = directDomains.contains(GlobalConst.geositeCN)
            val cnRegionFilter = { domain: String ->
                domain.startsWith("geosite:") && (domain.endsWith("-cn") || domain.endsWith("@cn"))
                        || domain == GlobalConst.geositeCN
            }
            val finalDirectDomains = if (isCnRoutingMode) directDomains.filterNot {
                cnRegionFilter(it)
            } else directDomains
            localDnsList.forEachIndexed { index, element ->
                val tag = GlobalConst.localDnsModuleTag + index
                servers.add(
                    JsonUtil.defaultJson.encodeToJsonElement(
                        XrayConfig.DnsBean.ServersBean(
                            address = element,
                            domains = finalDirectDomains,
                            skipFallback = true,
                            tag = tag
                        )
                    )
                )
                localDnsTags.add(tag)
            }
            if (isCnRoutingMode) {
                val geoipCn = listOf(GlobalConst.geoipCN)
                val cnRegionDomain = directDomains.filter(cnRegionFilter)
                localDnsList.forEachIndexed { index, element ->
                    val tag = GlobalConst.localDnsModuleTag + index + "_cn_expect"
                    servers.add(
                        JsonUtil.defaultJson.encodeToJsonElement(
                            XrayConfig.DnsBean.ServersBean(
                                address = element,
                                domains = cnRegionDomain,
                                expectIPs = geoipCn,
                                skipFallback = true,
                                tag = tag
                            )
                        )
                    )
                    localDnsTags.add(tag)
                }
            }
        }

        servers.addAll(remoteDnsList.map { JsonPrimitive(it) })

        val hosts = mutableMapOf<String, List<String>>()

        GlobalConst.commonHostPredefinedMap.forEach { (key, value) ->
            if (localDnsList.any { it.contains(key) }
                || remoteDnsList.any { it.contains(key) }
            ) {
                hosts[key] = value
            }
        }

        val userHosts = ecContext.engineConfig.dns.additionalHosts
        if (!userHosts.isEmpty()) {
            val userHostsMap = userHosts.lines()
                .filter { it.isNotEmpty() }
                .filter { it.contains(" ") }
                .associate { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    val key = parts[0]
                    val values = parts.drop(1)
                    key to values
                }
            hosts.putAll(userHostsMap)
        }

        config.dns = XrayConfig.DnsBean(
            servers = servers,
            hosts = hosts,
            tag = GlobalConst.dnsModuleTag,
            enableParallelQuery = if ((localDnsList.size + remoteDnsList.size) > 2
                || ecContext.engineConfig.dns.parallelQuery
            ) true else null,
            serveStale = if (ecContext.engineConfig.dns.serveStale) true else null,
        )

        // DNS routing
        if (localDnsTags.isNotEmpty()) {
            config.routing?.rules?.add(
                XrayConfig.RoutingBean.RulesBean(
                    inboundTag = localDnsTags,
                    outboundTag = GlobalConst.directTag,
                    domain = null,
                )
            )
        }
        config.routing?.rules?.add(
            XrayConfig.RoutingBean.RulesBean(
                inboundTag = listOf(GlobalConst.dnsModuleTag),
                outboundTag = GlobalConst.proxyTag,
                domain = null,
            )
        )
    }

    private fun genFakeDns() {
        config.fakedns = XrayConfig.FakednsBean()
    }
}