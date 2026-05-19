package com.clearpath.xray_compose.data

import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.enums.ERuleType
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.serialization.Serializable

@Serializable
data class ConfigItem(
    val subList: List<ConfigSubItem> = emptyList(),
    val engineSettingList: List<ConfigEngineItem> = listOf(ConfigEngineItem()),
)

@Serializable
data class ConfigSubItem(
    val id: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val remark: String = "",
    val url: String = "",
    val userAgent: String = "",
    val filter: String = "",
    val lastUpdateTime: String = "",
)

@Serializable
data class ConfigEngineItem(
    val id: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val remark: String = "",
    val routing: ConfigRoutingItem = ConfigRoutingItem(),
    val inbound: ConfigInboundItem = ConfigInboundItem(),
    val dns: ConfigDnsItem = ConfigDnsItem(),
)


@Serializable
data class ConfigRoutingItem(
    val remark: String = "",
    val rules: List<ConfigRuleItem> = emptyList(),
    val domainStrategy: String = "",
    val enableAutoDetachment: Boolean = false,
)

@Serializable
data class ConfigRuleItem(
    val id: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val remark: String = "",
    val ruleType: ERuleType = ERuleType.ALL,
    val outboundTag: String = "",
    val enable: Boolean = true,
    val ip: String = "",
    val domain: String = "",
)

@Serializable
data class ConfigInboundItem(
    val port: Int = GlobalConst.defaultSocksPort,
    val sniff: Boolean = true,
    val sniffOverrideDest: Boolean = false,
    val allowLan: Boolean = false,
    val disableTun: Boolean = false,
)

@Serializable
data class ConfigDnsItem(
    val remoteDns: String = GlobalConst.defaultRemoteDns,
    val localDns: String = GlobalConst.defaultLocalDns,
    val enableFakeDns: Boolean = false,
    val additionalHosts: String = "",
    val serveStale: Boolean = false,
    val parallelQuery: Boolean = false,
)
