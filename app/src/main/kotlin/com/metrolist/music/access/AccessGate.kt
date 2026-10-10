package com.metrolist.music.access

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GateState {
    /** Store not read yet (a few ms at cold start). */
    object Loading : GateState()

    /** Show AccessScreen. [prefillId] is the saved id (if any), [message] the reason. */
    data class Locked(val prefillId: String = "", val message: String? = null) : GateState()

    object Open : GateState()
}

/**
 * Single source of truth for access state + the active-time monitor.
 * Initialised from App.onCreate so Android Auto / the service are gated even without the Activity.
 */
object AccessGate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<GateState>(GateState.Loading)
    val state: StateFlow<GateState> = _state.asStateFlow()

    @Volatile var isOpen: Boolean = false
        private set

    // Active-time tracking
    @Volatile var foreground: Boolean = false
    @Volatile var playing: Boolean = false

    private lateinit var appContext: Context
    private lateinit var store: AccessStore
    private var started = false

    @Volatile private var pendingSeconds = 0
    @Volatile private var currentId = ""
    @Volatile private var lastVerifiedAt = 0L
    @Volatile var expiresAt: Long = 0L
        private set

    fun init(context: Context) {
        if (started) return
        started = true
        appContext = context.applicationContext
        store = AccessStore(appContext)
        scope.launch {
            val s = store.read()
            currentId = s.accessId
            lastVerifiedAt = s.lastVerifiedAt
            expiresAt = s.expiresAt
            pendingSeconds = s.pendingSeconds
            if (s.approved && s.accessId.isNotBlank()) {
                setState(GateState.Open)
            } else {
                setState(GateState.Locked(s.accessId))
            }
            monitorLoop()
        }
    }

    private fun setState(s: GateState) {
        isOpen = s is GateState.Open
        _state.value = s
    }

    fun maskedId(): String {
        // TUIK-ABCD-EF12 -> TUIK-****-EF12
        val p = currentId.split("-")
        return if (p.size == 3) "${p[0]}-****-${p[2]}" else currentId
    }

    fun currentExpiry(): Long = expiresAt

    suspend fun submit(id: String): String? {
        val clean = id.trim().uppercase()
        if (clean.isEmpty()) return "ID is not valid"
        return when (val r = AccessApi.verify(appContext, clean)) {
            is AccessResult.Ok -> {
                store.saveApproved(clean, r.expiresAt)
                currentId = clean
                lastVerifiedAt = System.currentTimeMillis()
                expiresAt = r.expiresAt
                setState(GateState.Open)
                null
            }
            is AccessResult.Denied -> messageFor(r.reason)
            is AccessResult.NetworkError -> messageFor("network") + if (r.detail.isNotBlank()) " (${r.detail})" else ""
        }
    }

    fun logout() {
        scope.launch {
            store.logout()
            currentId = ""
            pendingSeconds = 0
            expiresAt = 0L
            setState(GateState.Locked(""))
        }
    }

    private fun lock(reason: String) {
        scope.launch {
            store.lock()
            setState(GateState.Locked(currentId, messageFor(reason)))
        }
    }

    fun messageFor(reason: String): String = when (reason) {
        "invalid_id" -> "ID is not valid"
        "blocked" -> "This ID has been blocked"
        "device_limit" -> "This ID is already used on 2 devices"
        "device_removed" -> "This device was removed from your ID. Enter your ID again"
        "expired" -> "This ID has expired"
        "unauthorized" -> "App key mismatch, contact @mrtuik"
        "too_many_attempts" -> "Too many attempts, try again in 10 minutes"
        "offline_grace" -> "Connect to internet to verify"
        else -> "No internet / server problem, try again"
    }

    /** Ticks every 5s; counts active time; pings every PING_INTERVAL_SEC of active time. */
    private suspend fun monitorLoop() {
        var activeSinceLastPing = 0
        var firstPing = true
        var lastTick = System.currentTimeMillis()
        while (true) {
            delay(5_000)
            val now = System.currentTimeMillis()
            val elapsed = ((now - lastTick) / 1000).toInt().coerceIn(0, 30)
            lastTick = now
            if (!isOpen) continue
            if (foreground || playing) {
                pendingSeconds += elapsed
                activeSinceLastPing += elapsed
            }
            if (firstPing || activeSinceLastPing >= AccessConfig.PING_INTERVAL_SEC) {
                firstPing = false
                activeSinceLastPing = 0
                sendPing()
            }
        }
    }

    private suspend fun sendPing() {
        val send = pendingSeconds.coerceAtMost(AccessConfig.MAX_SECONDS_PER_PING)
        when (val r = AccessApi.ping(appContext, currentId, send)) {
            is AccessResult.Ok -> {
                pendingSeconds = (pendingSeconds - send).coerceAtLeast(0)
                lastVerifiedAt = System.currentTimeMillis()
                expiresAt = r.expiresAt
                store.markVerified(r.expiresAt)
                store.setPending(pendingSeconds)
            }
            is AccessResult.Denied -> {
                when (r.reason) {
                    "blocked", "device_removed", "invalid_id", "expired" -> lock(r.reason)
                    else -> Unit
                }
            }
            is AccessResult.NetworkError -> {
                store.setPending(pendingSeconds)
                if (lastVerifiedAt > 0 &&
                    System.currentTimeMillis() - lastVerifiedAt > AccessConfig.OFFLINE_GRACE_MS
                ) {
                    lock("offline_grace")
                }
            }
        }
    }
}
