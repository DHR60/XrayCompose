package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.viewmodel.uistate.ProfileWithTest
import com.github.f4b6a3.uuid.UuidCreator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSelectorViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val configRepository: ConfigRepository,
) : ViewModel() {
    private val _activeProfileIdFlow = MutableStateFlow<String?>(null)
    val activeProfileIdFlow = _activeProfileIdFlow.asStateFlow()

    private val _activeSubIdFlow =
        MutableStateFlow(preferencesRepository.activeSubIdFlow.value)
    val activeSubIdFlow = _activeSubIdFlow.asStateFlow()

    val subItemsFlow: StateFlow<List<ConfigSubItem>> = configRepository.subListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbProfilesFlow = _activeSubIdFlow.flatMapLatest { subId ->
        if (subId == null) {
            profileRepository.observeAllProfilesOrdered()
        } else {
            profileRepository.observeAllProfilesBySubidOrdered(subId)
        }
    }.flowOn(Dispatchers.Default)

    val allProfilesFlow: StateFlow<List<ProfileModel>> = dbProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val profilesWithTestFlow: StateFlow<List<ProfileWithTest>> = allProfilesFlow
        .flatMapLatest { profiles ->
            val idList = profiles.map { it.id }
            profileRepository.observeProfileTestsByIds(idList)
                .map { testList ->
                    profiles.map { profile ->
                        val test =
                            testList.find { test -> test.id == UuidCreator.fromString(profile.id) }
                        ProfileWithTest(profile, test)
                    }
                }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _activeProfileIdFlow.value = preferencesRepository.activeProfileIdFlow.value
        }
        viewModelScope.launch {
            _activeSubIdFlow.value = preferencesRepository.activeSubIdFlow.value
        }
    }

    fun switchSubId(subId: String?) {
        viewModelScope.launch {
            _activeSubIdFlow.value = subId
        }
    }
}