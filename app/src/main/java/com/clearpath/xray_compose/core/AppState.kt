package com.clearpath.xray_compose.core

import kotlinx.coroutines.flow.MutableStateFlow

object AppState {
    // 0 = not running, 1 = running, 2 = initializing
    val isProxyCoreRunningFlow = MutableStateFlow(0)
    val selectedProfileIdSetFlow = MutableStateFlow<HashSet<String>>(hashSetOf())
}