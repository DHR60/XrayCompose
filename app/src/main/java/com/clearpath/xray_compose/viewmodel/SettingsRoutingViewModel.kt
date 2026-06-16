package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ConfigRoutingItem
import com.clearpath.xray_compose.data.ConfigRuleItem
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsRoutingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val prefsActiveEngineSettingIdFlow = preferencesRepository.activeEngineSettingIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val configEngineSettingListFlow = configRepository.engineSettingListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeEngineSettingIdFlow = combine(
        prefsActiveEngineSettingIdFlow,
        configEngineSettingListFlow
    ) { activeId, list ->
        if (list.any { it.id == activeId }) activeId else list.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeEngineSettingFlow = combine(
        activeEngineSettingIdFlow,
        configEngineSettingListFlow
    ) { activeId, list ->
        list.find { it.id == activeId } ?: ConfigEngineItem()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConfigEngineItem())

    private val _domainStrategyFlow = MutableStateFlow(GlobalConst.AsIs)
    val domainStrategyFlow = _domainStrategyFlow.asStateFlow()

    private val _enableAutoDetachmentFlow = MutableStateFlow(false)
    val enableAutoDetachmentFlow = _enableAutoDetachmentFlow.asStateFlow()

    private val _ruleListFlow = MutableStateFlow<List<ConfigRuleItem>>(emptyList())
    val ruleListFlow = _ruleListFlow.asStateFlow()

    init {
        viewModelScope.launch {
            activeEngineSettingFlow.collect { setting ->
                _domainStrategyFlow.value = setting.routing.domainStrategy
                _enableAutoDetachmentFlow.value = setting.routing.enableAutoDetachment
                _ruleListFlow.value = setting.routing.rules
            }
        }
    }

    fun updateRoutingSetting(update: (ConfigRoutingItem) -> ConfigRoutingItem) {
        viewModelScope.launch {
            val activeId = preferencesRepository.getActiveEngineSettingId()
            configRepository.updateConfig { config ->
                val targetId = activeId ?: config.engineSettingList.firstOrNull()?.id
                if (targetId == null) return@updateConfig config

                val newList = config.engineSettingList.map { item ->
                    if (item.id == targetId) {
                        item.copy(routing = update(item.routing))
                    } else {
                        item
                    }
                }
                config.copy(engineSettingList = newList)
            }
        }
    }

    fun updateDomainStrategy(strategy: String) {
        _domainStrategyFlow.value = strategy
        updateRoutingSetting { currentRouting ->
            currentRouting.copy(
                domainStrategy = strategy
            )
        }
    }

    fun updateEnableAutoDetachment(enable: Boolean) {
        _enableAutoDetachmentFlow.value = enable
        updateRoutingSetting { currentRouting ->
            currentRouting.copy(
                enableAutoDetachment = enable
            )
        }
    }

    fun updateRuleList(newList: List<ConfigRuleItem>) {
        _ruleListFlow.value = newList
        updateRoutingSetting { currentState ->
            currentState.copy(
                rules = newList
            )
        }
    }

    fun createNewRule() {
        addRule(
            ConfigRuleItem(
                remark = "New Rule",
                outboundTag = GlobalConst.proxyTag,
            )
        )
    }

    fun addRule(rule: ConfigRuleItem) {
        val newList = listOf(rule) + _ruleListFlow.value
        updateRuleList(newList)
    }

    fun updateRule(updatedRule: ConfigRuleItem) {
        val newList = _ruleListFlow.value.map { if (it.id == updatedRule.id) updatedRule else it }
        updateRuleList(newList)
    }

    fun switchRuleEnabled(ruleId: String) {
        val newList = _ruleListFlow.value.map {
            if (it.id == ruleId) it.copy(enable = !it.enable) else it
        }
        updateRuleList(newList)
    }

    fun setRuleEnabled(ruleId: String, enabled: Boolean) {
        val newList = _ruleListFlow.value.map {
            if (it.id == ruleId) it.copy(enable = enabled) else it
        }
        updateRuleList(newList)
    }

    fun removeRule(ruleId: String) {
        val newList = _ruleListFlow.value.filterNot { it.id == ruleId }
        updateRuleList(newList)
    }
}