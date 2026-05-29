package com.clearpath.xray_compose.service.engine.control

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import com.clearpath.xray_compose.BuildConfig
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.utils.LogUtil
import java.lang.ref.SoftReference

@SuppressLint("VpnServicePolicy")
class EngineVpnService : VpnService(), IEngineServiceControl {
    private lateinit var mInterface: ParcelFileDescriptor
    private var isRunning = false

    private val defaultNetworkRequest by lazy {
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
    }

    private val connectivity by lazy { getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager }

    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // it's a good idea to refresh capabilities
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogUtil.i("StartEngine-VPN: Service created")
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        EngineServiceManager.serviceControl = SoftReference(this)
    }

    override fun onRevoke() {
        LogUtil.w("StartEngine-VPN: Permission revoked")
        stopAllService()
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.i("StartEngine-VPN: Service destroyed")

        // Ensure VPN interface is properly closed when the service is destroyed without
        // going through stopAllService() (e.g. when killed unexpectedly). isRunning is
        // set to false at the start of stopAllService(), so this guard prevents a double-close.
        if (isRunning) {
            try {
                if (::mInterface.isInitialized) {
                    mInterface.close()
                    LogUtil.i("StartEngine-VPN: VPN interface closed in onDestroy")
                }
            } catch (e: Exception) {
                LogUtil.e("StartEngine-VPN: Failed to close interface in onDestroy", e)
            }
        }

        // NotificationManager.cancelNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtil.i("StartEngine-VPN: Service command received")
        // NotificationManager.showNotification(null)
        setupVpnService()
        startService()
        return START_STICKY
        // return super.onStartCommand(intent, flags, startId)
    }

    override fun getService(): Service {
        return this
    }

    override fun startService() {
        if (!::mInterface.isInitialized) {
            LogUtil.e("StartEngine-VPN: Interface not initialized")
            return
        }
        if (!EngineServiceManager.startCoreLoop(mInterface)) {
            LogUtil.e("StartEngine-VPN: Failed to start core loop")
            stopAllService()
            return
        }
    }

    override fun stopService() {
        stopAllService(true)
    }

    override fun vpnProtect(socket: Int): Boolean {
        return protect(socket)
    }

    private fun setupVpnService() {
        val prepare = prepare(this)
        if (prepare != null) {
            LogUtil.e("StartEngine-VPN: Permission not granted")
            stopSelf()
            return
        }

        if (!configureVpnService()) {
            LogUtil.e("StartEngine-VPN: Configuration failed")
            stopSelf()
            return
        }
    }

    private fun configureVpnService(): Boolean {
        val builder = Builder()

        // Configure network settings (addresses, routing and DNS)
        configureNetworkSettings(builder)

        // Configure app-specific settings (session name and per-app proxy)
        configurePerAppProxy(builder)

        // Close the old interface since the parameters have been changed
        try {
            if (::mInterface.isInitialized) {
                mInterface.close()
            }
        } catch (e: Exception) {
            LogUtil.w("Failed to close old interface", e)
        }

        // Configure platform-specific features
        configurePlatformFeatures(builder)

        // Create a new interface using the builder and save the parameters
        try {
            mInterface = builder.establish()!!
            isRunning = true
            return true
        } catch (e: Exception) {
            LogUtil.e("Failed to establish VPN interface", e)
            stopAllService()
        }
        return false
    }

    private fun configureNetworkSettings(builder: Builder) {
        builder.setSession(GlobalConst.appName)
        builder.addAddress("10.0.1.1", 24)
        builder.addRoute("0.0.0.0", 0)
        builder.setMtu(9000)
    }

    private fun configurePlatformFeatures(builder: Builder) {
        // Android P (API 28) and above: Configure network callbacks
        try {
            connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
        } catch (e: Exception) {
            LogUtil.e("StartEngine-VPN: Failed to request network", e)
        }

        builder.setMetered(false)
    }

    private fun configurePerAppProxy(builder: Builder) {
        val selfPackageName = BuildConfig.APPLICATION_ID
        builder.addDisallowedApplication(selfPackageName)
    }

    private fun stopAllService(isForced: Boolean = true) {
        isRunning = false
        try {
            connectivity.unregisterNetworkCallback(defaultNetworkCallback)
        } catch (e: Exception) {
            LogUtil.w("StartEngine-VPN: Failed to unregister callback", e)
        }

        if (isForced) {
            // stopSelf has to be called ahead of mInterface.close(). otherwise v2ray core cannot be stooped
            // It's strage but true.
            // This can be verified by putting stopself() behind and call stopLoop and startLoop
            // in a row for several times. You will find that later created v2ray core report port in use
            // which means the first v2ray core somehow failed to stop and release the port.
            stopSelf()

            // Add a small delay to allow the async core stop operation to complete
            // before closing the VPN interface, preventing a race condition that can
            // leave the VPN icon in the status bar after stopping the service.
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                LogUtil.w("StartEngine-VPN: Sleep interrupted", e)
            }

            try {
                if (::mInterface.isInitialized) {
                    mInterface.close()
                    LogUtil.i("StartEngine-VPN: VPN interface closed")
                }
            } catch (e: Exception) {
                LogUtil.e("StartEngine-VPN: Failed to close interface", e)
            }
        }
    }
}