package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.data.tempstore.TempStore
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.service.fmt.FmtFact
import com.clearpath.xray_compose.utils.JsonUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@HiltViewModel(assistedFactory = ProfileListShareViewModel.Factory::class)
class ProfileListShareViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    private val profileRepository: ProfileRepository,
    private val configContextBuilder: EngineConfigContextBuilder
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): ProfileListShareViewModel
    }

    private val _titleFlow = MutableStateFlow("Share Profile")
    val titleFlow = _titleFlow.asStateFlow()

    private val _shareUrlFlow = MutableStateFlow("")
    val shareUrlFlow = _shareUrlFlow.asStateFlow()

    private val _fullConfigFlow = MutableStateFlow("")
    val fullConfigFlow = _fullConfigFlow.asStateFlow()

    private val _proxyOutboundsFlow = MutableStateFlow("")
    val proxyOutboundsFlow = _proxyOutboundsFlow.asStateFlow()

    private val _isBusy = MutableStateFlow(true)
    val isBusyFlow = _isBusy.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            try {
                val profile = TempStore.consume(id)
                    ?: profileRepository.getProfileById(id)
                    ?: run {
                        _titleFlow.value = "Error: Profile not found"
                        return@launch
                    }

                _titleFlow.value = profile.remark

                // 1. Generate URL
                FmtFact.getUrl(profile)
                    .onSuccess { _shareUrlFlow.value = it }
                    .onFailure { _shareUrlFlow.value = "Error: ${it.message}" }

                // 2. Build Config
                val ecContextResult = configContextBuilder.buildByProfile(profile)
                if (!ecContextResult.success) {
                    val error = ecContextResult.errors.joinToString("; ")
                    _fullConfigFlow.value = "Error: $error"
                } else {
                    val ecContext = ecContextResult.ecContext!!
                    val configStr = XrayConfigService(ecContext).buildBaseConfig()
                    _fullConfigFlow.value = JsonUtil.prettyJson(configStr)

                    // 3. Extract Proxy Outbounds
                    try {
                        val configJsonObject =
                            JsonUtil.defaultJson.parseToJsonElement(configStr).jsonObject
                        val outboundJsonArray =
                            configJsonObject["outbounds"]?.jsonArray ?: JsonArray(emptyList())
                        val proxyOutbounds = outboundJsonArray.filter {
                            val tag = it.jsonObject["tag"]?.let { t ->
                                if (t is JsonPrimitive) t.content else null
                            }
                            tag?.startsWith("proxy", ignoreCase = true) == true
                        }
                        _proxyOutboundsFlow.value =
                            JsonUtil.defaultIndentedJson.encodeToString(proxyOutbounds)
                    } catch (e: Exception) {
                        _proxyOutboundsFlow.value = "Error parsing outbounds: ${e.message}"
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun getContentForIndex(index: Int): String {
        return when (index) {
            0 -> shareUrlFlow.value
            1 -> fullConfigFlow.value
            2 -> proxyOutboundsFlow.value
            else -> ""
        }
    }
}
