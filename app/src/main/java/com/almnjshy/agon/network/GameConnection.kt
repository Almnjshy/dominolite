package com.almnjshy.agon.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

/**
 * Wraps one connected [Socket] as a channel of decoded [GameMessage]s in, and a
 * suspend `send` out. One instance per peer connection — the host holds one per
 * connected client, a client holds exactly one (to the host).
 */
class GameConnection(private val socket: Socket, private val scope: CoroutineScope) {

    private val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream()), true)
    private val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private val inbox = Channel<GameMessage>(capacity = Channel.UNLIMITED)

    val incoming: ReceiveChannel<GameMessage> get() = inbox
    val remoteAddress: String get() = socket.inetAddress?.hostAddress ?: "unknown"
    val isOpen: Boolean get() = socket.isConnected && !socket.isClosed

    init {
        scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    GameMessage.decode(line)?.let { inbox.trySend(it) }
                }
            } catch (_: Exception) {
                // socket closed / network dropped — fall through to cleanup below
            } finally {
                inbox.close()
                close()
            }
        }
    }

    suspend fun send(message: GameMessage) = withContext(Dispatchers.IO) {
        runCatching { writer.println(GameMessage.encode(message)) }
    }

    fun close() {
        runCatching { socket.close() }
    }
}
