package com.almnjshy.agon.board

import androidx.compose.ui.geometry.Offset

/**
 * Converts logical 2D board coordinates (in "tile units") into screen-space pixel
 * offsets using a true 2:1 isometric projection. The camera never rotates or zooms
 * mid-match, so this is the single source of truth for "where does logical (x,y) sit
 * on screen" for every renderer in the app.
 */
class IsometricTransform(
    private val tileUnitWidthPx: Float,
    private val tileUnitHeightPx: Float,
    private val originScreenX: Float,
    private val originScreenY: Float
) {
    fun toScreen(logicalX: Float, logicalY: Float): Offset {
        val screenX = (logicalX - logicalY) * (tileUnitWidthPx / 2f) + originScreenX
        val screenY = (logicalX + logicalY) * (tileUnitHeightPx / 2f) + originScreenY
        return Offset(screenX, screenY)
    }

    fun toScreen(logical: Offset): Offset = toScreen(logical.x, logical.y)

    /** Inverse transform: screen pixels -> logical board units. Used for hit-testing drags. */
    fun toLogical(screenX: Float, screenY: Float): Offset {
        val sx = screenX - originScreenX
        val sy = screenY - originScreenY
        val a = sx / (tileUnitWidthPx / 2f)
        val b = sy / (tileUnitHeightPx / 2f)
        val logicalX = (a + b) / 2f
        val logicalY = (b - a) / 2f
        return Offset(logicalX, logicalY)
    }
}