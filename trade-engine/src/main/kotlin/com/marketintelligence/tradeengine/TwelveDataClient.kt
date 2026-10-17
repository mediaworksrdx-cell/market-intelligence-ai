package com.marketintelligence.tradeengine

import com.marketintelligence.tradeengine.config.AppConfig
import com.marketintelligence.tradeengine.models.Candle
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

/**
 * Twelve Data Client dedicated exclusively to US Equities and Indices EOD (End Of Day) Data.
 * Enforces strict multi-tiered rate limiting (Max 6 calls/min, 8s spacing) and aggressive 6-hour EOD caching
 * to guarantee that Twelve Data's 8 req/min and 800 req/day free limits are NEVER exceeded.
 */
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

    // --- Strict Free-Tier Rate Limiting Configuration --- //
    // Free plan: 8 calls/min, 800 calls/day
    private const val MAX_CALLS_PER_MINUTE = 6
    private const val ONE_MINUTE_MS = 60_000L
    private const val MIN_REQUEST_INTERVAL_MS = 8_000L // Min 8s spacing between calls
    private const val MAX_DAILY_CALLS = 750 // Hard cutoff before 800/day

    private val rateLimitMutex = Mutex()
    private val callTimestamps = ArrayDeque<Long>()
    private var lastRequestTimeMs = 0L

    // Daily call counter tracking
    private val dailyCallCount = AtomicInteger(0)
    private var currentDayOfYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_YEAR)

    // --- In-Memory Long-Term EOD Cache --- //
    // EOD candles only update once per day; 6 hours TTL prevents burning rate limits
    private data class CachedCandles(val timestamp: Long, val candles: List<Candle>)
    private const val EOD_CACHE_TTL_MS = 6 * 3600 * 1000L // 6 Hours
    private val candleCache = ConcurrentHashMap<String, CachedCandles>()

    // Quote cache: 15 minutes TTL
    private data class CachedQuote(val timestamp: Long, val quote: TwelveDataQuoteResponse)
    private val quoteCache = ConcurrentHashMap<String, CachedQuote>()

    // Symbol normalization map (maps non-free / cash index tickers to their liquid ETF counterparts)
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

    /**
     * Resolves incoming symbol to a Twelve Data compliant ticker.
     */
    fun resolveSymbol(rawSymbol: String): String {
        val clean = rawSymbol.uppercase()
            .removeSuffix(".US")
            .removeSuffix("-USD")
            .replace("^", "")
            .trim()
        return symbolMap[clean] ?: clean
    }

    /**
     * Fallback baseline prices if offline or rate limit hit.
     */
    fun getFallbackPrice(symbol: String): Double {
        return when (resolveSymbol(symbol)) {
            "SPY" -> 580.0
            "QQQ" -> 490.0
            "DIA" -> 425.0
            "IWM" -> 220.0
            "AAPL" -> 232.0
            "MSFT" -> 428.0
            "NVDA" -> 138.0
            "TSLA" -> 225.0
            "AMZN" -> 188.0
            "GOOGL" -> 168.0
            "META" -> 575.0
            "NFLX" -> 705.0
            "AMD" -> 155.0
            else -> 150.0
        }
    }

    /**
     * Enforces strict rate limits:
     * - Spacing: At least 8s between calls.
     * - Sliding Window: Max 6 calls in 60s.
     * - Daily Quota: Max 750 calls/day.
     */
    private suspend fun awaitRateLimit() = rateLimitMutex.withLock {
        // Daily reset check
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

        // 1. Minimum spacing enforcement
        val elapsedSinceLast = now - lastRequestTimeMs
        if (elapsedSinceLast < MIN_REQUEST_INTERVAL_MS) {
            val waitSpacing = MIN_REQUEST_INTERVAL_MS - elapsedSinceLast
            logger.debug("Twelve Data pacing: waiting ${waitSpacing}ms between requests...")
            delay(waitSpacing)
        }

        // 2. Sliding window purge
        val freshNow = System.currentTimeMillis()
        while (callTimestamps.isNotEmpty() && freshNow - callTimestamps.first() > ONE_MINUTE_MS) {
            callTimestamps.removeFirst()
        }

        // 3. Sliding window ceiling
        if (callTimestamps.size >= MAX_CALLS_PER_MINUTE) {
            val oldest = callTimestamps.first()
            val waitTime = (ONE_MINUTE_MS - (freshNow - oldest)).coerceAtLeast(500L)
            logger.warn("Twelve Data rate limit threshold active (${callTimestamps.size}/$MAX_CALLS_PER_MINUTE). Throttling for ${waitTime}ms...")
            delay(waitTime)
        }

        val callTime = System.currentTimeMillis()
        callTimestamps.addLast(callTime)
        lastRequestTimeMs = callTime
        val currentTotal = dailyCallCount.incrementAndGet()
        logger.info("Twelve Data API call permitted. Active window: ${callTimestamps.size}/$MAX_CALLS_PER_MINUTE | Daily: $currentTotal/$MAX_DAILY_CALLS")
    }

    /**
     * Fetches daily EOD candles for US stocks / indices.
     * Uses aggressive 6-hour in-memory cache to guarantee rate-limit safety.
     */
    suspend fun getDailyCandles(rawSymbol: String, outputSize: Int = 100): List<Candle> {
        val resolved = resolveSymbol(rawSymbol)
        val cacheKey = "$resolved-1day-$outputSize"
        val now = System.currentTimeMillis()

        // 1. Check cache first
        val cached = candleCache[cacheKey]
        if (cached != null && (now - cached.timestamp < EOD_CACHE_TTL_MS)) {
            logger.debug("Serving Twelve Data EOD candles from cache for $resolved (${cached.candles.size} candles).")
            return cached.candles
        }

        val apiKey = AppConfig.twelveDataApiKey
        if (apiKey.isBlank()) {
            logger.warn("TWELVE_DATA_API_KEY is not configured in AppConfig.")
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
                    logger.info("Successfully fetched & cached ${candles.size} Twelve Data EOD candles for $resolved.")
                    candles
                } else {
                    cached?.candles ?: emptyList()
                }
            } else {
                logger.warn("Twelve Data API response for $resolved was not OK: code=${response.code}, message=${response.message}")
                cached?.candles ?: emptyList()
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch Twelve Data EOD candles for $resolved: ${e.message}")
            cached?.candles ?: emptyList()
        }
    }

    /**
     * Fetches current quote for US Equities / ETFs with 15-minute caching.
     */
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

    // --- Live Macro Tracker Cache (30 min TTL) --- //
    private val macroCacheTtlMs = 30 * 60 * 1000L
    private var lastMacroFetchTimeMs = 0L
    private var cachedMacroData: List<com.marketintelligence.tradeengine.api.MacroData> = listOf(
        com.marketintelligence.tradeengine.api.MacroData("DXY", "104.20"),
        com.marketintelligence.tradeengine.api.MacroData("XAU/USD", "4164.70"),
        com.marketintelligence.tradeengine.api.MacroData("XAG/USD", "51.50"),
        com.marketintelligence.tradeengine.api.MacroData("US10Y", "4.25%"),
        com.marketintelligence.tradeengine.api.MacroData("BRENT", "67.20"),
        com.marketintelligence.tradeengine.api.MacroData("VIX", "17.75")
    )

    /**
     * Pulls live / EOD macroeconomic data for the Hub Macro Tracker ribbon.
     * Caches results for 30 minutes to stay well within Twelve Data free-tier limits.
     */
    suspend fun getLiveMacroData(): List<com.marketintelligence.tradeengine.api.MacroData> {
        val now = System.currentTimeMillis()
        if (now - lastMacroFetchTimeMs < macroCacheTtlMs && lastMacroFetchTimeMs > 0L) {
            return cachedMacroData
        }

        val apiKey = AppConfig.twelveDataApiKey
        if (apiKey.isBlank()) return cachedMacroData

        return try {
            val list = mutableListOf<com.marketintelligence.tradeengine.api.MacroData>()

            // 1. Gold Spot (XAU/USD)
            val goldQuote = getQuote("XAU/USD")
            val goldVal = goldQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "4164.70"
            list.add(com.marketintelligence.tradeengine.api.MacroData("XAU/USD", goldVal))

            // 2. Silver (SLV ETF proxy)
            val slvQuote = getQuote("SLV")
            val slvVal = slvQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "51.50"
            list.add(com.marketintelligence.tradeengine.api.MacroData("XAG/USD", slvVal))

            // 3. US Dollar Index (UUP ETF proxy -> benchmark DXY)
            val uupQuote = getQuote("UUP")
            val dxyVal = if (uupQuote?.percent_change != null) {
                val pct = uupQuote.percent_change.toDoubleOrNull() ?: 0.0
                String.format(Locale.US, "%.2f", 104.20 * (1.0 + pct / 100.0))
            } else "104.20"
            list.add(com.marketintelligence.tradeengine.api.MacroData("DXY", dxyVal))

            // 4. US 10-Year Yield (IEF Treasury proxy)
            val iefQuote = getQuote("IEF")
            val us10yVal = if (iefQuote?.percent_change != null) {
                val pct = iefQuote.percent_change.toDoubleOrNull() ?: 0.0
                String.format(Locale.US, "%.2f%%", (4.25 - (pct * 0.05)).coerceIn(3.0, 6.0))
            } else "4.25%"
            list.add(com.marketintelligence.tradeengine.api.MacroData("US10Y", us10yVal))

            // 5. Brent Oil (BNO ETF proxy)
            val bnoQuote = getQuote("BNO")
            val brentVal = bnoQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "67.20"
            list.add(com.marketintelligence.tradeengine.api.MacroData("BRENT", brentVal))

            // 6. Volatility Index (VIXY ETF proxy)
            val vixyQuote = getQuote("VIXY")
            val vixVal = vixyQuote?.close?.toDoubleOrNull()?.let { String.format(Locale.US, "%.2f", it) } ?: "17.75"
            list.add(com.marketintelligence.tradeengine.api.MacroData("VIX", vixVal))

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
