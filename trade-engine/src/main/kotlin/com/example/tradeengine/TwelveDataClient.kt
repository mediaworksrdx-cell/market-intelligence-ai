package com.example.tradeengine

import com.example.tradeengine.config.AppConfig
import com.example.tradeengine.models.Candle
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class TwelveDataTimeSeriesResponse(
    val meta: TwelveDataMeta? = null,
    val values: List<TwelveDataValue>? = null,
    val status: String = "",
    val message: String? = null,
    val code: Int? = null
)

@Serializable
data class TwelveDataMeta(
    val symbol: String = "",
    val interval: String = "",
    val currency: String = "",
    @SerialName("exchange_timezone") val exchangeTimezone: String = ""
)

@Serializable
data class TwelveDataValue(
    val datetime: String,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: String? = "0"
)

@Serializable
data class TwelveDataQuoteResponse(
    val symbol: String = "",
    val name: String = "",
    val open: String? = null,
    val high: String? = null,
    val low: String? = null,
    val close: String? = null,
    val volume: String? = null,
    val previous_close: String? = null,
    val change: String? = null,
    val percent_change: String? = null,
    val status: String = ""
)

object TwelveDataClient {

    private val logger = LoggerFactory.getLogger(TwelveDataClient::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private const val MAX_CALLS_PER_MINUTE = 6
    private const val ONE_MINUTE_MS = 60_000L
    private const val MIN_REQUEST_INTERVAL_MS = 8_000L
    private const val MAX_DAILY_CALLS = 750

    private val rateLimitMutex = Mutex()
    private val callTimestamps = ArrayDeque<Long>()
    private var lastRequestTimeMs = 0L

    private val dailyCallCount = AtomicInteger(0)
    private var currentDayOfYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_YEAR)

    private data class CachedCandles(val timestamp: Long, val candles: List<Candle>)
    private const val EOD_CACHE_TTL_MS = 6 * 3600 * 1000L
    private val candleCache = ConcurrentHashMap<String, CachedCandles>()

    private data class CachedQuote(val timestamp: Long, val quote: TwelveDataQuoteResponse)
    private val quoteCache = ConcurrentHashMap<String, CachedQuote>()

    val symbolMap = mapOf(
        "SPX" to "SPY",
        "^GSPC" to "SPY",
        "S&P 500" to "SPY",
        "NDX" to "QQQ",
        "IXIC" to "QQQ",
        "COMP" to "QQQ",
        "NASDAQ" to "QQQ",
        "DJI" to "DIA",
        "^DJI" to "DIA",
        "DOW" to "DIA",
        "RUT" to "IWM",
        "RUSSELL" to "IWM"
    )

    val supportedSymbols = setOf(
        "SPX", "NDX", "DJI", "IXIC", "SPY", "QQQ", "DIA", "IWM",
        "AAPL", "MSFT", "NVDA", "TSLA", "AMZN", "GOOGL", "META",
        "NFLX", "AMD", "INTC", "BRK.B", "JPM", "V", "WMT", "DIS"
    )

    fun resolveSymbol(rawSymbol: String): String {
        val clean = rawSymbol.uppercase()
            .removeSuffix(".US")
            .removeSuffix("-USD")
            .replace("^", "")
            .trim()
        return symbolMap[clean] ?: clean
    }

    fun getFallbackPrice(symbol: String): Double {
        return when (resolveSymbol(symbol)) {
            "SPY" -> 765.0
            "QQQ" -> 715.0
            "DIA" -> 520.0
            "IWM" -> 245.0
            "AAPL" -> 343.5
            "MSFT" -> 529.0
            "NVDA" -> 214.1
            "TSLA" -> 354.7
            "AMZN" -> 250.0
            "GOOGL" -> 340.6
            "META" -> 733.4
            "NFLX" -> 705.3
            "AMD" -> 550.2
            "AVGO" -> 329.4
            "JPM" -> 337.5
            "LLY" -> 1256.8
            "COST" -> 989.1
            "COIN" -> 173.2
            "XOM" -> 174.6
            "DIS" -> 110.6
            "WMT" -> 118.8
            "V" -> 402.7
            else -> 150.0
        }
    }

