package com.metrolist.music.access

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.accessDataStore by preferencesDataStore(name = "access")

data class AccessSnapshot(
    val accessId: String,
    val approved: Boolean,
    val lastVerifiedAt: Long,
    val expiresAt: Long,
    val pendingSeconds: Int,
)

/** Separate DataStore ("access") so it never mixes with the app's settings. */
class AccessStore(context: Context) {
    private val ds = context.applicationContext.accessDataStore

    private val idKey = stringPreferencesKey("access_id")
    private val approvedKey = booleanPreferencesKey("approved")
    private val verifiedKey = longPreferencesKey("last_verified_at")
    private val expiresKey = longPreferencesKey("expires_at")
    private val pendingKey = longPreferencesKey("pending_seconds")

    suspend fun read(): AccessSnapshot {
        val p = ds.data.first()
        return AccessSnapshot(
            accessId = p[idKey] ?: "",
            approved = p[approvedKey] ?: false,
            lastVerifiedAt = p[verifiedKey] ?: 0L,
            expiresAt = p[expiresKey] ?: 0L,
            pendingSeconds = (p[pendingKey] ?: 0L).toInt(),
        )
    }

    suspend fun saveApproved(id: String, expiresAt: Long) {
        ds.edit {
            it[idKey] = id
            it[approvedKey] = true
            it[verifiedKey] = System.currentTimeMillis()
            it[expiresKey] = expiresAt
        }
    }

    suspend fun markVerified(expiresAt: Long) {
        ds.edit {
            it[verifiedKey] = System.currentTimeMillis()
            it[expiresKey] = expiresAt
        }
    }

    /** Lock the app but keep the id so the user can retry without retyping. */
    suspend fun lock() {
        ds.edit { it[approvedKey] = false }
    }

    suspend fun logout() {
        ds.edit {
            it.remove(idKey)
            it[approvedKey] = false
            it.remove(verifiedKey)
            it.remove(expiresKey)
            it.remove(pendingKey)
        }
    }

    suspend fun setPending(seconds: Int) {
        ds.edit { it[pendingKey] = seconds.toLong() }
    }
}
