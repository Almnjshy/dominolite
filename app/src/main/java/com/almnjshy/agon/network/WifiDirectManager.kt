package com.almnjshy.agon.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper around Android's WifiP2pManager: discover nearby devices, connect to one,
 * and resolve the group owner's IP once a group forms. WiFi Direct doesn't need a router
 * or internet connection — it's the most reliable of the two local-play options this app
 * offers. WiFi Direct's own "group owner" negotiation is an implementation detail and is
 * independent of the app-level host/client roles chosen in the lobby.
 *
 * Every method here that talks to WifiP2pManager needs ACCESS_FINE_LOCATION granted at
 * runtime (plus NEARBY_WIFI_DEVICES on Android 13+) — the caller (the lobby screen) is
 * responsible for requesting that before calling [startDiscovery] or [connectTo].
 */
class WifiDirectManager(private val context: Context) {

    private val manager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: Channel? = null
    private var receiver: BroadcastReceiver? = null

    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers: StateFlow<List<WifiP2pDevice>> = _peers

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    fun start() {
        val mgr = manager ?: return
        channel = mgr.initialize(context, context.mainLooper, null)

        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }

        val br = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        _isEnabled.value = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> requestPeers()
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> requestConnectionInfo()
                }
            }
        }
        receiver = br
        ContextCompat.registerReceiver(context, br, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        manager?.removeGroup(channel, null)
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.discoverPeers(ch, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { Log.d("WifiDirect", "discovery started") }
            override fun onFailure(reason: Int) { Log.w("WifiDirect", "discovery failed: $reason") }
        })
    }

    @SuppressLint("MissingPermission")
    private fun requestPeers() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.requestPeers(ch) { peerList -> _peers.value = peerList.deviceList.toList() }
    }

    private fun requestConnectionInfo() {
        val mgr = manager ?: return
        val ch = channel ?: return
        mgr.requestConnectionInfo(ch) { info -> _connectionInfo.value = info }
    }

    @SuppressLint("MissingPermission")
    fun connectTo(device: WifiP2pDevice, onResult: (Boolean) -> Unit) {
        val mgr = manager ?: return onResult(false)
        val ch = channel ?: return onResult(false)
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        mgr.connect(ch, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() = onResult(true)
            override fun onFailure(reason: Int) = onResult(false)
        })
    }

    fun disconnect() {
        manager?.removeGroup(channel, null)
    }
}
