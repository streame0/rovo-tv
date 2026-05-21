package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the remote pairing session for image uploads.
 * Replaces the local ImageUploadServer with a cloud-hosted pairing service.
 */
class ImageUploadServerManager(private val context: android.content.Context) {

    private var remoteClient: RemoteImageUploadClient? = null

    /**
     * Creates a remote pairing session on the Rovo server.
     * Returns [ServerInfo] with the pairing URL for QR code generation.
     */
    suspend fun startServer(
        tempFolder: File,
        onImageUploaded: (File) -> Unit
    ): ServerInfo? = withContext(Dispatchers.IO) {
        try {
            val client = RemoteImageUploadClient()
            val session = client.createSession() ?: return@withContext null

            client.startPolling(tempFolder) { file ->
                onImageUploaded(file)
            }
            remoteClient = client

            ServerInfo(
                ip = "",
                port = 0,
                pairingUrl = session.pairingUrl
            )
        } catch (e: Exception) {
            if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("ImageUploadServerManager", "Failed to start remote session", e)
            null
        }
    }

    fun stopServer() {
        remoteClient?.stopPolling()
        remoteClient = null
    }
}
