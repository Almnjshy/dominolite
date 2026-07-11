package com.almnjshy.agon.engine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almnjshy.agon.ai.AIEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Local, single-device hotseat game: one local human plus AI opponents. */
class GameViewModel : ViewModel(), GameController {

    private val _state = MutableStateFlow<GameState?>(null)
    override val state: StateFlow<GameState?> = _state

    private val _selectedTile = MutableStateFlow<DominoTile?>(null)
    override val selectedTile: StateFlow<DominoTile?> = _selectedTile

    override val localSeatIndex: Int = 0

    fun startNewRound(playerCount: Int, difficulty: AIDifficulty) {
        val names = buildList {
            add("You")
            repeat(playerCount - 1) { add("Player ${it + 2}") }
        }
        val kinds = buildList {
            add(PlayerKind.LOCAL_HUMAN)
            repeat(playerCount - 1) { add(PlayerKind.AI) }
        }
        val difficulties = List(playerCount) { difficulty }
        _state.value = GameEngine.newRound(playerCount, names, kinds, difficulties)
        _selectedTile.value = null
        resolveTurn()
    }

    override fun selectTile(tile: DominoTile) {
        val s = _state.value ?: return
        if (s.currentPlayer.isAI) return
        _selectedTile.value = if (_selectedTile.value?.id == tile.id) null else tile
    }

    override fun playSelectedTile(side: ChainSide) {
        val s = _state.value ?: return
        val tile = _selectedTile.value ?: return
        if (s.currentPlayer.isAI) return
        if (!GameEngine.isValidPlay(s, tile, side)) return
        _state.value = GameEngine.playTile(s, tile, side)
        _selectedTile.value = null
        resolveTurn()
    }

    /**
     * The single gatekeeper for turn progression. Called after every move (human or AI).
     * It auto-draws a stuck current player until they can move (or the boneyard runs
     * dry, in which case it auto-passes), and hands control to the AI engine whenever
     * it becomes an AI player's turn — repeating until control lands on a human with at
     * least one legal move, or the round ends. This runs after AI moves too, so a human
     * who's stuck right after an opponent's turn gets drawn for automatically instead of
     * the game silently freezing.
     */
    private fun resolveTurn() {
        val s = _state.value ?: return
        if (s.roundOver) return

        if (!GameEngine.hasAnyValidMove(s, s.currentPlayerIndex)) {
            if (s.boneyard.isNotEmpty()) {
                if (s.currentPlayer.isAI) {
                    viewModelScope.launch {
                        delay(300)
                        val current = _state.value ?: return@launch
                        if (current.roundOver) return@launch
                        _state.value = GameEngine.drawTile(current)
                        resolveTurn()
                    }
                } else {
                    _state.value = GameEngine.drawTile(s)
                    resolveTurn()
                }
            } else {
                _state.value = GameEngine.pass(s)
                resolveTurn()
            }
            return
        }

        if (s.currentPlayer.isAI) {
            viewModelScope.launch {
                delay(650) // brief pause so the AI's move is readable, not instant
                val current = _state.value ?: return@launch
                if (current.roundOver || !current.currentPlayer.isAI) return@launch
                val move = AIEngine.chooseMove(current, current.currentPlayerIndex)
                _state.value = if (move != null) {
                    GameEngine.playTile(current, move.tile, move.side)
                } else {
                    GameEngine.pass(current)
                }
                resolveTurn()
            }
        }
        // else: it's a human's turn and they have a legal move — wait for their input.
    }
}
