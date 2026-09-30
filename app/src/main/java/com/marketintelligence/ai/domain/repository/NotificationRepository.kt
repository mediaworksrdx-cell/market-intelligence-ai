package com.marketintelligence.ai.domain.repository

import com.marketintelligence.ai.domain.model.NotificationItem
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<NotificationItem>>
    val unreadCount: StateFlow<Int>
    fun markAllAsRead()
    fun markAsRead(id: String)
    fun dismissNotification(id: String)
    fun clearAll()
}
