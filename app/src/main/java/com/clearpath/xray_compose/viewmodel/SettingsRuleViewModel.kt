package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigRuleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsRuleViewModel(
    id: String,
    val settingsRoutingViewModel: SettingsRoutingViewModel,
    application: Application
) : AndroidViewModel(application) {
    private val _ruleFlow = MutableStateFlow(ConfigRuleItem())
    val ruleFlow = _ruleFlow.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRoutingViewModel.ruleListFlow.collect { ruleList ->
                val rule = ruleList.find { it.id == id } ?: ConfigRuleItem()
                _ruleFlow.value = rule
            }
        }
    }

    fun updateRule(update: (ConfigRuleItem) -> ConfigRuleItem) {
        val updatedRule = update(ruleFlow.value)
        _ruleFlow.value = updatedRule

        // Update the rule list in the parent view model
        settingsRoutingViewModel.updateRule(updatedRule)
    }
}