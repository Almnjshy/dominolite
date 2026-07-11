package com.almnjshy.agon.rendering

import androidx.compose.ui.geometry.Offset

/**
 * Pip center positions for each value 0-6, expressed as fractions (0f..1f) of a single
 * domino half's bounding box. Standard 3x3 layout used by every physical domino set.
 */
object PipPatterns {
    private const val L = 0.22f
    private const val C = 0.5f
    private const val R = 0.78f
    private const val T = 0.22f
    private const val M = 0.5f
    private const val B = 0.78f

    fun positionsFor(value: Int): List<Offset> = when (value) {
        0 -> emptyList()
        1 -> listOf(Offset(C, M))
        2 -> listOf(Offset(L, T), Offset(R, B))
        3 -> listOf(Offset(L, T), Offset(C, M), Offset(R, B))
        4 -> listOf(Offset(L, T), Offset(R, T), Offset(L, B), Offset(R, B))
        5 -> listOf(Offset(L, T), Offset(R, T), Offset(C, M), Offset(L, B), Offset(R, B))
        6 -> listOf(
            Offset(L, T), Offset(R, T),
            Offset(L, M), Offset(R, M),
            Offset(L, B), Offset(R, B)
        )
        else -> emptyList()
    }
}
