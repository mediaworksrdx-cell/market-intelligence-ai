package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.domain.model.MarketType
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class SessionState {
    PRE_MARKET,
    REGULAR,
    POST_MARKET,
    CLOSED,
    WEEKEND,
    HOLIDAY
}

data class MarketSessionInfo(
    val state: SessionState,
    val exchangeTime: String,
    val localTime: String,
    val exchangeTimeZone: String,
    val isLiveTrading: Boolean,
    val sessionLabel: String
)

@Singleton
class MarketSessionEngine @Inject constructor() {

    private val istZone = ZoneId.of("Asia/Kolkata")
    private val estZone = ZoneId.of("America/New_York")
    private val gstZone = ZoneId.of("Asia/Dubai")

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.US)

    private val inHolidays = setOf(
        MonthDay.of(1, 1),
        MonthDay.of(1, 26),
        MonthDay.of(8, 15),
        MonthDay.of(10, 2),
        MonthDay.of(12, 25)
    )

    private val usHolidays = setOf(
        MonthDay.of(1, 1),
        MonthDay.of(7, 4),
        MonthDay.of(12, 25)
    )

    private val uaeHolidays = setOf(
        MonthDay.of(1, 1),
        MonthDay.of(12, 2)
    )

    fun getSessionInfo(marketType: MarketType, timestamp: Long = System.currentTimeMillis()): MarketSessionInfo {
        val instant = Instant.ofEpochMilli(timestamp)
        val zone = getExchangeZone(marketType)
        val zonedDateTime = instant.atZone(zone)
        val localDateTime = instant.atZone(ZoneId.systemDefault())

        val dayOfWeek = zonedDateTime.dayOfWeek
        val monthDay = MonthDay.from(zonedDateTime)
        val time = zonedDateTime.toLocalTime()

        val marketHolidays = when (marketType) {
            MarketType.IN -> inHolidays
            MarketType.US -> usHolidays
            MarketType.UAE -> uaeHolidays
        }

        val state = if (isCrypto(marketType)) {
            SessionState.REGULAR
        } else if (marketHolidays.contains(monthDay)) {
            SessionState.HOLIDAY
        } else when (marketType) {
            MarketType.IN -> getIndianMarketSession(dayOfWeek, time)
            MarketType.US -> getUSMarketSession(dayOfWeek, time)
            MarketType.UAE -> getUAEMarketSession(dayOfWeek, time)
        }

        val isLive = state == SessionState.REGULAR || state == SessionState.PRE_MARKET

        val label = when (state) {
            SessionState.PRE_MARKET -> "PRE-MARKET"
            SessionState.REGULAR -> "LIVE (RTH)"
            SessionState.POST_MARKET -> "POST-MARKET"
            SessionState.CLOSED -> "MARKET CLOSED"
            SessionState.WEEKEND -> "WEEKEND CLOSED"
            SessionState.HOLIDAY -> "EXCHANGE HOLIDAY"
        }

        return MarketSessionInfo(
            state = state,
            exchangeTime = zonedDateTime.format(dateTimeFormatter),
            localTime = localDateTime.format(dateTimeFormatter),
            exchangeTimeZone = zone.id,
            isLiveTrading = isLive,
            sessionLabel = label
        )
    }

    fun getExchangeZone(marketType: MarketType): ZoneId {
        return when (marketType) {
            MarketType.IN -> istZone
            MarketType.US -> estZone
            MarketType.UAE -> gstZone
        }
    }

    fun formatCandleTime(timestamp: Long, marketType: MarketType, pattern: String = "HH:mm"): String {
        val zone = getExchangeZone(marketType)
        val zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
        return zonedDateTime.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    }

    private fun isCrypto(marketType: MarketType): Boolean {
        return false
    }

    private fun getIndianMarketSession(day: DayOfWeek, time: LocalTime): SessionState {
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return SessionState.WEEKEND
        val preMarketStart = LocalTime.of(9, 0)
        val normalStart = LocalTime.of(9, 15)
        val normalEnd = LocalTime.of(15, 30)
        val postMarketEnd = LocalTime.of(16, 0)

        return when {
            time.isBefore(preMarketStart) -> SessionState.CLOSED
            time.isBefore(normalStart) -> SessionState.PRE_MARKET
            time.isBefore(normalEnd) -> SessionState.REGULAR
            time.isBefore(postMarketEnd) -> SessionState.POST_MARKET
            else -> SessionState.CLOSED
        }
    }

    private fun getUSMarketSession(day: DayOfWeek, time: LocalTime): SessionState {
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return SessionState.WEEKEND
        val preMarketStart = LocalTime.of(4, 0)
        val normalStart = LocalTime.of(9, 30)
        val normalEnd = LocalTime.of(16, 0)
        val postMarketEnd = LocalTime.of(20, 0)

        return when {
            time.isBefore(preMarketStart) -> SessionState.CLOSED
            time.isBefore(normalStart) -> SessionState.PRE_MARKET
            time.isBefore(normalEnd) -> SessionState.REGULAR
            time.isBefore(postMarketEnd) -> SessionState.POST_MARKET
            else -> SessionState.CLOSED
        }
    }

    private fun getUAEMarketSession(day: DayOfWeek, time: LocalTime): SessionState {
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return SessionState.WEEKEND
        val normalStart = LocalTime.of(10, 0)
        val normalEnd = LocalTime.of(15, 0)

        return when {
            time.isBefore(normalStart) -> SessionState.CLOSED
            time.isBefore(normalEnd) -> SessionState.REGULAR
            else -> SessionState.CLOSED
        }
    }
}
