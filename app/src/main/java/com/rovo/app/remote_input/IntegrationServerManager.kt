package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.BindException

/**
 * Manages the local IntegrationServer for Stremio login via QR code.
 * Runs a NanoHTTPD server on the local network.
 */
class IntegrationServerManager(private val context: android.content.Context) {

    private var server: IntegrationServer? = null
    private var nsdManager: ServiceDiscoveryManager? = null

    companion object {
        private const val PORT_START = 8080
        private const val PORT_END = 8090
    }

    /**
     * Starts a local server on an available port.
     * Returns [ServerInfo] with the local URL for QR code generation.
     */
    suspend fun startServer(
        onCredentialsReceived: (email: String, password: String) -> Unit
    ): ServerInfo? = withContext(Dispatchers.IO) {
        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val integrationServer = IntegrationServer(port) { email, password ->
                    onCredentialsReceived(email, password)
                }
                integrationServer.start()
                server = integrationServer

                // Register for local network discovery
                nsdManager = ServiceDiscoveryManager(context).apply {
                    registerService(port, "Rovo-TV-Stremio-${System.currentTimeMillis() % 1000}")
                }

                return@withContext ServerInfo(
                    ip = ip ?: "127.0.0.1",
                    port = port,
                    pin = integrationServer.pin
                )
            } catch (e: BindException) {
                continue
            } catch (e: Exception) {
                if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("IntegrationServerManager", "Port binding failed", e)
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
