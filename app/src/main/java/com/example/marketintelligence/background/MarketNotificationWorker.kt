package com.example.marketintelligence.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.marketintelligence.MainActivity
import com.example.marketintelligence.data.local.NotificationStorage
import com.example.marketintelligence.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

class MarketNotificationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "MarketSurveillanceWorker"
        const val CHANNEL_NEWS = "MARKET_NEWS_CHANNEL"
        const val CHANNEL_CALENDAR = "ECONOMIC_CALENDAR_CHANNEL"
        const val CHANNEL_IPO = "IPO_WATCH_CHANNEL"
        const val CHANNEL_AI = "AI_SIGNAL_CHANNEL"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                val channels = listOf(
                    NotificationChannel(CHANNEL_NEWS, "Market News", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Breaking financial & market intelligence news alerts"
                    },
                    NotificationChannel(CHANNEL_CALENDAR, "Economic Calendar", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Key economic data releases and central bank announcements"
                    },
                    NotificationChannel(CHANNEL_IPO, "IPO Watch", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "IPO subscription updates, GMP tracking, and listing alerts"
                    },
                    NotificationChannel(CHANNEL_AI, "AI Trading Signals", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Institutional algorithmic market signals and setups"
                    }
                )
                channels.forEach { notificationManager.createNotificationChannel(it) }
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            ensureChannels(appContext)
            val storage = NotificationStorage(appContext)
            val existing = storage.getNotifications()

            val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val candidatePool = listOf(
                NotificationItem(
                    id = "bg_news_${System.currentTimeMillis() / 3600000}",
                    type = NotificationType.NEWS,
                    title = "Market Flash: FIIs Net Buy ₹3,218 Cr in Cash Segment",
                    time = timeString,
                    sentiment = Sentiment.POSITIVE,
                    read = false,
                    content = "Foreign Institutional Investors (FIIs) recorded robust net buying of ₹3,218 Cr in cash equities today, led by sustained accumulation in private banks, energy, and engineering leaders.\n\nInstitutional Activity Breakdown:\n• DIIs provided domestic SIP support with net purchases of ₹1,850 Cr\n• India VIX eased 4.2% to 12.85, indicating lower volatility pricing\n• Nifty PCR (Put-Call Ratio) improved to 1.18 indicating sustained support at 22,600.",
                    summary = "FIIs inject ₹3,200 Cr into Indian equities; benchmark indices demonstrate strong accumulation.",
                    source = "NSE Daily Institutional Bulletin",
                    category = "Institutional Flows",
                    actionSymbol = "NIFTY 50",
                    actionType = "INDEX"
                ),
                NotificationItem(
                    id = "bg_event_${System.currentTimeMillis() / 3600000}",
                    type = NotificationType.EVENT,
                    title = "Economic Alert: US Core PPI Final Demand at 2.4% YoY",
                    time = timeString,
                    sentiment = Sentiment.POSITIVE,
                    read = false,
                    eventData = EconomicEvent(
                        id = "evt_ppi_us",
                        time = "06:00 PM IST",
                        currency = "USD",
                        event = "US Producer Price Index (PPI) Core YoY",
                        impact = "HIGH",
                        actual = "2.40%",
                        forecast = "2.70%",
                        previous = "2.90%",
                        description = "US Core Producer Price Index came in below estimates at 2.4% YoY, indicating that wholesale pipeline inflationary pressure continues to cool ahead of next week's PCE release.\n\nKey Takeaways:\n• Services input costs flattened\n• Commodities component contracted 0.4% MoM\n• US treasury yields and mortgage rates softened across the curve.",
                        affectedMarkets = "US Indices, Gold, Emerging Market Currencies"
                    ),
                    content = "US Core PPI cools to 2.4% YoY, fueling bond rallies and rate cut optimism.",
                    summary = "Producer price index prints 2.4% below forecast; yields soften across curve.",
                    source = "US Bureau of Labor Statistics",
                    category = "Inflation Data",
                    actionSymbol = "SPX",
                    actionType = "INDEX"
                ),
                NotificationItem(
                    id = "bg_ipo_${System.currentTimeMillis() / 3600000}",
                    type = NotificationType.IPO,
                    title = "IPO Flash: Zenith Defense Dynamics Day 2 Subscription Hits 18.2x",
                    time = timeString,
                    sentiment = Sentiment.POSITIVE,
                    read = false,
                    ipoData = IPOData(
                        symbol = "ZENITHDEF",
                        name = "Zenith Defense Dynamics Ltd",
                        openDate = "06 Oct 2026",
                        closeDate = "08 Oct 2026",
                        priceBand = "₹620 - ₹655",
                        gmp = 220.0,
                        gmpPercent = 33.6,
                        status = "OPEN",
                        issueSize = "₹2,400 Cr",
                        lotSize = "22 Shares (₹14,410)",
                        listingDate = "13 Oct 2026",
                        description = "Zenith Defense Dynamics specializes in indigenous drone avionics, phased array radar systems, and counter-UAS platforms.\n\nBidding Dynamics:\n• NII category subscribed 28.4x\n• Retail portion subscribed 14.1x\n• Grey Market Premium (GMP) steady at ₹220 (+33.6%) with estimated listing at ₹875 per share.",
                        subscriptionStatus = "Day 2: 18.2x Overall"
                    ),
                    content = "Zenith Defense IPO reaches 18.2x subscription on Day 2 with strong retail and HNI appetite. Current GMP indicates ~33.6% listing gain.",
                    summary = "Zenith Defense IPO subscribed 18.2x on Day 2; GMP holds firm at ₹220.",
                    source = "Chittorgarh / BSE SME & Mainboard",
                    category = "Defense IPO",
                    actionSymbol = "ZENITHDEF",
                    actionType = "STOCK"
                )
            )

            val newAlert = candidatePool.firstOrNull { cand ->
                existing.none { it.id == cand.id || it.title == cand.title }
            }

            if (newAlert != null) {
                storage.addNotification(newAlert)
                postSystemNotification(newAlert)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success()
        }
    }

    private fun postSystemNotification(item: NotificationItem) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val channelId = when (item.type) {
            NotificationType.NEWS -> CHANNEL_NEWS
            NotificationType.EVENT -> CHANNEL_CALENDAR
            NotificationType.IPO -> CHANNEL_IPO
            NotificationType.AI_SIGNAL -> CHANNEL_AI
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "notifications")
            putExtra("NOTIFICATION_ID", item.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snippet = when {
            item.summary.isNotBlank() -> item.summary
            item.content.isNotBlank() -> item.content.take(120) + "..."
            else -> item.title
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(item.title)
            .setContentText(snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (item.content.isNotBlank()) item.content else snippet))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(item.id.hashCode(), notification)
    }
}
