package com.clearpath.xray_compose.service.engine.control

import android.annotation.SuppressLint
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.clearpath.xray_compose.BuildConfig
import com.clearpath.xray_compose.GlobalConst
import com.clearpath.xray_compose.utils.LogUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("VpnServicePolicy")
class EngineVpnService : VpnService(), IEngineService {
    private lateinit var mInterface: ParcelFileDescriptor
    private var isRunning = false

    @Inject
    lateinit var engineManager: EngineManager

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
        engineManager.attach(this)
        engineManager.initialize()
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        return engineManager.getBinder()
    }

    override fun onRevoke() {
        LogUtil.w("StartEngine-VPN: Permission revoked")
        stopService()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::mInterface.isInitialized) try {
            mInterface.close()
        } catch (_: Exception) {
        }
        try {
            connectivity.unregisterNetworkCallback(defaultNetworkCallback)
        } catch (_: Exception) {
        }
        engineManager.cancelForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        engineManager.showForegroundNotification()
        return START_STICKY
    }

    override fun getService() = this

    override fun startService() {
        if (engineManager.isVpnMode()) {
            setupVpnService()
        }
        if (!engineManager.startActiveProfile()) stopService()
    }

    override fun stopService() {
        isRunning = false
        engineManager.stopCoreLoop()
        stopSelf()
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
            val pfd = builder.establish()!!
            mInterface = pfd
            engineManager.vpnInterface = pfd
            isRunning = true
            return true
        } catch (e: Exception) {
            LogUtil.e("Failed to establish VPN interface", e)
            stopService()
        }
        return false
    }

    private fun configureNetworkSettings(builder: Builder) {
        builder.setSession(GlobalConst.appName)
        builder.addAddress("10.0.1.1", 24)
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("1.1.1.1")
        builder.addDnsServer("8.8.8.8")
        builder.setMtu(1500)
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
}