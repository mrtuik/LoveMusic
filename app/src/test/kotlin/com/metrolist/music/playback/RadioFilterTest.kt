/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioFilterTest {
    private fun song(
        id: String,
        duration: Int? = 200,
        type: String? = null,
        episode: Boolean = false,
    ) = SongItem(
        id = id,
        title = "t$id",
        artists = listOf(Artist(name = "a", id = null)),
        duration = duration,
        musicVideoType = type,
        thumbnail = "",
        isEpisode = episode,
    )

    @Test
    fun removesDuplicates() {
        val result = RadioFilter.filter(listOf(song("a"), song("b"), song("a")))
        assertEquals(listOf("a", "b"), result.map { it.id })
    }

    @Test
    fun removesPlayedAndQueuedIds() {
        val result = RadioFilter.filter(listOf(song("a"), song("b"), song("c")), excludeIds = setOf("b"))
        assertEquals(listOf("a", "c"), result.map { it.id })
    }

    @Test
    fun removesEpisodesAndPodcasts() {
        val result =
            RadioFilter.filter(
                listOf(
                    song("a"),
                    song("e", episode = true),
                    song("p", type = "MUSIC_VIDEO_TYPE_PODCAST_EPISODE"),
                ),
            )
        assertEquals(listOf("a"), result.map { it.id })
    }

    @Test
    fun removesVideosLongerThanEightMinutes() {
        val result = RadioFilter.filter(listOf(song("ok", 480), song("long", 481), song("unknown", null)))
        assertEquals(listOf("ok", "unknown"), result.map { it.id })
    }

    @Test
    fun keepIdSurvivesExclusionAndDuration() {
        val result =
            RadioFilter.filter(
                listOf(song("seed", 900), song("x")),
                excludeIds = setOf("seed"),
                keepId = "seed",
            )
        assertEquals(listOf("seed", "x"), result.map { it.id })
    }

    @Test
    fun needsMoreWhenFewerThanTenAhead() {
        assertTrue(RadioFilter.needsMore(mediaItemCount = 20, currentIndex = 10)) // 9 ahead
        assertFalse(RadioFilter.needsMore(mediaItemCount = 21, currentIndex = 10)) // 10 ahead
    }
}
