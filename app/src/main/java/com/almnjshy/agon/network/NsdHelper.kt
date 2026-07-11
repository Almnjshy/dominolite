package com.almnjshy.agon.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val SERVICE_TYPE = "_agon._tcp."
private const val SERVICE_NAME = "AgonGame"

/**
 * Advertises (host) or discovers (client) the game over the local network via mDNS, so
 * once everyone is on the same hotspot/WiFi-Direct network, clients find the host's IP
 * automatically instead of anyone typing addresses.
 */
class NsdHelper(private val context: Context) {

    private val nsdManager: NsdManager? =
        context.applicationContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private val _discoveredHost = MutableStateFlow<String?>(null)
    val discoveredHost: StateFlow<String?> = _discoveredHost

    fun registerService(port: Int = AGON_PORT) {
        val manager = nsdManager ?: return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun discoverService() {
        val manager = nsdManager ?: return
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceName.contains(SERVICE_NAME)) {
                    // Use resolveService with NsdManager.ResolveListener (modern API)
                    val resListener = object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            // Use hostAddresses (API 34+) with fallback to deprecated host
                            val address = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                info.hostAddresses.firstOrNull()?.hostAddress
                            } else {
                                @Suppress("DEPRECATION")
                                info.host?.hostAddress
                            }
                            _discoveredHost.value = address
                        }
                    }
                    resolveListener = resListener
                    manager.resolveService(service, resListener)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredHost.value = null
            }
        }
        discoveryListener = listener
        manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        val manager = nsdManager ?: return
        registrationListener?.let { runCatching { manager.unregisterService(it) } }
        discoveryListener?.let { runCatching { manager.stopServiceDiscovery(it) } }
        resolveListener = null
        registrationListener = null
        discoveryListener = null
    }
}
