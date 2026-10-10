package com.metrolist.music.access

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AccessResult {
    data class Ok(val name: String, val expiresAt: Long) : AccessResult()
    data class Denied(val reason: String, val expiresAt: Long = 0L) : AccessResult()
    object NetworkError : AccessResult()
}

object AccessApi {
    // OkHttp follows the Apps Script 302 redirect (POST -> GET on the echo URL), which is what Apps Script expects.
    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun deviceHash(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        val raw = androidId + context.packageName
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun deviceModel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    suspend fun verify(context: Context, id: String): AccessResult = post(
        JSONObject()
            .put("action", "verify")
            .put("appKey", AccessConfig.APP_KEY)
            .put("id", id)
            .put("device", deviceHash(context))
            .put("model", deviceModel()),
    )

    suspend fun ping(context: Context, id: String, seconds: Int): AccessResult = post(
        JSONObject()
            .put("action", "ping")
            .put("appKey", AccessConfig.APP_KEY)
            .put("id", id)
            .put("device", deviceHash(context))
            .put("seconds", seconds),
    )

    private suspend fun post(body: JSONObject): AccessResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(AccessConfig.SCRIPT_URL)
                .post(body.toString().toRequestBody(JSON))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                val json = try { JSONObject(text) } catch (e: Exception) { return@use AccessResult.NetworkError }
                if (json.optBoolean("ok", false)) {
                    AccessResult.Ok(json.optString("name", ""), json.optLong("expiresAt", 0L))
                } else {
                    val reason = json.optString("reason", "server_error")
                    if (reason == "server_error") AccessResult.NetworkError
                    else AccessResult.Denied(reason, json.optLong("expiresAt", 0L))
                }
            }
        } catch (e: Exception) {
            AccessResult.NetworkError
        }
    }
}
