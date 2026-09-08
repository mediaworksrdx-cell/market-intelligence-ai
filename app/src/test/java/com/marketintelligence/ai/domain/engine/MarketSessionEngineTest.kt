package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.domain.model.MarketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class MarketSessionEngineTest {

    private val engine = MarketSessionEngine()

    @Test
    fun testIndianMarketSessionRegularHours() {
        val istTime = LocalDateTime.of(2026, 8, 26, 11, 30)
        val epochMillis = istTime.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()

        val info = engine.getSessionInfo(MarketType.IN, epochMillis)

        assertNotNull(info)
        assertEquals(SessionState.REGULAR, info.state)
        assertEquals(true, info.isLiveTrading)
        assertEquals("LIVE (RTH)", info.sessionLabel)
    }

    @Test
    fun testIndianMarketSessionPreMarket() {
        val istTime = LocalDateTime.of(2026, 8, 26, 9, 5)
        val epochMillis = istTime.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()

        val info = engine.getSessionInfo(MarketType.IN, epochMillis)

        assertNotNull(info)
        assertEquals(SessionState.PRE_MARKET, info.state)
        assertEquals(true, info.isLiveTrading)
        assertEquals("PRE-MARKET", info.sessionLabel)
    }

    @Test
    fun testIndianMarketWeekendClosed() {
        val istTime = LocalDateTime.of(2026, 8, 30, 12, 0)
        val epochMillis = istTime.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()

        val info = engine.getSessionInfo(MarketType.IN, epochMillis)

        assertNotNull(info)
        assertEquals(SessionState.WEEKEND, info.state)
        assertEquals(false, info.isLiveTrading)
        assertEquals("WEEKEND CLOSED", info.sessionLabel)
    }
}
