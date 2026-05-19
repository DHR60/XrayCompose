package com.clearpath.xray_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.core.AppState
import com.clearpath.xray_compose.data.repo.configRepository
import com.clearpath.xray_compose.data.repo.preferencesRepository
import com.clearpath.xray_compose.data.repo.profileRepository
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = application.preferencesRepository
    private val profileRepository = application.profileRepository
    private val configRepository = application.configRepository

    val isProxyCoreRunningFlow = AppState.isProxyCoreRunningFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProfileFlow = preferencesRepository.activeProfileIdFlow
        .flatMapLatest { profileId ->
            if (profileId.isNullOrBlank()) {
                flowOf(null)
            } else {
                profileRepository.observeProfileById(UuidCreator.fromString(profileId))
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startProxyCore() {
        viewModelScope.launch {
            // AppState.startProxyCore()
            // simulate starting proxy core
            // initially set to 1, then after 2 seconds set to 2
            AppState.isProxyCoreRunningFlow.value = 2
            delay(2000)
            AppState.isProxyCoreRunningFlow.value = 1
        }
    }

    fun stopProxyCore() {
        viewModelScope.launch {
            // AppState.stopProxyCore()
            // simulate stopping proxy core
            AppState.isProxyCoreRunningFlow.value = 2
            delay(500)
            AppState.isProxyCoreRunningFlow.value = 0
        }
    }
}