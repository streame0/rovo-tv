package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.BindException

/**
 * Manages the AvatarUploadServer lifecycle with port hunting.
 * Similar to ServerManager but for avatar image uploads.
 */
class AvatarServerManager {

    private var server: AvatarUploadServer? = null
    private var boundPort: Int? = null

    companion object {
        private const val PORT_START = 8080
        private const val PORT_END = 8090
    }

    /**
     * Attempts to start the avatar upload server on an available port.
     * Returns ServerInfo on success, null on failure.
     */
    suspend fun startServer(onImageReceived: (ByteArray) -> Unit): ServerInfo? = withContext(Dispatchers.IO) {
        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val avatarServer = AvatarUploadServer(port, onImageReceived)
                avatarServer.start()
                server = avatarServer
                boundPort = port
                val hostIp = ip ?: "127.0.0.1"
                return@withContext ServerInfo(hostIp, port)
            } catch (e: BindException) {
                continue
            } catch (e: Exception) {
                if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("AvatarServerManager", "Port binding failed", e)
                continue
            }
        }

        null
    }

    /**
     * Returns the bound port for adb reverse setup.
     */
    fun getPort(): Int? = boundPort

    /**
     * Stops the running server if any.
     */
    fun stopServer() {
        server?.stop()
        server = null
        boundPort = null
    }
}
