package com.almnjshy.agon.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AgonScheme = darkColorScheme(
    primary = AgonColors.Gold,
    secondary = AgonColors.GoldBright,
    background = AgonColors.FeltGreenDark,
    surface = AgonColors.FeltGreenMid,
    onPrimary = AgonColors.WoodDark,
    onBackground = AgonColors.TileIvory,
    onSurface = AgonColors.TileIvory
)

@Composable
fun AgonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AgonScheme, content = content)
}
