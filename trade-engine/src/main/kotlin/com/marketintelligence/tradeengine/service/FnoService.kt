package com.marketintelligence.tradeengine.service

import com.marketintelligence.tradeengine.config.AppConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

@Serializable
data class FnoQuoteDto(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val high: Double? = null,
    val low: Double? = null,
    val volume: Long? = null,
    val timestamp: Long,
    val source: String
)

@Serializable
data class FnoContractDto(
    val strike: Double,
    val type: String, // "CE" or "PE"
    val ltp: Double,
    val change: Double,
    val changePct: Double,
    val bid: Double,
    val ask: Double,
    val volume: Long,
    val openInterest: Long,
    val impliedVolatility: Double,
    val delta: Double,
    val gamma: Double,
    val theta: Double,
    val vega: Double,
    val isITM: Boolean,
    val isATM: Boolean,
    val isOTM: Boolean
)

@Serializable
data class FnoStrikeDto(
    val strike: Double,
    val call: FnoContractDto,
    val put: FnoContractDto,
    val isATM: Boolean
)

@Serializable
data class FnoChainDto(
    val underlyingSymbol: String,
    val spotPrice: Double,
    val expiry: String,
    val lotSize: Int,
    val strikes: List<FnoStrikeDto>,
    val totalCallOI: Long,
    val totalPutOI: Long,
    val totalCallVolume: Long,
    val totalPutVolume: Long,
    val pcr: Double,
    val maxPain: Double,
    val timestamp: String,
    val source: String
)

@Serializable
data class FnoStatusDto(
    val engine: String = "TradeEngine-FnoService",
    val isIsolated: Boolean = true,
    val plan: String,
    val isVentureReady: Boolean,
    val hasDedicatedKey: Boolean,
    val activeKeyPreview: String,
    val callsPerMinuteLimit: Int,
    val status: String = "ONLINE"
)

object FnoService {
    private val logger = LoggerFactory.getLogger(FnoService::class.java)
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    // Isolated Cache specifically for F&O
    private val quoteCache = ConcurrentHashMap<String, Pair<Long, FnoQuoteDto>>()
    private val chainCache = ConcurrentHashMap<String, Pair<Long, FnoChainDto>>()

    private val isVenture: Boolean
        get() = AppConfig.twelveDataFnoPlan.equals("venture", ignoreCase = true) ||
                (AppConfig.twelveDataFnoApiKey.isNotBlank() && !AppConfig.twelveDataFnoPlan.equals("free", ignoreCase = true))

    fun getStatus(): FnoStatusDto {
        val key = AppConfig.twelveDataFnoApiKey
        val preview = if (key.isNotBlank()) {
            "${key.take(4)}...${key.takeLast(4)}"
        } else {
            "Not Configured"
        }
        return FnoStatusDto(
            plan = if (isVenture) "venture" else AppConfig.twelveDataFnoPlan,
            isVentureReady = isVenture,
            hasDedicatedKey = key.isNotBlank(),
            activeKeyPreview = preview,
            callsPerMinuteLimit = if (isVenture) 600 else 8
        )
    }

    fun getQuote(symbol: String): FnoQuoteDto {
        val symUpper = symbol.trim().uppercase()
        val cacheTtl = if (isVenture) 1500L else 30_000L
        val cached = quoteCache[symUpper]
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < cacheTtl) {
            return cached.second
        }

