package com.example.marketintelligence.domain.repository

import com.example.marketintelligence.domain.model.NotificationItem
import kotlinx.coroutines.flow.StateFlow

interface NotificationRepository {
    val notifications: StateFlow<List<NotificationItem>>
    val unreadCount: StateFlow<Int>
    fun markAllAsRead()
    fun markAsRead(id: String)
    fun dismissNotification(id: String)
    fun clearAll()
}
