package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.BindException

/**
 * Manages the ImageUploadServer lifecycle.
 */
class ImageUploadServerManager {

    private var server: ImageUploadServer? = null

    companion object {
        private const val PORT_START = 8080
        private const val PORT_END = 8090
    }

    /**
     * Attempts to start the server on an available port.
     * Returns ServerInfo on success, null on failure.
     */
    suspend fun startServer(
        tempFolder: File,
        onImageUploaded: (File) -> Unit
    ): ServerInfo? = withContext(Dispatchers.IO) {
        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val imageServer = ImageUploadServer(
                    port = port,
                    tempFolder = tempFolder,
                    onImageUploaded = onImageUploaded
                )
                imageServer.start()
                server = imageServer
                return@withContext ServerInfo(ip ?: "127.0.0.1", port)
            } catch (e: BindException) {
                continue
            } catch (e: Exception) {
                if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("ImageUploadServerManager", "Port binding failed", e)
                continue
            }
        }

        null
    }

    /**
     * Stops the running server if any.
     */
    fun stopServer() {
        server?.stop()
        server = null
    }
}
