package com.marketintelligence.ai.data.repository

import com.marketintelligence.ai.data.local.MockData
import com.marketintelligence.ai.domain.model.NotificationItem
import com.marketintelligence.ai.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor() : NotificationRepository {
    private val _notifications = MutableStateFlow<List<NotificationItem>>(MockData.NOTIFICATIONS_MOCK)
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(_notifications.value.count { !it.read })
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private fun updateUnread() {
        _unreadCount.value = _notifications.value.count { !it.read }
    }

    override fun markAllAsRead() {
        _notifications.update { list ->
            list.map { it.copy(read = true) }
        }
        updateUnread()
    }

    override fun markAsRead(id: String) {
        _notifications.update { list ->
            list.map { if (it.id == id) it.copy(read = true) else it }
        }
        updateUnread()
    }

    override fun dismissNotification(id: String) {
        _notifications.update { list ->
            list.filter { it.id != id }
        }
        updateUnread()
    }

    override fun clearAll() {
        _notifications.value = emptyList()
        updateUnread()
    }
}
