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
    // Spotify-style flat black: the animated purple/pink glow is intentionally disabled.
}
