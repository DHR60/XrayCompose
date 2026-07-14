package com.clearpath.xray_compose.viewmodel.uistate

import android.content.pm.ApplicationInfo

data class AppItemInfo(
    val appName: String,
    val packageName: String,
    // For app icon
    val applicationInfo: ApplicationInfo,
    // val appIcon: Drawable,
    val isSystemApp: Boolean
)