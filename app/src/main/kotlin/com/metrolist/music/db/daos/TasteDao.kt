package com.metrolist.music.db.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.metrolist.music.db.entities.ListenSignal
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.TasteWeight

@Dao
interface TasteDao {
    @Insert
    suspend fun insertSignal(signal: ListenSignal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeights(weights: List<TasteWeight>)

    @Query("SELECT * FROM taste_weight WHERE dim = :dim AND `key` IN (:keys)")
    suspend fun getWeights(dim: String, keys: List<String>): List<TasteWeight>

    @Query("SELECT * FROM taste_weight WHERE dim = :dim")
    suspend fun getAllWeights(dim: String): List<TasteWeight>

    /** Most recent songs the user clearly liked / fully listened to, newest first. */
    @Query(
        "SELECT songId FROM listen_signal WHERE score >= :minScore " +
            "GROUP BY songId ORDER BY MAX(timestamp) DESC LIMIT :limit",
    )
    suspend fun recentPositiveSongIds(minScore: Double, limit: Int): List<String>

    @Query("SELECT DISTINCT songId FROM listen_signal WHERE kind = 'DISLIKE'")
    suspend fun dislikedSongIds(): List<String>

    @Query("SELECT DISTINCT songId FROM listen_signal WHERE kind = 'LISTEN' AND score < 0 AND timestamp >= :since")
    suspend fun recentlySkippedSongIds(since: Long): List<String>

    @Query("SELECT DISTINCT songId FROM listen_signal WHERE kind = 'LISTEN' AND timestamp >= :since")
    suspend fun recentlyHeardSongIds(since: Long): List<String>

    /** Scores of the latest LISTEN signals, newest first (for skip-streak / mood). */
    @Query("SELECT score FROM listen_signal WHERE kind = 'LISTEN' ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentListenScores(limit: Int): List<Double>

    @Query("SELECT COUNT(*) FROM listen_signal WHERE songId = :songId AND kind = 'LISTEN' AND score >= 0.5 AND timestamp >= :since")
    suspend fun goodListensSince(songId: String, since: Long): Int

    @Query("DELETE FROM listen_signal WHERE timestamp < :before")
    suspend fun pruneSignals(before: Long)

    @Query("DELETE FROM taste_weight WHERE ABS(weight) < 0.05 AND updatedAt < :before")
    suspend fun pruneWeights(before: Long)

    @Transaction
    @Query("SELECT * FROM song WHERE (liked = 1 OR isDownloaded = 1) AND isEpisode = 0")
    suspend fun likedOrDownloadedSongs(): List<Song>
}
