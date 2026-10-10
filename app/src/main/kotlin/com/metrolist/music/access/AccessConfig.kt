package com.metrolist.music.access

import com.metrolist.music.BuildConfig

/**
 * Backend config. Values come from BuildConfig (local.properties or env vars at build time).
 * Never log these.
 */
object AccessConfig {
    val SCRIPT_URL: String = BuildConfig.ACCESS_SCRIPT_URL
    val APP_KEY: String = BuildConfig.ACCESS_APP_KEY

    const val PING_INTERVAL_SEC = 120
    const val MAX_SECONDS_PER_PING = 600
    const val OFFLINE_GRACE_MS = 7L * 24 * 60 * 60 * 1000
}
