package com.clearpath.xray_compose.service.engine.control

import android.app.Service

interface IEngineServiceControl {
    fun getService(): Service
    fun startService()
    fun stopService()
    fun vpnProtect(socket: Int): Boolean
}