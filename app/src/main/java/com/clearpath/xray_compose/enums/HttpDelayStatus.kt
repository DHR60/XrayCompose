package com.clearpath.xray_compose.enums

sealed interface HttpDelayStatus {
    data object NotTested : HttpDelayStatus
    data object Testing : HttpDelayStatus
    data class Success(val delayMs: Long) : HttpDelayStatus
    data object Timeout : HttpDelayStatus
}
