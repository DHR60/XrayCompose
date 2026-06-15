package com.clearpath.xray_compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigSubItem
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.service.ProfileImportInteractor
import com.clearpath.xray_compose.service.engine.control.tester.EngineTesterRepository
import com.clearpath.xray_compose.utils.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileWithTest(
    val profile: ProfileModel,
    val test: ProfileTestItem? = null
)

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val configRepository: ConfigRepository,
    private val engineTesterRepository: EngineTesterRepository,
    private val profileImportInteractor: ProfileImportInteractor
) : ViewModel() {

    val prefsActiveProfileIdFlow = preferencesRepository.activeProfileIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeProfileIdFlow = MutableStateFlow<String?>(null)
    val activeProfileIdFlow = _activeProfileIdFlow.asStateFlow()

    val prefsActiveSubIdFlow = preferencesRepository.activeSubIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeSubIdFlow =
        MutableStateFlow(preferencesRepository.activeSubIdFlow.value)
    val activeSubIdFlow = _activeSubIdFlow.asStateFlow()

    val subItemsFlow: StateFlow<List<ConfigSubItem>> = configRepository.subListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val windowProfilesFlow = combine(
        _activeSubIdFlow,
        subItemsFlow
    ) { activeId, subs ->
        val ids = listOf(null) + subs.map { it.id }
        val idx = ids.indexOf(activeId).coerceAtLeast(0)
        val range = (idx - 1)..(idx + 1)
        ids.filterIndexed { index, _ -> index in range }.toSet()
    }.distinctUntilChanged()
        .flatMapLatest { targetIds ->
            val flows = targetIds.map { id ->
                val flow = if (id == null) profileRepository.observeAllProfilesOrdered()
                else profileRepository.observeAllProfilesBySubidOrdered(id)
                flow.map { id to it }
            }
            if (flows.isEmpty()) flowOf(emptyMap())
            else combine(flows) { it.toMap() }
        }.flowOn(Dispatchers.Default)

    val allProfilesFlow: StateFlow<Map<String?, List<ProfileModel>>> = combine(
        windowProfilesFlow,
        _activeSubIdFlow
    ) { windowData, activeId ->
        windowData to activeId
    }.scan(emptyMap<String?, List<ProfileModel>>() to listOf<String?>()) { (oldMap, history), (new, activeId) ->
        val merged = oldMap + new
        val newHistory = (listOf(activeId) + history).distinct().take(10)
        merged.filterKeys { it in newHistory || it in new.keys } to newHistory
    }.map { it.first }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allProfileTestsFlow = profileRepository.observeAllProfileTests()
        .map { list -> list.associateBy { it.id.toString() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val profilesWithTestFlow: StateFlow<Map<String?, List<ProfileWithTest>>> = combine(
        allProfilesFlow,
        allProfileTestsFlow
    ) { allProfiles, allTests ->
        allProfiles.mapValues { (_, profiles) ->
            profiles.map { profile ->
                ProfileWithTest(profile, allTests[profile.id])
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val isTestingFlow = engineTesterRepository.isTestingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val testProgressFlow = engineTesterRepository.testProgressFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            EngineTesterRepository.TestProgress()
        )

    private val _isBusy = MutableStateFlow(false)
    val isBusyFlow = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow = _errorMessage.asStateFlow()

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

    fun testCurrentSubProfiles() {
        viewModelScope.launch {
            val currentSubId = _activeSubIdFlow.value
            val profileIds =
                profilesWithTestFlow.value[currentSubId]?.map { it.profile.id } ?: emptyList()

            if (profileIds.isNotEmpty()) {
                engineTesterRepository.startTestProfiles(profileIds)
            }
        }
    }

    fun testProfile(profileItem: ProfileModel) {
        viewModelScope.launch {
            engineTesterRepository.startTestProfiles(listOf(profileItem.id))
        }
    }

    fun stopTesting() {
        viewModelScope.launch {
            engineTesterRepository.stopTest()
        }
    }

    fun deleteProfile(profileItem: ProfileModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                profileRepository.deleteProfileById(profileItem.id)
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    fun importFromClipboard() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            _errorMessage.value = null
            try {
                profileImportInteractor.importFromClipboard(activeSubIdFlow.value ?: "")
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to import from clipboard"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun updateSubForNetwork() {
        viewModelScope.launch(Dispatchers.IO) {
            _isBusy.value = true
            _errorMessage.value = null
            try {
                val subId = activeSubIdFlow.value ?: run {
                    _errorMessage.value = "No active subscription"
                    return@launch
                }
                profileImportInteractor.updateSub(subId)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update subscription"
                LogUtil.e("ProfileListViewModel Failed to update sub for network", e)
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
