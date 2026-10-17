package com.marketintelligence.ai.data.local

import android.content.Context
import com.marketintelligence.ai.domain.model.NotificationItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NotificationStorage(private val context: Context) {
    private val prefs = context.getSharedPreferences("market_notifications_store", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    @Synchronized
    fun getNotifications(): List<NotificationItem> {
        val raw = prefs.getString("notifications_json", null)
        if (raw.isNullOrBlank()) {
            val initial = MockData.NOTIFICATIONS_MOCK
            saveNotifications(initial)
            return initial
        }
        return try {
            json.decodeFromString<List<NotificationItem>>(raw)
        } catch (e: Exception) {
            MockData.NOTIFICATIONS_MOCK
        }
    }

    @Synchronized
    fun saveNotifications(list: List<NotificationItem>) {
        try {
            val serialized = json.encodeToString(list)
            prefs.edit().putString("notifications_json", serialized).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun addNotification(item: NotificationItem) {
        val current = getNotifications().toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item) // Newest at top
        saveNotifications(current)
    }

    @Synchronized
    fun markAsRead(id: String) {
        val updated = getNotifications().map { if (it.id == id) it.copy(read = true) else it }
        saveNotifications(updated)
    }

    @Synchronized
    fun markAllAsRead() {
        val updated = getNotifications().map { it.copy(read = true) }
        saveNotifications(updated)
    }

    @Synchronized
    fun dismissNotification(id: String) {
        val updated = getNotifications().filter { it.id != id }
        saveNotifications(updated)
    }

    @Synchronized
    fun clearAll() {
        saveNotifications(emptyList())
    }
}
