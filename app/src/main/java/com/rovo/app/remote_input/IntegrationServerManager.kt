package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the remote pairing session for Stremio login.
 * Replaces the local IntegrationServer with a cloud-hosted pairing service.
 */
class IntegrationServerManager(private val context: android.content.Context) {

    private var remoteClient: RemoteIntegrationClient? = null

    /**
     * Creates a remote pairing session on the Rovo server.
     * Returns [ServerInfo] with the pairing URL for QR code generation.
     */
    suspend fun startServer(
        onCredentialsReceived: (email: String, password: String) -> Unit
    ): ServerInfo? = withContext(Dispatchers.IO) {
        try {
            val client = RemoteIntegrationClient()
            val session = client.createSession() ?: return@withContext null

            client.startPolling { email, password ->
                onCredentialsReceived(email, password)
            }
            remoteClient = client

            ServerInfo(
                ip = "",
                port = 0,
                pairingUrl = session.pairingUrl
            )
        } catch (e: Exception) {
            if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("IntegrationServerManager", "Failed to start remote session", e)
            null
        }
    }

    fun stopServer() {
        remoteClient?.stopPolling()
        remoteClient = null
    }
}
