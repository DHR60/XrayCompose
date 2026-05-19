package com.clearpath.xray_compose.service.engine.control.tester

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import com.clearpath.xray_compose.IEngineTesterCallback
import com.clearpath.xray_compose.IEngineTesterService
import com.clearpath.xray_compose.data.ProfileModel
import com.clearpath.xray_compose.data.db.entities.ProfileTestItem
import com.clearpath.xray_compose.data.repo.preferencesRepository
import com.clearpath.xray_compose.data.repo.profileRepository
import com.clearpath.xray_compose.service.engine.config.XrayConfigService
import com.clearpath.xray_compose.service.engine.connect.EngineNativeManager
import com.clearpath.xray_compose.service.engine.context.EngineConfigContextBuilder
import com.clearpath.xray_compose.utils.LogUtil
import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class EngineTesterService : Service() {
    companion object {
        private const val CONCURRENCY = 5
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var testJob: Job? = null
    private var isCurrentlyTesting = false

    private val callbacks = RemoteCallbackList<IEngineTesterCallback>()

    private val binder = object : IEngineTesterService.Stub() {
        override fun startTest(subId: String?) {
            LogUtil.i("EngineTesterService: Received startTest command for subId: $subId")
            val targetSubId = subId ?: run {
                // Fallback to activeSubId if subId is null
                serviceScope.launch {
                    val activeId = preferencesRepository.getActiveSubId()
                    if (activeId != null) {
                        doStartSubTest(activeId)
                    } else {
                        LogUtil.w("EngineTesterService: No subId provided and no activeSubId found")
                    }
                }
                return
            }
            doStartSubTest(targetSubId)
        }

        override fun startTestProfiles(profileIds: List<String>) {
            LogUtil.i("EngineTesterService: Received startTestProfiles command for ${profileIds.size} profiles")
            doStartProfilesTest(profileIds)
        }

        override fun stopTest() {
            LogUtil.i("EngineTesterService: Received stopTest command")
            doStopTest()
        }

        override fun isTesting(): Boolean {
            return isCurrentlyTesting
        }

        override fun registerCallback(callback: IEngineTesterCallback?) {
            callback?.let { callbacks.register(it) }
        }

        override fun unregisterCallback(callback: IEngineTesterCallback?) {
            callback?.let { callbacks.unregister(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun doStartSubTest(subId: String) {
        if (isCurrentlyTesting) {
            LogUtil.w("EngineTesterService: Test already in progress")
            return
        }

        testJob = serviceScope.launch {
            try {
                val profiles =
                    this@EngineTesterService.profileRepository.getAllProfilesBySubid(subId)
                runTest(profiles, subId)
            } catch (e: Exception) {
                LogUtil.e("EngineTesterService: Error loading sub profiles", e)
            }
        }
    }

    private fun doStartProfilesTest(profileIds: List<String>) {
        if (isCurrentlyTesting) {
            LogUtil.w("EngineTesterService: Test already in progress")
            return
        }

        testJob = serviceScope.launch {
            try {
                val profiles = profileIds.mapNotNull { id ->
                    profileRepository.getProfileById(id)
                }
                runTest(profiles, "custom_list")
            } catch (e: Exception) {
                LogUtil.e("EngineTesterService: Error loading profiles by ID", e)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun runTest(profiles: List<ProfileModel>, tag: String) {
        isCurrentlyTesting = true
        notifyTestStarted(tag)

        try {
            val total = profiles.size
            LogUtil.i("EngineTesterService: Starting concurrent test (limit: $CONCURRENCY) for $total profiles (tag: $tag)")

            notifyTestProgress(null, 0, total)

            val completedCount = AtomicInteger(0)

            profiles.asFlow()
                .flatMapMerge(concurrency = CONCURRENCY) { profile ->
                    flow {
                        LogUtil.d("EngineTesterService: Testing profile ${profile.remark} (${profile.id})")
                        doRealPing(profile)
                        emit(profile.id)
                    }
                }
                .collect { profileId ->
                    val current = completedCount.incrementAndGet()
                    notifyTestProgress(profileId, current, total)
                }

        } catch (e: Exception) {
            LogUtil.e("EngineTesterService: Error during test execution", e)
        } finally {
            isCurrentlyTesting = false
            notifyTestFinished(tag)
            stopSelf()
        }
    }

    private fun doStopTest() {
        testJob?.cancel()
        testJob = null
        isCurrentlyTesting = false
        // You might want to notify callbacks about cancellation here
    }

    private fun notifyTestStarted(subId: String) {
        val count = callbacks.beginBroadcast()
        for (i in 0 until count) {
            try {
                callbacks.getBroadcastItem(i).onTestStarted(subId)
            } catch (e: Exception) {
                LogUtil.e("EngineTesterService: Failed to notify onTestStarted", e)
            }
        }
        callbacks.finishBroadcast()
    }

    private fun notifyTestProgress(profileId: String?, current: Int, total: Int) {
        val count = callbacks.beginBroadcast()
        for (i in 0 until count) {
            try {
                callbacks.getBroadcastItem(i).onTestProgress(profileId, current, total)
            } catch (e: Exception) {
                LogUtil.e("EngineTesterService: Failed to notify onTestProgress", e)
            }
        }
        callbacks.finishBroadcast()
    }

    private fun notifyTestFinished(subId: String) {
        val count = callbacks.beginBroadcast()
        for (i in 0 until count) {
            try {
                callbacks.getBroadcastItem(i).onTestFinished(subId)
            } catch (e: Exception) {
                LogUtil.e("EngineTesterService: Failed to notify onTestFinished", e)
            }
        }
        callbacks.finishBroadcast()
    }

    override fun onDestroy() {
        super.onDestroy()
        doStopTest()
        callbacks.kill()
    }

    private suspend fun doRealPing(profile: ProfileModel) {
        var realPingDelay = -1L
        var errorMessage = ""
        val ecContextResult = EngineConfigContextBuilder(this).buildByProfile(profile)
        if (!ecContextResult.success
            || ecContextResult.ecContext == null
        ) {
            errorMessage = ecContextResult.errors.joinToString("; ")
        } else {
            val ecContext = ecContextResult.ecContext
            val configService = XrayConfigService(ecContext)
            val configContent = configService.buildBaseConfig()
            try {
                realPingDelay = EngineNativeManager.measureOutboundDelay(
                    configContent,
                    "https://www.google.com/generate_204"
                )
                if (realPingDelay < 0) {
                    errorMessage = "timeout"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "unknown error"
            }
        }
        val profileTestItem =
            this@EngineTesterService.profileRepository.getProfileTestById(profile.id)
        if (profileTestItem != null) {
            this@EngineTesterService.profileRepository.updateProfileTest(
                profileTestItem.copy(
                    delay = realPingDelay.toInt(),
                    message = errorMessage
                )
            )
        } else {
            this@EngineTesterService.profileRepository.insertProfileTest(
                ProfileTestItem(
                    id = UuidCreator.fromString(profile.id),
                    delay = realPingDelay.toInt(),
                    message = errorMessage
                )
            )
        }
    }
}
