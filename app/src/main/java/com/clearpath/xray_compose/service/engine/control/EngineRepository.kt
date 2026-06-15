package com.clearpath.xray_compose.service.engine.control

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.clearpath.xray_compose.IEngineServiceCallback
import com.clearpath.xray_compose.IEngineServiceControl
import com.clearpath.xray_compose.core.AppState
import com.clearpath.xray_compose.enums.EngineState
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.service.engine.model.TrafficSummary
import com.clearpath.xray_compose.utils.LogUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class EngineRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val configContextBuilder: EngineConfigContextBuilder
) {
    private var engineServiceControl: IEngineServiceControl? = null
    private var connectionDeferred: CompletableDeferred<IEngineServiceControl>? = null

    private var isServiceBound = false

    private val _engineState = MutableStateFlow(EngineState.STOPPED)
    val engineStateFlow = _engineState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastErrorFlow = _lastError.asStateFlow()

    private val _trafficSummary = MutableStateFlow(TrafficSummary())
    val trafficSummaryFlow = _trafficSummary.asStateFlow()

    private val trafficScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var trafficJob: Job? = null

    private val callback = object : IEngineServiceCallback.Stub() {
        override fun onStateChanged(state: Int) {
            val newState = EngineState.fromValue(state)
            updateEngineState(newState)
        }

        override fun onError(message: String?) {
            _lastError.value = message
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val control = IEngineServiceControl.Stub.asInterface(service)
            engineServiceControl = control
            isServiceBound = true
            try {
                control.registerCallback(callback)
                updateEngineState(EngineState.fromValue(control.state))
            } catch (e: Exception) {
                LogUtil.e("EngineRepository: Failed to register callback", e)
                updateEngineState(EngineState.STOPPED)
            }
            connectionDeferred?.complete(control)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cleanupConnection()
        }
    }

    init {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, EngineVpnService::class.java)
        try {
            applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            LogUtil.e("EngineRepository: Failed to bind service in init", e)
        }
    }

    private fun updateEngineState(state: EngineState) {
        _engineState.value = state
        updateAppEngineState(state)
    }

    private fun updateAppEngineState(state: EngineState) {
        AppState.engineStateFlow.value = state
        if (state == EngineState.STARTED) {
            startTrafficMonitor()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val configContextResult =
                        configContextBuilder.buildActiveProfile()
                    val ecContext = configContextResult.ecContext
                    if (!configContextResult.success
                        || ecContext == null
                    ) {
                        return@launch
                    }
                    AppState.proxyPort.value = ecContext.engineConfig.inbound.port
                } catch (e: Exception) {
                    LogUtil.e("EngineRepository: Failed to update app engine state", e)
                }
            }
        } else if (state == EngineState.STOPPED) {
            trafficJob?.cancel()
            trafficJob = null
            _trafficSummary.value = TrafficSummary()
        }
    }

    private fun startTrafficMonitor() {
        if (trafficJob?.isActive == true) return
        val control = engineServiceControl ?: return
        trafficJob = trafficScope.launch {
            while (isActive) {
                try {
                    val summary = control.trafficSummary
                    _trafficSummary.value = summary
                } catch (_: Exception) {
                }
                delay(EngineManager.QUERY_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun cleanupConnection() {
        if (isServiceBound) {
            try {
                context.unbindService(connection)
            } catch (_: Exception) {
            }
            isServiceBound = false
        }
        engineServiceControl = null
        updateEngineState(EngineState.STOPPED)
    }

    private suspend fun ensureConnected(): IEngineServiceControl {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, EngineVpnService::class.java)
        try {
            ContextCompat.startForegroundService(applicationContext, intent)
        } catch (e: Exception) {
            LogUtil.e("EngineRepository: Failed to start foreground service", e)
        }

        engineServiceControl?.let { return it }
        if (connectionDeferred != null && connectionDeferred!!.isActive) {
            return connectionDeferred!!.await()
        }
        connectionDeferred = CompletableDeferred()
        applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        return connectionDeferred!!.await()
    }

    suspend fun startActiveProfileEngine() {
        val configContextResult = configContextBuilder.buildActiveProfile()
        if (!configContextResult.success) {
            _lastError.value = configContextResult.errors.joinToString("; ")
            return
        }
        doStartActiveProfileEngine()
    }

    private suspend fun doStartActiveProfileEngine() {
        val control = ensureConnected()
        try {
            control.start()
            startTrafficMonitor()
        } catch (e: Exception) {
            LogUtil.e("EngineRepository: Failed to start engine", e)
            _lastError.value = e.message
            updateEngineState(EngineState.STOPPED)
        }
    }

    fun stopEngine() {
        connectionDeferred?.cancel()
        connectionDeferred = null
        engineServiceControl?.let {
            try {
                it.stop()
            } catch (_: Exception) {
                cleanupConnection()
            }
        } ?: cleanupConnection()
    }

    fun consumeError() {
        _lastError.value = null
    }

    suspend fun measureHttpDelay(): Long = withContext(Dispatchers.IO) {
        try {
            engineServiceControl?.measureHttpDelay() ?: -1L
        } catch (e: Exception) {
            LogUtil.e("Failed to measure delay", e)
            -1L
        }
    }
}