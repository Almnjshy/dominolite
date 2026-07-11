package com.almnjshy.agon.engine

import kotlinx.serialization.Serializable

/** Where a seat sits around the isometric table, from one particular viewer's point of
 *  view. This is computed at render time (see [SeatLayout.displaySeatFor]) — it is never
 *  stored on a player, because in network play every device must see *itself* at BOTTOM
 *  regardless of which absolute player index it actually is. */
enum class SeatPosition { BOTTOM, LEFT, TOP, RIGHT, TOP_LEFT, TOP_RIGHT }

enum class AIDifficulty { EASY, MEDIUM, HARD }

enum class ChainSide { LEFT, RIGHT }

/** Who/what is driving a given seat. */
enum class PlayerKind { LOCAL_HUMAN, REMOTE_HUMAN, AI }

/**
 * Maps player count -> seat layout, per the design spec.
 * Never assume 4 players anywhere else in the codebase; always go through this.
 */
object SeatLayout {
    private fun ring(playerCount: Int): List<SeatPosition> = when (playerCount) {
        2 -> listOf(SeatPosition.BOTTOM, SeatPosition.TOP)
        3 -> listOf(SeatPosition.BOTTOM, SeatPosition.TOP_LEFT, SeatPosition.TOP_RIGHT)
        4 -> listOf(SeatPosition.BOTTOM, SeatPosition.LEFT, SeatPosition.TOP, SeatPosition.RIGHT)
        else -> throw IllegalArgumentException("Agon supports 2-4 players, got $playerCount")
    }

    /** Kept for callers that just need "a" valid seat ring (e.g. table layout math that
     *  doesn't care about viewer perspective). */
    fun seatsFor(playerCount: Int): List<SeatPosition> = ring(playerCount)

    /**
     * The seat a given player index should be drawn at, from the point of view of
     * `viewerIndex` (the local device's own seat). The viewer always lands on BOTTOM;
     * everyone else rotates around them in table order. In local hotseat play
     * `viewerIndex` is always 0, so this is equivalent to the old fixed mapping.
     */
    fun displaySeatFor(playerIndex: Int, viewerIndex: Int, playerCount: Int): SeatPosition {
        val relative = ((playerIndex - viewerIndex) % playerCount + playerCount) % playerCount
        return ring(playerCount)[relative]
    }

    /** Official double-six rule: every player draws 7 tiles, regardless of player count
     *  (2p: 14 drawn / 14 in boneyard, 3p: 21/7, 4p: 28/0). */
    fun handSizeFor(playerCount: Int): Int = 7
}

@Serializable
data class PlayerState(
    val id: Int,
    val name: String,
    val kind: PlayerKind,
    val aiDifficulty: AIDifficulty = AIDifficulty.MEDIUM,
    val hand: List<DominoTile> = emptyList(),
    val matchScore: Int = 0
) {
    val isAI: Boolean get() = kind == PlayerKind.AI
}

/** A tile as placed in the chain, with the exposed value fixed for layout/rules purposes. */
@Serializable
data class ChainTile(
    val tile: DominoTile,
    /** True if the tile is rotated 90° (perpendicular) — always true for the spinner double
     *  and for any double placed mid-chain, per standard domino visual convention. */
    val isSpinnerOrientation: Boolean
)

@Serializable
data class GameState(
    val playerCount: Int,
    val players: List<PlayerState>,
    val boneyard: List<DominoTile>,
    val chain: List<ChainTile> = emptyList(),
    val leftEnd: Int? = null,
    val rightEnd: Int? = null,
    val currentPlayerIndex: Int = 0,
    val consecutivePasses: Int = 0,
    val roundOver: Boolean = false,
    val roundWinnerIndex: Int? = null,
    val blockedRound: Boolean = false,
    val lastAction: String? = null
) {
    val currentPlayer: PlayerState get() = players[currentPlayerIndex]

    fun playerAt(index: Int) = players[index]
}
