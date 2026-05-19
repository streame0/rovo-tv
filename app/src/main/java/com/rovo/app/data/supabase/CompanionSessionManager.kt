package com.rovo.app.data.supabase

import android.util.Log
import com.rovo.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CompanionSessionManager {
    private const val TAG = "CompanionSession"
    const val BASE_URL = "https://rovo-so.netlify.app"
    private const val POLL_INTERVAL = 2000L
    private const val SESSION_TTL = 600_000L
    private const val MAX_RETRIES = 3
    private const val BASE_DELAY = 1000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json".toMediaType()

    private val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL

    private val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY

    suspend fun createSession(action: String, data: Map<String, Any>? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val code = retryWithBackoff {
                val code = buildString { repeat(6) { append(('A'..'Z').random()) } }
                val now = System.currentTimeMillis()

                val body = JSONObject().apply {
                    put("session_code", code)
                    put("action", action)
                    put("status", "pending")
                    put("data", data?.let { toJsonObject(it) })
                    put("created_at", now)
                    put("updated_at", now)
                    put("expires_at", now + SESSION_TTL)
                }

                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/companion_sessions")
                    .addHeader("apikey", anonKey)
                    .addHeader("Authorization", "Bearer $anonKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Create session failed: ${response.code} ${response.body?.string()}")
                }
                Log.i(TAG, "Created session $code for $action")
                code
            }
            Result.success(code)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create session", e)
            Result.failure(e)
        }
    }

    suspend fun pollSession(code: String): SessionStatus? = withContext(Dispatchers.IO) {
        retryWithBackoff<SessionStatus?>(allowNull = true) {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/companion_sessions?session_code=eq.$code&select=status,action,data&limit=1")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@retryWithBackoff null

            val json = response.body?.string() ?: return@retryWithBackoff null
            val arr = org.json.JSONArray(json)
            if (arr.length() == 0) return@retryWithBackoff null

            val obj = arr.getJSONObject(0)
            SessionStatus(
                status = obj.optString("status", "pending"),
                action = obj.optString("action", ""),
                data = obj.optJSONObject("data")?.let { parseData(it) }
            )
        } ?: null
    }

    fun observeSession(code: String): Flow<SessionStatus> = flow {
        var lastUpdatedAt = 0L
        while (true) {
            val session = pollSession(code)
            if (session != null) {
                emit(session)
                if (session.status == "completed" || session.status == "expired") return@flow
            }
            delay(POLL_INTERVAL)
        }
    }

    suspend fun deleteSession(code: String) = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/companion_sessions?session_code=eq.$code")
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .delete()
                .build()
            client.newCall(request).execute()
        } catch (_: Exception) {}
    }

    private suspend fun <T> retryWithBackoff(
        allowNull: Boolean = false,
        block: suspend () -> T
    ): T {
        var lastError: Exception? = null
        for (attempt in 0..MAX_RETRIES) {
            try {
                val result = block()
                if (!allowNull && result == null) throw Exception("Null result")
                return result
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_RETRIES) {
                    val delayMs = BASE_DELAY * (1 shl attempt)
                    Log.w(TAG, "Retry $attempt/${MAX_RETRIES} after ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                }
            }
        }
        throw lastError ?: Exception("Retry exhausted")
    }

    private fun parseData(obj: JSONObject): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for (key in obj.keys()) {
            val value = obj.opt(key)
            result[key] = when (value) {
                is JSONObject -> parseData(value)
                is org.json.JSONArray -> {
                    (0 until value.length()).map { i ->
                        val item = value.opt(i)
                        when (item) {
                            is JSONObject -> parseData(item)
                            else -> item
                        }
                    }
                }
                else -> value
            }
        }
        return result
    }

    private fun toJsonObject(data: Map<String, Any>): Any {
        val obj = JSONObject()
        for ((key, value) in data) {
            when (value) {
                is List<*> -> obj.put(key, org.json.JSONArray(value.map { toJsonValue(it) }))
                is Map<*, *> -> obj.put(key, toJsonObject(value as Map<String, Any>))
                else -> obj.put(key, value)
            }
        }
        return obj
    }

    private fun toJsonValue(value: Any?): Any? {
        return when (value) {
            is Map<*, *> -> toJsonObject(value as Map<String, Any>)
            is List<*> -> org.json.JSONArray(value.map { toJsonValue(it) })
            else -> value
        }
    }
}

data class SessionStatus(
    val status: String,
    val action: String,
    val data: Map<String, Any>?
)
