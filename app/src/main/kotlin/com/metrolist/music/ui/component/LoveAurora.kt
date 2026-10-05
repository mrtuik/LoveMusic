/**
 * LoveMusic (C) 2026
 * Based on Metrolist (GPL-3.0). Licensed under GPL-3.0.
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Slowly drifting "aurora" glow made of three soft radial gradients.
 * Draws on top of the screen's own background colour, so it is always safe to place behind content.
 *
 * @param colors up to three glow colours (defaults to the theme's primary / secondary / tertiary)
 * @param intensity 0..1 overall strength of the glow
 */
@Composable
fun LoveAuroraBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
        ),
    intensity: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "love_aurora")
    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_a",
    )
    val b by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(21000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_b",
    )
    val c by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "aurora_c",
    )

    val c1 = colors.getOrElse(0) { Color(0xFFFF4D8D) }
    val c2 = colors.getOrElse(1) { Color(0xFF9B6BFF) }
    val c3 = colors.getOrElse(2) { Color(0xFFFFB784) }
    val strength = intensity.coerceIn(0f, 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val r = maxOf(w, h)

        fun glow(color: Color, center: Offset, radius: Float, alpha: Float) {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors = listOf(color.copy(alpha = alpha * strength), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                radius = radius,
                center = center,
            )
        }

        glow(c1, Offset(w * (0.15f + 0.45f * a), h * (0.10f + 0.12f * b)), r * 0.70f, 0.42f)
        glow(c2, Offset(w * (0.95f - 0.40f * b), h * (0.35f + 0.18f * c)), r * 0.62f, 0.34f)
        glow(c3, Offset(w * (0.20f + 0.50f * c), h * (0.80f - 0.15f * a)), r * 0.58f, 0.24f)
    }
}
