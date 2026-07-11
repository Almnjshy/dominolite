package com.almnjshy.agon.snake

import com.almnjshy.agon.board.TableBounds
import com.almnjshy.agon.engine.ChainTile

/** Direction the chain is currently being laid in, on the logical (x,y) plane. */
private enum class Direction(val dx: Float, val dy: Float, val baseRotation: Float) {
    EAST(1f, 0f, 0f),
    SOUTH(0f, 1f, 90f),
    WEST(-1f, 0f, 180f),
    NORTH(0f, -1f, 270f);

    fun turn(clockwise: Boolean): Direction {
        val order = listOf(EAST, SOUTH, WEST, NORTH)
        val i = order.indexOf(this)
        val next = if (clockwise) (i + 1) % 4 else (i + 3) % 4
        return order[next]
    }
}

data class TilePlacement(
    val chainTile: ChainTile,
    val centerX: Float,
    val centerY: Float,
    val rotationDegrees: Float
)

/**
 * Turns a flat chain (as stored in GameState, in pip-order) into real screen-ready
 * positions that snake around the table and turn automatically at the boundary — no
 * grid, no fixed rows/columns. The whole layout is recomputed from scratch every call,
 * which keeps this a pure function of state (safe to call every recomposition).
 */
object SnakeLayoutEngine {

    private const val LONG = 1.0f   // tile length along its long axis, in logical units
    private const val SHORT = 0.5f  // tile width along its short axis
    private const val GAP = 0.05f

    fun computeLayout(chain: List<ChainTile>, bounds: TableBounds): List<TilePlacement> {
        if (chain.isEmpty()) return emptyList()

        val pivot = chain.size / 2
        // Right half: pivot tile onward, walking east from table center.
        val rightHalf = chain.subList(pivot, chain.size)
        // Left half: everything before pivot, walking west from just left of center.
        val leftHalf = chain.subList(0, pivot).reversed()

        val rightPlacements = walk(rightHalf, startX = 0f, startY = 0f, startDir = Direction.EAST, clockwise = true, bounds)
        val leftPlacements = if (leftHalf.isEmpty()) emptyList() else {
            val firstStep = (extent(leftHalf.first()) / 2f) + GAP
            walk(leftHalf, startX = -firstStep, startY = 0f, startDir = Direction.WEST, clockwise = false, bounds)
        }

        return rightPlacements + leftPlacements
    }

    private fun extent(tile: ChainTile): Float =
        if (tile.isSpinnerOrientation) SHORT else LONG

    private fun walk(
        tiles: List<ChainTile>,
        startX: Float,
        startY: Float,
        startDir: Direction,
        clockwise: Boolean,
        bounds: TableBounds
    ): List<TilePlacement> {
        var dir = startDir
        var cx = startX
        var cy = startY
        val result = mutableListOf<TilePlacement>()

        for (i in tiles.indices) {
            val tile = tiles[i]
            val halfExtent = extent(tile) / 2f

            // Check whether placing this tile's far edge would cross the table bound;
            // if so, turn before placing it so the whole tile lands inside the table.
            val farEdgeX = cx + dir.dx * halfExtent
            val farEdgeY = cy + dir.dy * halfExtent
            if (farEdgeX > bounds.maxX || farEdgeX < bounds.minX || farEdgeY > bounds.maxY || farEdgeY < bounds.minY) {
                dir = dir.turn(clockwise)
                // step off from current point in the new direction by half this tile's
                // extent so it hugs the boundary we just turned away from instead of
                // overlapping the previous tile.
                val newHalfExtent = extent(tile) / 2f
                cx += dir.dx * newHalfExtent
                cy += dir.dy * newHalfExtent
            } else {
                cx = farEdgeX
                cy = farEdgeY
            }

            val rotation = dir.baseRotation + if (tile.isSpinnerOrientation) 90f else 0f
            result.add(TilePlacement(tile, cx, cy, rotation))

            // advance to the near edge for the *next* tile
            val trailingHalfExtent = extent(tile) / 2f
            cx += dir.dx * (trailingHalfExtent + GAP)
            cy += dir.dy * (trailingHalfExtent + GAP)
        }

        return result
    }
}
