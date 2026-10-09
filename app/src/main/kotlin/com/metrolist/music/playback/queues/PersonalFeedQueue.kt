package com.metrolist.music.playback.queues

import android.content.Context
import androidx.media3.common.MediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.taste.FeedBuilder

/**
 * Endless taste-based queue for the Scroll feed. [MusicService] refills non-radio queues when
 * <= 5 songs remain, so every refill re-reads the latest taste data and builds a fresh batch.
 */
class PersonalFeedQueue(
    private val context: Context,
    private val builder: FeedBuilder,
    private val fallbackSeedId: String? = null,
    private val batchSize: Int = 12,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private val served = HashSet<String>()

    override suspend fun getInitialStatus(): Queue.Status {
        val items = fetch()
        return Queue.Status(title = null, items = items, mediaItemIndex = 0)
    }

    override fun hasNextPage(): Boolean = true

    override suspend fun nextPage(): List<MediaItem> = fetch()

    private suspend fun fetch(): List<MediaItem> {
        val items = builder.nextBatch(context, batchSize, served, fallbackSeedId)
        items.forEach { served.add(it.mediaId) }
        return items
    }
}
