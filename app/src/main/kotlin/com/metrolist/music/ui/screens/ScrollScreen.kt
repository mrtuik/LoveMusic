package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.LocalDatabase
import com.metrolist.music.constants.ScrollSwipeHintShownKey
import com.metrolist.music.ui.menu.AddToPlaylistDialog
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.extensions.metadata
import com.metrolist.music.ui.utils.resize
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.metrolist.music.taste.SignalKind
import com.metrolist.music.taste.TasteEngine

/**
 * Reels-style "Scroll" tab. It does NOT own a player: it is a vertical pager over the
 * existing playback queue, so audio keeps playing in the background with the normal
 * media notification. Swiping jumps to that queue item; the service already loads
 * more radio songs automatically when the queue runs low.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val windows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val song by playerConnection.currentSong.collectAsStateWithLifecycle()
    val liked = song?.song?.liked == true
    val context = LocalContext.current
    val database = LocalDatabase.current
    var showPlaylistDialog by remember { mutableStateOf(false) }
    val (hintShown, setHintShown) = rememberPreference(ScrollSwipeHintShownKey, defaultValue = false)

    val scope = rememberCoroutineScope()
    val taste = remember(database) { TasteEngine.get(database) }

    if (windows.isEmpty()) {
        // Nothing queued: build the taste-based feed (online radio-from-taste, or liked + downloaded offline).
        LaunchedEffect(Unit) { playerConnection.service.startPersonalFeed() }
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(
                text = "Building your feed…",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    // NOTE: rememberPagerState is rememberSaveable, so when coming back to this tab it restores the
    // OLD page (not the song playing now). We therefore always re-align the pager with the player
    // first, and only after that do we let swipes drive playback.
    val pagerState = rememberPagerState(
        initialPage = currentIndex.coerceIn(0, windows.lastIndex),
        pageCount = { windows.size },
    )
    val latestWindows by rememberUpdatedState(windows)
    val latestIndex by rememberUpdatedState(currentIndex)
    var aligned by remember { mutableStateOf(false) }

    // 1) Player -> pager: whenever the playing song / queue changes (mini player, notification,
    //    search, song ended, queue replaced...) the pager jumps to the current song.
    LaunchedEffect(currentIndex, windows) {
        if (currentIndex !in windows.indices) return@LaunchedEffect
        // Don't fight a finger that is still dragging.
        if (pagerState.isScrollInProgress) {
            snapshotFlow { pagerState.isScrollInProgress }.first { !it }
        }
        val target = latestIndex.coerceIn(0, latestWindows.lastIndex)
        if (pagerState.currentPage != target) {
            if (!aligned) {
                pagerState.scrollToPage(target) // first entry: no animation, no flash of old song
            } else if (kotlin.math.abs(pagerState.currentPage - target) > 1) {
                pagerState.scrollToPage(target)
            } else {
                pagerState.animateScrollToPage(target)
            }
        }
        aligned = true
    }

    // 2) Pager -> player: only a settled swipe that differs from what is playing changes the song.
    LaunchedEffect(pagerState, aligned) {
        if (!aligned) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val w = latestWindows.getOrNull(page) ?: return@collect
                val player = playerConnection.player
                if (page != playerConnection.currentWindowIndex.value) {
                    player.seekToDefaultPosition(w.firstPeriodIndex)
                    player.playWhenReady = true
                }
            }
    }

    // Progress polling (only the visible page uses it).
    var progress by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(isPlaying, currentIndex) {
        while (true) {
            if (!dragging) {
                val p = playerConnection.player
                val d = p.duration
                progress = if (d > 0) (p.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            }
            delay(500)
        }
    }

    // First swipe dismisses the hint for good.
    LaunchedEffect(pagerState, hintShown) {
        if (!hintShown) {
            snapshotFlow { pagerState.isScrollInProgress }.collect { scrolling ->
                if (scrolling) {
                    setHintShown(true)
                }
            }
        }
    }

    val currentMeta = windows.getOrNull(currentIndex)?.mediaItem?.metadata
    AddToPlaylistDialog(
        isVisible = showPlaylistDialog && currentMeta != null,
        onGetSong = {
            currentMeta?.let { m -> database.withTransaction { insert(m) } }
            listOfNotNull(currentMeta?.id)
        },
        onGetSongIds = { listOfNotNull(currentMeta?.id) },
        onDismiss = { showPlaylistDialog = false },
    )

    Box(Modifier.fillMaxSize()) {
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        beyondViewportPageCount = 1,
    ) { page ->
        val meta = windows[page].mediaItem.metadata
        val isCurrent = page == currentIndex
        Box(
            Modifier
                .fillMaxSize()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (isCurrent) playerConnection.togglePlayPause() },
                    onDoubleClick = { if (isCurrent && !liked) playerConnection.toggleLike() },
                ),
        ) {
            val art = meta?.thumbnailUrl?.resize(800, 800)
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(48.dp).alpha(0.55f),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(0.35f), Color.Transparent, Color.Black.copy(0.85f)),
                    ),
                ),
            )
            AsyncImage(
                model = art,
                contentDescription = meta?.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            if (isCurrent && !isPlaying) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White.copy(0.9f),
                    modifier = Modifier.align(Alignment.Center).size(72.dp),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                    .padding(start = 20.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        meta?.title.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        meta?.artists?.joinToString { it.name }.orEmpty(),
                        color = Color.White.copy(0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isCurrent) {
                        Slider(
                            value = progress,
                            onValueChange = { dragging = true; progress = it },
                            onValueChangeFinished = {
                                val d = playerConnection.player.duration
                                if (d > 0) playerConnection.seekTo((progress * d).toLong())
                                dragging = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(0.25f),
                            ),
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        )
                    }
                }
                if (isCurrent) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { playerConnection.toggleLike() }) {
                            Icon(
                                painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                                contentDescription = null,
                                tint = if (liked) Color(0xFF1DB954) else Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        IconButton(onClick = {
                            meta?.id?.let { id ->
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$id")
                                }
                                context.startActivity(Intent.createChooser(send, null))
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        IconButton(onClick = {
                            meta?.let { m ->
                                scope.launch(Dispatchers.IO) { taste.recordEvent(m, SignalKind.DISLIKE) }
                                playerConnection.player.seekToNextMediaItem()
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Not interested",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    IconButton(
        onClick = {
            // Fresh batch from the latest taste data, seeded with what is playing now as a fallback.
            playerConnection.service.startPersonalFeed(currentMeta?.id)
        },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .padding(end = 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.refresh),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }

    AnimatedVisibility(
        visible = !hintShown,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
    ) {
        val t = rememberInfiniteTransition(label = "swipeHint")
        val lift by t.animateFloat(
            initialValue = 0f,
            targetValue = -18f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "lift",
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationY = lift.dp.toPx() },
        ) {
            Icon(
                painter = painterResource(R.drawable.expand_less),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
            Text("Swipe up", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
    }
}
