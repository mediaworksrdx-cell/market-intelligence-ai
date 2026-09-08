package com.marketintelligence.ai.data.source

import com.marketintelligence.ai.data.source.remote.MarketApiService
import com.marketintelligence.tradeengine.models.Candle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoricalDataOrchestrator(
    private val marketApiService: MarketApiService,
    private val candleRepository: CandleRepository
) {

    /**
     * Fetches historical candles from the remote API, validates them, and stores them in the local database.
     * This function also handles basic validation and relies on the database's conflict strategy to prevent duplicates.
     *
     * @param symbol The trading symbol (e.g., "BTCUSD").
     * @param timeframe The candle timeframe (e.g., "1m").
     * @param from The start time (inclusive) of the historical data range in Unix timestamp (milliseconds).
     * @param to The end time (exclusive) of the historical data range in Unix timestamp (milliseconds).
     * @param type The type of instrument (e.g., "STOCK", "CRYPTO").
     */
    suspend fun fetchAndStoreHistoricalData(symbol: String, timeframe: String, from: Long, to: Long, type: String) {
        withContext(Dispatchers.IO) {
            // Step 1: Fetch candles from REST API
            val remoteCandles = marketApiService.getCandles(symbol, timeframe, from, to, type)

            if (remoteCandles.isNotEmpty()) {
                // Step 2: Validate candles (basic validation)
                val validCandles = validateCandles(remoteCandles)

                // Step 3 & 4: Remove duplicates and Save in database
                // The DAO's `OnConflictStrategy.REPLACE` handles duplicates automatically based on the primary key.
                candleRepository.insertCandles(validCandles)
            }
            // Step 6: Gap filling can be implemented here by analyzing the returned candles
            // and fetching missing ranges if necessary. This can be complex, so for now we assume
            // the API returns continuous data for the requested range.
        }
    }

    /**
     * Performs basic validation on a list of candles.
     */
    private fun validateCandles(candles: List<Candle>): List<Candle> {
        // Filter out candles with invalid data, like non-positive prices or volumes.
        return candles.filter {
            it.open > 0 && it.high > 0 && it.low > 0 && it.close > 0 && it.volume >= 0 && it.openTime > 0 && it.closeTime > 0 && it.openTime < it.closeTime
        }
    }

    /**
     * Loads candles from the database for a specific time range.
     * This supports pagination by allowing loading of different time windows.
     *
     * @param symbol The trading symbol.
     * @param timeframe The candle timeframe.
     * @param from The start time of the range.
     * @param to The end time of the range.
     * @return A list of candles from the database.
     */
    suspend fun loadCandlesFromDb(symbol: String, timeframe: String, from: Long, to: Long): List<Candle> {
        return withContext(Dispatchers.IO) {
            candleRepository.getCandles(symbol, timeframe, from, to)
        }
    }
}
