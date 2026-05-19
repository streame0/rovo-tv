package com.rovo.app.remote_input

import com.rovo.app.domain.HubShape
import fi.iki.elonen.NanoHTTPD
import java.net.BindException

/**
 * Singleton manager for the Hub bulk image upload server.
 * Manages the HTTP server lifecycle for the web portal
 * where users can upload images for multiple hub items at once.
 */
object HubServerManager {

    private var server: NanoHTTPD? = null
    private var boundPort: Int? = null
    private const val PORT_START = 8080
    private const val PORT_END = 8090

    /**
     * Start the Bulk Hub upload server with port hunting.
     *
     * @param items List of items to manage
     * @param shape The shape constraint for all items
     * @param onImageReceived Callback when an image is uploaded for a specific ID
     * @return The URL string for QR code generation, or null on failure
     */
    fun startBulkServer(
        items: List<com.rovo.app.data.model.HubRowItemEntity>,
        shape: HubShape,
        onImageReceived: (String, ByteArray) -> Unit,
        onImageDeleted: ((String) -> Unit)? = null
    ): String? {
        stopServer()

        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val newServer = HubBulkUploadServer(
                    port = port,
                    items = items,
                    shape = shape,
                    onImageReceived = onImageReceived,
                    onImageDeleted = onImageDeleted
                )
                newServer.start()
                server = newServer
                boundPort = port
                val hostIp = ip ?: "127.0.0.1"
                return "http://$hostIp:$port"
            } catch (e: BindException) {
                continue
            } catch (e: Exception) {
                if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("HubServerManager", "Server start failed", e)
                continue
            }
        }

        return null
    }

    /**
     * Returns the bound port for adb reverse setup.
     */
    fun getPort(): Int? = boundPort

    /**
     * Stop the Hub upload server.
     */
    fun stopServer() {
        server?.stop()
        server = null
        boundPort = null
    }

}
