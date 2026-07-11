package com.almnjshy.agon.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Central place for every motion curve used in the app, so placement, selection and
 * settle animations feel consistent everywhere instead of each screen inventing its own.
 */
object Motion {
    val easeInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

    fun slideIn(durationMs: Int = 260) = tween<Float>(durationMillis = durationMs, easing = easeInOut)

    /** Small settle bounce once a tile lands in the chain. */
    fun settleBounce() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Snappy lift when a hand tile is selected. */
    fun selectLift() = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessHigh
    )
}
