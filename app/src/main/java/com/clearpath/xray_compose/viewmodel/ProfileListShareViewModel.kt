package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.utils.JsonUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ProfileListShareViewModel.Factory::class)
class ProfileListShareViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    private val configContextBuilder: EngineConfigContextBuilder
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): ProfileListShareViewModel
    }

    private val _displayFlow = MutableStateFlow("Share Profile List: $id")
    val displayFlow = _displayFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val ecContextResule = configContextBuilder.build(id)
            if (!ecContextResule.success) {
                _displayFlow.value =
                    "Error building EngineConfigContext: ${ecContextResule.errors.joinToString("; ")}"
            } else {
                val ecContext = ecContextResule.ecContext!!
                _displayFlow.value =
                    JsonUtil.prettyJson(XrayConfigService(ecContext).buildBaseConfig())
            }
        }
    }
}