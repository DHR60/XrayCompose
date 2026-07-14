package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsRuleProfileSelectorViewModel @Inject constructor() : ViewModel() {
    private val _selectedProfileRemark = MutableStateFlow("")
    val selectedProfileRemark = _selectedProfileRemark.asStateFlow()

    fun setSelectedProfileRemark(remark: String) {
        _selectedProfileRemark.value = remark
    }
}
