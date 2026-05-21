package com.rovo.app.remote_input

import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class RemoteImageUploadClient(
    private val baseUrl: String = RemoteConfig.BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var pollingJob: Job? = null
    private var pairingCode: String? = null

    data class SessionInfo(val pairingCode: String, val pairingUrl: String, val expiresAt: Long)

    suspend fun createSession(): SessionInfo? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().put("type", "upload").toString()
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
            if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("RemoteImageUploadClient", "Failed to create session", e)
            null
        }
    }

    fun startPolling(
        tempFolder: File,
        onImageUploaded: (File) -> Unit
    ) {
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
                        val images = data.optJSONArray("images")
                        if (images != null && images.length() > 0) {
                            for (i in 0 until images.length()) {
                                val img = images.getJSONObject(i)
                                val id = img.getString("id")
                                val filename = img.optString("filename", "upload.jpg")
                                val downloadUrl = img.getString("url")

                                downloadImage(downloadUrl, tempFolder, filename)?.let { file ->
                                    onImageUploaded(file)
                                }

                                val ackJson = JSONObject().apply {
                                    put("type", "image")
                                    put("id", id)
                                }
                                val ackBody = ackJson.toString().toRequestBody("application/json".toMediaType())
                                val ackRequest = Request.Builder()
                                    .url("$baseUrl/api/pair/$code/ack")
                                    .post(ackBody)
                                    .build()
                                client.newCall(ackRequest).execute()
                            }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("RemoteImageUploadClient", "Poll failed", e)
                }
                delay(RemoteConfig.POLL_INTERVAL_MS)
            }
        }
    }

    private fun downloadImage(url: String, tempFolder: File, filename: String): File? {
        return try {
            val fullUrl = if (url.startsWith("http")) url else "$baseUrl$url"
            val request = Request.Builder().url(fullUrl).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val targetFile = File(tempFolder, "upload_${System.currentTimeMillis()}_$filename")
            response.body!!.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (e: Exception) {
            if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("RemoteImageUploadClient", "Download failed", e)
            null
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        pairingCode = null
    }
}
