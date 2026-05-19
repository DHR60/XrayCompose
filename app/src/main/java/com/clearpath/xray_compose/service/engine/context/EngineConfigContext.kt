package com.clearpath.xray_compose.service.engine.context

import com.clearpath.xray_compose.data.ConfigEngineItem
import com.clearpath.xray_compose.data.ProfileModel

data class EngineConfigContext(
    val node: ProfileModel,
    val engineConfig: ConfigEngineItem,
    val allProxiesMap: Map<String, ProfileModel> = emptyMap(),
    val isTunEnabled: Boolean = false,
)
