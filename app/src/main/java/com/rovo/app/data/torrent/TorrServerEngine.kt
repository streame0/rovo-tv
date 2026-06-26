package com.rovo.app.data.torrent

import android.content.Context
import android.util.Log
import com.rovo.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrServerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "RovoTorrent"
        private const val PORT = 8090
        private const val BINARY_NAME = "libtorrserver.so"
    }

    private var process: Process? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getBaseUrl(): String = "http://127.0.0.1:$PORT"

    fun start() {
        if (isRunning()) {
            if (BuildConfig.DEBUG) Log.d(TAG, "TorrServer already running")
            return
        }

        val binaryPath = getBinaryPath()
        if (binaryPath == null) {
            throw IllegalStateException("TorrServer binary not found in native library directory")
        }

        val binaryFile = File(binaryPath)
        val abis = android.os.Build.SUPPORTED_ABIS.joinToString()
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "System ABIs: $abis")
            Log.i(TAG, "Starting TorrServer: $binaryPath (Size: ${binaryFile.length()}, CanExec: ${binaryFile.canExecute()})")
        }

        val configDir = File(context.filesDir, "torrserver")
        configDir.mkdirs()

        // Use direct execution as primary, fallback to shell only if needed
        val pb = ProcessBuilder(binaryPath, "-p", PORT.toString(), "-d", configDir.absolutePath)
            .redirectErrorStream(true)
        
        // Essential environment variables for Go-based TorrServer on Android
        pb.environment()["GODEBUG"] = "netdns=go"
        pb.environment()["HOME"] = configDir.absolutePath
        pb.environment()["TMPDIR"] = configDir.absolutePath
        
        try {
            process = pb.start()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Direct start failed, trying shell wrapper...", e)
            process = ProcessBuilder("sh", "-c", "\"$binaryPath\" -p $PORT -d \"${configDir.absolutePath}\"")
                .redirectErrorStream(true)
                .apply {
                    environment()["GODEBUG"] = "netdns=go"
                    environment()["HOME"] = configDir.absolutePath
                }
                .start()
        }

        // Log output in background for debugging
        if (BuildConfig.DEBUG) {
            val proc = process
            Thread({
                try {
                    proc?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.v(TAG, "TorrServer: $line")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "TorrServer log reader error", e)
                }
            }, "torrserver-log").apply { isDaemon = true }.start()
        }

        // Wait for server to be ready
        val deadline = System.currentTimeMillis() + 15_000L
        while (System.currentTimeMillis() < deadline) {
            if (echo()) {
                if (BuildConfig.DEBUG) Log.d(TAG, "TorrServer started successfully on port $PORT")
                return
            }
            
            // Check if process died early
            if (process?.isAlive == false) {
                val exitCode = process?.exitValue()
                val errorDetail = when(exitCode) {
                    126 -> "Exec format error (126). Likely 64-bit binary on 32-bit CPU, or Linux binary on Android. Supported ABIs: $abis"
                    127 -> "File not found (127). Missing dependencies or wrong path."
                    else -> "Exit code $exitCode"
                }
                throw IllegalStateException("TorrServer failed: $errorDetail. Binary Size: ${binaryFile.length()}")
            }
            Thread.sleep(500)
        }

        throw IllegalStateException("TorrServer failed to start within 15 seconds")
    }

    fun stop() {
        try {
            // Try graceful shutdown first
            val request = Request.Builder()
                .url("${getBaseUrl()}/shutdown")
                .get()
                .build()
            try {
                httpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Shutdown request failed", e)
            }

            process?.let { proc ->
                // Wait for graceful exit
                val exited = proc.waitFor(3, TimeUnit.SECONDS)
                if (!exited) {
                    proc.destroyForcibly()
                    if (BuildConfig.DEBUG) Log.w(TAG, "TorrServer force-killed")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Error stopping TorrServer", e)
            process?.destroyForcibly()
        } finally {
            process = null
            if (BuildConfig.DEBUG) Log.d(TAG, "TorrServer stopped")
        }
    }

    fun isRunning(): Boolean {
        val proc = process ?: return false
        return proc.isAlive && echo()
    }

    fun echo(): Boolean {
        return try {
            val request = Request.Builder()
                .url("${getBaseUrl()}/echo")
                .get()
                .build()
            val response = httpClient.newCall(request).execute()
            val ok = response.isSuccessful
            response.close()
            ok
        } catch (_: Exception) {
            false
        }
    }

    private fun getBinaryPath(): String? {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val binary = File(nativeLibDir, BINARY_NAME)
        return if (binary.exists()) binary.absolutePath else null
    }
}
