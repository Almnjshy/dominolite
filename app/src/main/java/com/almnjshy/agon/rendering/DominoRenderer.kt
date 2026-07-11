package com.almnjshy.agon.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import com.almnjshy.agon.ui.theme.AgonColors

/**
 * Draws one domino tile entirely with vector Canvas calls (rounded body, center divider,
 * pips, soft gradient, thin highlight) so it stays crisp at any scale/rotation — no PNGs.
 *
 * `left`/`right` are null-safe: when the tile is oriented vertically (a spinner / double
 * placed mid-chain), `left` is drawn on top and `right` on bottom.
 */
@Composable
fun DominoTileFace(
    left: Int,
    right: Int,
    isVertical: Boolean,
    widthDp: Dp,
    heightDp: Dp,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(widthDp, heightDp)) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(w * 0.12f, w * 0.12f)

        // Soft drop shadow first
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(w * 0.03f, h * 0.05f),
            size = Size(w, h),
            cornerRadius = corner
        )

        // Body with a subtle ivory gradient for depth
        val bodyBrush = Brush.linearGradient(
            colors = listOf(AgonColors.TileIvory, AgonColors.TileIvoryShadowEdge),
        )
        drawRoundRect(brush = bodyBrush, size = Size(w, h), cornerRadius = corner)

        // Border
        drawRoundRect(
            color = if (highlighted) AgonColors.GoldBright else AgonColors.TileBorder,
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = if (highlighted) w * 0.045f else w * 0.02f)
        )

        // Center divider + two pip halves
        if (isVertical) {
            drawLine(
                color = AgonColors.TileBorder,
                start = Offset(w * 0.12f, h / 2f),
                end = Offset(w * 0.88f, h / 2f),
                strokeWidth = w * 0.02f
            )
            drawPips(left, Offset.Zero, Size(w, h / 2f))
            withTransform({ translate(0f, h / 2f) }) {
                drawPips(right, Offset.Zero, Size(w, h / 2f))
            }
        } else {
            drawLine(
                color = AgonColors.TileBorder,
                start = Offset(w / 2f, h * 0.12f),
                end = Offset(w / 2f, h * 0.88f),
                strokeWidth = w * 0.02f
            )
            drawPips(left, Offset.Zero, Size(w / 2f, h))
            withTransform({ translate(w / 2f, 0f) }) {
                drawPips(right, Offset.Zero, Size(w / 2f, h))
            }
        }

        // Thin top-left sheen for a glossy, lacquered look
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.35f), Color.Transparent)
            ),
            topLeft = Offset(w * 0.05f, h * 0.05f),
            size = Size(w * 0.5f, h * 0.35f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPips(value: Int, origin: Offset, halfSize: Size) {
    val pipRadius = minOf(halfSize.width, halfSize.height) * 0.11f
    for (p in PipPatterns.positionsFor(value)) {
        val center = Offset(
            origin.x + p.x * halfSize.width,
            origin.y + p.y * halfSize.height
        )
        drawCircle(color = AgonColors.TilePip, radius = pipRadius, center = center)
        // tiny highlight dot for a slight glossy pip
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = pipRadius * 0.35f,
            center = center - Offset(pipRadius * 0.3f, pipRadius * 0.3f)
        )
    }
}

/** The face-down back of a tile, shown for opponents' hands. */
@Composable
fun DominoTileBack(widthDp: Dp, heightDp: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(widthDp, heightDp)) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(w * 0.12f, w * 0.12f)

        drawRoundRect(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(w * 0.03f, h * 0.05f),
            size = Size(w, h),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(AgonColors.TileBackNavyLight, AgonColors.TileBackNavy)
            ),
            size = Size(w, h),
            cornerRadius = corner
        )
        drawRoundRect(
            color = AgonColors.Gold,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.7f, h * 0.7f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.025f)
        )
    }
}
