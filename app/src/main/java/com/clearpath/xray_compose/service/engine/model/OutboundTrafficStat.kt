package com.clearpath.xray_compose.service.engine.model

data class OutboundTrafficStat(
    val tag: String,
    val direction: String,
    val value: Long,
)
