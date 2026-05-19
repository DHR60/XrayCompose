package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.data.repo.configRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsSubViewModel(application: Application) : AndroidViewModel(application) {
    private val configRepository = application.configRepository

    private val _subListFlow = MutableStateFlow<List<ConfigSubItem>>(emptyList())
    val subListFlow = _subListFlow.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.subListFlow.collect { subList ->
                _subListFlow.value = subList
            }
        }
    }

    fun updateSubList(subList: List<ConfigSubItem>) {
        _subListFlow.value = subList
        viewModelScope.launch {
            configRepository.updateSubList {
                subList
            }
        }
    }

    fun createNewSubItem() {
        addSubItem(
            ConfigSubItem(
                remark = "New Sub Item",
            )
        )
    }

    fun addSubItem(subItem: ConfigSubItem) {
        val newList = _subListFlow.value + listOf(subItem)
        updateSubList(newList)
    }

    fun updateSubItem(subItem: ConfigSubItem) {
        val newList = _subListFlow.value.map {
            if (it.id == subItem.id) {
                subItem
            } else {
                it
            }
        }
        updateSubList(newList)
    }

    fun removeSubItem(subId: String) {
        val newList = _subListFlow.value.filter { it.id != subId }
        updateSubList(newList)
    }
}