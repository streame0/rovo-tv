package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.BindException

/**
 * Manages the local ImageUploadServer for image uploads via QR code.
 * Runs a NanoHTTPD server on the local network.
 */
class ImageUploadServerManager(private val context: android.content.Context) {

    private var server: ImageUploadServer? = null
    private var nsdManager: ServiceDiscoveryManager? = null

    companion object {
        private const val PORT_START = 8091
        private const val PORT_END = 8100
    }

    /**
     * Starts a local server on an available port.
     * Returns [ServerInfo] with the local URL for QR code generation.
     */
    suspend fun startServer(
        tempFolder: File,
        onImageUploaded: (File) -> Unit
    ): ServerInfo? = withContext(Dispatchers.IO) {
        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val imageServer = ImageUploadServer(port, tempFolder) { file ->
                    onImageUploaded(file)
                }
                imageServer.start()
                server = imageServer

                // Register for local network discovery
                nsdManager = ServiceDiscoveryManager(context).apply {
                    registerService(port, "Rovo-TV-Image-${System.currentTimeMillis() % 1000}")
                }

                return@withContext ServerInfo(
                    ip = ip ?: "127.0.0.1",
                    port = port,
                    pin = imageServer.pin
                )
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
     * Stops the running server and unregisters NSD.
     */
    fun stopServer() {
        server?.stop()
        server = null
        nsdManager?.unregisterService()
        nsdManager = null
    }
}
