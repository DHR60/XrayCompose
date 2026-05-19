package com.clearpath.xray_compose.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ConfigItem
import com.clearpath.xray_compose.data.ConfigRoutingItem
import com.clearpath.xray_compose.data.ConfigRuleItem
import com.clearpath.xray_compose.data.ConfigSubItem
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.configRepository: ConfigRepository
    get() = ConfigRepository.getInstance(this)

val configRepository: ConfigRepository
    get() = ConfigRepository.getInstance()

@OptIn(DelicateCoroutinesApi::class)
class ConfigRepository private constructor(private val dataStore: DataStore<ConfigItem>) {
    companion object {
        @Volatile
        private var INSTANCE: ConfigRepository? = null

        fun getInstance(context: Context): ConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigRepository(context.applicationContext.configDataStore).also {
                    INSTANCE = it
                }
            }
        }

        fun getInstance(): ConfigRepository {
            return INSTANCE
                ?: error("ConfigRepository is not initialized. Please call getInstance(context) first.")
        }
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                dataStore.updateData { ensureDefaultConfig(it) }
            } catch (e: Exception) {
                e.printStackTrace()
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
                ip = "geoip:private"
            ),
            ConfigRuleItem(
                remark = "绕过局域网域名",
                outboundTag = GlobalConst.directTag,
                ip = "geosite:private"
            ),
            ConfigRuleItem(
                remark = "代理 Google",
                outboundTag = GlobalConst.proxyTag,
                domain = "geosite:google"
            ),
            ConfigRuleItem(
                remark = "国内直连IP",
                outboundTag = GlobalConst.directTag,
                ip = GlobalConst.geoipCN
            ),
            ConfigRuleItem(
                remark = "国内直连域名",
                outboundTag = GlobalConst.directTag,
                domain = GlobalConst.geositeCN
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

    val configFlow: Flow<ConfigItem> = dataStore.data

    val subListFlow: Flow<List<ConfigSubItem>> =
        configFlow.map { it.subList }.distinctUntilChanged()

    val engineSettingListFlow: Flow<List<ConfigEngineItem>> =
        configFlow.map { it.engineSettingList }.distinctUntilChanged()

    suspend fun getConfig(): ConfigItem {
        return dataStore.data.first()
    }

    suspend fun updateConfig(transform: suspend (ConfigItem) -> ConfigItem) {
        dataStore.updateData(transform)
    }

    suspend fun addSubItem(item: ConfigSubItem) {
        val finalItem =
            if (item.id.isEmpty()) item.copy(
                id = UuidCreator.getTimeOrderedEpoch().toString()
            ) else item
        updateConfig { it.copy(subList = it.subList + finalItem) }
    }

    suspend fun updateSubItem(item: ConfigSubItem) {
        updateConfig { config ->
            config.copy(subList = config.subList.map { if (it.id == item.id) item else it })
        }
    }

    suspend fun removeSubItem(id: String) {
        updateConfig { config ->
            config.copy(subList = config.subList.filter { it.id != id })
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