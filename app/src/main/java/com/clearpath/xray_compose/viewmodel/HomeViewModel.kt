package com.clearpath.xray_compose.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.data.repo.ProfileRepository
import com.clearpath.xray_compose.enums.EngineState
import com.clearpath.xray_compose.enums.HttpDelayStatus
import com.clearpath.xray_compose.service.engine.control.EngineRepository
import com.clearpath.xray_compose.service.engine.model.TrafficSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val configRepository: ConfigRepository,
    private val engineRepository: EngineRepository
) : ViewModel() {
    val engineStateFlow = engineRepository.engineStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EngineState.STOPPED)

    val engineErrorMsgFlow = engineRepository.lastErrorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trafficSummaryFlow = engineRepository.trafficSummaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrafficSummary())

    private val _engineHttpDelay = MutableStateFlow<HttpDelayStatus>(HttpDelayStatus.NotTested)
    val engineHttpDelayFlow = _engineHttpDelay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HttpDelayStatus.NotTested)

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
            engineRepository.startActiveProfileEngine()
        }
    }

    fun stopProxyCore() {
        viewModelScope.launch {
            engineRepository.stopEngine()
        }
    }

    fun consumeError() {
        engineRepository.consumeError()
    }

    fun measureHttpDelay() {
        viewModelScope.launch {
            _engineHttpDelay.value = HttpDelayStatus.Testing
            val delay = engineRepository.measureHttpDelay()
            _engineHttpDelay.value = if (delay > 0) {
                HttpDelayStatus.Success(delay)
            } else {
                HttpDelayStatus.Timeout
            }
        }
    }

    fun checkNotificationPermission() {
        viewModelScope.launch {
            val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
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
                context, Manifest.permission.QUERY_ALL_PACKAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (!hasManifestPermission) return false
        // NOTE: some rom (e.g. MIUI) may enable "pseudo permission" for QUERY_ALL_PACKAGES
        val testPackages = listOf(
            "com.android.settings",
            "com.google.android.webview",
        )
        var matchCount = 0
        try {
            val pm = context.packageManager
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
