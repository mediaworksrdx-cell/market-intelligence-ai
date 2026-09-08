package com.marketintelligence.tradeengine

import com.marketintelligence.tradeengine.config.AppConfig
import com.marketintelligence.tradeengine.models.Candle
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

/**
 * Massive / Polygon Options Models
 */
@Serializable
data class OptionContract(
    val ticker: String,
    @SerialName("underlying_ticker") val underlyingTicker: String,
    @SerialName("contract_type") val contractType: String, // "call" or "put"
    @SerialName("strike_price") val strikePrice: Double,
    @SerialName("expiration_date") val expirationDate: String,
    @SerialName("shares_per_contract") val sharesPerContract: Int = 100
)

@Serializable
data class OptionsContractsResponse(
    val status: String = "",
    val results: List<OptionContract> = emptyList(),
    @SerialName("next_url") val nextUrl: String? = null
)

@Serializable
data class OptionGreeks(
    val delta: Double = 0.0,
    val gamma: Double = 0.0,
    val theta: Double = 0.0,
    val vega: Double = 0.0
)

@Serializable
data class OptionDayBar(
    val change: Double = 0.0,
    @SerialName("change_percent") val changePercent: Double = 0.0,
    val close: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val open: Double = 0.0,
    val volume: Double = 0.0,
    val vwap: Double = 0.0
)

@Serializable
data class OptionSnapshot(
    @SerialName("break_even_price") val breakEvenPrice: Double = 0.0,
    val day: OptionDayBar? = null,
    val details: OptionContract? = null,
    val greeks: OptionGreeks? = null,
    @SerialName("implied_volatility") val impliedVolatility: Double = 0.0,
    @SerialName("open_interest") val openInterest: Double = 0.0
)

@Serializable
data class OptionsChainSnapshotResponse(
    val status: String = "",
    val results: List<OptionSnapshot> = emptyList()
)

@Serializable
data class OptionAggResult(
    @SerialName("v") val volume: Double = 0.0,
    @SerialName("o") val open: Double = 0.0,
    @SerialName("c") val close: Double = 0.0,
    @SerialName("h") val high: Double = 0.0,
    @SerialName("l") val low: Double = 0.0,
    @SerialName("t") val timestamp: Long = 0L
)

@Serializable
data class OptionAggResponse(
    val ticker: String = "",
    val status: String = "",
    val results: List<OptionAggResult> = emptyList()
)

/**
 * Massive API Client dedicated EXCLUSIVELY to Options trading & analytics.
 * Enforces strict 5 calls per minute rate-limiting for Free Tier compatibility.
 */
object MassiveClient {

