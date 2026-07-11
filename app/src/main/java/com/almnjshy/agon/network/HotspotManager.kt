package com.almnjshy.agon.network

import android.Manifest
import android.annotation.SuppressLint
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

class HotspotManager(private val context: Context) {

    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    private val _credentials = MutableStateFlow<HotspotCredentials?>(null)
    val credentials: StateFlow<HotspotCredentials?> = _credentials

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

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

    @SuppressLint("MissingPermission")
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

        try {
            val handler = Handler(Looper.getMainLooper())
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = res
                    // API 30+ uses softApConfiguration, older uses wifiConfiguration
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val softApConfig = res.softApConfiguration
                        _credentials.value = HotspotCredentials(
                            softApConfig?.ssid?.toString() ?: "Agon",
                            softApConfig?.passphrase ?: ""
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        val config = res.wifiConfiguration
                        _credentials.value = HotspotCredentials(
                            config?.SSID?.trim('"') ?: "Agon",
                            config?.preSharedKey ?: ""
                        )
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
        } catch (e: SecurityException) {
            _error.value = "تم رفض الإذن: ${e.message}"
        }
    }

    fun stop() {
        runCatching { reservation?.close() }
        reservation = null
        _credentials.value = null
    }
}
