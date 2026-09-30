package com.marketintelligence.ai.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "mi_auth_preferences")

/**
 * Institutional Authentication & Session Manager (Mirror).
 * Stores JWT tokens, Google User details, and manages session lifecycle.
 */
@Singleton
class AuthTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_AUTH_PROVIDER = stringPreferencesKey("auth_provider")

        // Azure & Aarka AI Server OAuth Configuration
        const val GOOGLE_CLIENT_ID = "298011224480-9ek1pv586air2nqb9srcht87k4fvkcvk.apps.googleusercontent.com"
        const val OAUTH_LOGIN_URL = "https://aarka-ai.com/auth/google/login"
        const val OAUTH_VERIFY_URL = "https://aarka-ai.com/auth/google/verify"
        const val DEEP_LINK_SCHEME_AARKAAI = "aarkaai"
        const val DEEP_LINK_SCHEME_MI = "marketintelligence"
        const val DEEP_LINK_HOST = "auth-callback"
    }

    val token: Flow<String?> = context.authDataStore.data.map { it[KEY_TOKEN] }
    val userId: Flow<String?> = context.authDataStore.data.map { it[KEY_USER_ID] }
    val userName: Flow<String?> = context.authDataStore.data.map { it[KEY_USER_NAME] }
    val userEmail: Flow<String?> = context.authDataStore.data.map { it[KEY_USER_EMAIL] }
    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val authProvider: Flow<String?> = context.authDataStore.data.map { it[KEY_AUTH_PROVIDER] ?: "guest" }

    suspend fun saveAuth(token: String, userId: String, name: String?, email: String? = null, provider: String = "google") {
        context.authDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_NAME] = name ?: "Institutional Trader"
            if (email != null) prefs[KEY_USER_EMAIL] = email
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_AUTH_PROVIDER] = provider
        }
    }

    suspend fun clearAuth() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs[KEY_IS_LOGGED_IN] = false
            prefs[KEY_AUTH_PROVIDER] = "guest"
        }
    }
}
