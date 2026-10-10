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
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch

private val Bg = Color(0xFF05100B)
private val Sheet = Color(0xFF0C1A13)
private val Field = Color(0xFF112419)
private val Accent = Color(0xFF2EE59D)
private val AccentDeep = Color(0xFF12805A)
private val OnAccent = Color(0xFF03210F)
private val TextMain = Color(0xFFE8F5EE)
private val TextMuted = Color(0xFF8FA89B)
private val ErrorCol = Color(0xFFFF7A7A)
private const val TELEGRAM_URL = "https://t.me/mrtuik"

@Composable
fun AccessScreen(
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

    fun openTelegram() {
        runCatching { uriHandler.openUri(TELEGRAM_URL) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .imePadding(),
    ) {
        // Soft green glow behind the hero
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Accent.copy(alpha = 0.22f), Color.Transparent),
                        radius = 900f,
                    ),
                ),
        )

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
                Box(
                    Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccentDeep, Accent)))
                        .border(1.dp, Accent.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.small_icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(OnAccent),
                        modifier = Modifier.size(52.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "LOVEMUSIC",
                    color = TextMain,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp,
                )
                if (!keyboardOpen) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your music. Your vibe.",
                        color = TextMuted,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(22.dp))
                    Equalizer()
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeatureChip("Stream")
                        FeatureChip("Lyrics")
                        FeatureChip("Offline")
                    }
                }
            }

            // ---------------- BOTTOM SHEET (cannot be dismissed) ----------------
            AnimatedVisibility(
                visible = sheetIn,
                enter = slideInVertically(tween(450)) { it } + fadeIn(tween(450)),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                        .background(Sheet)
                        .border(
                            BorderStroke(1.dp, Accent.copy(alpha = 0.18f)),
                            RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                        )
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(TextMuted.copy(alpha = 0.4f)),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Enter your Access ID",
                        color = TextMain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "One-time activation on this device",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(18.dp))
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
                                letterSpacing = 2.sp,
                            )
                        },
                        textStyle = TextStyle(
                            color = TextMain,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center,
                        ),
                        isError = error != null,
                        enabled = !loading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Field,
                            unfocusedContainerColor = Field,
                            disabledContainerColor = Field,
                            errorContainerColor = Field,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Accent.copy(alpha = 0.25f),
                            errorBorderColor = ErrorCol,
                            cursorColor = Accent,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain,
                            focusedPlaceholderColor = TextMuted.copy(alpha = 0.6f),
                            unfocusedPlaceholderColor = TextMuted.copy(alpha = 0.6f),
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
                            color = ErrorCol,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { submit() },
                        enabled = !loading && id.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = OnAccent,
                            disabledContainerColor = Accent.copy(alpha = 0.18f),
                            disabledContentColor = TextMuted,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = OnAccent,
                            )
                        } else {
                            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.weight(1f).height(1.dp).background(TextMuted.copy(alpha = 0.2f)))
                        Text(
                            "  Need an ID?  ",
                            color = TextMuted,
                            fontSize = 12.sp,
                        )
                        Box(Modifier.weight(1f).height(1.dp).background(TextMuted.copy(alpha = 0.2f)))
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { openTelegram() },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Accent.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("Message @mrtuik on Telegram", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(label: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Accent.copy(alpha = 0.10f))
            .border(1.dp, Accent.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(label, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Small animated equalizer used as the hero visual. */
@Composable
private fun Equalizer() {
    val transition = rememberInfiniteTransition(label = "eq")
    val durations = listOf(620, 840, 540, 960, 720, 480, 900, 660, 800, 560, 940, 700)
    Row(
        Modifier.height(44.dp),
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
                    initialStartOffset = StartOffset(i * 70),
                ),
                label = "bar$i",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Brush.verticalGradient(listOf(Accent, AccentDeep))),
            )
        }
    }
}
