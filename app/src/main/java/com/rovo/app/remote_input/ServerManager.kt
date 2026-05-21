package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.BindException

/**
 * Holds information about the running server or remote pairing session.
 * For local servers, [url] is derived from [ip] and [port].
 * For remote pairing, [url] is the [pairingUrl] from the server.
 */
data class ServerInfo(
    val ip: String,
    val port: Int,
    val pairingUrl: String? = null
) {
    val url: String get() = pairingUrl ?: "http://$ip:$port"
}

/**
 * Manages the LinkServer lifecycle with port hunting and NSD.
 */
class ServerManager(private val context: android.content.Context) {

    private var server: LinkServer? = null
    private var nsdManager: ServiceDiscoveryManager? = null

    companion object {
        private const val PORT_START = 8080
        private const val PORT_END = 8090
    }

    /**
     * Attempts to start the server on an available port and register via NSD.
     */
    suspend fun startServer(onLinkReceived: (String) -> Unit): ServerInfo? = withContext(Dispatchers.IO) {
        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val linkServer = LinkServer(port, onLinkReceived)
                linkServer.start()
                server = linkServer

                // Register for local network discovery
                nsdManager = ServiceDiscoveryManager(context).apply {
                    registerService(port, "Rovo-TV-Paste-${System.currentTimeMillis() % 1000}")
                }

                return@withContext ServerInfo(ip ?: "127.0.0.1", port)
            } catch (e: BindException) {
                continue
            } catch (e: Exception) {
                if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("ServerManager", "Port binding failed", e)
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
