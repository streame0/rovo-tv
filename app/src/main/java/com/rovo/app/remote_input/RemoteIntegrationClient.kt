package com.rovo.app.remote_input

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RemoteIntegrationClient(
    private val baseUrl: String = RemoteConfig.BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var pollingJob: Job? = null
    private var pairingCode: String? = null

    private val _credentials = MutableStateFlow<Credentials?>(null)
    val credentials: StateFlow<Credentials?> = _credentials

    data class Credentials(val email: String, val password: String)
    data class SessionInfo(val pairingCode: String, val pairingUrl: String, val expiresAt: Long)

    suspend fun createSession(): SessionInfo? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().put("type", "stremio").toString()
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/api/pair")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val data = JSONObject(response.body!!.string())
            val code = data.getString("pairingCode")
            pairingCode = code
            SessionInfo(
                pairingCode = code,
                pairingUrl = data.getString("pairingUrl"),
                expiresAt = data.getLong("expiresAt")
            )
        } catch (e: Exception) {
            if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("RemoteIntegrationClient", "Failed to create session", e)
            null
        }
    }

    fun startPolling(onCredentialsReceived: (email: String, password: String) -> Unit) {
        val code = pairingCode ?: return
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            val deadline = System.currentTimeMillis() + RemoteConfig.SESSION_TIMEOUT_MS
            while (isActive && System.currentTimeMillis() < deadline) {
                try {
                    val request = Request.Builder()
                        .url("$baseUrl/api/pair/$code/pending")
                        .get()
                        .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val data = JSONObject(response.body!!.string())
                        if (!data.isNull("login")) {
                            val login = data.getJSONObject("login")
                            val id = login.getString("id")
                            val email = login.getString("email")
                            val password = login.getString("password")

                            onCredentialsReceived(email, password)

                            val ackJson = JSONObject().apply {
                                put("type", "login")
                                put("id", id)
                            }
                            val ackBody = ackJson.toString().toRequestBody("application/json".toMediaType())
                            val ackRequest = Request.Builder()
                                .url("$baseUrl/api/pair/$code/ack")
                                .post(ackBody)
                                .build()
                            client.newCall(ackRequest).execute()
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("RemoteIntegrationClient", "Poll failed", e)
                }
                delay(RemoteConfig.POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        pairingCode = null
    }
}
