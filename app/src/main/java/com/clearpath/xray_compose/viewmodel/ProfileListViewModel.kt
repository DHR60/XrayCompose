package com.clearpath.xray_compose.viewmodel

import androidx.compose.foundation.lazy.LazyListItemInfo
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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

    private val _manualProfiles = MutableStateFlow<Map<String?, List<ProfileModel>>>(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dbProfilesFlow = _activeSubIdFlow.flatMapLatest { subId ->
        val flow = if (subId == null) {
            profileRepository.observeAllProfilesOrdered()
        } else {
            profileRepository.observeAllProfilesBySubidOrdered(subId)
        }
        flow.map { subId to it }
    }.scan(emptyMap<String?, List<ProfileModel>>()) { acc, (subId, list) ->
        acc + (subId to list)
    }.flowOn(Dispatchers.Default)

    val allProfilesFlow: StateFlow<Map<String?, List<ProfileModel>>> = combine(
        dbProfilesFlow,
        _manualProfiles
    ) { dbMap, manualMap ->
        val merged = dbMap.toMutableMap()
        manualMap.forEach { (subId, manualList) ->
            val dbSubList = dbMap[subId] ?: emptyList()
            if (manualList.map { it.id } != dbSubList.map { it.id }) {
                merged[subId] = manualList
            }
        }
        merged
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val profilesWithTestFlow: StateFlow<Map<String?, List<ProfileWithTest>>> = combine(
        allProfilesFlow,
        profileRepository.observeAllProfileTests().map { list -> list.associateBy { it.id.toString() } }
    ) { allProfiles, allTests ->
        allProfiles.mapValues { (_, profiles) ->
            profiles.map { profile ->
                ProfileWithTest(profile, allTests[profile.id])
            }
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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
        viewModelScope.launch {
            dbProfilesFlow.collect { dbMap ->
                _manualProfiles.update { manualMap ->
                    manualMap.filter { (subId, manualList) ->
                        val dbList = dbMap[subId] ?: emptyList()
                        dbList.map { it.id } != manualList.map { it.id }
                    }
                }
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

    private var reorderJob: Job? = null
    private val pendingUpdates = mutableMapOf<String, ProfileModel>()
    private var needsRebalance = false

    fun reorderProfiles(from: LazyListItemInfo, to: LazyListItemInfo) {
        val currentSubId = _activeSubIdFlow.value
        val profileList = allProfilesFlow.value[currentSubId] ?: return

        if (profileList.getOrNull(from.index)?.id != from.key ||
            profileList.getOrNull(to.index)?.id != to.key
        ) {
            LogUtil.e("ProfileListViewModel Failed to reorder profiles: Index mismatch")
            return
        }

        val fromIndex = profileList.indexOfFirst { it.id == from.key }
        val toIndex = profileList.indexOfFirst { it.id == to.key }
        if (fromIndex == -1 || toIndex == -1) {
            LogUtil.e("ProfileListViewModel Failed to reorder profiles: Key not found")
            return
        }

        val fromProfile = profileList[fromIndex]
        val toProfile = profileList[toIndex]
        val isDown = fromIndex < toIndex
        val toNearbyIndex = if (isDown) toIndex + 1 else toIndex - 1
        val toNearbyProfile = profileList.getOrNull(toNearbyIndex)

        val newOrder = if (toNearbyProfile != null) {
            (toProfile.sortOrder + toNearbyProfile.sortOrder) / 2.0
        } else {
            toProfile.sortOrder + if (isDown) 1 else -1
        }

        val updatedProfile = fromProfile.copy(sortOrder = newOrder)

        // Optimistic UI update - order is preserved by list sequence
        val newList = profileList.toMutableList()
        newList.removeAt(from.index)
        newList.add(to.index, updatedProfile)
        _manualProfiles.update { it + (currentSubId to newList) }

        // Check if the precision gap is too small to trigger background rebalance.
        // At timestamp scale (10^12), Double precision limit is ~0.0002.
        // We trigger at 0.1 to stay well within safe bounds.
        val gap = if (toNearbyProfile != null) {
            val diff = toProfile.sortOrder - toNearbyProfile.sortOrder
            (if (diff < 0) -diff else diff) / 2.0
        } else {
            1.0
        }
        if (gap < 0.1) {
            needsRebalance = true
        }

        // Debounced DB update
        pendingUpdates[updatedProfile.id] = updatedProfile
        reorderJob?.cancel()
        reorderJob = viewModelScope.launch {
            delay(500.milliseconds)
            val updates = pendingUpdates.values.toList()
            val shouldRebalance = needsRebalance
            pendingUpdates.clear()
            needsRebalance = false

            withContext(Dispatchers.IO) {
                try {
                    profileRepository.upsertProfiles(updates)
                    if (shouldRebalance) {
                        profileRepository.rebalanceProfiles(currentSubId)
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    LogUtil.e("ProfileListViewModel Failed to update profiles in DB", e)
                    _manualProfiles.update { it - currentSubId }
                }
            }
        }
    }
}
