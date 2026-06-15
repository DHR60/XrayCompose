package com.clearpath.xray_compose.service.engine.control.tester

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.clearpath.xray_compose.IEngineTesterCallback
import com.clearpath.xray_compose.IEngineTesterService
import com.clearpath.xray_compose.utils.LogUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineTesterRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var testerService: IEngineTesterService? = null
    private var connectionDeferred: CompletableDeferred<IEngineTesterService>? = null
    private var isServiceBound = false

    private val _isTesting = MutableStateFlow(false)
    val isTestingFlow = _isTesting.asStateFlow()

    private val _testProgress = MutableStateFlow(TestProgress())
    val testProgressFlow = _testProgress.asStateFlow()

    data class TestProgress(
        val current: Int = 0,
        val total: Int = 0,
        val subId: String? = null
    )

    private val callback = object : IEngineTesterCallback.Stub() {
        override fun onTestStarted(subId: String?) {
            _isTesting.value = true
            _testProgress.value = TestProgress(total = 0, current = 0, subId = subId)
        }

        override fun onTestProgress(profileId: String?, current: Int, total: Int) {
            _testProgress.value = _testProgress.value.copy(current = current, total = total)
        }

        override fun onTestFinished(subId: String?) {
            _isTesting.value = false
            _testProgress.value = TestProgress()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val control = IEngineTesterService.Stub.asInterface(service)
            testerService = control
            isServiceBound = true
            try {
                control.registerCallback(callback)
                _isTesting.value = control.isTesting()
            } catch (e: Exception) {
                LogUtil.e("EngineTesterRepository: Failed to register callback", e)
            }
            connectionDeferred?.complete(control)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cleanupConnection()
        }
    }

    init {
        bindTesterService()
    }

    private fun bindTesterService() {
        val applicationContext = context.applicationContext
        val intent = Intent(applicationContext, EngineTesterService::class.java)
        try {
            applicationContext.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            LogUtil.e("EngineTesterRepository: Failed to bind service", e)
        }
    }

    private fun cleanupConnection() {
        testerService = null
        isServiceBound = false
        _isTesting.value = false
        connectionDeferred = null
    }

    private suspend fun ensureConnected(): IEngineTesterService {
        testerService?.let { return it }
        if (connectionDeferred != null && connectionDeferred!!.isActive) {
            return connectionDeferred!!.await()
        }
        connectionDeferred = CompletableDeferred()
        bindTesterService()
        return connectionDeferred!!.await()
    }

    suspend fun startTest(subId: String? = null) {
        try {
            val service = ensureConnected()
            service.startTest(subId)
        } catch (e: Exception) {
            LogUtil.e("EngineTesterRepository: Failed to start test", e)
        }
    }

    suspend fun startTestProfiles(profileIds: List<String>) {
        try {
            val service = ensureConnected()
            service.startTestProfiles(profileIds)
        } catch (e: Exception) {
            LogUtil.e("EngineTesterRepository: Failed to start profiles test", e)
        }
    }

    suspend fun stopTest() {
        try {
            testerService?.stopTest()
        } catch (e: Exception) {
            LogUtil.e("EngineTesterRepository: Failed to stop test", e)
        }
    }
}
