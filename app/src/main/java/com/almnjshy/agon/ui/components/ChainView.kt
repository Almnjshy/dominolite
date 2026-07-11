package com.almnjshy.agon.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.almnjshy.agon.board.ScreenTransform
import com.almnjshy.agon.board.TableBounds
import com.almnjshy.agon.engine.ChainTile
import com.almnjshy.agon.rendering.DominoTileFace
import com.almnjshy.agon.snake.SnakeLayoutEngine

@Composable
fun ChainView(
    chain: List<ChainTile>,
    bounds: TableBounds,
    transform: ScreenTransform,
    unitPx: Float,
    modifier: Modifier = Modifier
) {
    val placements = SnakeLayoutEngine.computeLayout(chain, bounds)
    val density = LocalDensity.current

    val baseW = with(density) { (unitPx * 0.5f).toDp() }
    val baseH = with(density) { unitPx.toDp() }

    Box(modifier = modifier) {
        for (placement in placements) {
            val screen = transform.toScreen(placement.centerX, placement.centerY)
            val isVertical = placement.rotationDegrees == 90f
            val w = if (isVertical) baseH else baseW
            val h = if (isVertical) baseW else baseH

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (screen.x - w.toPx() / 2).toInt(),
                            (screen.y - h.toPx() / 2).toInt()
                        )
                    }
                    .size(w, h)
            ) {
                DominoTileFace(
                    left = placement.chainTile.tile.left,
                    right = placement.chainTile.tile.right,
                    isVertical = isVertical,
                    widthDp = w,
                    heightDp = h
                )
            }
        }
    }
}
