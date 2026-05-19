package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.core.AppState
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.repo.configRepository
import com.clearpath.xray_compose.data.repo.preferencesRepository
import com.clearpath.xray_compose.data.repo.profileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileListViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = application.preferencesRepository
    private val profileRepository = application.profileRepository
    private val configRepository = application.configRepository

    val prefsActiveProfileIdFlow = preferencesRepository.activeProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeProfileIdFlow = MutableStateFlow<String?>(null)
    val activeProfileIdFlow = _activeProfileIdFlow.asStateFlow()

    val prefsActiveSubIdFlow = preferencesRepository.activeSubIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeSubIdFlow = MutableStateFlow<String?>(null)
    val activeSubIdFlow = _activeSubIdFlow.asStateFlow()

    val subItemsFlow: StateFlow<List<ConfigSubItem>> = configRepository.subListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeSubProfilesFlow = _activeSubIdFlow
        .flatMapLatest { subId ->
            if (subId.isNullOrEmpty()) {
                profileRepository.observeAllProfilesOrdered()
            } else {
                profileRepository.observeAllProfilesBySubidOrdered(subId)
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedProfileIdSetFlow = AppState.selectedProfileIdSetFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            prefsActiveProfileIdFlow.collect { profileId ->
                _activeProfileIdFlow.value = profileId
            }
        }
        viewModelScope.launch {
            prefsActiveSubIdFlow.collect { subId ->
                _activeSubIdFlow.value = subId
            }
        }
    }

    fun switchSelectedProfileId(profileId: String?) {
        if (profileId == null) {
            AppState.selectedProfileIdSetFlow.value = hashSetOf()
            return
        }

        AppState.selectedProfileIdSetFlow.update { currentSet ->
            if (currentSet.contains(profileId)) {
                (currentSet - profileId).toHashSet()
            } else {
                (currentSet + profileId).toHashSet()
            }
        }
    }

    fun switchActiveProfileId(newProfileId: String?) {
        _activeProfileIdFlow.value = newProfileId
        viewModelScope.launch {
            preferencesRepository.setActiveProfileId(newProfileId)
        }
    }

    fun switchActiveSubId(newSubId: String?) {
        _activeSubIdFlow.value = newSubId
        viewModelScope.launch {
            preferencesRepository.setActiveSubId(newSubId)
        }
    }

    fun addDemoProfile() {
        viewModelScope.launch {
            profileRepository.insertProfile(
                ProfileModel.Empty().copy(
                    remark = "Demo Profile ${System.currentTimeMillis()}",
                    subId = prefsActiveSubIdFlow.value ?: "",
                )
            )
        }
    }

    fun deleteProfile(profileItem: ProfileModel) {
        viewModelScope.launch {
            try {
                viewModelScope.launch(Dispatchers.IO) {
                    profileRepository.deleteProfileById(profileItem.id)
                }
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}