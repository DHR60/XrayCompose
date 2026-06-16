package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.data.tempstore.TempStore
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.service.formatter.FmtFact
import com.clearpath.xray_compose.utils.JsonUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _titleFlow = MutableStateFlow("Share Profile: $id")
    val titleFlow = _titleFlow.asStateFlow()

    private val _shareUrlFlow = MutableStateFlow("")
    val shareUrlFlow = _shareUrlFlow.asStateFlow()

    private val _fullConfigFlow = MutableStateFlow("")
    val fullConfigFlow = _fullConfigFlow.asStateFlow()

    private val _proxyOutboundsFlow = MutableStateFlow("")
    val proxyOutboundsFlow = _proxyOutboundsFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = TempStore.consume(id)
                ?: profileRepository.getProfileById(id)
                ?: run {
                    _titleFlow.value = "Error: Profile with ID $id not found."
                    return@launch
                }
            _titleFlow.value = profile.remark
            FmtFact.getUrl(profile)
                .onSuccess { url ->
                    _shareUrlFlow.value = url
                }
                .onFailure {
                    _shareUrlFlow.value = "Error: ${it.message}"
                }
            val ecContextResule = configContextBuilder.buildByProfile(profile)
            if (!ecContextResule.success) {
                _titleFlow.value =
                    "Error building EngineConfigContext: ${ecContextResule.errors.joinToString("; ")}"
            } else {
                val ecContext = ecContextResule.ecContext!!
                val configStr = XrayConfigService(ecContext).buildBaseConfig()
                _fullConfigFlow.value =
                    JsonUtil.prettyJson(configStr)

                try {
                    val configJsonObject =
                        JsonUtil.defaultJson.parseToJsonElement(configStr).jsonObject
                    val outboundJsonArray = configJsonObject["outbounds"]!!.jsonArray
                    val proxyOutbounds = outboundJsonArray
                        .filter {
                            val tagElement = it.jsonObject["tag"]
                            tagElement != null && tagElement is JsonPrimitive && tagElement.content.startsWith(
                                "proxy",
                                ignoreCase = true
                            )
                        }
                    _proxyOutboundsFlow.value = JsonUtil.defaultIndentedJson
                        .encodeToString(proxyOutbounds)
                } catch (e: Exception) {
                    _proxyOutboundsFlow.value = "Error parsing outbounds: ${e.message}"
                    return@launch
                }
            }
        }
    }

    fun getContentForIndex(index: Int): String {
        return when (index) {
            0 -> _shareUrlFlow.value
            1 -> _fullConfigFlow.value
            2 -> _proxyOutboundsFlow.value
            else -> ""
        }
    }
}