package com.clearpath.xray_compose.service.engine.config.processor

import com.clearpath.xray_compose.service.engine.config.XrayConfig
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext

class XrayConfigLogProcessor(
    private val ecContext: EngineConfigContext,
    private val config: XrayConfig
) {
    fun genLog() {
        config.log.loglevel = "debug"
    }
}