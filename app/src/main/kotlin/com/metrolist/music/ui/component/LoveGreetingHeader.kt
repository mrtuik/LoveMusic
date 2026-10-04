/**
 * LoveMusic (C) 2026
 * Based on Metrolist (GPL-3.0). Licensed under GPL-3.0.
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import java.time.LocalTime

/**
 * Time-aware greeting card shown at the top of Home.
 * The heart icon is the SVG-generated `lm_heart_wave` drawable (svg-icons/lm_heart_wave.svg).
 */
@Composable
fun LoveGreetingHeader(modifier: Modifier = Modifier) {
    val greetingRes =
        remember {
            when (LocalTime.now().hour) {
                in 5..11 -> R.string.love_greeting_morning
                in 12..16 -> R.string.love_greeting_afternoon
                in 17..21 -> R.string.love_greeting_evening
                else -> R.string.love_greeting_night
            }
        }

    val transition = rememberInfiniteTransition(label = "love_heartbeat")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "love_heartbeat_scale",
    )

    val colors = MaterialTheme.colorScheme

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(colors.primaryContainer, colors.secondaryContainer),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(greetingRes),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.love_greeting_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
        Icon(
            painter = painterResource(R.drawable.lm_heart_wave),
            contentDescription = null,
            tint = colors.primary,
            modifier =
                Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    },
        )
    }
}
