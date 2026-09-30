package com.marketintelligence.ai.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.ai.domain.model.NotificationItem
import com.marketintelligence.ai.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = notificationRepository.notifications
        .map { NotificationsUiState(notifications = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationsUiState(notifications = notificationRepository.notifications.value)
        )

    fun markAllAsRead() {
        notificationRepository.markAllAsRead()
    }

    fun dismissNotification(id: String) {
        notificationRepository.dismissNotification(id)
    }

    fun clearAll() {
        notificationRepository.clearAll()
    }
}
