package com.rovo.app.remote_input

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.BindException

class CloudServerManager {

    private var server: CloudSignInServer? = null

    companion object {
        private const val PORT_START = 8091
        private const val PORT_END = 8100
    }

    suspend fun startServer(
        onSignIn: (email: String, password: String) -> Unit,
        onSignUp: (email: String, password: String) -> Unit
    ): ServerInfo? = withContext(Dispatchers.IO) {
        val ip = NetworkUtils.getLocalIpAddress()

        for (port in PORT_START..PORT_END) {
            try {
                val cloudServer = CloudSignInServer(
                    port = port,
                    onSignIn = onSignIn,
                    onSignUp = onSignUp
                )
                cloudServer.start()
                server = cloudServer
                return@withContext ServerInfo(ip ?: "127.0.0.1", port)
            } catch (e: BindException) {
                continue
            } catch (e: Exception) {
                if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("CloudServerManager", "Port bind failed", e)
                continue
            }
        }
        null
    }

    fun stopServer() {
        server?.stop()
        server = null
    }
}
