package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity

/** Decaying preference weight for one (dimension, key), e.g. ("artist", "arijit singh"). */
@Immutable
@Entity(tableName = "taste_weight", primaryKeys = ["dim", "key"])
data class TasteWeight(
    val dim: String,
    val key: String,
    val weight: Double,
    val updatedAt: Long,
)
