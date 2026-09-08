package com.marketintelligence.ai.ui.notifications

import androidx.lifecycle.ViewModel
import com.marketintelligence.ai.data.local.MockData
import com.marketintelligence.ai.domain.model.NotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        _uiState.update { it.copy(notifications = MockData.NOTIFICATIONS_MOCK) }
    }

    fun dismissNotification(id: String) {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.filter { it.id != id })
        }
    }

    fun clearAll() {
        _uiState.update { it.copy(notifications = emptyList()) }
    }
}
