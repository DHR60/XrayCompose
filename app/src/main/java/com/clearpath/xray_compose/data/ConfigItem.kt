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
    val lastUpdate: Long = 0,
)

@Serializable
data class ConfigEngineItem(
    val id: String = UuidCreator.getTimeOrderedEpoch().toString(),
    val remark: String = "",
    val routing: ConfigRoutingItem = ConfigRoutingItem(),
    val inbound: ConfigInboundItem = ConfigInboundItem(),
    val dns: ConfigDnsItem = ConfigDnsItem(),
    val perApp: ConfigPerAppItem = ConfigPerAppItem(),
    val misc: ConfigMiscItem = ConfigMiscItem(),
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
    val ip: List<String> = emptyList(),
    val domain: List<String> = emptyList(),
)

@Serializable
data class ConfigInboundItem(
    val port: Int = GlobalConst.defaultSocksPort,
    val sniff: Boolean = true,
    val sniffOverrideDest: Boolean = false,
    val allowLan: Boolean = false,
    val tun: ConfigTunInboundItem = ConfigTunInboundItem(),
) {
    @Serializable
    data class ConfigTunInboundItem(
        val enable: Boolean = true,
        val mtu: Int = 1500,
        val addressCIDRList: List<String> = emptyList(),
        val dnsList: List<String> = emptyList(),
        val excludeCIDRList: List<String> = emptyList(),
    )
}

@Serializable
data class ConfigDnsItem(
    val remoteDns: String = GlobalConst.defaultRemoteDns,
    val localDns: String = GlobalConst.defaultLocalDns,
    val enableFakeDns: Boolean = false,
    val additionalHosts: String = "",
    val serveStale: Boolean = false,
    val parallelQuery: Boolean = false,
)

@Serializable
data class ConfigPerAppItem(
    val enable: Boolean = false,
    val bypass: Boolean = false,
    val packageList: List<String> = emptyList(),
)

@Serializable
data class ConfigMiscItem(
    val test: ConfigTestItem = ConfigTestItem(),
) {
    @Serializable
    data class ConfigTestItem(
        val testUrl: String = "",
        val testBatchSize: Int = 10,
    )
}
