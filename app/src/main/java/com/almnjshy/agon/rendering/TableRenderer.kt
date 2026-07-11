package com.almnjshy.agon.rendering

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.almnjshy.agon.ui.theme.AgonColors

enum class TableTheme { CLASSIC_FELT, WOOD, MARBLE }

@Composable
fun TableSurface(theme: TableTheme, modifier: Modifier = Modifier.fillMaxSize()) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val (top, bottom) = when (theme) {
            TableTheme.CLASSIC_FELT -> AgonColors.FeltGreenLight to AgonColors.FeltGreenDark
            TableTheme.WOOD -> AgonColors.WoodLight to AgonColors.WoodDark
            TableTheme.MARBLE -> Color(0xFFEDEAE3) to Color(0xFFC9C2B3)
        }

        drawRect(brush = Brush.radialGradient(colors = listOf(top, bottom), radius = w))

        // Gold inlay border framing the isometric play surface
        drawRoundRect(
            color = AgonColors.Gold.copy(alpha = 0.55f),
            topLeft = Offset(w * 0.03f, h * 0.05f),
            size = Size(w * 0.94f, h * 0.9f),
            cornerRadius = CornerRadius(48f, 48f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )

        if (theme == TableTheme.WOOD) {
            // Faint wood-grain lines
            var y = h * 0.1f
            while (y < h * 0.9f) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(w, y + 12f),
                    strokeWidth = 2f
                )
                y += h * 0.06f
            }
        }
    }
}
