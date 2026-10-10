package com.metrolist.music.access

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.LoveMusicTheme
import com.metrolist.music.ui.theme.SpotifyGreen
import kotlinx.coroutines.launch

private const val TELEGRAM_URL = "https://t.me/mrtuik"

/**
 * Uses the app's own theme (flat black, white primary, Spotify green only as the "playing" accent),
 * so it looks like part of LoveMusic and not a separate screen.
 */
@Composable
fun AccessScreen(
    prefillId: String,
    initialMessage: String?,
    onExit: () -> Unit,
) {
    LoveMusicTheme {
        AccessContent(prefillId, initialMessage, onExit)
    }
}

@Composable
private fun AccessContent(
    prefillId: String,
    initialMessage: String?,
    onExit: () -> Unit,
) {
    // Back must not bypass the gate: exit the app instead.
    BackHandler(onBack = onExit)

    var id by remember { mutableStateOf(prefillId) }
    var error by remember { mutableStateOf(initialMessage) }
    var loading by remember { mutableStateOf(false) }
    var sheetIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { sheetIn = true }

    fun submit() {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            error = AccessGate.submit(id)
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---------------- HERO ----------------
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.small_icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(cs.onBackground),
                    modifier = Modifier.size(if (keyboardOpen) 48.dp else 76.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "LoveMusic",
                    style = MaterialTheme.typography.headlineLarge,
                    color = cs.onBackground,
                )
                if (!keyboardOpen) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Music for everyone you love.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    Equalizer()
                }
            }

            // ---------------- BOTTOM SHEET (cannot be dismissed) ----------------
            AnimatedVisibility(
                visible = sheetIn,
                enter = slideInVertically(tween(400)) { it } + fadeIn(tween(400)),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(cs.surfaceContainer)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(cs.outline.copy(alpha = 0.6f)),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Enter your Access ID",
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "One-time activation on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it.uppercase().filter { c -> !c.isWhitespace() } },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "TUIK-XXXX-XXXX",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp,
                            )
                        },
                        textStyle = TextStyle(
                            color = cs.onSurface,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center,
                        ),
                        isError = error != null,
                        enabled = !loading,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cs.surfaceContainerHighest,
                            unfocusedContainerColor = cs.surfaceContainerHighest,
                            disabledContainerColor = cs.surfaceContainerHighest,
                            errorContainerColor = cs.surfaceContainerHighest,
                            focusedBorderColor = cs.onSurface,
                            unfocusedBorderColor = cs.surfaceContainerHighest,
                            errorBorderColor = cs.error,
                            cursorColor = SpotifyGreen,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface,
                            focusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = 0.6f),
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error!!,
                            color = cs.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { submit() },
                        enabled = !loading && id.isNotBlank(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary,
                            disabledContainerColor = cs.surfaceContainerHighest,
                            disabledContentColor = cs.onSurfaceVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = cs.onPrimary,
                            )
                        } else {
                            Text("Continue", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { runCatching { uriHandler.openUri(TELEGRAM_URL) } },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, cs.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        Text("Need an ID? Message @mrtuik on Telegram", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

/** The same kind of green "now playing" bars the player uses, as a small hero visual. */
@Composable
private fun Equalizer() {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(620, 840, 540, 960, 720, 480, 900)
    Row(
        Modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        durations.forEachIndexed { i, d ->
            val h by transition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(d),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 80),
                ),
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SpotifyGreen),
            )
        }
    }
}
