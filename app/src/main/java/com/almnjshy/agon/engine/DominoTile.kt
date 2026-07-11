package com.almnjshy.agon.engine

import kotlinx.serialization.Serializable

/**
 * A single domino tile. `left`/`right` are the pip values on each half.
 * Orientation on the board is decided at layout time, not stored here.
 */
@Serializable
data class DominoTile(
    val id: Int,
    val left: Int,
    val right: Int
) {
    val isDouble: Boolean get() = left == right
    val pipSum: Int get() = left + right

    /** Does this tile show `value` on either half? */
    fun hasValue(value: Int): Boolean = left == value || right == value

    /** The other value on this tile, given one known end. */
    fun otherEnd(known: Int): Int = if (left == known) right else left

    companion object {
        /** Standard double-six set: 28 tiles. */
        fun standardSet(): List<DominoTile> {
            val tiles = mutableListOf<DominoTile>()
            var id = 0
            for (a in 0..6) {
                for (b in a..6) {
                    tiles.add(DominoTile(id++, a, b))
                }
            }
            return tiles
        }
    }
}
