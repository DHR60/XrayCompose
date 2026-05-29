package com.clearpath.xray_compose.service.engine.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.content.ContextCompat
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.connect.EngineNativeManager
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.utils.LogUtil
import com.clearpath.xray_compose.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.ProcessFinder
import java.lang.ref.SoftReference
import java.net.InetSocketAddress

object EngineServiceManager {
    private val engineController: CoreController =
        EngineNativeManager.newCoreController(EngineCallback())
    private val mMsgReceive = ReceiveMessageHandler()
    private var currentConfigContext: EngineConfigContext? = null
    private var processFinder: XrayProcessFinder? = null

    const val CHANNEL_ID = "vpn_service_channel"

    var serviceControl: SoftReference<IEngineServiceControl>? = null
        set(value) {
            field = value
            val service = value?.get()?.getService()
            EngineNativeManager.initCoreEnv(service)
            if (service != null && processFinder == null) {
                processFinder = XrayProcessFinder(service)
                engineController.registerProcessFinder(processFinder)
            }
        }

    fun startVService(context: Context) {
        LogUtil.i("StartEngine-Manager: startVService from ${context::class.java.simpleName}")

        try {
            startContextService(context)
        } catch (e: Exception) {
            LogUtil.e("StartEngine-Manager: ${e.message}", e)
        }
    }

