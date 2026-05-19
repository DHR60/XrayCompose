package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.repo.configRepository
import com.clearpath.xray_compose.data.repo.preferencesRepository
import com.clearpath.xray_compose.data.repo.profileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = application.preferencesRepository
    private val profileRepository = application.profileRepository
    private val configRepository = application.configRepository

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