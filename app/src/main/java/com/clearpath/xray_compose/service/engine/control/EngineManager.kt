package com.clearpath.xray_compose.service.engine.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteCallbackList
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.IEngineServiceCallback
import com.clearpath.xray_compose.IEngineServiceControl
import com.clearpath.xray_compose.MainActivity
import com.clearpath.xray_compose.enums.EngineState
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.connect.EngineNativeManager
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.service.engine.model.DirectionalTraffic
import com.clearpath.xray_compose.service.engine.model.OutboundTrafficStat
import com.clearpath.xray_compose.service.engine.model.TrafficMetrics
import com.clearpath.xray_compose.service.engine.model.TrafficSummary
import com.clearpath.xray_compose.utils.LogUtil
import com.clearpath.xray_compose.utils.Utils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.ProcessFinder
import java.net.InetSocketAddress
import javax.inject.Inject
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

class EngineManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val configContextBuilder: EngineConfigContextBuilder
) {
    private var serviceControl: IEngineService? = null

    fun attach(control: IEngineService) {
        this.serviceControl = control
    }

    companion object {
        const val BROADCAST_ACTION_SERVICE = "${GlobalConst.appId}.action.service"
        const val ACTION_START_ENGINE = 1005
        const val ACTION_STOP_ENGINE = 1006
        const val ACTION_RESTART_ENGINE = 1007

        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_PENDING_INTENT_CONTENT = 0
        private const val NOTIFICATION_PENDING_INTENT_STOP_ENGINE = 1
        private const val NOTIFICATION_PENDING_INTENT_RESTART_ENGINE = 2

        const val QUERY_INTERVAL_MS = 3000L

        private const val CHANNEL_ID = "${GlobalConst.appId}.engine_notification_channel"
        private const val CHANNEL_NAME = "Engine Service Notifications"
    }

    private val engineController: CoreController =
        EngineNativeManager.newCoreController(EngineCallback())
    private val mMsgReceive = ReceiveMessageHandler()
    private var serviceNotification: EngineServiceNotification? = null
    private val trafficStatMonitor = EngineTrafficStatMonitor()
    private var currentConfigContext: EngineConfigContext? = null
    private var processFinder: XrayProcessFinder? = null
    var vpnInterface: ParcelFileDescriptor? = null

    private var currentState = EngineState.STOPPED

    private val callbacks =
        RemoteCallbackList<IEngineServiceCallback>()

    private val binder = object : IEngineServiceControl.Stub() {
        override fun start() {
            LogUtil.i("StartEngine-Manager: Received start command")
            serviceControl?.startService()
        }

        override fun stop() {
            LogUtil.i("StartEngine-Manager: Received stop command")
            stopCoreLoop()
        }

        override fun getState(): Int {
            return currentState.value
        }

        override fun measureHttpDelay(): Long {
            return this@EngineManager.measureHttpDelay()
        }

        override fun measureHttpDelayWithUrl(url: String?): Long {
            return this@EngineManager.measureHttpDelayWithUrl(url ?: "")
        }

        override fun getTrafficSummary(): TrafficSummary {
            // return trafficStatMonitor.queryOnce()
            return trafficStatMonitor.trafficStatFlow.value
        }

        override fun startTrafficStatMonitor() {
            trafficStatMonitor.startQuery()
        }

        override fun stopTrafficStatMonitor() {
            trafficStatMonitor.stopQuery()
        }

        override fun registerCallback(callback: IEngineServiceCallback?) {
            if (callback != null) {
                callbacks.register(callback)
            }
        }

        override fun unregisterCallback(callback: IEngineServiceCallback?) {
            if (callback != null) {
                callbacks.unregister(callback)
            }
        }
    }

    fun getBinder(): IBinder {
        return binder
    }

    private fun changeState(state: EngineState) {
        currentState = state
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onStateChanged(state.value)
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    private fun notifyError(errorMessage: String?) {
        val count = callbacks.beginBroadcast()
        try {
            for (i in 0 until count) {
                callbacks.getBroadcastItem(i).onError(errorMessage)
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    fun initialize() {
        LogUtil.i("StartEngine-Manager: Initializing engine controller")
        trafficStatMonitor.init()
        EngineNativeManager.initCoreEnv(context)
        if (processFinder == null) {
            processFinder = XrayProcessFinder()
            engineController.registerProcessFinder(processFinder)
        }
    }

    suspend fun buildConfigContext() {
        val configContextResult = configContextBuilder.buildActiveProfile()
        if (!configContextResult.success) {
            val errorMessage = configContextResult.errors.joinToString("; ")
            LogUtil.e("StartEngine-Manager: Failed to build config context: $errorMessage")
            error(errorMessage)
        }
        currentConfigContext = configContextResult.ecContext ?: run {
            error("Engine config context is null")
        }
    }

    fun getCurrentConfigContext(): EngineConfigContext {
        return currentConfigContext ?: error("Call buildConfigContext() first")
    }

    fun startActiveProfile(): Boolean = try {
        changeState(EngineState.STARTING)
        doStartActiveProfile()
        true
    } catch (e: Exception) {
        LogUtil.e("StartEngine-Manager: Failed to start active profile", e)
        notifyError(e.message ?: e.javaClass.simpleName)
        stopCoreLoop()
        false
    }

    @Throws(Exception::class)
    private fun doStartActiveProfile() {
        if (engineController.isRunning) {
            LogUtil.w("StartEngine-Manager: Engine already running")
            return
        }
        val configContext = getCurrentConfigContext()

        LogUtil.i("StartEngine-Manager: Starting engine with config context node: ${configContext.node.remark}")

        showForegroundNotification()

        val configService = XrayConfigService(configContext)
        val engineConfig = configService.buildBaseConfig()

        val mFilter = IntentFilter(BROADCAST_ACTION_SERVICE)
        mFilter.addAction(Intent.ACTION_SCREEN_ON)
        mFilter.addAction(Intent.ACTION_SCREEN_OFF)
        mFilter.addAction(Intent.ACTION_USER_PRESENT)
        ContextCompat.registerReceiver(
            context,
            mMsgReceive, mFilter, Utils.receiverFlags()
        )

        val tunFd = vpnInterface?.fd ?: 0

        engineController.startLoop(engineConfig, tunFd)

        if (!engineController.isRunning) {
            error("Core failed to start")
        }

        trafficStatMonitor.startQuery()
        serviceNotification?.tryStartSpeedNotification()

        LogUtil.i("StartEngine-Manager: Core started successfully")
    }

    fun stopCoreLoop() {
        if (currentState == EngineState.STOPPED || currentState == EngineState.STOPPING) return
        changeState(EngineState.STOPPING)

        try {
            vpnInterface?.close()
        } finally {
            vpnInterface = null
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (engineController.isRunning) engineController.stopLoop()
            } catch (e: Exception) {
                LogUtil.e("StopEngine: ${e.message}")
            } finally {
                changeState(EngineState.STOPPED)
                serviceControl?.stopService()
            }
        }
        trafficStatMonitor.stopQuery()
        cancelForegroundNotification()
        try {
            context.unregisterReceiver(mMsgReceive)
        } catch (_: Exception) {
        }
    }

    fun showForegroundNotification() {
        if (serviceNotification == null) {
            serviceNotification = EngineServiceNotification()
        }
        serviceNotification?.showNotification()
    }

    fun cancelForegroundNotification() {
        serviceNotification?.cancelNotification()
        serviceNotification = null
    }

    fun isVpnMode(): Boolean {
        val configContext = getCurrentConfigContext()
        val vpnMode = configContext.engineConfig.inbound.tun.enable
        return vpnMode
    }

    // fun isRunning() = engineController.isRunning

    fun getRunningServerName() = currentConfigContext?.node?.remark ?: "Unknown"

    fun queryAllOutboundTrafficStats(): List<OutboundTrafficStat> {
        val payload = engineController.queryAllOutboundTrafficStats()

        val result = ArrayList<OutboundTrafficStat>()

        payload.split(';').forEach { entry ->
            if (entry.isBlank()) return@forEach

            val parts = entry.split(',', limit = 3)
            if (parts.size != 3) return@forEach

            val value = parts[2].toLongOrNull() ?: return@forEach

            result.add(
                OutboundTrafficStat(
                    tag = parts[0],
                    direction = parts[1],
                    value = value,
                )
            )
        }
        return result
    }

    fun measureHttpDelay(): Long {
        return measureHttpDelayWithUrl("https://www.google.com/generate_204")
    }

    fun measureHttpDelayWithUrl(url: String): Long {
        if (!engineController.isRunning) {
            return -1L
        }

        var time = -1L
        var errorStr = ""
        for (i in 0..3) {
            if (time > 0) {
                break
            }
            try {
                time = engineController.measureDelay(url)
            } catch (e: Exception) {
                LogUtil.e("StartEngine-Manager: Failed to measure HTTP delay", e)
                errorStr = e.message?.substringAfter("\":") ?: e.javaClass.simpleName
            }
        }
        if (errorStr.isNotBlank()) {
            notifyError("Failed to measure HTTP delay: $errorStr")
        }
        return time
    }

    private fun getService(): Service {
        return serviceControl?.getService() ?: error("Service not attached")
    }

    private inner class EngineCallback : CoreCallbackHandler {
        override fun startup(): Long {
            changeState(EngineState.STARTED)
            return 0
        }

        override fun shutdown(): Long {
            return try {
                changeState(EngineState.STOPPED)
                serviceControl?.stopService()
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

    private inner class XrayProcessFinder : ProcessFinder {
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

    private inner class ReceiveMessageHandler : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                ACTION_STOP_ENGINE -> {
                    LogUtil.i("StartEngine-Manager: Received stop engine command")
                    changeState(EngineState.STOPPING)
                    stopCoreLoop()
                }

                ACTION_RESTART_ENGINE -> {
                    LogUtil.i("StartEngine-Manager: Received restart engine command")
                    changeState(EngineState.STOPPING)
                    stopCoreLoop()
                    startActiveProfile()
                }

                ACTION_START_ENGINE -> {
                    LogUtil.i("StartEngine-Manager: Received start engine command")
                    startActiveProfile()
                }
            }
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    LogUtil.i("StartEngine-Manager: Screen off")
                    serviceNotification?.stopSpeedNotification()
                }

                Intent.ACTION_SCREEN_ON -> {
                    LogUtil.i("StartEngine-Manager: Screen on")
                    serviceNotification?.tryStartSpeedNotification()
                }
            }
        }
    }

    private inner class EngineServiceNotification {
        private var notification: Notification? = null

        private var notificationTrafficBindJob: Job? = null
        private val isTrackingTraffic = MutableStateFlow(false)

        fun showNotification() {
            val builder = createBaseBuilder()
            getService().startForeground(NOTIFICATION_ID, builder.build())
        }

        fun cancelNotification() {
            getService().stopForeground(Service.STOP_FOREGROUND_REMOVE)
        }

        @Suppress("SameReturnValue")
        fun tryStartSpeedNotification(): Boolean {
            // get is enabled speed notification setting from currentConfigContext
            isTrackingTraffic.value = true
            if (notificationTrafficBindJob != null) {
                return true
            }

            notificationTrafficBindJob = CoroutineScope(Dispatchers.IO).launch {
                trafficStatMonitor.trafficStatFlow.collect { trafficSummary ->
                    if (isTrackingTraffic.value) {
                        updateSpeedNotificationOnce(trafficSummary)
                    }
                }
            }

            return true
        }

        fun stopSpeedNotification() {
            isTrackingTraffic.value = false
        }

        private fun createBaseBuilder(): NotificationCompat.Builder {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

            val startMainIntent = Intent(getService(), MainActivity::class.java)
            val contentPendingIntent = PendingIntent.getActivity(
                getService(),
                NOTIFICATION_PENDING_INTENT_CONTENT,
                startMainIntent,
                flags
            )

            val stopEngineIntent = Intent(BROADCAST_ACTION_SERVICE).apply {
                putExtra("key", ACTION_STOP_ENGINE)
                `package` = GlobalConst.appId
            }
            val stopEnginePendingIntent = PendingIntent.getBroadcast(
                getService(),
                NOTIFICATION_PENDING_INTENT_STOP_ENGINE,
                stopEngineIntent,
                flags
            )

            val restartEngineIntent = Intent(BROADCAST_ACTION_SERVICE).apply {
                putExtra("key", ACTION_RESTART_ENGINE)
                `package` = GlobalConst.appId
            }
            val restartEnginePendingIntent = PendingIntent.getBroadcast(
                getService(),
                NOTIFICATION_PENDING_INTENT_RESTART_ENGINE,
                restartEngineIntent,
                flags
            )

            createNotificationChannel()

            return NotificationCompat.Builder(getService(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getRunningServerName())
                .setContentText("VPN is running")
                .setSubText("Proxy: 0 B  Direct: 0 B")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopEnginePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_media_play,
                    "Restart",
                    restartEnginePendingIntent
                )
        }

        private fun createNotificationChannel() {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                importance = NotificationManager.IMPORTANCE_LOW
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            val manager = getService().getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        private fun updateSpeedNotificationOnce(trafficSummary: TrafficSummary) {
            val proxyTotal = trafficSummary.proxy.up.total + trafficSummary.proxy.down.total
            val directTotal = trafficSummary.direct.up.total + trafficSummary.direct.down.total
            val text = StringBuilder()
            appendSpeedString(
                text, GlobalConst.proxyTag,
                trafficSummary.proxy.up.speed,
                trafficSummary.proxy.down.speed
            )

            appendSpeedString(
                text, GlobalConst.directTag,
                trafficSummary.direct.up.speed,
                trafficSummary.direct.down.speed
            )
            updateNotification(text.toString(), proxyTotal, directTotal)
        }

        private fun appendSpeedString(
            text: StringBuilder,
            name: String?,
            up: Long,
            down: Long
        ) {
            var n = name ?: "no tag"
            n = n.take(min(n.length, 6))
            text.append(n)
            for (i in n.length..6 step 2) {
                text.append("\t")
            }
            text.append("•  ${longToSpeedString(up)}↑  ${longToSpeedString(down)}↓\n")
        }

        private fun longToSpeedString(value: Long): String {
            if (value <= 0) return "0 B/s"
            val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
            var speed = value.toDouble()
            var unitIndex = 0
            while (speed >= 1024 && unitIndex < units.size - 1) {
                speed /= 1024
                unitIndex++
            }
            return if (unitIndex == 0) {
                "$value B/s"
            } else {
                String.format(java.util.Locale.US, "%.1f %s", speed, units[unitIndex])
            }
        }

        private fun longToSizeString(value: Long): String {
            if (value <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var size = value.toDouble()
            var unitIndex = 0
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            return if (unitIndex == 0) "$value B"
            else String.format(java.util.Locale.US, "%.1f %s", size, units[unitIndex])
        }

        private fun updateNotification(
            contentText: String?,
            proxyTrafficTotal: Long,
            directTrafficTotal: Long
        ) {
            val builder = createBaseBuilder()
                .setContentTitle(
                    "${getRunningServerName()}    Proxy: ${
                        longToSizeString(
                            proxyTrafficTotal
                        )
                    }"
                )
                .setContentText(contentText ?: "VPN is running")
                .setSubText(
                    "Proxy: ${longToSizeString(proxyTrafficTotal)}  Direct: ${
                        longToSizeString(
                            directTrafficTotal
                        )
                    }"
                )
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(contentText ?: "VPN is running")
                )

            notification = builder.build()
            val manager =
                getService().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private inner class EngineTrafficStatMonitor {
        private var lastTrafficSummary = TrafficSummary()

        private val _isTracking = MutableStateFlow(false)
        private var collectJob: Job? = null

        private val _trafficStatFlow = MutableStateFlow(TrafficSummary())
        val trafficStatFlow = _trafficStatFlow.asStateFlow()

        fun init() {
            collectJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    _isTracking.first { it } // wait until tracking is started
                    val summary = queryOnce()
                    _trafficStatFlow.value = summary
                    delay(QUERY_INTERVAL_MS.milliseconds)
                }
            }
        }

        fun startQuery() {
            _isTracking.value = true
        }

        fun stopQuery() {
            _isTracking.value = false
        }

        fun shutdown() {
            collectJob?.cancel()
            collectJob = null
        }

        fun queryOnce(): TrafficSummary {
            val queryTime = System.currentTimeMillis()
            val sinceLastQueryIn = (queryTime - lastTrafficSummary.timestamp)

            if (sinceLastQueryIn == 0L) {
                return lastTrafficSummary
            }

            val sinceLastQueryInSeconds = sinceLastQueryIn / 1000.0

            var proxyUplink = 0L
            var proxyDownlink = 0L
            var directUplink = 0L
            var directDownlink = 0L

            queryAllOutboundTrafficStats().forEach { stat ->
                when {
                    stat.tag == GlobalConst.directTag -> {
                        when (stat.direction) {
                            GlobalConst.uplink -> directUplink += stat.value
                            GlobalConst.downlink -> directDownlink += stat.value
                        }
                    }

                    stat.tag.startsWith(GlobalConst.proxyTag) -> {
                        when (stat.direction) {
                            GlobalConst.uplink -> proxyUplink += stat.value
                            GlobalConst.downlink -> proxyDownlink += stat.value
                        }
                    }
                }
            }

            val trafficSummary = TrafficSummary(
                direct = DirectionalTraffic(
                    up = TrafficMetrics(
                        speed = (directUplink / sinceLastQueryInSeconds).toLong(),
                        total = directUplink + (lastTrafficSummary.direct.up.total)
                    ),
                    down = TrafficMetrics(
                        speed = (directDownlink / sinceLastQueryInSeconds).toLong(),
                        total = directDownlink + (lastTrafficSummary.direct.down.total)
                    )
                ),
                proxy = DirectionalTraffic(
                    up = TrafficMetrics(
                        speed = (proxyUplink / sinceLastQueryInSeconds).toLong(),
                        total = proxyUplink + (lastTrafficSummary.proxy.up.total)
                    ),
                    down = TrafficMetrics(
                        speed = (proxyDownlink / sinceLastQueryInSeconds).toLong(),
                        total = proxyDownlink + (lastTrafficSummary.proxy.down.total)
                    )
                ),
                timestamp = queryTime
            )

            lastTrafficSummary = trafficSummary

            return trafficSummary
        }
    }
}