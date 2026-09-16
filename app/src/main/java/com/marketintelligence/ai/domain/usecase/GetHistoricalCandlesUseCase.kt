package com.marketintelligence.ai.domain.usecase

import com.marketintelligence.ai.data.source.HistoricalDataOrchestrator
import com.marketintelligence.tradeengine.models.Candle
import javax.inject.Inject

class GetHistoricalCandlesUseCase @Inject constructor(
    private val historicalDataOrchestrator: HistoricalDataOrchestrator
) {
    suspend fun execute(symbol: String, timeframe: String, from: Long, to: Long, type: String): List<Candle> {
        // Ensure data is available in the database
        val remoteCandles = historicalDataOrchestrator.fetchAndStoreHistoricalData(symbol, timeframe, from, to, type)
        // Load data from the database
        val dbCandles = historicalDataOrchestrator.loadCandlesFromDb(symbol, timeframe, from, to)
        return if (dbCandles.isNotEmpty()) dbCandles else remoteCandles
    }
}