    private val logger = LoggerFactory.getLogger(MassiveClient::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    // --- Strict Free-Tier Rate Limiter: Maximum 5 calls per 60 seconds --- //
    private val rateLimitMutex = Mutex()
    private val callTimestamps = ArrayDeque<Long>()
    private const val MAX_CALLS_PER_MINUTE = 5
    private const val ONE_MINUTE_MS = 60_000L

    // In-memory cache to prevent burning the 5 calls/min limit on repeat requests
    private data class CachedData<T>(val timestamp: Long, val data: T)
    private val cacheTtlMs = 120_000L // 2 minutes TTL
    private val optionsChainCache = ConcurrentHashMap<String, CachedData<List<OptionSnapshot>>>()
    private val contractsCache = ConcurrentHashMap<String, CachedData<List<OptionContract>>>()

    private suspend fun awaitRateLimit() = rateLimitMutex.withLock {
        val now = System.currentTimeMillis()
        // Purge timestamps older than 60 seconds
        while (callTimestamps.isNotEmpty() && now - callTimestamps.first() > ONE_MINUTE_MS) {
            callTimestamps.removeFirst()
        }

        if (callTimestamps.size >= MAX_CALLS_PER_MINUTE) {
            val oldest = callTimestamps.first()
            val waitTime = (ONE_MINUTE_MS - (now - oldest)).coerceAtLeast(100L)
            logger.warn("Massive Options API free-tier rate limit reached (5 calls/min). Cooling down for ${waitTime}ms...")
            delay(waitTime)
        }

        callTimestamps.addLast(System.currentTimeMillis())
        logger.info("Massive Options API call permitted. Active window calls: ${callTimestamps.size}/$MAX_CALLS_PER_MINUTE")
    }

    /**
     * Fetches real-time Options Chain snapshot for an underlying asset (e.g. "SPY", "QQQ", "AAPL").
     * Includes Greeks (Delta, Gamma, Theta, Vega), Implied Volatility, and Open Interest.
     */
    suspend fun getOptionsChain(underlyingAsset: String): List<OptionSnapshot> {
        val cleanUnderlying = underlyingAsset.uppercase().trim()
        val cached = optionsChainCache[cleanUnderlying]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            logger.info("Serving Options Chain for $cleanUnderlying from memory cache (preserving 5 calls/min budget).")
            return cached.data
        }

        awaitRateLimit()
        val apiKey = AppConfig.massiveApiKey
        val url = "https://api.polygon.io/v3/snapshot/options/$cleanUnderlying"

        return try {
            val responseText = client.get(url) {
                parameter("limit", "100")
                parameter("apiKey", apiKey)
                header("Authorization", "Bearer $apiKey")
            }.bodyAsText()

            val parsed = json.decodeFromString<OptionsChainSnapshotResponse>(responseText)
            if (parsed.results.isNotEmpty()) {
                optionsChainCache[cleanUnderlying] = CachedData(System.currentTimeMillis(), parsed.results)
            }
            parsed.results
        } catch (e: Exception) {
            logger.error("Failed to fetch Options Chain for $cleanUnderlying: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches reference Options contracts for an underlying ticker (e.g. "SPY", "NVDA").
     */
    suspend fun getOptionsContracts(
        underlyingAsset: String,
        contractType: String? = null,
        expirationDate: String? = null
    ): List<OptionContract> {
        val cleanUnderlying = underlyingAsset.uppercase().trim()
        val cacheKey = "$cleanUnderlying-$contractType-$expirationDate"
        val cached = contractsCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
            logger.info("Serving Options Contracts for $cacheKey from memory cache.")
            return cached.data
        }

        awaitRateLimit()
        val apiKey = AppConfig.massiveApiKey
        val url = "https://api.polygon.io/v3/reference/options/contracts"

        return try {
            val responseText = client.get(url) {
                parameter("underlying_ticker", cleanUnderlying)
                if (!contractType.isNullOrBlank()) parameter("contract_type", contractType.lowercase())
                if (!expirationDate.isNullOrBlank()) parameter("expiration_date", expirationDate)
                parameter("limit", "100")
                parameter("apiKey", apiKey)
                header("Authorization", "Bearer $apiKey")
            }.bodyAsText()

            val parsed = json.decodeFromString<OptionsContractsResponse>(responseText)
            if (parsed.results.isNotEmpty()) {
                contractsCache[cacheKey] = CachedData(System.currentTimeMillis(), parsed.results)
            }
            parsed.results
        } catch (e: Exception) {
            logger.error("Failed to fetch Options Contracts for $cleanUnderlying: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches historical aggregate bars for a specific Option Contract (e.g. "O:SPY240621C00500000").
     */
    suspend fun getOptionAggregates(
        optionTicker: String,
        multiplier: Int = 1,
        timespan: String = "day",
        fromDate: String,
        toDate: String
    ): List<Candle> {
        val cleanTicker = if (optionTicker.startsWith("O:")) optionTicker else "O:$optionTicker"
        awaitRateLimit()
        val apiKey = AppConfig.massiveApiKey
        val url = "https://api.polygon.io/v2/aggs/ticker/$cleanTicker/range/$multiplier/$timespan/$fromDate/$toDate"

        return try {
            val responseText = client.get(url) {
                parameter("adjusted", "true")
                parameter("sort", "asc")
                parameter("limit", "5000")
                parameter("apiKey", apiKey)
                header("Authorization", "Bearer $apiKey")
            }.bodyAsText()

            val parsed = json.decodeFromString<OptionAggResponse>(responseText)
            val timeframeLabel = "${multiplier}${timespan.first()}"

            parsed.results.map { bar ->
                Candle(
                    symbol = cleanTicker,
                    timeframe = timeframeLabel,
                    openTime = bar.timestamp,
                    open = bar.open,
                    high = bar.high,
                    low = bar.low,
                    close = bar.close,
                    volume = bar.volume,
                    closeTime = bar.timestamp,
                    isClosed = true
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch aggregates for option $cleanTicker: ${e.message}", e)
            emptyList()
        }
    }
}
