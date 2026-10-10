package com.metrolist.music.playback.queues

import android.content.Context
import androidx.media3.common.MediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.taste.FeedBuilder

/**
 * Endless taste-based queue for the Scroll feed. [MusicService] refills non-radio queues when
 * few songs remain, so every refill re-reads the latest taste data and builds a fresh batch.
 * It never reports "no more": [FeedBuilder] relaxes its filters step by step instead of giving up.
 */
class PersonalFeedQueue(
    private val context: Context,
    private val builder: FeedBuilder,
    private val fallbackSeedId: String? = null,
    private val batchSize: Int = 12,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private val served = HashSet<String>()

    /** Songs currently in the player queue (set by MusicService before each refill). */
    @Volatile var queuedIds: Set<String> = emptySet()

    /** Songs already played this session (set by MusicService before each refill). */
    @Volatile var playedIds: Set<String> = emptySet()

    /** Last queued song: its radio continues the current vibe. */
    @Volatile var seedHint: String? = null

    override suspend fun getInitialStatus(): Queue.Status {
        val items = fetch()
        return Queue.Status(title = null, items = items, mediaItemIndex = 0)
    }

    override fun hasNextPage(): Boolean = true

    override suspend fun nextPage(): List<MediaItem> = fetch()

    private suspend fun fetch(): List<MediaItem> {
        val items = builder.nextBatch(
            context = context,
            size = batchSize,
            served = HashSet(served),
            queued = queuedIds,
            played = playedIds,
            fallbackSeedId = seedHint ?: fallbackSeedId,
        )
        items.forEach { served.add(it.mediaId) }
        return items
    }
}
