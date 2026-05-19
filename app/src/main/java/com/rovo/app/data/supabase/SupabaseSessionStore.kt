package com.rovo.app.data.supabase

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSessionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "supabase_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private var activeProfileId: Int = 1

    fun setActiveProfile(profileId: Int) {
        activeProfileId = profileId
    }

    private fun pk(key: String): String = "p${activeProfileId}_$key"

    var accessToken: String?
        get() = prefs.getString(pk(KEY_ACCESS_TOKEN), null)
        set(value) {
            if (value != null) {
                prefs.edit().putString(pk(KEY_ACCESS_TOKEN), value).apply()
            } else {
                prefs.edit().remove(pk(KEY_ACCESS_TOKEN)).apply()
            }
        }

    var refreshToken: String?
        get() = prefs.getString(pk(KEY_REFRESH_TOKEN), null)
        set(value) {
            if (value != null) {
                prefs.edit().putString(pk(KEY_REFRESH_TOKEN), value).apply()
            } else {
                prefs.edit().remove(pk(KEY_REFRESH_TOKEN)).apply()
            }
        }

    var userId: String?
        get() = prefs.getString(pk(KEY_USER_ID), null)
        set(value) {
            if (value != null) {
                prefs.edit().putString(pk(KEY_USER_ID), value).apply()
            } else {
                prefs.edit().remove(pk(KEY_USER_ID)).apply()
            }
        }

    var isAnonymous: Boolean
        get() = prefs.getBoolean(pk(KEY_IS_ANONYMOUS), false)
        set(value) = prefs.edit().putBoolean(pk(KEY_IS_ANONYMOUS), value).apply()

    var email: String?
        get() = prefs.getString(pk(KEY_EMAIL), null)
        set(value) {
            if (value != null) {
                prefs.edit().putString(pk(KEY_EMAIL), value).apply()
            } else {
                prefs.edit().remove(pk(KEY_EMAIL)).apply()
            }
        }

    var lastSyncTimestamp: Long
        get() = prefs.getLong(pk(KEY_LAST_SYNC), 0L)
        set(value) = prefs.edit().putLong(pk(KEY_LAST_SYNC), value).apply()

    var syncEnabled: Boolean
        get() = prefs.getBoolean(pk(KEY_SYNC_ENABLED), false)
        set(value) = prefs.edit().putBoolean(pk(KEY_SYNC_ENABLED), value).apply()

    fun clearProfile(profileId: Int) {
        val old = activeProfileId
        activeProfileId = profileId
        prefs.edit()
            .remove(pk(KEY_ACCESS_TOKEN))
            .remove(pk(KEY_REFRESH_TOKEN))
            .remove(pk(KEY_USER_ID))
            .remove(pk(KEY_IS_ANONYMOUS))
            .remove(pk(KEY_EMAIL))
            .remove(pk(KEY_LAST_SYNC))
            .remove(pk(KEY_SYNC_ENABLED))
            .apply()
        activeProfileId = old
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_ANONYMOUS = "is_anonymous"
        private const val KEY_EMAIL = "email"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
    }
}
