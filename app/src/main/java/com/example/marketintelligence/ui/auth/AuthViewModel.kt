package com.example.marketintelligence.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.auth.AuthTokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoggedIn: Boolean = false,
    val token: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val authProvider: String = "guest",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenManager: AuthTokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tokenManager.isLoggedIn,
                tokenManager.token,
                tokenManager.userId,
                tokenManager.userName,
                tokenManager.userEmail
            ) { isLoggedIn, token, userId, userName, userEmail ->
                AuthState(
                    isLoggedIn = isLoggedIn,
                    token = token,
                    userId = userId,
                    userName = userName,
                    userEmail = userEmail,
                    authProvider = if (isLoggedIn) "google" else "guest"
                )
            }.collect { state ->
                _authState.value = state
            }
        }
    }

    fun launchGoogleSignIn(context: Context) {
        val loginUri = Uri.parse(AuthTokenManager.OAUTH_LOGIN_URL)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()
            customTabsIntent.launchUrl(context, loginUri)
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, loginUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun handleOAuthCallback(token: String, userId: String, name: String?, email: String? = null) {
        viewModelScope.launch {
            tokenManager.saveAuth(
                token = token,
                userId = userId,
                name = name,
                email = email,
                provider = "google"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            tokenManager.clearAuth()
        }
    }
}
