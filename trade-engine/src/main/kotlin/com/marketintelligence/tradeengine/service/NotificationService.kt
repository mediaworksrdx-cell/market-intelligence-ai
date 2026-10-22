package com.marketintelligence.tradeengine.service

import com.marketintelligence.tradeengine.models.AppNotification
import com.marketintelligence.tradeengine.models.NotificationCounts
import com.marketintelligence.tradeengine.models.NotificationFeedResponse
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object NotificationService {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    val notificationSessions: MutableSet<WebSocketSession> = ConcurrentHashMap.newKeySet<WebSocketSession>()

    private val jsonEncoder = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    private val BASELINE_SIGNALS = listOf(
        AppNotification(
            id = "sig-nifty-breakout",
            type = "SIGNAL",
            icon = "⚡",
            badge = "TECHNICAL BREAKOUT",
            title = "NIFTY 50 Structural Resistance Sweep",
            desc = "Broke above 24,580 structural resistance with 1.8x institutional volume surge.",
            time = "4m ago",
            timestamp = System.currentTimeMillis() - 4 * 60 * 1000L,
            symbol = "NIFTY",
            market = "INDIA",
            href = "/chart/NIFTY?market=INDIA",
            priority = "HIGH"
        ),
        AppNotification(
            id = "sig-gold-absorption",
            type = "SIGNAL",
            icon = "🛢️",
            badge = "ORDER FLOW ABSORPTION",
            title = "Gold Spot Limit Absorption",
            desc = "XAU/USD holding $2,653 support block with aggressive institutional limit bid stacking.",
            time = "18m ago",
            timestamp = System.currentTimeMillis() - 18 * 60 * 1000L,
            symbol = "GOLD",
            market = "USA",
            href = "/chart/GOLD?market=USA",
            priority = "MEDIUM"
        ),
        AppNotification(
            id = "sig-eurusd-liquidity",
            type = "SIGNAL",
            icon = "💱",
            badge = "FOREX SPREAD",
            title = "EUR/USD Interbank Spread Tightening",
            desc = "Interbank spread narrowed to 0.8 pips at 1.0845 on London/NY market overlap flow.",
            time = "35m ago",
            timestamp = System.currentTimeMillis() - 35 * 60 * 1000L,
            symbol = "EURUSD",
            market = "USA",
            href = "/chart/EURUSD?market=USA",
            priority = "LOW"
        )
    )

    suspend fun getNotifications(type: String = "ALL", limit: Int = 25): NotificationFeedResponse {
        val news = NewsIngestionService.getNews()
        val calendar = CalendarService.getEvents()

        val aggregated = mutableListOf<AppNotification>()

        // 1. News Alerts (Top 10)
        for (n in news.take(10)) {
            val pubTime = try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(n.publishedAt)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }

            aggregated.add(
                AppNotification(
                    id = "notif-news-${n.id}",
                    type = "NEWS",
                    icon = if (n.isBreaking) "⚡" else "📰",
                    badge = if (n.isBreaking) "BREAKING NEWS" else "LIVE ${n.category}",
                    title = n.title,
                    desc = if (n.summary.length > 180) n.summary.substring(0, 177) + "..." else n.summary,
                    time = n.timeAgo,
                    timestamp = pubTime,
                    market = n.market,
                    symbol = n.tickers.firstOrNull() ?: if (n.market == "INDIA") "NIFTY" else "SPX",
                    href = if (n.tickers.isNotEmpty()) "/news?ticker=${n.tickers.first()}" else "/news",
                    isBreaking = n.isBreaking,
                    priority = if (n.isBreaking) "HIGH" else "MEDIUM"
                )
            )
        }

        // 2. High & Medium Impact Calendar Alerts (Top 8)
        val highImpactCal = calendar.filter { it.impact == "HIGH" || it.impact == "MEDIUM" }.take(8)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        for (c in highImpactCal) {
            val isReleased = !c.actual.isNullOrBlank()
            val descText = if (isReleased) {
                "Actual: ${c.actual} vs Forecast: ${c.forecast ?: "N/A"} (Prior: ${c.previous ?: "N/A"})${if (c.isBeat == true) " — Beat consensus" else if (c.isBeat == false) " — Missed consensus" else ""}"
            } else {
                "Scheduled: ${c.dateStr} at ${c.timeStr} · Forecast: ${c.forecast ?: "N/A"} (Prior: ${c.previous ?: "N/A"})"
            }

            aggregated.add(
                AppNotification(
                    id = "notif-cal-${c.id}",
                    type = "CALENDAR",
                    icon = c.flag.ifBlank { "📅" },
                    badge = "${c.impact} IMPACT MACRO",
                    title = "${c.countryCode}: ${c.event}",
                    desc = descText,
                    time = if (c.dateStr == todayStr) "Today ${c.timeStr}" else "${c.dateStr} ${c.timeStr}",
                    timestamp = c.timestamp,
                    market = c.country,
                    symbol = if (c.country == "INDIA") "RBI / IN" else if (c.country == "USA") "FED / US" else "${c.countryCode} MACRO",
                    href = "/calendar?region=${c.country}",
                    priority = if (c.impact == "HIGH") "HIGH" else "MEDIUM"
                )
            )
        }

        // 3. Technical Signals
        aggregated.addAll(BASELINE_SIGNALS)

        // Filter by type
        var filtered = if (type != "ALL") {
            aggregated.filter { it.type.equals(type, ignoreCase = true) }
        } else {
            aggregated
        }

        // Sort: Breaking first, then latest
        filtered = filtered.sortedWith(compareByDescending<AppNotification> { it.isBreaking }.thenByDescending { it.timestamp })

        val resultList = filtered.take(limit)

        return NotificationFeedResponse(
            success = true,
            total = resultList.size,
            counts = NotificationCounts(
                all = aggregated.size,
                news = aggregated.count { it.type == "NEWS" },
                calendar = aggregated.count { it.type == "CALENDAR" },
                signals = aggregated.count { it.type == "SIGNAL" }
            ),
            notifications = resultList,
            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())
        )
    }

    suspend fun broadcastNotification(notification: AppNotification) {
        if (notificationSessions.isEmpty()) return
        try {
            val jsonStr = jsonEncoder.encodeToString(AppNotification.serializer(), notification)
            val frame = Frame.Text(jsonStr)
            val dead = mutableListOf<WebSocketSession>()
            for (session in notificationSessions) {
                try {
                    session.send(frame)
                } catch (_: Exception) {
                    dead.add(session)
                }
            }
            if (dead.isNotEmpty()) {
                notificationSessions.removeAll(dead.toSet())
            }
        } catch (e: Exception) {
            logger.error("Error broadcasting notification: ${e.message}")
        }
    }
}
