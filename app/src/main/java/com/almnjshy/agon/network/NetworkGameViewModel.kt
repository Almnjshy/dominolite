package com.almnjshy.agon.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almnjshy.agon.ai.AIEngine
import com.almnjshy.agon.engine.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Drives a networked match. On the host device it owns the authoritative [GameEngine]
 * state and a [GameServer]; on a joining friend's device it owns a [GameClient] and just
 * renders whatever [GameMessage.StateSync] the host last sent — a client never runs the
 * engine itself, which is what keeps every device in perfect agreement.
 */
class NetworkGameViewModel : ViewModel(), GameController {

    var isHost: Boolean = false
        private set

    override var localSeatIndex: Int = 0
        private set

    private var totalPlayerCount: Int = 2
    private var localPlayerName: String = "You"
    private var aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM

    private var server: GameServer? = null
    private var client: GameClient? = null

    private val _state = MutableStateFlow<GameState?>(null)
    override val state: StateFlow<GameState?> = _state

    private val _selectedTile = MutableStateFlow<DominoTile?>(null)
    override val selectedTile: StateFlow<DominoTile?> = _selectedTile

    /** Host-only: names of friends connected so far, for the lobby screen. */
    private val _connectedPlayerNames = MutableStateFlow<List<String>>(emptyList())
    val connectedPlayerNames: StateFlow<List<String>> = _connectedPlayerNames

    /** Client-only: true once the host accepts us and assigns a seat. */
    private val _clientReady = MutableStateFlow(false)
    val clientReady: StateFlow<Boolean> = _clientReady

    private val _clientError = MutableStateFlow<String?>(null)
    val clientError: StateFlow<String?> = _clientError

    // ---------------------------------------------------------------- Host

    fun startHosting(playerCount: Int, hostPlayerName: String, difficulty: AIDifficulty) {
        if (server != null) return // already hosting — ignore duplicate calls
        isHost = true
        localSeatIndex = 0
        totalPlayerCount = playerCount
        localPlayerName = hostPlayerName
        aiDifficulty = difficulty

        val srv = GameServer(viewModelScope, maxClients = playerCount - 1)
        srv.onMessage = { seatIndex, message -> handleHostMessage(seatIndex, message) }
        server = srv
        srv.start()

        viewModelScope.launch {
            srv.connectedPlayers.collect { names -> _connectedPlayerNames.value = names }
        }
    }

    fun hostStartNewRound() {
        if (!isHost) return
        val srv = server ?: return

        viewModelScope.launch {
            val connected = srv.snapshotClientNames()

            val names = MutableList(totalPlayerCount) { "" }
            val kinds = MutableList(totalPlayerCount) { PlayerKind.AI }
            names[0] = localPlayerName
            kinds[0] = PlayerKind.LOCAL_HUMAN

            for (seat in 1 until totalPlayerCount) {
                val friendName = connected[seat]
                if (friendName != null) {
                    names[seat] = friendName
                    kinds[seat] = PlayerKind.REMOTE_HUMAN
                } else {
                    names[seat] = "AI $seat"
                    kinds[seat] = PlayerKind.AI
                }
            }

            val newState = GameEngine.newRound(totalPlayerCount, names, kinds, List(totalPlayerCount) { aiDifficulty })
            _state.value = newState
            _selectedTile.value = null
            broadcastState()
            resolveTurnAsHost()
        }
    }

    private fun handleHostMessage(seatIndex: Int, message: GameMessage) {
        if (message !is GameMessage.MoveRequest) return
        val s = _state.value ?: return
        if (s.roundOver || s.currentPlayerIndex != seatIndex) return
        val tile = s.players[seatIndex].hand.find { it.id == message.tileId } ?: return
        if (!GameEngine.isValidPlay(s, tile, message.side)) return

        _state.value = GameEngine.playTile(s, tile, message.side)
        broadcastState()
        resolveTurnAsHost()
    }

