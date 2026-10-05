/**
 * LoveMusic (C) 2026
 * Based on Metrolist (GPL-3.0). Licensed under GPL-3.0.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Waveform-style seek bar (bars instead of a line). The bar heights are a stable pseudo-random
 * pattern derived from [seed], so every song keeps the same "shape" while it plays.
 * Tap or drag anywhere on it to seek. API mirrors Material3 `Slider` for easy swapping.
 */
@Composable
fun LoveWaveformSeekBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.25f),
    seed: Int = 0,
) {
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestOnFinished by rememberUpdatedState(onValueChangeFinished)
    val latestRange by rememberUpdatedState(valueRange)

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        val r = latestRange
                        latestOnChange(r.start + f * (r.endInclusive - r.start))
                        latestOnFinished()
                    }
                }.pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val f = (offset.x / size.width).coerceIn(0f, 1f)
                            val r = latestRange
                            latestOnChange(r.start + f * (r.endInclusive - r.start))
                        },
                        onDragEnd = { latestOnFinished() },
                        onDragCancel = { latestOnFinished() },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val f = (change.position.x / size.width).coerceIn(0f, 1f)
                            val r = latestRange
                            latestOnChange(r.start + f * (r.endInclusive - r.start))
                        },
                    )
                },
    ) {
        val barWidth = 3.dp.toPx()
        val gap = 2.5.dp.toPx()
        val step = barWidth + gap
        val count = ((size.width + gap) / step).toInt().coerceAtLeast(8)
        val usedWidth = count * step - gap
        val startX = (size.width - usedWidth) / 2f
        val s = (seed % 997).toFloat()
        val minH = size.height * 0.16f
        val maxH = size.height
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

        for (i in 0 until count) {
            val x = i.toFloat()
            val wave =
                abs(sin(x * 0.37f + s) * cos(x * 0.113f + s * 0.7f)) * 0.65f +
                    abs(sin(x * 0.91f + s * 1.3f)) * 0.35f
            val h = minH + (maxH - minH) * wave.coerceIn(0f, 1f)
            val played = (i + 0.5f) / count <= fraction
            drawRoundRect(
                color = if (played) activeColor else inactiveColor,
                topLeft = Offset(startX + i * step, (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = radius,
            )
        }
    }
}
