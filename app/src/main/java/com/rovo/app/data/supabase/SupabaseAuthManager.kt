package com.rovo.app.data.supabase

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class CloudAuthState {
    data object Unauthenticated : CloudAuthState()
    data object Anonymous : CloudAuthState()
    data class Authenticated(val email: String) : CloudAuthState()
}

@Singleton
class SupabaseAuthManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val sessionStore: SupabaseSessionStore
) {
    companion object {
        private const val TAG = "SupabaseAuth"
    }

    private val _authState = MutableStateFlow<CloudAuthState>(CloudAuthState.Unauthenticated)
    val authState: StateFlow<CloudAuthState> = _authState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun initialize() {
        try {
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                sessionStore.accessToken = session.accessToken
                sessionStore.refreshToken = session.refreshToken
                sessionStore.userId = session.user?.id ?: ""

                val email = session.user?.email
                if (email != null) {
                    sessionStore.email = email
                    sessionStore.isAnonymous = false
                    _authState.value = CloudAuthState.Authenticated(email)
                } else {
                    sessionStore.isAnonymous = true
                    _authState.value = CloudAuthState.Anonymous
                }
            } else {
                restoreAndRefresh()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Session restore failed", e)
            _authState.value = CloudAuthState.Unauthenticated
        }
    }

    private suspend fun restoreAndRefresh() {
        val storedRefreshToken = sessionStore.refreshToken
        if (storedRefreshToken != null) {
            try {
                val session = supabase.auth.refreshSession(storedRefreshToken)
                sessionStore.accessToken = session.accessToken
                sessionStore.refreshToken = session.refreshToken
                sessionStore.userId = session.user?.id ?: ""

                val email = session.user?.email
                if (email != null) {
                    sessionStore.email = email
                    sessionStore.isAnonymous = false
                    _authState.value = CloudAuthState.Authenticated(email)
                } else {
                    sessionStore.isAnonymous = true
                    _authState.value = CloudAuthState.Anonymous
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Token refresh failed", e)
                sessionStore.clear()
            }
        }

        if (sessionStore.syncEnabled) {
            signInAnonymously()
        } else {
            _authState.value = CloudAuthState.Unauthenticated
        }
    }

    suspend fun signInAnonymously(): Result<Unit> {
        _isLoading.value = true
        return try {
            supabase.auth.signInAnonymously()
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                sessionStore.accessToken = session.accessToken
                sessionStore.refreshToken = session.refreshToken
                sessionStore.userId = session.user?.id ?: ""
            }
            sessionStore.isAnonymous = true
            sessionStore.syncEnabled = true
            _authState.value = CloudAuthState.Anonymous
            Log.i(TAG, "Signed in anonymously")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        _isLoading.value = true
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                sessionStore.accessToken = session.accessToken
                sessionStore.refreshToken = session.refreshToken
                sessionStore.userId = session.user?.id ?: ""
            }
            sessionStore.email = email
            sessionStore.isAnonymous = false
            sessionStore.syncEnabled = true
            _authState.value = CloudAuthState.Authenticated(email)
            Log.i(TAG, "Signed in: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> {
        _isLoading.value = true
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val session = supabase.auth.currentSessionOrNull()
            if (session != null) {
                sessionStore.accessToken = session.accessToken
                sessionStore.refreshToken = session.refreshToken
                sessionStore.userId = session.user?.id ?: ""
            }
            sessionStore.email = email
            sessionStore.isAnonymous = false
            sessionStore.syncEnabled = true
            _authState.value = CloudAuthState.Authenticated(email)
            Log.i(TAG, "Signed up: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up failed", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun linkEmail(email: String, password: String): Result<Unit> {
        _isLoading.value = true
        return try {
            supabase.auth.updateUser {
                this.email = email
                this.password = password
            }
            sessionStore.email = email
            sessionStore.isAnonymous = false
            _authState.value = CloudAuthState.Authenticated(email)
            Log.i(TAG, "Email linked to anonymous account: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Email linking failed", e)
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (_: Exception) {}
        sessionStore.clear()
        _authState.value = CloudAuthState.Unauthenticated
        Log.i(TAG, "Signed out")
    }

    fun isConnected(): Boolean = _authState.value !is CloudAuthState.Unauthenticated
}
