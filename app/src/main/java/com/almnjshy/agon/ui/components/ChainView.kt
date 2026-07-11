package com.almnjshy.agon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.almnjshy.agon.board.IsometricTransform
import com.almnjshy.agon.board.TableBounds
import com.almnjshy.agon.engine.ChainTile
import com.almnjshy.agon.rendering.DominoTileFace
import com.almnjshy.agon.snake.SnakeLayoutEngine

/**
 * Draws every tile in the chain at its snake-computed position, projected through the
 * isometric transform. Each tile is composed once in its canonical "long axis horizontal"
 * form and then rotated with `rotationZ` to match the direction the snake is travelling
 * at that point — this is what actually makes turns (and perpendicular doubles) look
 * correct instead of every tile staying horizontal after the chain bends.
 *
 * The whole layout is recomputed from the current chain each call, so the visual is
 * always a pure function of GameState — no incremental/stale state.
 */
@Composable
fun ChainView(
    chain: List<ChainTile>,
    bounds: TableBounds,
    isoTransform: IsometricTransform,
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
            val screen = isoTransform.toScreen(placement.centerX, placement.centerY)

            Box(
                modifier = Modifier
                    .offset { IntOffset(screen.x.toInt(), screen.y.toInt()) }
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
