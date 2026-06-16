package com.clearpath.xray_compose.data.repo

import androidx.datastore.core.DataStore
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ConfigItem
import com.clearpath.xray_compose.data.ConfigRoutingItem
import com.clearpath.xray_compose.data.ConfigRuleItem
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.utils.LogUtil
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(private val dataStore: DataStore<ConfigItem>) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            try {
                dataStore.updateData { ensureDefaultConfig(it) }
            } catch (e: Exception) {
                LogUtil.e("ConfigRepository ensures default configuration fails", e)
            }
        }
    }

    private fun ensureDefaultConfig(currentData: ConfigItem): ConfigItem {
        val hasDefaultConfig = currentData.engineSettingList.any { it.remark == "默认配置" }
        val isEmptyConfig = currentData.engineSettingList.isEmpty() ||
                (currentData.engineSettingList.size == 1 &&
                        currentData.engineSettingList.first().remark.isEmpty() &&
                        currentData.engineSettingList.first().routing.rules.isEmpty())

        if (hasDefaultConfig && !isEmptyConfig) {
            return currentData
        }

        val defaultRules = listOf(
            ConfigRuleItem(
                remark = "绕过局域网 IP",
                outboundTag = GlobalConst.directTag,
                ip = listOf("geoip:private")
            ),
            ConfigRuleItem(
                remark = "绕过局域网域名",
                outboundTag = GlobalConst.directTag,
                domain = listOf("geosite:private")
            ),
            ConfigRuleItem(
                remark = "代理 Google",
                outboundTag = GlobalConst.proxyTag,
                domain = listOf("geosite:google")
            ),
            ConfigRuleItem(
                remark = "国内直连IP",
                outboundTag = GlobalConst.directTag,
                ip = listOf(GlobalConst.geoipCN)
            ),
            ConfigRuleItem(
                remark = "国内直连域名",
                outboundTag = GlobalConst.directTag,
                domain = listOf(GlobalConst.geositeCN)
            )
        )

        val defaultEngine = ConfigEngineItem(
            remark = "默认配置",
            routing = ConfigRoutingItem(rules = defaultRules)
        )

        val newEngineList = currentData.engineSettingList.toMutableList()
        if (isEmptyConfig) {
            newEngineList.clear()
            newEngineList.add(defaultEngine)
        } else if (!hasDefaultConfig) {
            newEngineList.add(0, defaultEngine)
        }
        return currentData.copy(engineSettingList = newEngineList)
    }

    val configFlow: StateFlow<ConfigItem> = dataStore.data
        .stateIn(repositoryScope, SharingStarted.Eagerly, ConfigItem())

    val subListFlow: StateFlow<List<ConfigSubItem>> =
        configFlow.map { it.subList }.distinctUntilChanged()
            .stateIn(repositoryScope, SharingStarted.Eagerly, ConfigItem().subList)

    val engineSettingListFlow: StateFlow<List<ConfigEngineItem>> =
        configFlow.map { it.engineSettingList }.distinctUntilChanged()
            .stateIn(repositoryScope, SharingStarted.Eagerly, ConfigItem().engineSettingList)

    suspend fun getConfig(): ConfigItem {
        return dataStore.data.first()
    }

    suspend fun updateConfig(transform: suspend (ConfigItem) -> ConfigItem) {
        dataStore.updateData(transform)
    }

    suspend fun updateSubList(transform: suspend (List<ConfigSubItem>) -> List<ConfigSubItem>) {
        updateConfig { it.copy(subList = transform(it.subList)) }
    }

    suspend fun addSubItem(item: ConfigSubItem) {
        val finalItem =
            if (item.id.isEmpty()) item.copy(
                id = UuidCreator.getTimeOrderedEpoch().toString()
            ) else item
        updateSubList { it + finalItem }
    }

    suspend fun updateSubItem(item: ConfigSubItem) {
        updateSubList { subList ->
            subList.map { if (it.id == item.id) item else it }
        }
    }

    suspend fun removeSubItem(id: String) {
        updateSubList { subList ->
            subList.filter { it.id != id }
        }
    }

    suspend fun addEngineSettingItem(item: ConfigEngineItem) {
        val finalItem =
            if (item.id.isEmpty()) item.copy(
                id = UuidCreator.getTimeOrderedEpoch().toString()
            ) else item
        updateConfig { it.copy(engineSettingList = it.engineSettingList + finalItem) }
    }

    suspend fun updateEngineSettingItem(item: ConfigEngineItem) {
        updateConfig { config ->
            config.copy(engineSettingList = config.engineSettingList.map { if (it.id == item.id) item else it })
        }
    }

    suspend fun removeEngineSettingItem(id: String) {
        updateConfig { config ->
            config.copy(engineSettingList = config.engineSettingList.filter { it.id != id })
        }
    }
}