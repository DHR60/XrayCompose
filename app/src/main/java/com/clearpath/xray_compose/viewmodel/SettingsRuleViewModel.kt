package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigRuleItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SettingsRuleViewModel.Factory::class)
class SettingsRuleViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    @Assisted val settingsRoutingViewModel: SettingsRoutingViewModel
) : ViewModel() {
    private val _ruleFlow = MutableStateFlow(ConfigRuleItem())
    val ruleFlow = _ruleFlow.asStateFlow()

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
            settingsRoutingViewModel: SettingsRoutingViewModel
        ): SettingsRuleViewModel
    }

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