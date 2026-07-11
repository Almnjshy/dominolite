package com.almnjshy.agon.ui.theme

import androidx.compose.ui.graphics.Color

object AgonColors {
    val FeltGreenDark = Color(0xFF0B3D2E)
    val FeltGreenMid = Color(0xFF145A3E)
    val FeltGreenLight = Color(0xFF1E7A52)

    val Gold = Color(0xFFD4AF37)
    val GoldBright = Color(0xFFF1D26B)
    val GoldDark = Color(0xFF8A6B1F)

    val TileIvory = Color(0xFFFBF7EE)
    val TileIvoryShadowEdge = Color(0xFFE4DBC4)
    val TilePip = Color(0xFF1B1B1B)
    val TileBorder = Color(0xFFC9BE9C)

    val TileBackNavy = Color(0xFF16233B)
    val TileBackNavyLight = Color(0xFF243759)

    val WoodDark = Color(0xFF3B2416)
    val WoodMid = Color(0xFF5B3A22)
    val WoodLight = Color(0xFF7A5232)

    val PlayerBlue = Color(0xFF4FA3E0)
    val PlayerRed = Color(0xFFE0574F)
    val PlayerAmber = Color(0xFFE0A34F)
    val PlayerViolet = Color(0xFFA05FE0)

    fun colorForSeatIndex(i: Int): Color = when (i % 4) {
        0 -> PlayerBlue
        1 -> PlayerRed
        2 -> PlayerAmber
        else -> PlayerViolet
    }
}
