package com.almnjshy.agon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.almnjshy.agon.engine.SeatPosition
import com.almnjshy.agon.rendering.DominoTileBack

/**
 * Renders an opponent's hand as face-down tiles only — pip values are never shown.
 * Orientation follows the seat: LEFT/RIGHT seats fan vertically, TOP/TOP_LEFT/TOP_RIGHT
 * fan horizontally, matching how the seat reads at the table.
 */
@Composable
fun OpponentHandView(tileCount: Int, seat: SeatPosition, modifier: Modifier = Modifier) {
    val vertical = seat == SeatPosition.LEFT || seat == SeatPosition.RIGHT
    val tileW = if (vertical) 22.dp else 34.dp
    val tileH = if (vertical) 34.dp else 22.dp

    if (vertical) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy((-14).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(tileCount) { DominoTileBack(widthDp = tileW, heightDp = tileH) }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy((-14).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(tileCount) { DominoTileBack(widthDp = tileW, heightDp = tileH) }
        }
    }
}
