package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsRuleAppSelectorViewModel @Inject constructor() : ViewModel() {
    private val _selectedAppPackageList: MutableStateFlow<List<String>> =
        MutableStateFlow(emptyList())
    val selectedAppPackageList = _selectedAppPackageList.asStateFlow()

    fun setSelectedAppPackageList(list: List<String>) {
        _selectedAppPackageList.value = list
    }
}