package com.clearpath.xray_compose.service.engine.control

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.clearpath.xray_compose.utils.LogUtil
import java.lang.ref.SoftReference

class EngineProxyOnlyService : Service(), IEngineServiceControl {
    override fun onCreate() {
        super.onCreate()
        LogUtil.i("StartEngine-Proxy: Service created")
        EngineServiceManager.serviceControl = SoftReference(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i("StartEngine-Proxy: Service command received")
        EngineServiceManager.startCoreLoop(null)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        EngineServiceManager.stopCoreLoop()
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        // do nothing
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean {
        return true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}