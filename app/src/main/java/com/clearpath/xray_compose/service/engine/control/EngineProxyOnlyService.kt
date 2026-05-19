package com.clearpath.xray_compose.service.engine.control

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.clearpath.xray_compose.utils.LogUtil

class EngineProxyOnlyService : Service(), IEngineService {
    private var engineManager: EngineManager? = null

    override fun onCreate() {
        super.onCreate()
        LogUtil.i("StartEngine-Proxy: Service created")
        engineManager = EngineManager(this).apply {
            initialize()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return engineManager?.getBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        LogUtil.i("StartEngine-Proxy: Service command received")
        engineManager?.showForegroundNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.i("StartEngine-Proxy: Service destroyed")
        engineManager?.stopCoreLoop()
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        engineManager?.startActiveProfile()
    }

    override fun stopService() {
        stopSelf()
    }

    override fun vpnProtect(socket: Int): Boolean {
        return true
    }
}