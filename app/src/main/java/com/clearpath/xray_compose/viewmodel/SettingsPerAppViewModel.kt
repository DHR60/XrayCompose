package com.clearpath.xray_compose.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ConfigPerAppItem
import com.clearpath.xray_compose.data.repo.ConfigRepository
import com.clearpath.xray_compose.data.repo.PreferencesRepository
import com.clearpath.xray_compose.utils.LogUtil
import com.clearpath.xray_compose.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppItemInfo(
    val appName: String,
    val packageName: String,
    // For app icon
    val applicationInfo: ApplicationInfo,
    // val appIcon: Drawable,
    val isSystemApp: Boolean
)

@HiltViewModel
class SettingsPerAppViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val configRepository: ConfigRepository
) : ViewModel() {
    private val prefsActiveEngineSettingIdFlow = preferencesRepository.activeEngineSettingIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val configEngineSettingListFlow = configRepository.engineSettingListFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val activeEngineSettingIdFlow = combine(
        prefsActiveEngineSettingIdFlow,
        configEngineSettingListFlow
    ) { activeId, list ->
        if (list.any { it.id == activeId }) activeId else list.firstOrNull()?.id
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeEngineSettingFlow = combine(
        activeEngineSettingIdFlow,
        configEngineSettingListFlow
    ) { activeId, list ->
        list.find { it.id == activeId } ?: ConfigEngineItem()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConfigEngineItem())

    private val _allAppPackagesFlow = MutableStateFlow<List<AppItemInfo>>(emptyList())
    val allAppPackagesFlow = _allAppPackagesFlow.asStateFlow()

    val iconCache = mutableStateMapOf<String, Drawable>()

    private val _selectedAppPackagesFlow = MutableStateFlow<List<String>>(emptyList())
    val selectedAppPackagesFlow = _selectedAppPackagesFlow.asStateFlow()

    private val _perAppEnabledFlow = MutableStateFlow(false)
    val perAppEnabledFlow = _perAppEnabledFlow.asStateFlow()

    private val _perAppBypassFlow = MutableStateFlow(false)
    val perAppBypassFlow = _perAppBypassFlow.asStateFlow()

    private val _searchQueryFlow = MutableStateFlow("")
    val searchQueryFlow = _searchQueryFlow.asStateFlow()

    val displayAppPackagesFlow = combine(
        allAppPackagesFlow,
        searchQueryFlow,
    ) { appList, query ->
        val selectedPackages = _selectedAppPackagesFlow.value
        val filteredList = if (query.isBlank()) {
            appList
        } else {
            filterApps(query)
        }

        filteredList.sortedWith(
            compareByDescending<AppItemInfo> { selectedPackages.contains(it.packageName) }
                .thenBy { it.isSystemApp }
                .thenBy { it.appName }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isBusy = MutableStateFlow(false)
    val isBusyFlow = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessageFlow = _errorMessage.asStateFlow()

    private val fetchingPackages = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            activeEngineSettingFlow.collect { setting ->
                _perAppEnabledFlow.value = setting.perApp.enable
                _perAppBypassFlow.value = setting.perApp.bypass
                _selectedAppPackagesFlow.value = setting.perApp.packageList
            }
        }

        refreshAllAppPackages()
    }

    fun updatePerAppSetting(update: (ConfigPerAppItem) -> ConfigPerAppItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = preferencesRepository.getActiveEngineSettingId()
            configRepository.updateConfig { config ->
                val targetId = activeId ?: config.engineSettingList.firstOrNull()?.id
                if (targetId == null) return@updateConfig config

                val newList = config.engineSettingList.map { item ->
                    if (item.id == targetId) {
                        item.copy(perApp = update(item.perApp))
                    } else {
                        item
                    }
                }
                config.copy(engineSettingList = newList)
            }
        }
    }

    fun updatePerAppMode(enabled: Boolean, bypass: Boolean) {
        _perAppEnabledFlow.value = enabled
        _perAppBypassFlow.value = bypass
        updatePerAppSetting { currentPerApp ->
            currentPerApp.copy(
                enable = enabled,
                bypass = bypass
            )
        }
    }

    fun updatePerAppEnabled(enabled: Boolean) {
        _perAppEnabledFlow.value = enabled
        updatePerAppSetting { currentPerApp ->
            currentPerApp.copy(
                enable = enabled
            )
        }
    }

    fun updatePerAppBypass(bypass: Boolean) {
        _perAppBypassFlow.value = bypass
        updatePerAppSetting { currentPerApp ->
            currentPerApp.copy(
                bypass = bypass
            )
        }
    }

    fun toggleAppPackageSelection(packageName: String) {
        val currentList = _selectedAppPackagesFlow.value.toMutableList()
        if (currentList.contains(packageName)) {
            currentList.remove(packageName)
        } else {
            currentList.add(packageName)
        }
        _selectedAppPackagesFlow.value = currentList

        updatePerAppSetting { currentPerApp ->
            val repoList = currentPerApp.packageList.toMutableList()
            if (repoList.contains(packageName)) {
                repoList.remove(packageName)
            } else {
                repoList.add(packageName)
            }
            currentPerApp.copy(
                packageList = repoList
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refreshAllAppPackages() {
        _isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appList = fetchAllApps()
                _allAppPackagesFlow.value = appList
                val testPackages = listOf(
                    "com.android.settings",
                    "com.google.android.webview",
                )
                val isContain = appList.any { testPackages.contains(it.packageName) }
                if (!isContain) {
                    _errorMessage.value =
                        "Please Check Query All Packages Permission!"
                }
            } catch (e: Exception) {
                LogUtil.e("SettingsPerAppViewModel Failed to refresh all app packages", e)
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun fetchAppIcon(appInfo: ApplicationInfo) {
        val packageName = appInfo.packageName
        if (iconCache.containsKey(packageName) || fetchingPackages.contains(packageName)) return

        fetchingPackages.add(packageName)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val icon = getAppIcon(appInfo)
                if (icon != null) {
                    iconCache[packageName] = icon
                }
            } finally {
                fetchingPackages.remove(packageName)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQueryFlow.value = query
    }

    fun importFromClipboard() {
        _isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clipboardText = Utils.getClipboard(context)
                if (clipboardText.isBlank()) return@launch

                val lines = clipboardText.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) return@launch

                val bypass = when (lines.first()) {
                    GlobalConst.trueStr -> true
                    GlobalConst.falseStr -> false
                    else -> return@launch
                }
                val packages = lines.drop(1)

                _perAppBypassFlow.value = bypass
                _selectedAppPackagesFlow.value = packages

                updatePerAppSetting { currentPerApp ->
                    currentPerApp.copy(
                        bypass = bypass,
                        packageList = packages
                    )
                }
            } catch (e: Exception) {
                LogUtil.e("SettingsPerAppViewModel Failed to import from clipboard", e)
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun exportToClipboard() {
        _isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val builder = StringBuilder()
                if (perAppBypassFlow.value) {
                    builder.appendLine(GlobalConst.trueStr)
                } else {
                    builder.appendLine(GlobalConst.falseStr)
                }
                selectedAppPackagesFlow.value.forEach {
                    builder.appendLine(it)
                }
                val str = builder.toString()
                Utils.setClipboard(context, str)
            } catch (e: Exception) {
                LogUtil.e("SettingsPerAppViewModel Failed to export to clipboard", e)
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun filterApps(query: String): List<AppItemInfo> {
        return _allAppPackagesFlow.value.filter { appInfo ->
            appInfo.appName.contains(query, ignoreCase = true)
                    || appInfo.packageName.contains(query, ignoreCase = true)
        }
    }

    private fun getAppIcon(appInfo: ApplicationInfo): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(appInfo.packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            LogUtil.e("SettingsPerAppViewModel Failed to load icon for ${appInfo.packageName}", e)
            null
        }
    }

    private fun fetchAllApps(): List<AppItemInfo> {
        val appList = mutableListOf<AppItemInfo>()
        val pm = context.packageManager

        val flags = PackageManager.GET_PERMISSIONS
        val installedPackages: List<PackageInfo> = pm.getInstalledPackages(flags)

        for (packageInfo in installedPackages) {
            val appInfo = packageInfo.applicationInfo ?: continue
            val permissionsList = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val hasInternetPermission = permissionsList.contains(Manifest.permission.INTERNET)
            if (!hasInternetPermission) {
                context
            }
            val appName = appInfo.loadLabel(pm).toString()
            val packageName = appInfo.packageName
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0)

            val appItem = AppItemInfo(
                appName = appName,
                packageName = packageName,
                applicationInfo = appInfo,
                isSystemApp = isSystemApp
            )
            appList.add(appItem)
        }
        return appList.sortedWith(
            compareBy<AppItemInfo> { it.isSystemApp }
                .thenBy { it.appName }
        )
    }
}