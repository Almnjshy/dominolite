package com.almnjshy.agon.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

/** Runs on a joining friend's device: one socket to the host. */
class GameClient(private val scope: CoroutineScope) {

    private var connection: GameConnection? = null

    private val _mySeatIndex = MutableStateFlow<Int?>(null)
    val mySeatIndex: StateFlow<Int?> = _mySeatIndex

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    var onMessage: ((GameMessage) -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onRejected: ((String) -> Unit)? = null

    suspend fun connect(hostAddress: String, playerName: String): Boolean {
        val socket = withContext(Dispatchers.IO) {
            runCatching { Socket(hostAddress, AGON_PORT) }.getOrNull()
        } ?: return false

        val conn = GameConnection(socket, scope)
        connection = conn
        conn.send(GameMessage.Hello(playerName))

        scope.launch(Dispatchers.IO) {
            for (message in conn.incoming) {
                when (message) {
                    is GameMessage.Welcome -> {
                        _mySeatIndex.value = message.seatIndex
                        _connected.value = true
                    }
                    is GameMessage.Rejected -> onRejected?.invoke(message.reason)
                    else -> onMessage?.invoke(message)
                }
            }
            _connected.value = false
            onDisconnected?.invoke()
        }
        return true
    }

    suspend fun send(message: GameMessage) {
        connection?.send(message)
    }

    fun disconnect() {
        connection?.close()
    }
}