    @Throws(Exception::class)
    private fun startContextService(context: Context) {
        if (engineController.isRunning) {
            LogUtil.w("StartEngine-Manager: Engine already running")
            return
        }
        val engineConfigContextBuilder = EngineConfigContextBuilder(context)
        val quickCheckResult = runBlocking {
            engineConfigContextBuilder.quickCheck()
        }
        if (quickCheckResult.isNotEmpty()) {
            LogUtil.e(
                "StartEngine-Manager: Engine config quick check failed: ${
                    quickCheckResult.joinToString(
                        "; "
                    )
                }"
            )
            return
        }
        val isVpnMode = runBlocking {
            engineConfigContextBuilder.isVpnMode()
        }
        val intent = if (isVpnMode) {
            LogUtil.i("StartEngine-Manager: Starting VPN service")
            Intent(context.applicationContext, EngineVpnService::class.java)
        } else {
            LogUtil.i("StartEngine-Manager: Starting Proxy service")
            Intent(context.applicationContext, EngineProxyOnlyService::class.java)
        }

        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: SecurityException) {
            LogUtil.e("StartEngine-Manager: Missing permission to start foreground service", e)
            throw IllegalStateException(e.message ?: e.javaClass.simpleName, e)
        } catch (e: RuntimeException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException"
            ) {
                LogUtil.e("StartEngine-Manager: Foreground service start not allowed", e)
                throw IllegalStateException(e.message ?: e.javaClass.simpleName, e)
            }
            throw e
        }
    }

    fun startCoreLoop(vpnInterface: ParcelFileDescriptor?): Boolean {
        if (engineController.isRunning) {
            LogUtil.w("StartEngine-Manager: Engine already running")
            return false
        }
        val service = getService()
        if (service == null) {
            LogUtil.e("StartEngine-Manager: Service control not available, cannot start engine")
            return false
        }
        return try {
            doStartCoreLoop(service, vpnInterface)
            LogUtil.i("StartEngine-Manager: Engine started successfully")
            true
        } catch (e: Exception) {
            LogUtil.e("StartEngine-Manager: Failed to start engine", e)
            false
        }
    }

    fun stopCoreLoop(): Boolean {
        val service = getService() ?: return false

        if (engineController.isRunning) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    engineController.stopLoop()
                } catch (e: Exception) {
                    LogUtil.e("StartEngine-Manager: Failed to stop core loop", e)
                }
            }
        }

        // NotificationManager.cancelNotification()

        try {
            service.unregisterReceiver(mMsgReceive)
        } catch (e: Exception) {
            LogUtil.e("StartEngine-Manager: Failed to unregister receiver", e)
        }

        return true
    }

    @Throws(Exception::class)
    private fun doStartCoreLoop(service: Service, vpnInterface: ParcelFileDescriptor?) {
        val configContextResult = runBlocking {
            EngineConfigContextBuilder(service).buildActiveProfile()
        }

        if (!configContextResult.success) {
            LogUtil.e(
                "StartEngine-Manager: Failed to build engine config context: ${
                    configContextResult.errors.joinToString(
                        "; "
                    )
                }"
            )
            return
        }

        val configContext = configContextResult.ecContext ?: run {
            LogUtil.e("StartEngine-Manager: Engine config context is null")
            return
        }

        currentConfigContext = configContext

        LogUtil.i("StartEngine-Manager: Starting engine with config context node: ${configContext.node.remark}")

        val configService = XrayConfigService(configContext)
        val engineConfig = configService.buildBaseConfig()

        val mFilter = IntentFilter(GlobalConst.broadcastActionService)
        mFilter.addAction(Intent.ACTION_SCREEN_ON)
        mFilter.addAction(Intent.ACTION_SCREEN_OFF)
        mFilter.addAction(Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(
            service,
            mMsgReceive, mFilter, Utils.receiverFlags()
        )

        createNotificationChannel(getService()!!)
        val notification =
            Notification.Builder(getService(), CHANNEL_ID)
                .setContentTitle(GlobalConst.appName)
                .setContentText("VPN is running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build()

        val tunFd = vpnInterface?.fd ?: 0

        engineController.startLoop(engineConfig, tunFd)

        if (!engineController.isRunning) {
            error("Core failed to start")
        }

        LogUtil.i("StartCore-Manager: Core started successfully")
    }

    private fun createNotificationChannel(context: Context) {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "VPN Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    fun stopVService(context: Context) {
        // to ReceiveMessageHandler
        // sendMsg2Service(context, MSG_STATE_STOP, "")
    }

    fun isRunning() = engineController.isRunning

    fun getRunningServerName() = currentConfigContext?.node?.remark ?: "Unknown"

    private fun getService(): Service? {
        return serviceControl?.get()?.getService()
    }

    private class EngineCallback : CoreCallbackHandler {
        override fun startup(): Long {
            return 0
        }

        override fun shutdown(): Long {
            val serviceControl = serviceControl?.get() ?: return -1
            return try {
                serviceControl.stopService()
                0
            } catch (e: Exception) {
                LogUtil.e("StartEngine-Manager: Failed to stop service", e)
                -1
            }
        }

        override fun onEmitStatus(l: Long, s: String?): Long {
            return 0
        }
    }

    private class XrayProcessFinder(context: Context) : ProcessFinder {
        private val cm: ConnectivityManager? =
            context.getSystemService(ConnectivityManager::class.java)

        override fun findProcessByConnection(
            network: String,
            srcIP: String,
            srcPort: Long,
            destIP: String,
            destPort: Long
        ): Long {
            if (cm == null) return -1L
            val proto = when (network) {
                "tcp" -> OsConstants.IPPROTO_TCP
                "udp" -> OsConstants.IPPROTO_UDP
                else -> return -1L
            }

            if (destIP.isBlank() || destPort == 0L) {
                LogUtil.d("ProcessFinder: Find $network connection from $srcIP:$srcPort to :$destPort, (no dest)")
                return -1L
            }

            return try {
                val uid = cm.getConnectionOwnerUid(
                    proto,
                    InetSocketAddress(srcIP, srcPort.toInt()),
                    InetSocketAddress(destIP, destPort.toInt())
                ).toLong()
                LogUtil.d("ProcessFinder: Find $network connection from $srcIP:$srcPort to $destIP:$destPort, uid=$uid")

                uid
            } catch (_: Exception) {
                -1L
            }
        }
    }

    private class ReceiveMessageHandler : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            // val serviceControl = serviceControl?.get() ?: return
            // when (intent?.getIntExtra("key", 0)) {
            //     AppConfig.MSG_REGISTER_CLIENT -> {
            //         if (coreController.isRunning) {
            //             MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_RUNNING, "")
            //         } else {
            //             MessageUtil.sendMsg2UI(serviceControl.getService(), AppConfig.MSG_STATE_NOT_RUNNING, "")
            //         }
            //     }
            //
            //     AppConfig.MSG_UNREGISTER_CLIENT -> {
            //         // nothing to do
            //     }
            //
            //     AppConfig.MSG_STATE_START -> {
            //         // nothing to do
            //     }
            //
            //     AppConfig.MSG_STATE_STOP -> {
            //         LogUtil.i(AppConfig.TAG, "StartCore-Manager: Stop service")
            //         serviceControl.stopService()
            //     }
            //
            //     AppConfig.MSG_STATE_RESTART -> {
            //         LogUtil.i(AppConfig.TAG, "StartCore-Manager: Restart service")
            //         serviceControl.stopService()
            //         Thread.sleep(500L)
            //         startVService(serviceControl.getService())
            //     }
            //
            //     AppConfig.MSG_MEASURE_DELAY -> {
            //         measureV2rayDelay()
            //     }
            // }
            //
            // when (intent?.action) {
            //     Intent.ACTION_SCREEN_OFF -> {
            //         LogUtil.i(AppConfig.TAG, "StartCore-Manager: Screen off")
            //         NotificationManager.stopSpeedNotification()
            //     }
            //
            //     Intent.ACTION_SCREEN_ON -> {
            //         LogUtil.i(AppConfig.TAG, "StartCore-Manager: Screen on")
            //         NotificationManager.startSpeedNotification()
            //     }
            // }
        }
    }
}