package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileListShareViewModel(id: String, application: Application) :
    AndroidViewModel(application) {

    private val _displayFlow = MutableStateFlow("Share Profile List: $id")
    val displayFlow = _displayFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val ecContextResule = EngineConfigContextBuilder(application).build(id)
            if (!ecContextResule.success) {
                _displayFlow.value =
                    "Error building EngineConfigContext: ${ecContextResule.errors.joinToString("; ")}"
            } else {
                val ecContext = ecContextResule.ecContext!!
                _displayFlow.value =
                    XrayConfigService(ecContext).buildBaseConfig()
            }
        }
    }
}