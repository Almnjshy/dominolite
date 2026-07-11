package com.almnjshy.agon.board

import androidx.compose.ui.geometry.Offset

class ScreenTransform(
    private val scalePx: Float,
    private val originScreenX: Float,
    private val originScreenY: Float
) {
    fun toScreen(logicalX: Float, logicalY: Float): Offset {
        return Offset(
            logicalX * scalePx + originScreenX,
            logicalY * scalePx + originScreenY
        )
    }

    fun toScreen(logical: Offset): Offset = toScreen(logical.x, logical.y)
}
