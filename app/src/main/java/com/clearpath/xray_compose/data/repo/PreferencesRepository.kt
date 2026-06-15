package com.clearpath.xray_compose.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.clearpath.xray_compose.data.PreferencesKeys
import com.clearpath.xray_compose.data.StateItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val configState: StateFlow<StateItem> = dataStore.data
        .map { preferences ->
            StateItem(
                activeProfileId = preferences[PreferencesKeys.ACTIVE_PROFILE_ID],
                activeSubId = preferences[PreferencesKeys.ACTIVE_SUB_ID],
                activeEngineSettingId = preferences[PreferencesKeys.ACTIVE_ENGINE_SETTING_ID]
            )
        }
        .distinctUntilChanged()
        .stateIn(repositoryScope, SharingStarted.Eagerly, StateItem())

    val activeProfileIdFlow: StateFlow<String?> = dataStore.data
        .map { it[PreferencesKeys.ACTIVE_PROFILE_ID] }
        .distinctUntilChanged()
        .stateIn(repositoryScope, SharingStarted.Eagerly, null)

    val activeSubIdFlow: StateFlow<String?> = dataStore.data
        .map { it[PreferencesKeys.ACTIVE_SUB_ID] }
        .distinctUntilChanged()
        .stateIn(repositoryScope, SharingStarted.Eagerly, null)

    val activeEngineSettingIdFlow: StateFlow<String?> = dataStore.data
        .map { it[PreferencesKeys.ACTIVE_ENGINE_SETTING_ID] }
        .distinctUntilChanged()
        .stateIn(repositoryScope, SharingStarted.Eagerly, null)

    suspend fun getActiveProfileId(): String? =
        dataStore.data.map { it[PreferencesKeys.ACTIVE_PROFILE_ID] }.first()

    suspend fun getActiveSubId(): String? =
        dataStore.data.map { it[PreferencesKeys.ACTIVE_SUB_ID] }.first()

    suspend fun getActiveEngineSettingId(): String? =
        dataStore.data.map { it[PreferencesKeys.ACTIVE_ENGINE_SETTING_ID] }.first()

    suspend fun setActiveProfileId(profileId: String?) {
        dataStore.edit { prefs ->
            if (profileId != null) {
                prefs[PreferencesKeys.ACTIVE_PROFILE_ID] = profileId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
            }
        }
    }

    suspend fun setActiveSubId(subId: String?) {
        dataStore.edit { prefs ->
            if (subId != null) {
                prefs[PreferencesKeys.ACTIVE_SUB_ID] = subId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_SUB_ID)
            }
        }
    }

    suspend fun setActiveEngineSettingId(engineSettingId: String?) {
        dataStore.edit { prefs ->
            if (engineSettingId != null) {
                prefs[PreferencesKeys.ACTIVE_ENGINE_SETTING_ID] = engineSettingId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_ENGINE_SETTING_ID)
            }
        }
    }

    suspend fun updateConfig(state: StateItem) {
        dataStore.edit { prefs ->
            if (state.activeProfileId != null) {
                prefs[PreferencesKeys.ACTIVE_PROFILE_ID] = state.activeProfileId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
            }
            if (state.activeSubId != null) {
                prefs[PreferencesKeys.ACTIVE_SUB_ID] = state.activeSubId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_SUB_ID)
            }
            if (state.activeEngineSettingId != null) {
                prefs[PreferencesKeys.ACTIVE_ENGINE_SETTING_ID] = state.activeEngineSettingId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_ENGINE_SETTING_ID)
            }
        }
    }
}