    private suspend fun awaitRateLimit() = rateLimitMutex.withLock {
        val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_YEAR)
        if (today != currentDayOfYear) {
            currentDayOfYear = today
            dailyCallCount.set(0)
            logger.info("Twelve Data daily quota counter reset for day $today.")
        }

        if (dailyCallCount.get() >= MAX_DAILY_CALLS) {
            logger.warn("Twelve Data daily safety threshold reached (${dailyCallCount.get()}/$MAX_DAILY_CALLS). Skipping external API call to protect quota.")
            throw IllegalStateException("Twelve Data daily API limit reached ($MAX_DAILY_CALLS calls). Serving from cache.")
        }

        val now = System.currentTimeMillis()

        val elapsedSinceLast = now - lastRequestTimeMs
        if (elapsedSinceLast < MIN_REQUEST_INTERVAL_MS) {
            val waitSpacing = MIN_REQUEST_INTERVAL_MS - elapsedSinceLast
            delay(waitSpacing)
        }

        val freshNow = System.currentTimeMillis()
        while (callTimestamps.isNotEmpty() && freshNow - callTimestamps.first() > ONE_MINUTE_MS) {
            callTimestamps.removeFirst()
        }

        if (callTimestamps.size >= MAX_CALLS_PER_MINUTE) {
            val oldest = callTimestamps.first()
            val waitTime = (ONE_MINUTE_MS - (freshNow - oldest)).coerceAtLeast(500L)
            delay(waitTime)
        }

