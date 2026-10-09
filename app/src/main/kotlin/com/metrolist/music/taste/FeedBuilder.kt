package com.metrolist.music.taste

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.media3.common.MediaItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.RadioFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Builds a batch of songs for the Scroll feed from on-device taste data:
 * ~70% taste match, ~20% explore (unseen artists), ~10% trending. Explore grows to 40% after a
 * skip streak. Offline (or when the network fetch fails) it ranks downloaded + liked songs instead.
 */
class FeedBuilder(private val engine: TasteEngine) {
    private data class Candidate(val item: SongItem?, val song: Song?, val f: SongFeatures) {
        fun asMediaItem(): MediaItem = item?.toMediaItem() ?: song!!.toMediaItem()
    }

    private var trendingCache: Pair<Long, List<SongItem>>? = null

    suspend fun nextBatch(
        context: Context,
        size: Int,
        exclude: Set<String>,
        fallbackSeedId: String? = null,
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val blocked = engine.blockedIds() + exclude
        val online = isOnline(context)
        val batch = if (online) onlineBatch(size, blocked, fallbackSeedId) else emptyList()
        val result = batch.ifEmpty { offlineBatch(size, blocked, exclude) }
        result.map { it.asMediaItem() }
    }

    private suspend fun onlineBatch(size: Int, blocked: Set<String>, fallbackSeedId: String?): List<Candidate> = coroutineScope {
        val seeds = engine.seedSongIds().ifEmpty { listOfNotNull(fallbackSeedId) }
        val radios = seeds.map { seed ->
            async { runCatching { YouTube.next(WatchEndpoint(videoId = seed, playlistId = "RDAMVM$seed")).getOrNull()?.items.orEmpty() }.getOrDefault(emptyList()) }
        }
        val trendingJob = async { trending() }
        val radio = RadioFilter.filter(radios.awaitAll().flatten(), blocked + seeds)
        val trendingItems = RadioFilter.filter(trendingJob.await(), blocked + radio.map { it.id })
        if (radio.isEmpty() && trendingItems.isEmpty()) return@coroutineScope emptyList()
        mix(radio.map { Candidate(it, null, featuresOf(it)) }, trendingItems.map { Candidate(it, null, featuresOf(it)) }, size)
    }

    private suspend fun offlineBatch(size: Int, blocked: Set<String>, hardExclude: Set<String>): List<Candidate> {
        var pool = engine.offlineCandidates().filter { it.id !in blocked }
        // Everything was recently heard: relax to "just not already queued / disliked".
        if (pool.isEmpty()) pool = engine.offlineCandidates().filter { it.id !in hardExclude }
        return mix(pool.map { Candidate(null, it, TasteEngine.featuresOf(it)) }, emptyList(), size)
    }

    private suspend fun mix(pool: List<Candidate>, trending: List<Candidate>, size: Int): List<Candidate> {
        val scores = engine.scoreAll(pool.map { it.f } + trending.map { it.f })
        val unseen = engine.unseenArtists(pool.map { it.f })
        val exploreRatio = if (engine.skipStreak() >= 3) 0.4 else 0.2

        // Anything the user clearly turned against (-2 or lower) is dropped from the taste lists.
        val eligible = pool.filter { (scores[it.f.id] ?: 0.0) > -2.0 }
        val exploreList = eligible.filter { it.f.id in unseen }.shuffled()
        val tasteList = eligible.filter { it.f.id !in unseen }.sortedByDescending { (scores[it.f.id] ?: 0.0) + Math.random() * 0.8 }

        val exploreN = (size * exploreRatio).roundToInt()
        val trendN = if (trending.isEmpty()) 0 else (size * 0.1).roundToInt().coerceAtLeast(1)
        val tasteN = size - exploreN - trendN

        val taste = tasteList.take(tasteN).toMutableList()
        val explore = exploreList.take(exploreN).toMutableList()
        val trend = trending.filter { (scores[it.f.id] ?: 0.0) > -2.0 }.shuffled().take(trendN).toMutableList()

        // Shortfalls are filled from whatever is left, best taste match first.
        var missing = size - taste.size - explore.size - trend.size
        if (missing > 0) {
            val used = (taste + explore + trend).map { it.f.id }.toSet()
            val rest = (tasteList + exploreList).filter { it.f.id !in used }.take(missing)
            taste += rest
            missing -= rest.size
        }
        return interleave(taste, explore, trend)
    }

    /** Spreads explore/trending picks through the batch instead of clumping them at the end. */
    private fun interleave(taste: MutableList<Candidate>, explore: MutableList<Candidate>, trend: MutableList<Candidate>): List<Candidate> {
        val out = ArrayList<Candidate>()
        while (taste.isNotEmpty() || explore.isNotEmpty() || trend.isNotEmpty()) {
            val total = taste.size + explore.size + trend.size
            val r = Math.random() * total
            val src = when {
                r < taste.size -> taste
                r < taste.size + explore.size -> explore
                else -> trend
            }
            // The very first card should be a taste match when we have one.
            val pick = if (out.isEmpty() && taste.isNotEmpty()) taste else src
            out += pick.removeAt(0)
        }
        return out
    }

    private suspend fun trending(): List<SongItem> {
        val cached = trendingCache
        if (cached != null && System.currentTimeMillis() - cached.first < 30 * 60_000L) return cached.second
        val items = runCatching {
            YouTube.getChartsPage().getOrNull()?.sections.orEmpty().flatMap { it.items }.filterIsInstance<SongItem>()
        }.getOrDefault(emptyList())
        if (items.isNotEmpty()) trendingCache = System.currentTimeMillis() to items
        return items
    }

    private fun featuresOf(item: SongItem) = SongFeatures(
        id = item.id,
        artists = item.artists.map { it.name.trim().lowercase() },
        language = TasteEngine.detectLanguage(item.title + " " + item.artists.joinToString(" ") { it.name }),
        era = null,
    )

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
