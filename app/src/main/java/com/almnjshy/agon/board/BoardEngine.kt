package com.almnjshy.agon.board

import com.almnjshy.agon.engine.SeatPosition

/**
 * Defines the playable surface as a logical rectangle in tile-units (NOT pixels, NOT a
 * grid of cells). The snake layout engine walks inside these bounds and turns when it
 * would cross them. Bounds are symmetric around (0,0) so the chain can grow left/right/
 * up/down from the center with no bias.
 */
data class TableBounds(
    val halfWidthUnits: Float,
    val halfHeightUnits: Float
) {
    val minX get() = -halfWidthUnits
    val maxX get() = halfWidthUnits
    val minY get() = -halfHeightUnits
    val maxY get() = halfHeightUnits
}

object BoardEngine {

    /** A sensible default playing surface; tuned so ~14-16 tiles fit before a turn is needed. */
    fun defaultBounds(): TableBounds = TableBounds(halfWidthUnits = 4.2f, halfHeightUnits = 2.6f)

    /**
     * Anchor point (in logical units, relative to table center) where each seat's hand
     * visually "originates" from — used to fan out opponent tile-backs and to aim the
     * player's own hand toward the table.
     */
    fun seatAnchor(seat: SeatPosition, bounds: TableBounds): Pair<Float, Float> = when (seat) {
        SeatPosition.BOTTOM -> 0f to bounds.maxY + 1.2f
        SeatPosition.TOP -> 0f to bounds.minY - 1.2f
        SeatPosition.LEFT -> bounds.minX - 1.2f to 0f
        SeatPosition.RIGHT -> bounds.maxX + 1.2f to 0f
        SeatPosition.TOP_LEFT -> bounds.minX - 0.6f to bounds.minY - 0.6f
        SeatPosition.TOP_RIGHT -> bounds.maxX + 0.6f to bounds.minY - 0.6f
    }
}
