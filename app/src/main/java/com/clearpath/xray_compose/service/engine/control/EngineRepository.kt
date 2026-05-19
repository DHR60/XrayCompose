package com.clearpath.xray_compose.service.engine.control

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.clearpath.xray_compose.IEngineServiceCallback
import com.clearpath.xray_compose.IEngineServiceControl
import com.clearpath.xray_compose.enums.EngineState
import com.clearpath.xray_compose.service.engine.context.EngineConfigContext
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.service.engine.model.TrafficSummary
import com.clearpath.xray_compose.utils.LogUtil
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
import kotlin.time.Duration.Companion.milliseconds

val Context.engineRepository: EngineRepository
    get() = EngineRepository.getInstance(this)

class EngineRepository private constructor(private val context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: EngineRepository? = null

        fun getInstance(context: Context): EngineRepository {
            return instance ?: synchronized(this) {
                instance ?: EngineRepository(context).also { instance = it }
            }
        }
    }

    private var engineServiceControl: IEngineServiceControl? = null
    private var connectionDeferred: CompletableDeferred<IEngineServiceControl>? = null

    private var isLastTunMode = false
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
            if (newState == EngineState.STOPPED) {
                cleanupConnection()
            }
        }

        override fun onError(message: String?) {
            _lastError.value = message
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val control = IEngineServiceControl.Stub.asInterface(service)
            if (connectionDeferred == null) {
                try {
                    control.stop()
                    context.unbindService(this)
                } catch (_: Exception) {
                }
                return
            }
            engineServiceControl = control
            isServiceBound = true
            try {
                control.registerCallback(callback)
                updateEngineState(EngineState.fromValue(control.state))
            } catch (e: Exception) {
                updateEngineState(EngineState.STOPPED)
            }
            connectionDeferred?.complete(control)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cleanupConnection()
        }
    }

    private fun updateEngineState(state: EngineState) {
        _engineState.value = state
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
        trafficJob?.cancel()
        _trafficSummary.value = TrafficSummary()
    }

    private suspend fun ensureConnected(ecContext: EngineConfigContext): IEngineServiceControl {
        val applicationContext = context.applicationContext
        val tunMode = !ecContext.engineConfig.inbound.disableTun

        if (engineServiceControl != null && isLastTunMode != tunMode) {
            stopEngine()
            while (engineServiceControl != null) {
                delay(100.milliseconds)
            }
        }

        engineServiceControl?.let { return it }
        if (connectionDeferred != null && connectionDeferred!!.isActive) {
            return connectionDeferred!!.await()
        }
        connectionDeferred = CompletableDeferred()
        isLastTunMode = tunMode
        val intent = if (isLastTunMode) {
            Intent(applicationContext, EngineVpnService::class.java)
        } else {
            Intent(applicationContext, EngineProxyOnlyService::class.java)
        }
        ContextCompat.startForegroundService(applicationContext, intent)
        applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        return connectionDeferred!!.await()
    }

    suspend fun startActiveProfileEngine() {
        val configContextResult = EngineConfigContextBuilder(context).buildActiveProfile()
        if (!configContextResult.success) {
            _lastError.value = configContextResult.errors.joinToString("; ")
            return
        }
        val ecContext = configContextResult.ecContext ?: return
        doStartActiveProfileEngine(ecContext)
    }

    private suspend fun doStartActiveProfileEngine(ecContext: EngineConfigContext) {
        val control = ensureConnected(ecContext)
        try {
            control.start()

            trafficJob?.cancel()
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