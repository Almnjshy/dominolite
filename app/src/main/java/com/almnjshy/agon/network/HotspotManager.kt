package com.almnjshy.agon.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
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

    /**
     * Checks whether the app has the required permission to start a local-only hotspot.
     */
    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun start() {
        if (!hasPermission()) {
            _error.value = "يحتاج التطبيق إلى إذن الموقع / الأجهزة القريبة"
            return
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            _error.value = "WiFi غير متاح على هذا الجهاز"
            return
        }

        // Use a Handler tied to the main looper for the callback
        val handler = Handler(Looper.getMainLooper())

        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                reservation = res
                // Use softApConfiguration (modern API) instead of deprecated wifiConfiguration
                val softApConfig = res.softApConfiguration
                if (softApConfig != null) {
                    _credentials.value = HotspotCredentials(
                        softApConfig.ssid?.toString() ?: "Agon",
                        softApConfig.passphrase ?: ""
                    )
                } else {
                    // Fallback for older devices (Android 10 and below)
                    @Suppress("DEPRECATION")
                    val config = res.wifiConfiguration
                    if (config != null) {
                        _credentials.value = HotspotCredentials(
                            config.SSID?.trim('"') ?: "Agon",
                            config.preSharedKey ?: ""
                        )
                    } else {
                        _credentials.value = HotspotCredentials("Agon", "")
                    }
                }
            }

            override fun onStopped() {
                reservation = null
                _credentials.value = null
            }

            override fun onFailed(reason: Int) {
                _error.value = "تعذر إنشاء نقطة الاتصال (كود $reason)"
            }
        }, handler)
    }

    fun stop() {
        runCatching { reservation?.close() }
        reservation = null
        _credentials.value = null
    }
}
