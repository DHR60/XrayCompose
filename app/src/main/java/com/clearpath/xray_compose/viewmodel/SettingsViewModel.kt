package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {

    val prefsActiveEngineSettingIdFlow = preferencesRepository.activeEngineSettingIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val configEngineSettingListFlow = configRepository.engineSettingListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineSettingListFlow = configEngineSettingListFlow

    val activeEngineSettingIdFlow = combine(
        prefsActiveEngineSettingIdFlow,
        configEngineSettingListFlow
    ) { activeId, list ->
        if (list.any { it.id == activeId }) activeId else list.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeEngineSettingFlow = combine(
        activeEngineSettingIdFlow,
        engineSettingListFlow
    ) { activeId, list ->
        list.find { it.id == activeId } ?: ConfigEngineItem()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConfigEngineItem())

    fun switchActiveEngineSetting(id: String?) {
        viewModelScope.launch {
            preferencesRepository.setActiveEngineSettingId(id)
        }
    }

    fun updateEngineSetting(item: ConfigEngineItem) {
        viewModelScope.launch {
            configRepository.updateEngineSettingItem(item)
        }
    }

    fun updateActiveEngineSetting(update: (ConfigEngineItem) -> ConfigEngineItem) {
        viewModelScope.launch {
            configRepository.updateEngineSettingItem(
                update(
                    activeEngineSettingFlow.value
                )
            )
        }
    }

    fun addEngineSetting(item: ConfigEngineItem) {
        viewModelScope.launch {
            configRepository.addEngineSettingItem(item)
        }
    }

    fun removeEngineSetting(id: String) {
        viewModelScope.launch {
            configRepository.removeEngineSettingItem(id)
            if (activeEngineSettingIdFlow.value == id) {
                switchActiveEngineSetting(engineSettingListFlow.value.firstOrNull { it.id != id }?.id)
            }
        }
    }
}