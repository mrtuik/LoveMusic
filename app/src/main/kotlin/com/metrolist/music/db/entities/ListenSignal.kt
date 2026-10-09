package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One taste signal (listen/skip/like/dislike/download/playlist-add). No FK so history survives song deletion. */
@Immutable
@Entity(
    tableName = "listen_signal",
    indices = [Index("songId"), Index("timestamp")],
)
data class ListenSignal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val kind: String,
    val score: Double,
    val timestamp: Long,
    @ColumnInfo(defaultValue = "0") val listenedMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val durationMs: Long = 0,
)
