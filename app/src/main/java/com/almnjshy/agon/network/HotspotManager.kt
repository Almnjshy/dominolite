package com.almnjshy.agon.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HotspotCredentials(val ssid: String, val password: String)

/**
 * Host-side "Hotspot" mode: spins up a temporary, app-scoped WiFi network via Android's
 * LocalOnlyHotspot API (no internet, no router, no root — this is the same mechanism
 * apps like file-sharing tools use). Friends join it manually from their phone's WiFi
 * settings using the shown SSID/password (Android doesn't allow one app to silently join
 * another device to a network — that step needs the user), and once they're on it, the
 * app finds the host automatically via NSD (see [NsdHelper]) instead of asking anyone to
 * type an IP address.
 */
class HotspotManager(private val context: Context) {

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    private val _credentials = MutableStateFlow<HotspotCredentials?>(null)
    val credentials: StateFlow<HotspotCredentials?> = _credentials

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun start() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            _error.value = "WiFi غير متاح على هذا الجهاز"
            return
        }
        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                reservation = res
                val config = res.wifiConfiguration
                if (config != null) {
                    _credentials.value = HotspotCredentials(config.SSID.trim('"'), config.preSharedKey ?: "")
                } else {
                    // Android 11+ path
                    val softApConfig = res.softApConfiguration
                    _credentials.value = HotspotCredentials(
                        softApConfig?.ssid ?: "Agon",
                        softApConfig?.passphrase ?: ""
                    )
                }
            }

            override fun onStopped() {
                _credentials.value = null
            }

            override fun onFailed(reason: Int) {
                _error.value = "تعذر إنشاء نقطة الاتصال (كود $reason)"
            }
        }, null)
    }

    fun stop() {
        runCatching { reservation?.close() }
        reservation = null
        _credentials.value = null
    }
}
