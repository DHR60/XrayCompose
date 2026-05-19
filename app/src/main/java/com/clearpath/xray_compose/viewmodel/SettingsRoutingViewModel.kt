package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ConfigRoutingItem
import com.clearpath.xray_compose.data.ConfigRuleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsRoutingViewModel(val settingsViewModel: SettingsViewModel, application: Application) :
    AndroidViewModel(application) {
    private val _domainStrategyFlow = MutableStateFlow(GlobalConst.AsIs)
    val domainStrategyFlow = _domainStrategyFlow.asStateFlow()

    private val _enableAutoDetachmentFlow = MutableStateFlow(false)
    val enableAutoDetachmentFlow = _enableAutoDetachmentFlow.asStateFlow()

    private val _ruleListFlow = MutableStateFlow<List<ConfigRuleItem>>(emptyList())
    val ruleListFlow = _ruleListFlow.asStateFlow()

    init {
        viewModelScope.launch {
            settingsViewModel.activeEngineSettingFlow.collect { setting ->
                _domainStrategyFlow.value = setting.routing.domainStrategy
                _enableAutoDetachmentFlow.value = setting.routing.enableAutoDetachment
                _ruleListFlow.value = setting.routing.rules
            }
        }
    }

    fun updateRoutingSetting(update: (ConfigRoutingItem) -> ConfigRoutingItem) {
        settingsViewModel.updateActiveEngineSetting { currentState ->
            currentState.copy(
                routing = update(currentState.routing)
            )
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