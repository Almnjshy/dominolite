package com.almnjshy.agon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.almnjshy.agon.board.TableBounds
import com.almnjshy.agon.engine.ChainTile
import com.almnjshy.agon.rendering.DominoTileFace
import com.almnjshy.agon.snake.SnakeLayoutEngine

/**
 * Draws every tile in the chain at its snake-computed position.
 *
 * IMPORTANT: this is a FLAT (orthogonal) projection, not a skewed isometric one.
 * [SnakeLayoutEngine] lays tiles out as plain sequential (x, y) positions — a straight
 * horizontal run has a constant y, a straight vertical run has a constant x, exactly
 * like a real physical domino chain on a table. An isometric skew transform (screenX
 * depending on x-y, screenY depending on x+y) turns any such straight run into a
 * diagonal staircase on screen, because it's designed for skewed grid coordinates, not
 * for these already-orthogonal ones. So here we map logical units straight to screen
 * pixels with a single uniform scale — origin + logical * unitPx — which is what keeps
 * horizontal runs horizontal, vertical runs vertical, and turns at true right angles,
 * matching a real domino board.
 *
 * Each tile is composed once in its canonical "long axis horizontal" form and then
 * rotated with `rotationZ` to match the direction the snake is travelling at that point
 * — this is what makes turns (and perpendicular doubles) look correct instead of every
 * tile staying horizontal after the chain bends.
 *
 * The whole layout is recomputed from the current chain each call, so the visual is
 * always a pure function of GameState — no incremental/stale state.
 */
@Composable
fun ChainView(
    chain: List<ChainTile>,
    bounds: TableBounds,
    originX: Float,
    originY: Float,
    unitPx: Float,
    modifier: Modifier = Modifier
) {
    val placements = SnakeLayoutEngine.computeLayout(chain, bounds)
    val density = LocalDensity.current

    val tileLenPx = unitPx
    val tileWidPx = unitPx * 0.5f
    val wDp: Dp
    val hDp: Dp
    with(density) {
        wDp = tileLenPx.toDp()
        hDp = tileWidPx.toDp()
    }

    Box(modifier = modifier) {
        for (placement in placements) {
            // Flat mapping: NOT (x - y) / (x + y) isometric skew — see kdoc above.
            val screenX = originX + placement.centerX * unitPx
            val screenY = originY + placement.centerY * unitPx

            Box(
                modifier = Modifier
                    .offset { IntOffset(screenX.toInt(), screenY.toInt()) }
                    .graphicsLayer {
                        translationX = -(wDp.toPx() / 2f)
                        translationY = -(hDp.toPx() / 2f)
                        rotationZ = placement.rotationDegrees
                    }
            ) {
                DominoTileFace(
                    left = placement.chainTile.tile.left,
                    right = placement.chainTile.tile.right,
                    isVertical = false,
                    widthDp = wDp,
                    heightDp = hDp
                )
            }
        }
    }
}
