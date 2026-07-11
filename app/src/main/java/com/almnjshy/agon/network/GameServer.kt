package com.almnjshy.agon.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.ServerSocket

const val AGON_PORT = 8765

data class ConnectedClient(val seatIndex: Int, val name: String, val connection: GameConnection)

/**
 * Runs on the host device. Accepts incoming connections (one per friend), assigns each
 * one a seat index (1, 2, 3 — the host itself is always seat 0), and gives the caller a
 * simple callback for inbound messages instead of exposing raw sockets everywhere else.
 *
 * `clients` is mutated from a separate coroutine per accepted connection (so two friends
 * connecting at nearly the same moment really do race on separate IO-dispatcher threads),
 * so every read/write of it goes through [mutex] rather than trusting plain MutableMap.
 */
class GameServer(private val scope: CoroutineScope, private val maxClients: Int) {

    private var serverSocket: ServerSocket? = null
    private val clients = mutableMapOf<Int, ConnectedClient>()
    private val mutex = Mutex()

    private val _connectedPlayers = MutableStateFlow<List<String>>(emptyList())
    val connectedPlayers: StateFlow<List<String>> = _connectedPlayers

    var onMessage: ((seatIndex: Int, message: GameMessage) -> Unit)? = null
    var onClientDisconnected: ((seatIndex: Int) -> Unit)? = null

    fun start() {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val server = ServerSocket(AGON_PORT)
                serverSocket = server
                while (true) {
                    val socket = server.accept()
                    val conn = GameConnection(socket, scope)
                    scope.launch(Dispatchers.IO) {
                        val hello = conn.incoming.receiveCatching().getOrNull()
                        if (hello !is GameMessage.Hello) {
                            conn.close()
                            return@launch
                        }
                        val seatIndex = mutex.withLock {
                            val free = nextFreeSeatLocked()
                            if (free != null) clients[free] = ConnectedClient(free, hello.playerName, conn)
                            free
                        }
                        if (seatIndex == null) {
                            conn.send(GameMessage.Rejected("Game is full"))
                            conn.close()
                            return@launch
                        }
                        conn.send(GameMessage.Welcome(seatIndex, maxClients + 1))
                        broadcastLobby()

                        for (message in conn.incoming) {
                            onMessage?.invoke(seatIndex, message)
                        }
                        mutex.withLock { clients.remove(seatIndex) }
                        broadcastLobby()
                        onClientDisconnected?.invoke(seatIndex)
                    }
                }
            }
        }
    }

    /** Must only be called while holding [mutex]. */
    private fun nextFreeSeatLocked(): Int? {
        for (i in 1..maxClients) if (!clients.containsKey(i)) return i
        return null
    }

    private suspend fun broadcastLobby() {
        val names = mutex.withLock { clients.values.sortedBy { it.seatIndex }.map { it.name } }
        _connectedPlayers.value = names
    }

    suspend fun broadcast(message: GameMessage) {
        val targets = mutex.withLock { clients.values.toList() }
        targets.forEach { it.connection.send(message) }
    }

    suspend fun sendTo(seatIndex: Int, message: GameMessage) {
        val target = mutex.withLock { clients[seatIndex] }
        target?.connection?.send(message)
    }

    suspend fun playerNameFor(seatIndex: Int): String? = mutex.withLock { clients[seatIndex]?.name }

    /** One-shot snapshot of seatIndex -> player name for every connected client. */
    suspend fun snapshotClientNames(): Map<Int, String> =
        mutex.withLock { clients.mapValues { it.value.name } }

    fun stop() {
        runCatching { serverSocket?.close() }
        clients.values.forEach { it.connection.close() }
        clients.clear()
    }
}
