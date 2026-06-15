package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = SettingsSubEditorViewModel.Factory::class)
class SettingsSubEditorViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    @Assisted val settingsSubViewModel: SettingsSubViewModel
) : ViewModel() {
    private val _subItemFlow = MutableStateFlow(ConfigSubItem())
    val subItemFlow = _subItemFlow.asStateFlow()

    @AssistedFactory
    interface Factory {
        fun create(
            id: String,
            settingsSubViewModel: SettingsSubViewModel
        ): SettingsSubEditorViewModel
    }

    init {
        viewModelScope.launch {
            settingsSubViewModel.subListFlow.collect { subListFlow ->
                val subItem = subListFlow.find { it.config.id == id }?.config ?: ConfigSubItem()
                _subItemFlow.value = subItem
            }
        }
    }

    fun updateSubItem(update: (ConfigSubItem) -> ConfigSubItem) {
        val updatedSubItem = update(subItemFlow.value)
        _subItemFlow.value = updatedSubItem

        settingsSubViewModel.updateSubItem(updatedSubItem)
    }
}