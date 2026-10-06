/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.innertube.models.SongItem

/**
 * Pure filter for "same vibe" radio recommendations (no Android dependencies, unit-testable).
 *
 * Drops: ids in [excludeIds] (already played this session / already queued), duplicates,
 * podcast episodes, and anything longer than [MAX_DURATION_SEC].
 */
object RadioFilter {
    /** Songs longer than this (8 min) are skipped. */
    const val MAX_DURATION_SEC = 480

    /** Keep at least this many songs ahead of the current index. */
    const val MIN_AHEAD = 10

    private const val TYPE_PODCAST_EPISODE = "MUSIC_VIDEO_TYPE_PODCAST_EPISODE"

    fun isMusicCandidate(
        item: SongItem,
        maxDurationSec: Int = MAX_DURATION_SEC,
    ): Boolean {
        if (item.isEpisode) return false
        if (item.musicVideoType == TYPE_PODCAST_EPISODE) return false
        val duration = item.duration
        if (duration != null && duration > maxDurationSec) return false
        return true
    }

    /**
     * @param keepId an id that is never dropped by [excludeIds] / duration rules (the seed song of
     * the radio, so the queue index stays valid). It is still de-duplicated.
     */
    fun filter(
        items: List<SongItem>,
        excludeIds: Set<String> = emptySet(),
        keepId: String? = null,
        maxDurationSec: Int = MAX_DURATION_SEC,
    ): List<SongItem> {
        val seen = HashSet<String>()
        val out = ArrayList<SongItem>(items.size)
        for (item in items) {
            if (!seen.add(item.id)) continue
            val isKeep = keepId != null && item.id == keepId
            if (!isKeep) {
                if (item.id in excludeIds) continue
                if (!isMusicCandidate(item, maxDurationSec)) continue
            }
            out += item
        }
        return out
    }

    /** True when fewer than [MIN_AHEAD] songs remain after [currentIndex]. */
    fun needsMore(
        mediaItemCount: Int,
        currentIndex: Int,
        minAhead: Int = MIN_AHEAD,
    ): Boolean = mediaItemCount - currentIndex - 1 < minAhead
}
