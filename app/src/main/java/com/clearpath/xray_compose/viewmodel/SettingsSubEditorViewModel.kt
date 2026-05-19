package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsSubEditorViewModel(
    id: String,
    val settingsSubViewModel: SettingsSubViewModel,
    application: Application
) : AndroidViewModel(application) {
    private val _subItemFlow = MutableStateFlow(ConfigSubItem())
    val subItemFlow = _subItemFlow.asStateFlow()

    init {
        viewModelScope.launch {
            settingsSubViewModel.subListFlow.collect { subListFlow ->
                val subItem = subListFlow.find { it.id == id } ?: ConfigSubItem()
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