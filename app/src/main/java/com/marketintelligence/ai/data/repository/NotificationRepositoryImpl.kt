package com.marketintelligence.ai.data.repository

import android.content.Context
import com.marketintelligence.ai.data.local.NotificationStorage
import com.marketintelligence.ai.domain.model.NotificationItem
import com.marketintelligence.ai.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRepository {
    private val storage = NotificationStorage(context)
    private val _notifications = MutableStateFlow<List<NotificationItem>>(storage.getNotifications())
    override val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(_notifications.value.count { !it.read })
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private fun refresh() {
        val updated = storage.getNotifications()
        _notifications.value = updated
        _unreadCount.value = updated.count { !it.read }
    }

    override fun markAllAsRead() {
        storage.markAllAsRead()
        refresh()
    }

    override fun markAsRead(id: String) {
        storage.markAsRead(id)
        refresh()
    }

    override fun dismissNotification(id: String) {
        storage.dismissNotification(id)
        refresh()
    }

    override fun clearAll() {
        storage.clearAll()
        refresh()
    }
}
