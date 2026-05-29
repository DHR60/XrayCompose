package com.clearpath.xray_compose.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.core.AppState
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.repo.configRepository
import com.clearpath.xray_compose.data.repo.preferencesRepository
import com.clearpath.xray_compose.data.repo.profileRepository
import com.clearpath.xray_compose.service.engine.control.EngineServiceManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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

    val prefsActiveEngineSettingIdFlow = preferencesRepository.activeEngineSettingIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val configEngineSettingListFlow = configRepository.engineSettingListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val engineSettingListFlow = configEngineSettingListFlow

    val activeEngineSettingIdFlow = combine(
        prefsActiveEngineSettingIdFlow,
        configEngineSettingListFlow
    ) { activeId, list ->
        if (list.any { it.id == activeId }) activeId else list.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeEngineSettingFlow = combine(
        activeEngineSettingIdFlow,
        engineSettingListFlow
    ) { activeId, list ->
        list.find { it.id == activeId } ?: ConfigEngineItem()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConfigEngineItem())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeProfileFlow = preferencesRepository.activeProfileIdFlow
        .flatMapLatest { profileId ->
            if (profileId.isNullOrBlank()) {
                flowOf(null)
            } else {
                profileRepository.observeProfileById(profileId)
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isNotificationPermissionGrantedFlow = MutableStateFlow(false)
    val isNotificationPermissionGrantedFlow = _isNotificationPermissionGrantedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 0 = unknown, 1 = granted, 2 = denied
    private val _isQueryAllPackagesPermissionGrantedFlow = MutableStateFlow(0)
    val isQueryAllPackagesPermissionGrantedFlow = _isQueryAllPackagesPermissionGrantedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startProxyCore() {
        viewModelScope.launch {
            AppState.isProxyCoreRunningFlow.value = 2
            EngineServiceManager.startVService(application)
            // AppState.isProxyCoreRunningFlow.value = 1
            AppState.isProxyCoreRunningFlow.value = when (EngineServiceManager.isRunning()) {
                true -> 1
                false -> 0
            }
        }
    }

    fun stopProxyCore() {
        viewModelScope.launch {
            AppState.isProxyCoreRunningFlow.value = 2
            EngineServiceManager.stopVService(application)
            AppState.isProxyCoreRunningFlow.value = 0
        }
    }

    fun checkNotificationPermission() {
        viewModelScope.launch {
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
            _isNotificationPermissionGrantedFlow.value = granted
        }
    }

    fun setNotificationPermissionGranted(isGranted: Boolean) {
        _isNotificationPermissionGrantedFlow.value = isGranted
    }

    fun checkQueryAllPackagesPermissionReallyGranted() {
        viewModelScope.launch {
            val granted = isQueryAllPackagesReallyGranted()
            _isQueryAllPackagesPermissionGrantedFlow.value = if (granted) 1 else 2
        }
    }

    fun isQueryAllPackagesReallyGranted(): Boolean {
        val hasManifestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                application, Manifest.permission.QUERY_ALL_PACKAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasManifestPermission) return false
        // NOTE: some rom (e.g. MIUI) may enable "pseudo permission" for QUERY_ALL_PACKAGES
        val testPackages = listOf(
            "com.android.settings",
            "com.google.android.gms",
        )
        var matchCount = 0
        try {
            val pm = application.packageManager
            val packageList = pm.getInstalledPackages(0)
            for (pkg in packageList) {
                if (testPackages.contains(pkg.packageName)) {
                    matchCount++
                }
            }
        } catch (_: Exception) {
            // Ignore
        }
        return matchCount > 0
    }

    fun setQueryAllPackagesPermissionGranted(isGranted: Boolean) {
        if (!isGranted) {
            _isQueryAllPackagesPermissionGrantedFlow.value = 2
        } else {
            // For granted, we need to double-check if it's really granted due to some rom may enable "pseudo permission"
            checkQueryAllPackagesPermissionReallyGranted()
        }
    }
}