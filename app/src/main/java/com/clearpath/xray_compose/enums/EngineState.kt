package com.clearpath.xray_compose.enums

enum class EngineState(val value: Int) {
    STOPPED(0),
    STARTING(1),
    STARTED(2),
    STOPPING(3),
    ERROR(4);

    companion object {
        fun fromValue(value: Int): EngineState {
            return entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Invalid EngineState value: $value")
        }
    }
}