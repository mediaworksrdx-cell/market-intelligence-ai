package com.example.tradeengine.service

import com.example.tradeengine.CoingeckoClient
import com.example.tradeengine.KiteClient
import com.example.tradeengine.live.CandleBuilder
import com.example.tradeengine.models.Candle
import com.github.benmanes.caffeine.cache.AsyncCacheLoader
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.zerodhatech.models.HistoricalData
import com.zerodhatech.models.Tick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class CandleService(private val scope: CoroutineScope) {

    private val logger = LoggerFactory.getLogger(CandleService::class.java)
    private val candleBuilders = ConcurrentHashMap<String, CandleBuilder>()

    private val historicalCache: AsyncLoadingCache<String, List<Candle>> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS)
        .maximumSize(100)
        .buildAsync(object : AsyncCacheLoader<String, List<Candle>> {
            override fun asyncLoad(key: String, executor: Executor): CompletableFuture<List<Candle>> {
                val (symbol, timeframe) = key.split("-")
                return loadCandles(symbol, timeframe)
            }
        })

    private val _candleUpdates = MutableSharedFlow<Candle>(replay = 10)
    val candleUpdates = _candleUpdates.asSharedFlow()

    fun processTick(tick: Tick) {
        val symbol = tick.instrumentToken.toString()
        val timeframe = "minute"
        val key = "$symbol-$timeframe"

        val builder = candleBuilders.getOrPut(key) {
            CandleBuilder(symbol, timeframe)
        }

        builder.addTick(tick)?.let { finishedCandle ->
            scope.launch {
                _candleUpdates.emit(finishedCandle)
            }
        }
    }

    suspend fun getCandles(symbol: String, timeframe: String): List<Candle> {
        val cacheKey = "$symbol-$timeframe"
        val historicalCandles = historicalCache.get(cacheKey).await() ?: emptyList()

        val liveCandles = candleBuilders[cacheKey]?.getCurrentCandle()?.let { listOf(it) } ?: emptyList()

        return historicalCandles + liveCandles
    }

    fun addCandles(symbol: String, timeframe: String, candles: List<Candle>) {
        val cacheKey = "$symbol-$timeframe"
        historicalCache.put(cacheKey, CompletableFuture.completedFuture(candles))
    }

    private fun loadCandles(symbol: String, timeframe: String): CompletableFuture<List<Candle>> {
        return if (symbol.endsWith(".NS")) { // A simple way to check for a stock
            val to = Date()
            val from = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.time
            val instrumentToken = 0L // Placeholder, we need a way to get the token for a symbol
            val historicalDataFuture: CompletableFuture<HistoricalData> = KiteClient.getHistoricalData(instrumentToken, timeframe, from, to)
            historicalDataFuture.thenApply {
                transformHistoricalDataToCandles(it, instrumentToken, timeframe)
            }
        } else { // Assume it's crypto
            scope.future {
                val ohlcData = CoingeckoClient.getOhlcData(symbol, "usd", 30)
                ohlcData.map { data ->
                    Candle(
                        symbol = symbol,
                        timeframe = timeframe,
                        openTime = data[0].toLong(),
                        open = data[1].toDouble(),
                        high = data[2].toDouble(),
                        low = data[3].toDouble(),
                        close = data[4].toDouble(),
                        volume = 0.0,
                        closeTime = 0,
                        isClosed = true
                    )
                }
            }
        }
    }

    private fun transformHistoricalDataToCandles(historicalData: HistoricalData, instrumentToken: Long, interval: String): List<Candle> {
        val kiteTimestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return historicalData.dataArrayList.map {
            val symbol = instrumentToken.toString()
            val parsedDate = kiteTimestampFormat.parse(it.timeStamp)
            val timestampMs = parsedDate.time
            Candle(
                symbol = symbol,
                timeframe = interval,
                openTime = timestampMs,
                open = it.open.toDouble(),
                high = it.high.toDouble(),
                low = it.low.toDouble(),
                close = it.close.toDouble(),
                volume = it.volume.toDouble(),
                closeTime = 0,
                isClosed = true
            )
        }
    }
}