    private fun broadcastState() {
        val s = _state.value ?: return
        val srv = server ?: return
        viewModelScope.launch { srv.broadcast(GameMessage.StateSync(s)) }
    }

    /** Host-side turn gatekeeper — mirrors [GameViewModel]'s resolveTurn, but AI moves
     *  AND remote-human moves both end with a broadcast to every connected client. */
    private fun resolveTurnAsHost() {
        val s = _state.value ?: return
        if (s.roundOver) return

        if (!GameEngine.hasAnyValidMove(s, s.currentPlayerIndex)) {
            viewModelScope.launch {
                if (s.currentPlayer.kind == PlayerKind.AI) delay(300)
                val current = _state.value ?: return@launch
                if (current.roundOver) return@launch
                _state.value = if (current.boneyard.isNotEmpty()) {
                    GameEngine.drawTile(current)
                } else {
                    GameEngine.pass(current)
                }
                broadcastState()
                resolveTurnAsHost()
            }
            return
        }

        if (s.currentPlayer.kind == PlayerKind.AI) {
            viewModelScope.launch {
                delay(650)
                val current = _state.value ?: return@launch
                if (current.roundOver || current.currentPlayer.kind != PlayerKind.AI) return@launch
                val move = AIEngine.chooseMove(current, current.currentPlayerIndex)
                _state.value = if (move != null) {
                    GameEngine.playTile(current, move.tile, move.side)
                } else {
                    GameEngine.pass(current)
                }
                broadcastState()
                resolveTurnAsHost()
            }
        }
        // else: a local or remote human is up — wait for their input/network message.
    }

    // -------------------------------------------------------------- Client

    fun joinGame(hostAddress: String, playerName: String) {
        if (client != null) return // already connecting/connected — ignore duplicate calls
        isHost = false
        localPlayerName = playerName

        val cl = GameClient(viewModelScope)
        cl.onMessage = { message -> handleClientMessage(message) }
        cl.onRejected = { reason -> _clientError.value = reason }
        cl.onDisconnected = { _clientError.value = "انقطع الاتصال بالمضيف" }
        client = cl

        viewModelScope.launch {
            val ok = cl.connect(hostAddress, playerName)
            if (!ok) _clientError.value = "تعذر الاتصال بالمضيف"
        }
        viewModelScope.launch {
            cl.mySeatIndex.collect { seat ->
                if (seat != null) {
                    localSeatIndex = seat
                    _clientReady.value = true
                }
            }
        }
    }

    private fun handleClientMessage(message: GameMessage) {
        if (message is GameMessage.StateSync) {
            _state.value = message.state
            _selectedTile.value = null
        }
    }

    // ------------------------------------------------------ Shared control

    override fun selectTile(tile: DominoTile) {
        val s = _state.value ?: return
        if (s.currentPlayerIndex != localSeatIndex) return
        _selectedTile.value = if (_selectedTile.value?.id == tile.id) null else tile
    }

    override fun playSelectedTile(side: ChainSide) {
        val s = _state.value ?: return
        val tile = _selectedTile.value ?: return
        if (s.currentPlayerIndex != localSeatIndex) return
        if (!GameEngine.isValidPlay(s, tile, side)) return

        if (isHost) {
            _state.value = GameEngine.playTile(s, tile, side)
            _selectedTile.value = null
            broadcastState()
            resolveTurnAsHost()
        } else {
            _selectedTile.value = null
            viewModelScope.launch { client?.send(GameMessage.MoveRequest(tile.id, side)) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        server?.stop()
        client?.disconnect()
    }

    /** Tears down any active hosting/connection so the same ViewModel instance can be
     *  reused for a brand-new lobby session (e.g. after returning to the main menu). */
    fun reset() {
        server?.stop()
        client?.disconnect()
        server = null
        client = null
        isHost = false
        localSeatIndex = 0
        _state.value = null
        _selectedTile.value = null
        _connectedPlayerNames.value = emptyList()
        _clientReady.value = false
        _clientError.value = null
    }
}
