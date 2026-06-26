package com.rovo.app.data.trakt

import android.util.Log
import com.rovo.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that attaches the Trakt Bearer token to outgoing requests.
 * Token refresh on 401 is handled by [TraktAuthenticator] — no runBlocking needed.
 */
@Singleton
class TraktAuthInterceptor @Inject constructor(
    private val traktAuthManager: TraktAuthManager
) : Interceptor {

    companion object {
        private const val TAG = "TraktAuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = traktAuthManager.getAccessToken()
        return chain.proceed(buildRequest(chain.request(), token))
    }

    private fun buildRequest(original: okhttp3.Request, token: String?): okhttp3.Request {
        val builder = original.newBuilder()
            .header("Content-Type", "application/json")
            .header("trakt-api-version", "2")
            .header("trakt-api-key", BuildConfig.TRAKT_CLIENT_ID)
            .header("User-Agent", "Rovo/${BuildConfig.VERSION_NAME}")

        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        return builder.build()
    }
}
