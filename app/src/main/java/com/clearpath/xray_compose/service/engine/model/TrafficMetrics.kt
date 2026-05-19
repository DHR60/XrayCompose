package com.clearpath.xray_compose.service.engine.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TrafficMetrics(
    val speed: Long = 0,
    val total: Long = 0,
) : Parcelable {
    fun isZeroSpeed(): Boolean = speed == 0L
}

@Parcelize
data class DirectionalTraffic(
    val up: TrafficMetrics = TrafficMetrics(),
    val down: TrafficMetrics = TrafficMetrics(),
) : Parcelable {
    fun isZeroSpeed(): Boolean = up.isZeroSpeed() && down.isZeroSpeed()
}

@Parcelize
data class TrafficSummary(
    val direct: DirectionalTraffic = DirectionalTraffic(),
    val proxy: DirectionalTraffic = DirectionalTraffic(),
    val timestamp: Long = System.currentTimeMillis(),
) : Parcelable {
    fun isZeroSpeed(): Boolean = direct.isZeroSpeed() && proxy.isZeroSpeed()
}
