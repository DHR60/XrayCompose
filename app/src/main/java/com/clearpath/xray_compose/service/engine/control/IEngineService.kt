package com.clearpath.xray_compose.service.engine.control

import android.app.Service

interface IEngineService {
    fun getService(): Service
    fun startService()
    fun stopService()
    fun vpnProtect(socket: Int): Boolean
}