        val callTime = System.currentTimeMillis()
        callTimestamps.addLast(callTime)
        lastRequestTimeMs = callTime
        dailyCallCount.incrementAndGet()
    }

    suspend fun getDailyCandles(rawSymbol: String, outputSize: Int = 100): List<Candle> {
        val resolved = resolveSymbol(rawSymbol)
        val cacheKey = "$resolved-1day-$outputSize"
        val now = System.currentTimeMillis()

        val cached = candleCache[cacheKey]
        if (cached != null && (now - cached.timestamp < EOD_CACHE_TTL_MS)) {
            return cached.candles
        }

        val apiKey = AppConfig.twelveDataApiKey
        if (apiKey.isBlank()) {
            return cached?.candles ?: emptyList()
        }

        return try {
            awaitRateLimit()

            val url = "https://api.twelvedata.com/time_series?symbol=$resolved&interval=1day&outputsize=$outputSize&apikey=$apiKey"
            val response: TwelveDataTimeSeriesResponse = client.get(url).body()

            if (response.status.equals("ok", ignoreCase = true) && !response.values.isNullOrEmpty()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("America/New_York")
                }

                val candles = response.values.mapNotNull { v ->
                    try {
                        val parsedDate = dateFormat.parse(v.datetime)
                        val openTime = parsedDate?.time ?: return@mapNotNull null
                        val closeTime = openTime + 86_400_000L - 1L
                        Candle(
                            symbol = rawSymbol.uppercase(),
                            timeframe = "1d",
                            openTime = openTime,
                            open = v.open.toDouble(),
                            high = v.high.toDouble(),
                            low = v.low.toDouble(),
                            close = v.close.toDouble(),
                            volume = v.volume?.toDoubleOrNull() ?: 0.0,
                            closeTime = closeTime,
                            isClosed = true
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.openTime }

                if (candles.isNotEmpty()) {
                    candleCache[cacheKey] = CachedCandles(now, candles)
                    candles
                } else {
                    cached?.candles ?: emptyList()
                }
            } else {
                cached?.candles ?: emptyList()
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch Twelve Data EOD candles for $resolved: ${e.message}")
            cached?.candles ?: emptyList()
        }
    }

    suspend fun getQuote(rawSymbol: String): TwelveDataQuoteResponse? {
        val resolved = resolveSymbol(rawSymbol)
        val now = System.currentTimeMillis()

        val cached = quoteCache[resolved]
        if (cached != null && (now - cached.timestamp < 15 * 60 * 1000L)) {
            return cached.quote
        }

        val apiKey = AppConfig.twelveDataApiKey
        if (apiKey.isBlank()) return null

        return try {
            awaitRateLimit()
            val url = "https://api.twelvedata.com/quote?symbol=$resolved&apikey=$apiKey"
            val quote: TwelveDataQuoteResponse = client.get(url).body()
            if (quote.status.equals("ok", ignoreCase = true) || quote.close != null) {
                quoteCache[resolved] = CachedQuote(now, quote)
                quote
            } else {
                cached?.quote
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch Twelve Data quote for $resolved: ${e.message}")
            cached?.quote
        }
    }

    private val macroCacheTtlMs = 30 * 60 * 1000L
    private var lastMacroFetchTimeMs = 0L
    private var cachedMacroData: List<com.example.tradeengine.api.MacroData> = listOf(
        com.example.tradeengine.api.MacroData("DXY", "104.20"),
        com.example.tradeengine.api.MacroData("XAU/USD", "4164.70"),
        com.example.tradeengine.api.MacroData("XAG/USD", "51.50"),
        com.example.tradeengine.api.MacroData("US10Y", "4.25%"),
        com.example.tradeengine.api.MacroData("BRENT", "67.20"),
        com.example.tradeengine.api.MacroData("VIX", "17.75")
    )

    suspend fun getLiveMacroData(): List<com.example.tradeengine.api.MacroData> {
        val now = System.currentTimeMillis()
        if (now - lastMacroFetchTimeMs < macroCacheTtlMs && lastMacroFetchTimeMs > 0L) {
            return cachedMacroData
        }

        val apiKey = AppConfig.twelveDataApiKey
        if (apiKey.isBlank()) return cachedMacroData

        return try {
            val list = mutableListOf<com.example.tradeengine.api.MacroData>()

            val goldQuote = getQuote("XAU/USD")
            val goldVal = goldQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "4164.70"
            list.add(com.example.tradeengine.api.MacroData("XAU/USD", goldVal))

            val slvQuote = getQuote("SLV")
            val slvVal = slvQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "51.50"
            list.add(com.example.tradeengine.api.MacroData("XAG/USD", slvVal))

            val uupQuote = getQuote("UUP")
            val dxyVal = if (uupQuote?.percent_change != null) {
                val pct = uupQuote.percent_change.toDoubleOrNull() ?: 0.0
                String.format(Locale.US, "%.2f", 104.20 * (1.0 + pct / 100.0))
            } else "104.20"
            list.add(com.example.tradeengine.api.MacroData("DXY", dxyVal))

            val iefQuote = getQuote("IEF")
            val us10yVal = if (iefQuote?.percent_change != null) {
                val pct = iefQuote.percent_change.toDoubleOrNull() ?: 0.0
                String.format(Locale.US, "%.2f%%", (4.25 - (pct * 0.05)).coerceIn(3.0, 6.0))
            } else "4.25%"
            list.add(com.example.tradeengine.api.MacroData("US10Y", us10yVal))

            val bnoQuote = getQuote("BNO")
            val brentVal = bnoQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "67.20"
            list.add(com.example.tradeengine.api.MacroData("BRENT", brentVal))

            val vixyQuote = getQuote("VIXY")
            val vixVal = vixyQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "17.75"
            list.add(com.example.tradeengine.api.MacroData("VIX", vixVal))

            cachedMacroData = list
            lastMacroFetchTimeMs = now
            logger.info("Updated live Macro Tracker data from Twelve Data: $list")
            list
        } catch (e: Exception) {
            logger.error("Failed to fetch live macro data: ${e.message}")
            cachedMacroData
        }
    }
}
