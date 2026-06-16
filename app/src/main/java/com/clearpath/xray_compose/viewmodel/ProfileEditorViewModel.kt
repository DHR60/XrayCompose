package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.ProtocolExtraItem
import com.clearpath.xray_compose.data.TransportExtraItem
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.data.tempstore.TempStore
import com.clearpath.xray_compose.enums.ETransport
import com.clearpath.xray_compose.utils.JsonUtil
import com.clearpath.xray_compose.viewmodel.uistate.ProfileUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.enums.enumEntries

@HiltViewModel(assistedFactory = ProfileEditorViewModel.Factory::class)
class ProfileEditorViewModel @AssistedInject constructor(
    @Assisted private val id: String,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(id: String): ProfileEditorViewModel
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    val profileModel = uiState
        .map { it.profileModel }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ProfileModel.Empty()
        )
    val protoExtra = profileModel
        .map { it.protocolExtra }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ProfileModel.Empty().protocolExtra
        )
    val transportExtra = profileModel
        .map { it.transportExtra }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            ProfileModel.Empty().transportExtra
        )
    val transportNetwork: StateFlow<ETransport> = profileModel
        .map { model ->
            enumEntries<ETransport>().firstOrNull { it.value == model.network }
                ?: GlobalConst.defaultTransportNetworkEnum
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            GlobalConst.defaultTransportNetworkEnum
        )

    init {
        // Try get from temp store first
        val tempProfile = TempStore.consume<ProfileUiState>(id)
        if (tempProfile != null) {
            _uiState.value = tempProfile
        } else {
            _uiState.value = ProfileUiState(loading = true)
            // load from db
            viewModelScope.launch {
                val profileItem = profileRepository.getProfileById(id)
                if (profileItem != null) {
                    _uiState.value = ProfileUiState.fromProfileModel(profileItem)
                } else {
                    _uiState.value = ProfileUiState(
                        error = "Profile not found: $id",
                    )
                }
            }
        }
    }

    fun updateUiState(update: (ProfileUiState) -> ProfileUiState) {
        _uiState.update(update)
    }

    fun updateProfileModel(update: (ProfileModel) -> ProfileModel) {
        _uiState.update {
            it.copy(
                profileModel = update(it.profileModel)
            )
        }
    }

    fun updateProtocolExtra(update: (ProtocolExtraItem) -> ProtocolExtraItem) {
        _uiState.update {
            val current = it.profileModel.protocolExtra
            it.copy(
                profileModel = it.profileModel.copy(
                    protoExtraRaw = JsonUtil.innerJson.encodeToString(
                        ProtocolExtraItem.serializer(),
                        update(current)
                    )
                )
            )
        }
    }

    fun updateTransportExtra(update: (TransportExtraItem) -> TransportExtraItem) {
        _uiState.update {
            val current = it.profileModel.transportExtra
            it.copy(
                profileModel = it.profileModel.copy(
                    transportExtraRaw = JsonUtil.innerJson.encodeToString(
                        TransportExtraItem.serializer(),
                        update(current)
                    )
                )
            )
        }
    }

    fun saveProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        viewModelScope.launch {
            try {
                val profileItem = currentState.toProfileModel()
                profileRepository.upsertProfile(profileItem)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteProfile(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        viewModelScope.launch {
            try {
                val profileItem = currentState.toProfileModel()
                viewModelScope.launch(Dispatchers.IO) {
                    profileRepository.deleteProfileById(profileItem.id)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }
}