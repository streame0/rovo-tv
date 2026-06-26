package com.rovo.app.data.trakt

import android.util.Log
import com.rovo.app.BuildConfig
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that handles Trakt 401 responses by refreshing the token
 * and retrying the request. Runs on OkHttp's dispatcher threads — no ANR risk.
 *
 * Replaces the runBlocking-based proactive refresh in TraktAuthInterceptor.
 */
@Singleton
class TraktAuthenticator @Inject constructor(
    private val traktAuthManager: TraktAuthManager
) : Authenticator {

    companion object {
        private const val TAG = "TraktAuthenticator"
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Avoid infinite retry loops
        if (responseCount(response) >= 2) return null

        Log.d(TAG, "Got ${response.code}, attempting token refresh")

        val newToken = try {
            // Block here — this runs on OkHttp's dispatcher thread, not main
            kotlinx.coroutines.runBlocking {
                traktAuthManager.refreshAccessToken()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh failed", e)
            return null
        }

        if (newToken == null) {
            Log.w(TAG, "Token refresh returned null — user may need to re-authenticate")
            return null
        }

        Log.d(TAG, "Token refreshed, retrying request")
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
