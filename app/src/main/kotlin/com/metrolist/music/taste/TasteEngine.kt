package com.metrolist.music.taste

import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ListenSignal
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.TasteWeight
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import kotlin.math.pow

/** Taste-relevant view of a song. [era] is a decade (e.g. 2010) and only known when a release year exists. */
data class SongFeatures(
    val id: String,
    val artists: List<String>,
    val language: String,
    val era: Int?,
)

enum class SignalKind(val score: Double) {
    LISTEN(0.0),
    LIKE(2.0),
    UNLIKE(-1.5),
    DISLIKE(-3.0),
    DOWNLOAD(3.0),
    PLAYLIST_ADD(2.5),
}

/**
 * On-device, rule-based taste model. Every signal is stored in `listen_signal`; per-artist /
 * language / era / time-of-day preferences live in `taste_weight` and decay with a half-life so a
 * new mood overrides old habits.
 */
class TasteEngine private constructor(private val database: MusicDatabase) {
    private val dao = database.tasteDao
    private val writeLock = Mutex()
    private var writeCount = 0

    companion object {
        const val DIM_ARTIST = "artist"
        const val DIM_LANG = "lang"
        const val DIM_ERA = "era"
        const val DIM_TIME_ARTIST = "time_artist"

        private const val HALF_LIFE_MS = 14.0 * 24 * 60 * 60 * 1000
        private const val MAX_WEIGHT = 10.0
        private const val SKIP_MS = 10_000L
        private const val SIGNAL_RETENTION_MS = 120L * 24 * 60 * 60 * 1000

        @Volatile
        private var instance: TasteEngine? = null

        fun get(database: MusicDatabase): TasteEngine =
            instance ?: synchronized(this) {
                instance ?: TasteEngine(database).also { instance = it }
            }

        fun featuresOf(meta: MediaMetadata): SongFeatures =
            SongFeatures(
                id = meta.id,
                artists = meta.artists.map { it.name.trim().lowercase() }.filter { it.isNotEmpty() },
                language = detectLanguage(meta.title + " " + meta.artists.joinToString(" ") { it.name }),
                era = null,
            )

        fun featuresOf(song: Song): SongFeatures =
            SongFeatures(
                id = song.id,
                artists = song.artists.map { it.name.trim().lowercase() },
                language = detectLanguage(song.title + " " + song.artists.joinToString(" ") { it.name }),
                era = song.song.year?.let { it / 10 * 10 },
            )

        /** Script-based guess. Romanized Bangla/Hindi can't be told apart from English, so they land in "latin". */
        fun detectLanguage(text: String): String {
            val counts = HashMap<String, Int>()
            for (ch in text) {
                val block = Character.UnicodeBlock.of(ch) ?: continue
                val lang = when (block) {
                    Character.UnicodeBlock.BENGALI -> "bn"
                    Character.UnicodeBlock.DEVANAGARI -> "hi"
                    Character.UnicodeBlock.TAMIL -> "ta"
                    Character.UnicodeBlock.TELUGU -> "te"
                    Character.UnicodeBlock.GURMUKHI -> "pa"
                    Character.UnicodeBlock.HANGUL_SYLLABLES, Character.UnicodeBlock.HANGUL_JAMO -> "ko"
                    Character.UnicodeBlock.HIRAGANA, Character.UnicodeBlock.KATAKANA -> "ja"
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS -> "zh"
                    Character.UnicodeBlock.ARABIC -> "ar"
                    Character.UnicodeBlock.CYRILLIC -> "ru"
                    Character.UnicodeBlock.BASIC_LATIN, Character.UnicodeBlock.LATIN_1_SUPPLEMENT -> if (ch.isLetter()) "latin" else null
                    else -> null
                } ?: continue
                counts[lang] = (counts[lang] ?: 0) + 1
            }
            val nonLatin = counts.filterKeys { it != "latin" }.maxByOrNull { it.value }
            return nonLatin?.key ?: romanizedLanguage(text)
        }

        private val BANGLA_WORDS = setOf(
            "ami", "tumi", "tomar", "amar", "tomake", "amake", "tomay", "amay", "bhalobasha", "bhalobashi",
            "valobasha", "valobashi", "bhalobese", "keno", "kothay", "nai", "moner", "bondhu", "jibon",
            "chokh", "shopno", "kemon", "achi", "ache", "cholo", "hobe", "amra", "tomra", "tui", "tor",
            "ekhon", "jodi", "tobe", "kintu", "brishti", "megh", "nodi", "pakhi", "rater", "bhor", "sondhya",
            "ekta", "ekti", "gaan", "praner", "dekhi", "bolo", "bol", "jaw", "jabo", "thakbo", "shono", "shunbo",
        )
        private val HINDI_WORDS = setOf(
            "tum", "tumhe", "tumhari", "tumhara", "mera", "meri", "mujhe", "main", "hum", "humko", "tera",
            "teri", "tujhe", "tujhse", "tumse", "pyaar", "pyar", "ishq", "mohabbat", "dil", "zindagi", "sapna",
            "kyun", "kyu", "kaise", "kahan", "nahi", "nahin", "hai", "hain", "tha", "thi", "bina", "saath",
            "raat", "aankhen", "ankhein", "yaar", "dost", "kabhi", "abhi", "phir", "mere", "humsafar", "sanam",
            "judaai", "barsaat", "jaan", "deewana", "aashiq", "tere", "teray", "kuch", "sab", "bhi", "woh", "wo",
        )

        /** Banglish -> "bn", Hinglish -> "hi" (shared with the native-script weights); needs a clear winner. */
        fun romanizedLanguage(text: String): String {
            val words = text.lowercase().split(Regex("[^a-z]+")).filter { it.length > 1 }
            val bn = words.count { it in BANGLA_WORDS }
            val hi = words.count { it in HINDI_WORDS }
            return when {
                bn > hi -> "bn"
                hi > bn -> "hi"
                else -> "latin"
            }
        }

        fun timeBucket(now: Long = System.currentTimeMillis()): String {
            val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..10 -> "morning"
                in 11..16 -> "day"
                in 17..21 -> "evening"
                else -> "night"
            }
        }