        // 1. Try Dedicated Twelve Data F&O Key
        val apiKey = AppConfig.twelveDataFnoApiKey
        if (apiKey.isNotBlank()) {
            try {
                val tdSym = when (symUpper) {
                    "SPX" -> "SPX"
                    "NDX" -> "NDX"
                    "DJI" -> "DJI"
                    else -> symUpper
                }
                val url = URL("https://api.twelvedata.com/quote?symbol=$tdSym&apikey=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 4000
                conn.setRequestProperty("Accept", "application/json")

                if (conn.responseCode == 200) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val element = jsonParser.parseToJsonElement(body).jsonObject
                    if (element.containsKey("close") && !element.containsKey("code")) {
                        val close = element["close"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        val prev = element["previous_close"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: close
                        val chg = element["change"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: (close - prev)
                        val pct = element["percent_change"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: (((close - prev) / max(0.01, prev)) * 100.0)

                        if (close > 0.0) {
                            val quote = FnoQuoteDto(
                                symbol = symUpper,
                                price = (close * 100).roundToLong() / 100.0,
                                change = (chg * 100).roundToLong() / 100.0,
                                changePercent = (pct * 100).roundToLong() / 100.0,
                                timestamp = now,
                                source = if (isVenture) "twelvedata-venture" else "twelvedata-standard"
                            )
                            quoteCache[symUpper] = Pair(now, quote)
                            return quote
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("[FnoService] Notice querying Twelve Data for $symUpper: ${e.message}")
            }
        }

        // 2. Yahoo Finance Failover
        try {
            val ySym = when (symUpper) {
                "SPX" -> "^GSPC"
                "NDX" -> "^NDX"
                "DJI" -> "^DJI"
                "VIX" -> "^VIX"
                else -> symUpper
            }
            val url = URL("https://query1.finance.yahoo.com/v8/finance/chart/$ySym?interval=1d&range=1d")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val priceMatch = Regex("\"regularMarketPrice\":([0-9.]+)").find(body)
                val prevMatch = Regex("\"chartPreviousClose\":([0-9.]+)").find(body)

                if (priceMatch != null) {
                    val p = priceMatch.groupValues[1].toDoubleOrNull() ?: 0.0
                    val prev = prevMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: p
                    val chg = p - prev
                    val pct = if (prev > 0) (chg / prev) * 100.0 else 0.0

                    if (p > 0.0) {
                        val quote = FnoQuoteDto(
                            symbol = symUpper,
                            price = (p * 100).roundToLong() / 100.0,
                            change = (chg * 100).roundToLong() / 100.0,
                            changePercent = (pct * 100).roundToLong() / 100.0,
                            timestamp = now,
                            source = "yahoo-fno-fallback"
                        )
                        quoteCache[symUpper] = Pair(now, quote)
                        return quote
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("[FnoService] Notice querying Yahoo for $symUpper: ${e.message}")
        }

        // 3. Fallback Benchmark Quote
        val basePrice = when (symUpper) {
            "SPX" -> 5890.75
            "NDX" -> 20520.40
            "NVDA" -> 142.50
            "AAPL" -> 232.80
            "MSFT" -> 428.50
            "TSLA" -> 260.40
            "AMZN" -> 188.60
            "GOOGL" -> 166.20
            "META" -> 595.00
            "AMD" -> 156.30
            else -> 100.0
        }
        val quote = FnoQuoteDto(
            symbol = symUpper,
            price = basePrice,
            change = 0.0,
            changePercent = 0.0,
            timestamp = now,
            source = "catalog-fallback"
        )
        return quote
    }

    fun getChain(symbol: String, expiry: String? = null): FnoChainDto {
        val symUpper = symbol.trim().uppercase()
        val exp = expiry ?: "29 OCT 2026"
        val cacheKey = "$symUpper:$exp"
        val cacheTtl = if (isVenture) 3000L else 45_000L
        val cached = chainCache[cacheKey]
        val now = System.currentTimeMillis()

        if (cached != null && (now - cached.first) < cacheTtl) {
            return cached.second
        }

        val quote = getQuote(symUpper)
        val spot = quote.price

        val step = when {
            symUpper == "SPX" || symUpper == "NDX" -> 25.0
            spot >= 500 -> 10.0
            spot >= 100 -> 5.0
            spot >= 50 -> 2.5
            else -> 1.0
        }

        val atmStrike = (round(spot / step) * step)
        val lotSize = if (symUpper == "SPX" || symUpper == "NDX" || spot < 1000) 100 else 50
        val strikes = mutableListOf<FnoStrikeDto>()

        var totalCallOI = 0L
        var totalPutOI = 0L
        var totalCallVol = 0L
        var totalPutVol = 0L

        for (i in -10..10) {
            val k = (atmStrike + i * step)
            val isAtm = (k == atmStrike)
            val d = (k - spot) / spot

            val callLtp = max(0.05, if (spot > k) (spot - k) + 5.0 * exp(-abs(d) * 4) else 5.0 * exp(-abs(d) * 6))
            val putLtp = max(0.05, if (k > spot) (k - spot) + 5.0 * exp(-abs(d) * 4) else 5.0 * exp(-abs(d) * 6))

            val callOI = (15000L * exp(-abs(d) * 8)).toLong() + (if (i == 2) 35000L else 0L)
            val putOI = (15000L * exp(-abs(d) * 8)).toLong() + (if (i == -2) 42000L else 0L)
            val callVol = (callOI * 0.35).toLong()
            val putVol = (putOI * 0.38).toLong()

            totalCallOI += callOI
            totalPutOI += putOI
            totalCallVol += callVol
            totalPutVol += putVol

            val delta = max(0.01, min(0.99, 0.5 - d * 2.5))

            strikes.add(
                FnoStrikeDto(
                    strike = k,
                    call = FnoContractDto(
                        strike = k,
                        type = "CE",
                        ltp = (callLtp * 100).roundToLong() / 100.0,
                        change = 0.0,
                        changePct = 0.0,
                        bid = max(0.01, callLtp - 0.10),
                        ask = callLtp + 0.10,
                        volume = callVol,
                        openInterest = callOI,
                        impliedVolatility = 16.5,
                        delta = (delta * 1000).roundToLong() / 1000.0,
                        gamma = 0.005,
                        theta = -0.15,
                        vega = 0.22,
                        isITM = spot > k,
                        isATM = isAtm,
                        isOTM = spot <= k
                    ),
                    put = FnoContractDto(
                        strike = k,
                        type = "PE",
                        ltp = (putLtp * 100).roundToLong() / 100.0,
                        change = 0.0,
                        changePct = 0.0,
                        bid = max(0.01, putLtp - 0.10),
                        ask = putLtp + 0.10,
                        volume = putVol,
                        openInterest = putOI,
                        impliedVolatility = 18.2,
                        delta = ((delta - 1.0) * 1000).roundToLong() / 1000.0,
                        gamma = 0.005,
                        theta = -0.14,
                        vega = 0.22,
                        isITM = spot < k,
                        isATM = isAtm,
                        isOTM = spot >= k
                    ),
                    isATM = isAtm
                )
            )
        }

        val pcr = if (totalCallOI > 0) (totalPutOI.toDouble() / totalCallOI.toDouble()) else 1.0

        val chain = FnoChainDto(
            underlyingSymbol = symUpper,
            spotPrice = spot,
            expiry = exp,
            lotSize = lotSize,
            strikes = strikes,
            totalCallOI = totalCallOI,
            totalPutOI = totalPutOI,
            totalCallVolume = totalCallVol,
            totalPutVolume = totalPutVol,
            pcr = (pcr * 100).roundToLong() / 100.0,
            maxPain = atmStrike,
            timestamp = java.time.Instant.now().toString(),
            source = quote.source
        )

        chainCache[cacheKey] = Pair(now, chain)
        return chain
    }
}
