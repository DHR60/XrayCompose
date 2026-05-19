package com.clearpath.xray_compose.service.engine.connect

import android.content.Context
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.utils.LogUtil
import com.clearpath.xray_compose.utils.Utils
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object EngineNativeManager {
    private val initialized = AtomicBoolean(false)

    /**
     * Initialize V2Ray core environment.
     * This method is thread-safe and ensures initialization happens only once.
     * Subsequent calls will be ignored silently.
     *
     */
    fun initCoreEnv(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            try {
                Seq.setContext(context.applicationContext)
                val assetPath = Utils.userAssetPath(context)
                val deviceId = Utils.getDeviceIdForXUDPBaseKey()
                Libv2ray.initCoreEnv(assetPath, deviceId)
                LogUtil.i("V2Ray core environment initialized successfully")
            } catch (e: Exception) {
                LogUtil.e("Failed to initialize V2Ray core environment", e)
                initialized.set(false)
                throw e
            }
            try {
                LogUtil.i("Copying V2Ray core assets to external storage")
                val targetDir = File(context.getExternalFilesDir(null), "assets")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val geoFileList = listOf(GlobalConst.geoipDat, GlobalConst.geositeDat)
                for (fileName in geoFileList) {
                    val targetFile = File(targetDir, fileName)
                    if (!targetFile.exists()) {
                        context.assets.open(fileName).use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        LogUtil.i("Copied asset $fileName to ${targetFile.absolutePath}")
                    }
                }
                LogUtil.i("V2Ray core assets copied successfully")
            } catch (e: Exception) {
                LogUtil.e("Failed to copy V2Ray core assets", e)
            }
        } else {
            LogUtil.d("V2Ray core environment already initialized, skipping")
        }
    }

    fun reconcileBrowserDialer(dialerAddr: String) {
        try {
            Libv2ray.reconcileBrowserDialer(dialerAddr)
            LogUtil.i(
                "Browser dialer reconciled successfully with address: $dialerAddr"
            )
        } catch (e: Exception) {
            LogUtil.e(
                "Failed to reconcile browser dialer with address: $dialerAddr",
                e
            )
        }
    }


    /**
     * Get V2Ray core version.
     *
     * @return Version string of the V2Ray core
     */
    fun getLibVersion(): String {
        return try {
            Libv2ray.checkVersionX()
        } catch (e: Exception) {
            LogUtil.e("Failed to check V2Ray version", e)
            "Unknown"
        }
    }

    /**
     * Measure outbound connection delay.
     *
     * @param config The configuration JSON string
     * @param testUrl The URL to test against
     * @return Delay in milliseconds, or -1 if test failed
     */
    fun measureOutboundDelay(config: String, testUrl: String): Long {
        return Libv2ray.measureOutboundDelay(config, testUrl)
    }

    fun measureOutboundDelayOrTimeout(config: String, testUrl: String): Long {
        return try {
            measureOutboundDelay(config, testUrl)
        } catch (e: Exception) {
            LogUtil.e("Failed to measure outbound delay", e)
            -1L
        }
    }

    /**
     * Create a new core controller instance.
     *
     * @param handler The callback handler for core events
     * @return A new CoreController instance
     */
    fun newCoreController(handler: CoreCallbackHandler): CoreController {
        return try {
            Libv2ray.newCoreController(handler)
        } catch (e: Exception) {
            LogUtil.e("Failed to create core controller", e)
            throw e
        }
    }
}