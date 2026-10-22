package com.marketintelligence.tradeengine.models

import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val type: String,       // NEWS, CALENDAR, SIGNAL
    val icon: String,
    val badge: String,
    val title: String,
    val desc: String,
    val time: String,
    val timestamp: Long,
    val market: String,
    val symbol: String? = null,
    val href: String,
    val isBreaking: Boolean = false,
    val priority: String = "MEDIUM"
)

@Serializable
data class NotificationFeedResponse(
    val success: Boolean,
    val total: Int,
    val counts: NotificationCounts,
    val notifications: List<AppNotification>,
    val timestamp: String
)

@Serializable
data class NotificationCounts(
    val all: Int,
    val news: Int,
    val calendar: Int,
    val signals: Int
)
