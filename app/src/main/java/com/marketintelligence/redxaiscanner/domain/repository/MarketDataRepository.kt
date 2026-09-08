package com.marketintelligence.redxaiscanner.domain.repository

import com.marketintelligence.redxaiscanner.domain.model.Candle
import com.marketintelligence.redxaiscanner.domain.model.Timeframe
import kotlinx.coroutines.flow.Flow

interface MarketDataRepository {
    /**
     * Fetches a stream of the latest candle data for a list of symbols and a given timeframe.
     * The implementation will handle caching and data source abstraction.
     *
     * @param symbols The list of market symbols (e.g., "BTCUSDT", "ETHUSDT").
     * @param timeframe The candle timeframe to fetch.
     * @return A Flow emitting a map where the key is the symbol and the value is a list of candles.
     */
    fun getCandles(
        symbols: List<String>,
        timeframe: Timeframe
    ): Flow<Map<String, List<Candle>>>
}