        private fun decayed(w: TasteWeight, now: Long): Double =
            w.weight * 0.5.pow((now - w.updatedAt).coerceAtLeast(0) / HALF_LIFE_MS)

        /** Score of one finished listen. Skip < 10s = negative, full play/repeat = strongly positive. */
        fun listenScore(listenedMs: Long, durationMs: Long): Double {
            if (durationMs <= 0) return if (listenedMs < SKIP_MS) -1.0 else 0.5
            val f = listenedMs.toDouble() / durationMs
            return when {
                listenedMs < SKIP_MS && f < 0.9 -> -1.0
                f >= 1.5 -> 1.5
                f >= 0.9 -> 1.0
                f < 0.3 -> -0.4
                else -> (f - 0.3) / 0.6 * 0.8
            }
        }
    }

    suspend fun recordListen(meta: MediaMetadata, listenedMs: Long) {
        if (meta.isEpisode) return
        val durationMs = meta.duration * 1000L
        var score = listenScore(listenedMs, durationMs)
        // Same song again and again = "this is my mood right now".
        if (score >= 0.5 && dao.goodListensSince(meta.id, System.currentTimeMillis() - 6 * 3_600_000L) >= 1) {
            score += 0.75
        }
        record(featuresOf(meta), SignalKind.LISTEN, score, listenedMs, durationMs)
    }

    suspend fun recordEvent(meta: MediaMetadata, kind: SignalKind) {
        if (meta.isEpisode) return
        record(featuresOf(meta), kind, kind.score, 0, meta.duration * 1000L)
    }

    suspend fun recordEventById(songId: String, kind: SignalKind) {
        database.getSongById(songId)?.let { recordEvent(it, kind) }
    }

    suspend fun recordEvent(song: Song, kind: SignalKind) {
        if (song.song.isEpisode) return
        record(featuresOf(song), kind, kind.score, 0, song.song.duration * 1000L)
    }

    private suspend fun record(f: SongFeatures, kind: SignalKind, score: Double, listenedMs: Long, durationMs: Long) {
        val now = System.currentTimeMillis()
        writeLock.withLock {
            dao.insertSignal(ListenSignal(songId = f.id, kind = kind.name, score = score, timestamp = now, listenedMs = listenedMs, durationMs = durationMs))
            val bucket = timeBucket(now)
            val deltas = ArrayList<Triple<String, String, Double>>()
            f.artists.forEachIndexed { i, a ->
                val share = if (i == 0) 1.0 else 0.5
                deltas += Triple(DIM_ARTIST, a, score * share)
                deltas += Triple(DIM_TIME_ARTIST, "$bucket|$a", score * 0.5 * share)
            }
            deltas += Triple(DIM_LANG, f.language, score * 0.5)
            f.era?.let { deltas += Triple(DIM_ERA, it.toString(), score * 0.3) }
            applyDeltas(deltas, now)
            if (++writeCount % 50 == 0) {
                dao.pruneSignals(now - SIGNAL_RETENTION_MS)
                dao.pruneWeights(now - 60L * 24 * 60 * 60 * 1000)
            }
        }
    }

    private suspend fun applyDeltas(deltas: List<Triple<String, String, Double>>, now: Long) {
        val out = ArrayList<TasteWeight>()
        for ((dim, group) in deltas.groupBy { it.first }) {
            val existing = dao.getWeights(dim, group.map { it.second }.distinct()).associateBy { it.key }
            val merged = HashMap<String, Double>()
            for ((_, key, delta) in group) {
                val base = merged[key] ?: existing[key]?.let { decayed(it, now) } ?: 0.0
                merged[key] = (base + delta).coerceIn(-MAX_WEIGHT, MAX_WEIGHT)
            }
            merged.forEach { (k, w) -> out += TasteWeight(dim, k, w, now) }
        }
        if (out.isNotEmpty()) dao.upsertWeights(out)
    }

    /** Taste score per song id, using decayed weights and the current time of day. Higher = better match. */
    suspend fun scoreAll(songs: List<SongFeatures>): Map<String, Double> {
        if (songs.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        val bucket = timeBucket(now)
        val artists = songs.flatMap { it.artists }.distinct()
        val artistW = if (artists.isEmpty()) emptyMap() else dao.getWeights(DIM_ARTIST, artists).associate { it.key to decayed(it, now) }
        val timeW = if (artists.isEmpty()) emptyMap() else dao.getWeights(DIM_TIME_ARTIST, artists.map { "$bucket|$it" }).associate { it.key to decayed(it, now) }
        val langW = dao.getAllWeights(DIM_LANG).associate { it.key to decayed(it, now) }
        val eraW = dao.getAllWeights(DIM_ERA).associate { it.key to decayed(it, now) }
        return songs.associate { s ->
            val a = s.artists.maxOfOrNull { artistW[it] ?: 0.0 } ?: 0.0
            val t = s.artists.maxOfOrNull { timeW["$bucket|$it"] ?: 0.0 } ?: 0.0
            val l = langW[s.language] ?: 0.0
            val e = s.era?.let { eraW[it.toString()] } ?: 0.0
            s.id to (a + 0.5 * t + 0.5 * l + 0.3 * e)
        }
    }

    /** True when we have never seen this artist (candidate for the "explore" slice). */
    suspend fun unseenArtists(songs: List<SongFeatures>): Set<String> {
        val artists = songs.flatMap { it.artists }.distinct()
        if (artists.isEmpty()) return emptySet()
        val known = dao.getWeights(DIM_ARTIST, artists).map { it.key }.toSet()
        return songs.filter { s -> s.artists.isNotEmpty() && s.artists.none { it in known } }.map { it.id }.toSet()
    }

    /** Last 2-4 songs the user liked / listened to fully, newest first. */
    suspend fun seedSongIds(): List<String> {
        // Latest positive song always seeds; the rest are random picks from a bigger recent pool,
        // so every batch explores a different corner of the user's taste (no same-4-seeds loop).
        val pool = dao.recentPositiveSongIds(0.8, 30)
        if (pool.size <= 4) return pool
        return listOf(pool.first()) + pool.drop(1).shuffled().take(3)
    }

    /** Only songs the user explicitly disliked. Used when the normal block list leaves nothing. */
    suspend fun dislikedOnlyIds(): Set<String> = dao.dislikedSongIds().toSet()

    /** Songs that must not show up again: disliked forever, skipped in 14d, heard in 3d. */
    suspend fun blockedIds(): Set<String> {
        val now = System.currentTimeMillis()
        val day = 24L * 3_600_000L
        return (dao.dislikedSongIds() + dao.recentlySkippedSongIds(now - 14 * day) + dao.recentlyHeardSongIds(now - 3 * day)).toSet()
    }

    /** Number of consecutive skips at the head of the history ("this type isn't working"). */
    suspend fun skipStreak(): Int = dao.recentListenScores(8).takeWhile { it < 0 }.size

    suspend fun offlineCandidates(): List<Song> = dao.likedOrDownloadedSongs()
}
