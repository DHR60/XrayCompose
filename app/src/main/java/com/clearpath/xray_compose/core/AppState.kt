package com.clearpath.xray_compose.core

import com.clearpath.xray_compose.enums.EngineState
import kotlinx.coroutines.flow.MutableStateFlow

object AppState {
    val engineStateFlow = MutableStateFlow(EngineState.STOPPED)
}