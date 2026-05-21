package com.rovo.app.remote_input

import android.os.Build
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    private val EMULATOR_INDICATORS = listOf(
        "google_sdk", "sdk_gphone", "emulator", "generic",
        "Android SDK built for x86", "goldfish", "ranchu"
    )

    fun isEmulator(): Boolean {
        return Build.FINGERPRINT.let { fp ->
            EMULATOR_INDICATORS.any { fp.contains(it, ignoreCase = true) }
        } || Build.MODEL.let { model ->
            EMULATOR_INDICATORS.any { model.contains(it, ignoreCase = true) }
        } || Build.MANUFACTURER.equals("Genymotion", ignoreCase = true)
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (address in networkInterface.inetAddresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            if (com.rovo.app.BuildConfig.DEBUG) android.util.Log.w("NetworkUtils", "Failed to get local IP", e)
        }
        return null
    }

    fun getAdbReverseUrl(port: Int): String {
        return "http://localhost:$port"
    }

    var debugTunnelUrl: String? = null
}
