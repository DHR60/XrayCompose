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
import com.clearpath.xray_compose.utils.LogUtil
import com.clearpath.xray_compose.viewmodel.uistate.AppItemInfo
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

@HiltViewModel
class AppSelectorViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _allAppPackagesFlow = MutableStateFlow<List<AppItemInfo>>(emptyList())
    val allAppPackagesFlow = _allAppPackagesFlow.asStateFlow()

    val iconCache = mutableStateMapOf<String, Drawable>()

    private val _selectedAppPackagesFlow = MutableStateFlow<List<String>>(emptyList())

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

    private val fetchingPackages = mutableSetOf<String>()

    init {
        refreshAllAppPackages()
    }

    fun setSelectedPackages(packages: List<String>) {
        _selectedAppPackagesFlow.value = packages
    }

    fun refreshAllAppPackages() {
        _isBusy.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appList = fetchAllApps()
                _allAppPackagesFlow.value = appList
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
            // if (packageName == GlobalConst.unidentifiedPackageName) {
            //     context.getDrawable(R.drawable.ic_question_mark)?.let {
            //         iconCache[packageName] = it
            //     }
            //     fetchingPackages.remove(packageName)
            //     return@launch
            // }

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