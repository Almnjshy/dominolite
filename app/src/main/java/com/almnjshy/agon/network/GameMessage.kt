package com.almnjshy.agon.network

import com.almnjshy.agon.engine.ChainSide
import com.almnjshy.agon.engine.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Every message the host and clients exchange over the socket, one JSON object per line
 * (newline-delimited — simplest framing that works fine at this message size/rate).
 * The host is authoritative: it's the only side that ever runs [com.almnjshy.agon.engine.GameEngine];
 * clients only ever send *requests* and render whatever [StateSync] they're given.
 */
@Serializable
sealed class GameMessage {

    /** Client -> host, right after the socket connects. */
    @Serializable
    data class Hello(val playerName: String) : GameMessage()

    /** Host -> that client, confirming which seat they were assigned. */
    @Serializable
    data class Welcome(val seatIndex: Int, val playerCount: Int) : GameMessage()

    /** Host -> all clients, the lobby's current player list (names + which seats are
     *  still open), sent whenever someone joins or leaves so lobby UIs stay in sync. */
    @Serializable
    data class LobbyUpdate(val playerNames: List<String>, val playerCount: Int) : GameMessage()

    /** Host -> all clients: authoritative game state after every transition. */
    @Serializable
    data class StateSync(val state: GameState) : GameMessage()

    /** Client -> host: "I'd like to play this tile." Host validates before applying. */
    @Serializable
    data class MoveRequest(val tileId: Int, val side: ChainSide) : GameMessage()

    /** Either direction: keep-alive / connection check. */
    @Serializable
    data object Ping : GameMessage()

    /** Host -> a client: something about their last request was invalid. */
    @Serializable
    data class Rejected(val reason: String) : GameMessage()

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            classDiscriminator = "type"
        }

        fun encode(message: GameMessage): String = json.encodeToString(serializer(), message)

        fun decode(line: String): GameMessage? =
            runCatching { json.decodeFromString(serializer(), line) }.getOrNull()
    }
}
