package com.clearpath.xray_compose.data.repo

import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ConfigSubItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

// Recommendation: Use this Repo for read-only use. If the ViewModel needs to be changed, use the cache and sub-repository.
@Singleton
class StoreRepository @Inject constructor(
    val preferencesRepository: PreferencesRepository,
    val profileRepository: ProfileRepository,
    val configRepository: ConfigRepository,
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val activeEngineSettingFlow = combine(
        preferencesRepository.activeEngineSettingIdFlow,
        configRepository.engineSettingListFlow
    ) { activeId, list ->
        list.find { it.id == activeId } ?: list.firstOrNull() ?: ConfigEngineItem()
    }.stateIn(repositoryScope, SharingStarted.Eagerly, ConfigEngineItem())

    val activeSubItemFlow = combine(
        preferencesRepository.activeSubIdFlow,
        configRepository.subListFlow
    ) { activeId, list ->
        list.find { it.id == activeId } ?: list.firstOrNull() ?: ConfigSubItem()
    }.stateIn(repositoryScope, SharingStarted.Eagerly, ConfigSubItem())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProfilesOrderedFlow = preferencesRepository.activeSubIdFlow
        .flatMapLatest { subId ->
            if (subId == null) {
                profileRepository.observeAllProfilesOrdered()
            } else {
                profileRepository.observeAllProfilesBySubidOrdered(subId)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProfileFlow = preferencesRepository.activeProfileIdFlow
        .flatMapLatest { profileId ->
            if (profileId.isNullOrBlank()) {
                flowOf(null)
            } else {
                profileRepository.observeProfileById(profileId)
            }
        }.stateIn(repositoryScope, SharingStarted.Eagerly, null)
}