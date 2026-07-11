package com.almnjshy.agon.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.almnjshy.agon.engine.ChainSide
import com.almnjshy.agon.engine.DominoTile
import com.almnjshy.agon.rendering.DominoTileFace

/**
 * The human player's hand. Tiles can be tapped to select (they lift + enlarge slightly)
 * or dragged upward toward the table to attempt a play in one motion.
 */
@Composable
fun PlayerHandView(
    hand: List<DominoTile>,
    selectedTileId: Int?,
    canPlaySide: (DominoTile) -> List<ChainSide>,
    onSelect: (DominoTile) -> Unit,
    onDragPlay: (DominoTile) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
        verticalAlignment = Alignment.Bottom
    ) {
        hand.forEach { tile ->
            key(tile.id) {
                HandTile(
                    tile = tile,
                    isSelected = tile.id == selectedTileId,
                    isPlayable = canPlaySide(tile).isNotEmpty(),
                    onTap = { onSelect(tile) },
                    onDraggedUp = { onDragPlay(tile) }
                )
            }
        }
    }
}

@Composable
private fun HandTile(
    tile: DominoTile,
    isSelected: Boolean,
    isPlayable: Boolean,
    onTap: () -> Unit,
    onDraggedUp: () -> Unit
) {
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val lift by animateDpAsState(
        targetValue = if (isSelected && !isDragging) (-14).dp else 0.dp,
        label = "lift"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        label = "scale"
    )
    val liftPx = with(androidx.compose.ui.platform.LocalDensity.current) { lift.toPx() }

    Box(
        modifier = Modifier
            .graphicsLayer {
                translationY = liftPx + (if (isDragging) dragOffsetY else 0f)
                scaleX = scale
                scaleY = scale
                alpha = if (isPlayable) 1f else 0.55f
            }
            .pointerInput(tile.id) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(tile.id) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        if (dragOffsetY < -80f && isPlayable) onDraggedUp()
                        dragOffsetY = 0f
                    },
                    onDragCancel = { isDragging = false; dragOffsetY = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetY += dragAmount.y
                    }
                )
            }
    ) {
        DominoTileFace(
            left = tile.left,
            right = tile.right,
            isVertical = false,
            widthDp = 52.dp,
            heightDp = 30.dp,
            highlighted = isSelected
        )
    }
}
