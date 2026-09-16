package com.example.marketintelligence.data.source

import com.example.marketintelligence.data.source.remote.MarketApiService
import com.example.tradeengine.models.Candle
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
    suspend fun fetchAndStoreHistoricalData(symbol: String, timeframe: String, from: Long, to: Long, type: String): List<Candle> {
        return withContext(Dispatchers.IO) {
            // Step 1: Fetch candles from REST API
            val remoteCandles = try {
                marketApiService.getCandles(symbol, timeframe, from, to, type)
            } catch (e: Exception) {
                emptyList()
            }

            if (remoteCandles.isNotEmpty()) {
                val duration = when (timeframe.lowercase()) {
                    "1m" -> 60_000L
                    "5m" -> 300_000L
                    "15m" -> 900_000L
                    "30m" -> 1800_000L
                    "1h", "60m" -> 3600_000L
                    "4h" -> 14400_000L
                    "1d", "day" -> 86400_000L
                    "1w", "week" -> 7 * 86400_000L
                    "1m", "month" -> 30 * 86400_000L
                    else -> 86400_000L
                }
                val mappedCandles = remoteCandles.map { c ->
                    val resolvedCloseTime = if (c.closeTime > c.openTime) c.closeTime else c.openTime + duration
                    c.copy(symbol = symbol, timeframe = timeframe, closeTime = resolvedCloseTime)
                }
                val validCandles = validateCandles(mappedCandles)
                try {
                    candleRepository.insertCandles(validCandles)
                } catch (_: Exception) {}
                validCandles
            } else {
                emptyList()
            }
        }
    }

    /**
     * Performs basic validation on a list of candles.
     */
    private fun validateCandles(candles: List<Candle>): List<Candle> {
        // Filter out candles with invalid data, like non-positive prices or volumes.
        return candles.filter {
            it.open > 0 && it.high > 0 && it.low > 0 && it.close > 0 && it.volume >= 0 && it.openTime > 0
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
