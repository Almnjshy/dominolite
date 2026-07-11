package com.almnjshy.agon.ai

import com.almnjshy.agon.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AIMove(val tile: DominoTile, val side: ChainSide)

/**
 * EASY: random legal move.
 * MEDIUM: single-ply heuristic (shed heavy tiles, play doubles, keep follow-ups).
 * HARD: Monte Carlo determinization — since we can't see opponents' tiles, we sample
 * several plausible hidden distributions consistent with what's public (hand sizes,
 * boneyard size), play each candidate move forward several plies in each sample using
 * the MEDIUM policy for every player (including future turns of our own), score the
 * resulting positions, and average per-move across all samples/rollouts. This is a real
 * search over the game tree rather than a fixed lookup table, while staying fast enough
 * to run on a phone — a full exhaustive minimax isn't feasible with hidden information
 * and up to 4 players, so this determinize-and-rollout approach is the practical
 * equivalent used by most strong hidden-information game AIs (e.g. ISMCTS-style).
 */
object AIEngine {

    private const val DETERMINIZATIONS = 8
    private const val SEARCH_DEPTH = 6 // plies simulated forward per rollout

    suspend fun chooseMove(state: GameState, playerIndex: Int): AIMove? {
        val player = state.players[playerIndex]
        val legalMoves = legalMovesFor(state, playerIndex)
        if (legalMoves.isEmpty()) return null
        if (legalMoves.size == 1) return legalMoves.first()

        return when (player.aiDifficulty) {
            AIDifficulty.EASY -> legalMoves.random()
            AIDifficulty.MEDIUM -> legalMoves.maxByOrNull { scoreMedium(state, player, it) }
            AIDifficulty.HARD -> withContext(Dispatchers.Default) {
                chooseMoveHard(state, playerIndex, legalMoves)
            }
        }
    }

    private fun legalMovesFor(state: GameState, playerIndex: Int): List<AIMove> {
        val hand = state.players[playerIndex].hand
        return hand.flatMap { tile -> GameEngine.playableSides(state, tile).map { AIMove(tile, it) } }
    }

    // ---------------------------------------------------------------------
    // MEDIUM: fast single-ply heuristic. Also used as the rollout policy for
    // every player (including HARD's own future turns) during HARD's search,
    // so it has to be both cheap and reasonable.
    // ---------------------------------------------------------------------

    private fun scoreMedium(state: GameState, player: PlayerState, move: AIMove): Double {
        var score = move.tile.pipSum * 1.0       // shed heavy tiles first
        if (move.tile.isDouble) score += 6.0      // doubles are awkward to hold, play them

        val newEnd = exposedEndAfter(state, move)
        val followUps = player.hand.count { it.id != move.tile.id && it.hasValue(newEnd) }
        score += followUps * 1.5                  // prefer moves that keep us flexible
        return score
    }

    private fun exposedEndAfter(state: GameState, move: AIMove): Int {
        if (state.chain.isEmpty()) return move.tile.otherEnd(move.tile.left)
        val currentEnd = if (move.side == ChainSide.LEFT) state.leftEnd!! else state.rightEnd!!
        return move.tile.otherEnd(currentEnd)
    }

    // ---------------------------------------------------------------------
    // HARD: determinize + rollout search
    // ---------------------------------------------------------------------

    private fun chooseMoveHard(state: GameState, playerIndex: Int, legalMoves: List<AIMove>): AIMove {
        val totals = DoubleArray(legalMoves.size)

        repeat(DETERMINIZATIONS) {
            val sample = determinize(state, playerIndex)
            for ((i, move) in legalMoves.withIndex()) {
                val afterMove = GameEngine.playTile(sample, move.tile, move.side)
                totals[i] += rollout(afterMove, playerIndex, SEARCH_DEPTH)
            }
        }

        val bestIndex = totals.indices.maxByOrNull { totals[it] } ?: 0
        return legalMoves[bestIndex]
    }

    /**
     * Builds a fully-known game state consistent with everything `playerIndex` can
     * actually observe: their own hand and the visible chain stay exact; every hidden
     * tile (every opponent's hand + the boneyard) is reshuffled randomly across those
     * same slot sizes, since from our point of view any such arrangement is equally
     * likely.
     */
    private fun determinize(state: GameState, playerIndex: Int): GameState {
        val knownIds = state.players[playerIndex].hand.map { it.id }.toSet() +
            state.chain.map { it.tile.id }.toSet()
        val hiddenPool = DominoTile.standardSet().filterNot { it.id in knownIds }.shuffled()

        var cursor = 0
        val newPlayers = state.players.mapIndexed { i, p ->
            if (i == playerIndex) {
                p
            } else {
                val count = p.hand.size
                val dealt = hiddenPool.subList(cursor, cursor + count)
                cursor += count
                p.copy(hand = dealt)
            }
        }
        val newBoneyard = hiddenPool.subList(cursor, hiddenPool.size)
        return state.copy(players = newPlayers, boneyard = newBoneyard)
    }

    /**
     * Plays a determinized state forward using the MEDIUM policy for every player
     * (simulating the whole table, not just us) for up to `depthRemaining` plies or
     * until the round ends, then scores the resulting position from `perspective`.
     */
    private fun rollout(state: GameState, perspective: Int, depthRemaining: Int): Double {
        var s = state
        var depth = depthRemaining
        while (!s.roundOver && depth > 0) {
            s = advanceOnePly(s)
            depth--
        }
        return evaluate(s, perspective)
    }

    private fun advanceOnePly(state: GameState): GameState {
        var s = state
        while (!s.roundOver && !GameEngine.hasAnyValidMove(s, s.currentPlayerIndex)) {
            s = if (s.boneyard.isNotEmpty()) GameEngine.drawTile(s) else GameEngine.pass(s)
        }
        if (s.roundOver) return s

        val actor = s.players[s.currentPlayerIndex]
        val moves = actor.hand.flatMap { tile -> GameEngine.playableSides(s, tile).map { AIMove(tile, it) } }
        val chosen = moves.maxByOrNull { scoreMedium(s, actor, it) } ?: return GameEngine.pass(s)
        return GameEngine.playTile(s, chosen.tile, chosen.side)
    }

    /**
     * Higher is better for `perspective`. A finished round gets a large terminal
     * bonus/penalty (scaled by how decisive it was); an unfinished rollout is scored
     * by hand weight — light hand for us is good, light hands for opponents are bad,
     * and fewer tiles in our own hand is weighted extra since it's closer to winning.
     */
    private fun evaluate(state: GameState, perspective: Int): Double {
        if (state.roundOver) {
            return when (state.roundWinnerIndex) {
                perspective -> 500.0 - state.players.sumOf { p -> p.hand.sumOf { it.pipSum } } * 0.1
                null -> 0.0
                else -> -500.0
            }
        }
        val ownPips = state.players[perspective].hand.sumOf { it.pipSum }
        val opponentPips = state.players.filterIndexed { i, _ -> i != perspective }
            .sumOf { p -> p.hand.sumOf { it.pipSum } }
        val ownTileCount = state.players[perspective].hand.size
        return (opponentPips - ownPips) * 1.0 - ownTileCount * 2.0
    }
}
