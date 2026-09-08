package com.marketintelligence.tradeengine

import com.marketintelligence.tradeengine.models.Candle
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * Ticker response model from Binance API.
 */
@Serializable
data class TickerData(
    val symbol: String,
    val priceChange: String,
    val priceChangePercent: String,
    val lastPrice: String,
    val volume: String,
    val highPrice: String,
    val lowPrice: String
)

/**
 * Binance REST API Client for fetching public market data.
 */
object BinanceClient {

    private val jsonConfig = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { 
            json(jsonConfig)
        }
    }
    
    // Concurrency limit to avoid being rate-limited by Binance API
    private val requestSemaphore = Semaphore(5)

    /**
     * Fetches candlestick data (klines) for a specific symbol and interval.
     * 
     * @param symbol The trading pair symbol, e.g., "BTCUSDT".
     * @param interval The timeframe interval, e.g., "15m", "1h".
     * @param limit The number of candles to fetch (default: 500, max: 1000).
     */
    suspend fun getKlines(symbol: String, interval: String, limit: Int = 500): List<Candle> {
        val responseText = client.get("https://api.binance.com/api/v3/klines") {
            url {
                parameters.append("symbol", symbol)
                parameters.append("interval", interval)
                parameters.append("limit", limit.toString())
            }
        }.bodyAsText()

        val jsonArray = jsonConfig.parseToJsonElement(responseText).jsonArray

        return jsonArray.map { element ->
            val klineArray = element.jsonArray
            // Note: Binance kline array indices:
            // 0: openTime, 1: open, 2: high, 3: low, 4: close, 5: volume, 6: closeTime, ...
            Candle(
                symbol = symbol,
                timeframe = interval,
                openTime = klineArray[0].jsonPrimitive.long,
                open = klineArray[1].jsonPrimitive.double,
                high = klineArray[2].jsonPrimitive.double,
                low = klineArray[3].jsonPrimitive.double,
                close = klineArray[4].jsonPrimitive.double,
                volume = klineArray[5].jsonPrimitive.double,
                closeTime = klineArray[6].jsonPrimitive.long,
                isClosed = true // Assuming historical candles are closed
            )
        }
    }

    /**
     * Fetches 24-hour rolling window price change statistics.
     * 
     * @param symbol The trading pair symbol, e.g., "BTCUSDT".
     */
    suspend fun get24hrTicker(symbol: String): TickerData {
        val responseText = client.get("https://api.binance.com/api/v3/ticker/24hr") {
            url {
                parameters.append("symbol", symbol)
            }
        }.bodyAsText()
        
        return jsonConfig.decodeFromString(TickerData.serializer(), responseText)
    }

    /**
     * Concurrently fetches klines for multiple symbols while respecting rate limits.
     * 
     * @param symbols List of trading pair symbols.
     * @param interval The timeframe interval for the candles.
     * @param limit The number of candles to fetch per symbol.
     */
    suspend fun getMultipleKlines(
        symbols: List<String>, 
        interval: String, 
        limit: Int = 500
    ): Map<String, List<Candle>> = coroutineScope {
        symbols.map { symbol ->
            async {
                requestSemaphore.withPermit {
                    symbol to getKlines(symbol, interval, limit)
                }
            }
        }.associate { it.await() }
    }
}
