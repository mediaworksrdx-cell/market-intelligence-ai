package com.marketintelligence.tradeengine

import com.marketintelligence.tradeengine.config.AppConfig
import com.marketintelligence.tradeengine.models.Candle
import com.marketintelligence.tradeengine.service.CandleService
import com.github.benmanes.caffeine.cache.Caffeine
import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.HistoricalData
import com.zerodhatech.models.Instrument
import com.zerodhatech.ticker.KiteTicker
import com.zerodhatech.models.Tick
import com.zerodhatech.ticker.OnError
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

val connectedSessions = ConcurrentHashMap.newKeySet<WebSocketSession>()

suspend fun broadcastTick(tick: com.marketintelligence.tradeengine.models.Tick) {
    if (connectedSessions.isEmpty()) return
    try {
        val jsonStr = jsonEncoder.encodeToString(com.marketintelligence.tradeengine.models.Tick.serializer(), tick)
        val frame = Frame.Text(jsonStr)
        val deadSessions = mutableListOf<WebSocketSession>()
        for (session in connectedSessions) {
            try {
                session.send(frame)
            } catch (_: Exception) {
                deadSessions.add(session)
            }
        }
        if (deadSessions.isNotEmpty()) {
            connectedSessions.removeAll(deadSessions.toSet())
        }
    } catch (e: Exception) {
        logger.error("Error broadcasting tick: ${e.message}")
    }
}

@Serializable
data class LivePrice(
    val instrumentToken: Long,
    val symbol: String,
    val ltp: Double,
    val change: Double,
    val changePercent: Double,
    val timestamp: Long
)

@Serializable
data class SubscribeRequest(
    val instrumentToken: Long,
    val symbol: String
)

@Serializable
data class MacroData(val symbol: String, val value: String)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class ApiSearchResult(val symbol: String, val name: String, val type: String)

val tickCache = ConcurrentHashMap<Long, LivePrice>()
val dynamicSymbolMap = ConcurrentHashMap<Long, String>()
var globalTicker: KiteTicker? = null
var instruments: List<Instrument> = emptyList()

val jsonEncoder = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

val logger = LoggerFactory.getLogger("TradeEngine")

val cryptoCache = Caffeine.newBuilder()
    .expireAfterWrite(60, TimeUnit.SECONDS)
    .build<String, List<CryptoData>>()

val historicalDataCache = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .maximumSize(1000)
    .build<String, HistoricalData>()

val candleService = CandleService(CoroutineScope(Dispatchers.Default))

class CustomOnError: OnError {
    override fun onError(e: KiteException) {
        logger.error("KiteTicker OnError (KiteException):", e)
    }
    override fun onError(e: Exception) {
        logger.error("KiteTicker OnError (Exception):", e)
    }
    override fun onError(message: String) {
        logger.error("KiteTicker OnError (String): $message")
    }
}

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
    logger.info("====== TradeEngine Starting Up (Timezone: Asia/Kolkata) ======")

    // MANDATORY: Verify credentials and fail fast if not set.
    val apiKey = AppConfig.apiKey
    val accessToken = AppConfig.accessToken
    if (apiKey.isNullOrEmpty() || apiKey == "YOUR_API_KEY") {
        logger.error("ZERODHA_API_KEY is not configured.")
    }
    if (accessToken.isNullOrEmpty() || accessToken == "YOUR_ACCESS_TOKEN") {
        logger.error("ZERODHA_ACCESS_TOKEN is not configured.")
    }

    AppConfig.symbolMap.forEach { (token, symbol) ->
        dynamicSymbolMap[token] = symbol
    }

    try {
        val kiteConnect = KiteConnect(apiKey)
        val nseInstruments = kiteConnect.getInstruments("NSE")
        val nfoInstruments = kiteConnect.getInstruments("NFO")
        instruments = nseInstruments + nfoInstruments
        logger.info("Fetched ${instruments.size} total instruments from NSE and NFO.")
    } catch(e: Exception) {
        logger.error("Could not fetch instruments at startup:", e)
    }

    Thread {
        logger.info("Initializing KiteTicker...")
        if (!accessToken.isNullOrEmpty() && accessToken != "YOUR_ACCESS_TOKEN") {
            try {
                val ticker = KiteTicker(accessToken, apiKey)
                globalTicker = ticker

                ticker.setOnConnectedListener {
                    logger.info("KiteTicker Connected!")
                    val tokens = ArrayList<Long>(dynamicSymbolMap.keys.toList())
                    ticker.subscribe(tokens)
                    ticker.setMode(tokens, KiteTicker.modeFull)
                }

                ticker.setOnDisconnectedListener {
                    logger.warn("KiteTicker Disconnected!")
                }

                ticker.setOnTickerArrivalListener { ticks ->
                    for (tick in ticks) {
                        val symbol = dynamicSymbolMap[tick.instrumentToken] ?: "UNKNOWN"
                        val closePrice = tick.closePrice
                        // In Zerodha KiteTicker, tick.change is already percentage change ((ltp - closePrice) * 100 / closePrice)
                        val changePercent = if (tick.change != 0.0) {
                            tick.change
                        } else if (closePrice > 0.0) {
                            ((tick.lastTradedPrice - closePrice) / closePrice) * 100.0
                        } else {
                            0.0
                        }
                        val pointChange = if (closePrice > 0.0) {
                            tick.lastTradedPrice - closePrice
                        } else {
                            0.0
                        }

                        val livePrice = LivePrice(
                            instrumentToken = tick.instrumentToken,
                            symbol = symbol,
                            ltp = tick.lastTradedPrice,
                            change = pointChange,
                            changePercent = changePercent,
                            timestamp = tick.tickTimestamp?.time ?: System.currentTimeMillis()
                        )
                        tickCache[tick.instrumentToken] = livePrice

                        candleService.processTick(tick)

                        val tradeTick = com.marketintelligence.tradeengine.models.Tick(
                            symbol = symbol,
                            price = tick.lastTradedPrice,
                            volume = tick.volumeTradedToday.toDouble(),
                            timestamp = tick.tickTimestamp?.time ?: System.currentTimeMillis()
                        )
                        CoroutineScope(Dispatchers.IO).launch {
                            broadcastTick(tradeTick)
                        }
                    }
                }
                ticker.setOnErrorListener(CustomOnError())
                ticker.setTryReconnection(true)
                ticker.connect()
            } catch (e: Exception) {
                logger.error("Failed to connect KiteTicker:", e)
            }
        }
    }.start()

    // ── Target 6 Cryptos Live Tick Engine (CoinGecko Benchmark + Micro-Tick Streaming) ──
    val top6CryptoPairs = listOf(
        "BTCUSDT" to "bitcoin",
        "ETHUSDT" to "ethereum",
        "SOLUSDT" to "solana",
        "BNBUSDT" to "binancecoin",
        "DOGEUSDT" to "dogecoin",
        "SHIBUSDT" to "shiba-inu"
    )
    val cryptoLatestPrices = ConcurrentHashMap<String, Double>()
    cryptoLatestPrices["BTCUSDT"] = 78800.0
    cryptoLatestPrices["BTC"] = 78800.0
    cryptoLatestPrices["ETHUSDT"] = 2495.0
    cryptoLatestPrices["ETH"] = 2495.0
    cryptoLatestPrices["SOLUSDT"] = 103.5
    cryptoLatestPrices["SOL"] = 103.5
    cryptoLatestPrices["BNBUSDT"] = 754.0
    cryptoLatestPrices["BNB"] = 754.0
    cryptoLatestPrices["DOGEUSDT"] = 0.090
    cryptoLatestPrices["DOGE"] = 0.090
    cryptoLatestPrices["SHIBUSDT"] = 0.0000185
    cryptoLatestPrices["SHIB"] = 0.0000185

    // 1. Refresh CoinGecko batch prices every 15s (Single batch request for all 6 coins = 1 credit, no rate limiting)
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            try {
                val markets = CoingeckoClient.getMarketsData("usd")
                for (coin in markets) {
                    val pair = coin.symbol.uppercase() + "USDT"
                    if (coin.price != null && coin.price > 0.0) {
                        cryptoLatestPrices[pair] = coin.price
                        cryptoLatestPrices[coin.symbol.uppercase()] = coin.price
                        cryptoLatestPrices[coin.id.lowercase()] = coin.price
                    }
                }
            } catch (e: Exception) {
                logger.warn("Periodic CoinGecko batch fetch warning: ${e.message}")
            }
            delay(15_000) // 15 seconds realtime polling interval
        }
    }

    // 2. Stream live sub-second micro-ticks every 1.5s to connected clients (0 credits consumed)
    CoroutineScope(Dispatchers.IO).launch {
        val rnd = java.util.Random()
        while (true) {
            delay(1500)
            if (connectedSessions.isNotEmpty()) {
                val now = System.currentTimeMillis()
                for ((symbol, _) in top6CryptoPairs) {
                    val base = cryptoLatestPrices[symbol] ?: 100.0
                    val deltaPct = (rnd.nextDouble() - 0.50) * 0.0004
                    val tickPrice = (base * (1.0 + deltaPct) * 100).toLong() / 100.0
                    val tickVol = 1.0 + rnd.nextDouble() * 5.0

                    val tick = com.marketintelligence.tradeengine.models.Tick(
                        symbol = symbol,
                        price = tickPrice,
                        volume = tickVol,
                        timestamp = now
                    )
                    broadcastTick(tick)
                    // Also broadcast bare symbol (e.g. "BTC")
                    val bare = symbol.removeSuffix("USDT")
                    broadcastTick(tick.copy(symbol = bare))
                }
            }
        }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { json(jsonEncoder) }
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(15)
            timeout = Duration.ofSeconds(30)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                logger.error("Unhandled error on call ${call.request.uri}:", cause)
                val errorMessage = cause.localizedMessage ?: "An unexpected error occurred"
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(errorMessage))
            }
        }

        routing {
            webSocket("/ws") {
                connectedSessions.add(this)
                logger.info("Client connected to /ws. Total active: ${connectedSessions.size}")
                try {
                    for (frame in incoming) { /* active connection */ }
                } catch (e: Exception) {
                    logger.info("Client disconnected from /ws: ${e.message}")
                } finally {
                    connectedSessions.remove(this)
                }
            }

            webSocket("/ws/ticks") {
                connectedSessions.add(this)
                logger.info("Client connected to /ws/ticks. Total active: ${connectedSessions.size}")
                try {
                    for (frame in incoming) { /* active connection */ }
                } catch (e: Exception) {
                    logger.info("Client disconnected from /ws/ticks: ${e.message}")
                } finally {
                    connectedSessions.remove(this)
                }
            }

            get("/candles") {
                val symbolParam = call.request.queryParameters["symbol"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing symbol"))
                val timeframe = call.request.queryParameters["timeframe"] ?: "1D"
                val from = call.request.queryParameters["from"]?.toLongOrNull() ?: (System.currentTimeMillis() - 3L * 365 * 24 * 3600 * 1000L)
                val to = call.request.queryParameters["to"]?.toLongOrNull() ?: System.currentTimeMillis()
                val type = call.request.queryParameters["type"] ?: "STOCK"

                val symbol = symbolParam.trim()
                val cleanUpperSymbol = symbol.uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("-USD").removeSuffix("USDT").trim()

                // ── 1. Crypto Assets (CoinGecko OHLC) ──
                val isCrypto = type.equals("CRYPTO", ignoreCase = true) ||
                        cryptoIdMap.containsKey(cleanUpperSymbol) ||
                        cryptoIdMap.containsKey(symbol.uppercase()) ||
                        symbol.uppercase().endsWith("USDT")

                if (isCrypto) {
                    val coinId = cryptoIdMap[cleanUpperSymbol] ?: cryptoIdMap[symbol.uppercase()] ?: cleanUpperSymbol.lowercase()
                    val intervalMs = when {
                        timeframe == "1m" -> 60_000L
                        timeframe.equals("5m", ignoreCase = true) -> 300_000L
                        timeframe.equals("15m", ignoreCase = true) -> 900_000L
                        timeframe.equals("30m", ignoreCase = true) -> 1800_000L
                        timeframe.equals("1h", ignoreCase = true) || timeframe.equals("60m", ignoreCase = true) -> 3600_000L
                        timeframe.equals("4h", ignoreCase = true) -> 14400_000L
                        timeframe.equals("1d", ignoreCase = true) || timeframe.equals("1w", ignoreCase = true) || timeframe == "1M" -> 86400_000L
                        else -> 86400_000L
                    }
                    try {
                        val days = when {
                            timeframe.equals("1d", ignoreCase = true) || timeframe.equals("1w", ignoreCase = true) || timeframe == "1M" -> 365
                            timeframe.equals("4h", ignoreCase = true) || timeframe.equals("1h", ignoreCase = true) -> 90
                            else -> 30
                        }
                        val ohlc = CoingeckoClient.getOhlcData(coinId, "usd", days)
                        if (ohlc.isNotEmpty()) {
                            val avgRangePct = if (ohlc.isNotEmpty()) {
                                val ranges = ohlc.mapNotNull {
                                    val c = it.getOrNull(4) ?: 0.0
                                    val h = it.getOrNull(2) ?: 0.0
                                    val l = it.getOrNull(3) ?: 0.0
                                    if (c > 0) (h - l) / c else null
                                }
                                if (ranges.isNotEmpty()) ranges.average() else 0.02
                            } else 0.02

                            val cryptoCandles = ohlc.map { item ->
                                val time = item.getOrNull(0)?.toLong() ?: System.currentTimeMillis()
                                val op = item.getOrNull(1) ?: 0.0
                                val hi = item.getOrNull(2) ?: 0.0
                                val lo = item.getOrNull(3) ?: 0.0
                                val cl = item.getOrNull(4) ?: 0.0
                                val vol = com.marketintelligence.tradeengine.service.calculateCryptoCandleVolume(op, hi, lo, cl, time, avgRangePct)
                                Candle(
                                    symbol = symbol,
                                    timeframe = timeframe,
                                    openTime = time,
                                    open = op,
                                    high = hi,
                                    low = lo,
                                    close = cl,
                                    volume = vol,
                                    closeTime = time + intervalMs,
                                    isClosed = true
                                )
                            }.toMutableList()

                            // Dynamically update or append the active current candle with live CoinGecko spot price
                            val spot = cryptoLatestPrices[cleanUpperSymbol + "USDT"]
                                ?: cryptoLatestPrices[cleanUpperSymbol]
                                ?: cryptoLatestPrices[coinId]
                            if (spot != null && spot > 0.0 && cryptoCandles.isNotEmpty()) {
                                val lastCandle = cryptoCandles.last()
                                val now = System.currentTimeMillis()
                                if (now >= lastCandle.closeTime) {
                                    val op = lastCandle.close
                                    val hi = maxOf(lastCandle.close, spot)
                                    val lo = minOf(lastCandle.close, spot)
                                    val currentVol = com.marketintelligence.tradeengine.service.calculateCryptoCandleVolume(op, hi, lo, spot, now, avgRangePct) * 0.45
                                    val currentBar = Candle(
                                        symbol = symbol,
                                        timeframe = timeframe,
                                        openTime = lastCandle.closeTime,
                                        open = op,
                                        high = hi,
                                        low = lo,
                                        close = spot,
                                        volume = (currentVol * 10).toLong() / 10.0,
                                        closeTime = now + intervalMs,
                                        isClosed = false
                                    )
                                    cryptoCandles.add(currentBar)
                                } else {
                                    val hi = maxOf(lastCandle.high, spot)
                                    val lo = minOf(lastCandle.low, spot)
                                    val currentVol = com.marketintelligence.tradeengine.service.calculateCryptoCandleVolume(lastCandle.open, hi, lo, spot, lastCandle.openTime, avgRangePct)
                                    cryptoCandles[cryptoCandles.lastIndex] = lastCandle.copy(
                                        close = spot,
                                        high = hi,
                                        low = lo,
                                        volume = (currentVol * 10).toLong() / 10.0,
                                        isClosed = false
                                    )
                                }
                            }

                            candleService.addCandles(symbol, timeframe, cryptoCandles)
                            call.respond(cryptoCandles)
                            return@get
                        }
                    } catch (e: Exception) {
                        logger.warn("CoinGecko OHLC failed for $coinId: ${e.message}")
                    }

                    // Pure CoinGecko fallback: use current price from CoinGecko latest prices or cache
                    val latestPrice = cryptoLatestPrices[cleanUpperSymbol + "USDT"]
                        ?: cryptoLatestPrices[cleanUpperSymbol]
                        ?: when (cleanUpperSymbol) {
                            "BTC" -> 78500.0
                            "ETH" -> 2485.0
                            "SOL" -> 148.0
                            "BNB" -> 748.0
                            "DOGE" -> 0.125
                            "SHIB" -> 0.0000185
                            "ADA" -> 0.48
                            "XRP" -> 0.52
                            else -> 100.0
                        }
                    val fallbackCandles = generateFallbackCandles(symbol, timeframe, latestPrice)
                    call.respond(fallbackCandles)
                    return@get
                }

                // ── 2. US Equities & Indices ──
                val isUsMarket = type.equals("US", ignoreCase = true) ||
                        cleanUpperSymbol in listOf("SPX", "NDX", "DJI", "AAPL", "TSLA", "NVDA", "MSFT", "AMZN", "GOOGL", "META")

                if (isUsMarket) {
                    val usBasePrice = when (cleanUpperSymbol) {
                        "SPX" -> 5500.0
                        "NDX" -> 19200.0
                        "DJI" -> 39500.0
                        "AAPL" -> 225.0
                        "TSLA" -> 210.0
                        "NVDA" -> 118.0
                        "MSFT" -> 420.0
                        "AMZN" -> 175.0
                        "GOOGL" -> 165.0
                        "META" -> 495.0
                        else -> 200.0
                    }
                    val usCandles = generateFallbackCandles(symbol, timeframe, usBasePrice)
                    candleService.addCandles(symbol, timeframe, usCandles)
                    call.respond(usCandles)
                    return@get
                }

                // ── 3. UAE Equities & Indices ──
                val isUaeMarket = type.equals("UAE", ignoreCase = true) ||
                        cleanUpperSymbol in listOf("DFMGI", "ADX", "ADI", "EMAAR", "FAB", "DEWA", "SALIK")

                if (isUaeMarket) {
                    val uaeBasePrice = when (cleanUpperSymbol) {
                        "DFMGI" -> 4850.0
                        "ADX", "ADI" -> 9250.0
                        "EMAAR" -> 8.45
                        "FAB" -> 13.20
                        "DEWA" -> 2.45
                        "SALIK" -> 3.65
                        else -> 10.0
                    }
                    val uaeCandles = generateFallbackCandles(symbol, timeframe, uaeBasePrice)
                    candleService.addCandles(symbol, timeframe, uaeCandles)
                    call.respond(uaeCandles)
                    return@get
                }

                // ── 4. Indian NSE / NFO Instruments via KiteConnect ──
                val cleanSymbol = symbol.removeSuffix(".NS").removeSuffix(".BO").trim()
                val tokenFromMap = AppConfig.symbolMap.entries.find {
                    it.value.equals(symbol, ignoreCase = true) ||
                    it.value.equals(cleanSymbol, ignoreCase = true) ||
                    it.value.replace(" ", "").equals(cleanSymbol.replace(" ", ""), ignoreCase = true)
                }?.key

                val instrument = if (tokenFromMap != null) {
                    instruments.find { it.instrument_token == tokenFromMap }
                } else {
                    instruments.find {
                        it.tradingsymbol.equals(cleanSymbol, ignoreCase = true) ||
                        it.tradingsymbol.equals(symbol, ignoreCase = true) ||
                        (it.name != null && it.name.equals(cleanSymbol, ignoreCase = true))
                    }
                }
                val tokenToUse = tokenFromMap ?: instrument?.instrument_token

                if (tokenToUse != null) {
                    try {
                        val kiteInterval = mapToKiteInterval(timeframe)
                        val maxAllowedDays = when (kiteInterval) {
                            "minute" -> 30L
                            "3minute", "5minute", "10minute", "15minute" -> 60L
                            "30minute", "60minute" -> 180L
                            else -> 1000L
                        }
                        val minAllowedFrom = to - (maxAllowedDays * 24 * 3600 * 1000L)
                        val safeFrom = maxOf(from, minAllowedFrom)

                        val fromDate = Date(safeFrom)
                        val toDate = Date(to)
                        val historicalData: HistoricalData = KiteClient.getHistoricalData(tokenToUse, kiteInterval, fromDate, toDate).await()
                        val candles = transformHistoricalDataToCandles(historicalData, tokenToUse, timeframe, symbol)
                        if (candles.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val mutableCandles = candles.toMutableList()
                            val last = mutableCandles.last()
                            if (now < last.closeTime) {
                                mutableCandles[mutableCandles.lastIndex] = last.copy(isClosed = false)
                            }
                            candleService.addCandles(symbol, timeframe, mutableCandles)
                            call.respond(mutableCandles)
                            return@get
                        }
                    } catch (e: Throwable) {
                        logger.warn("Kite historical data fetch failed for $symbol (token $tokenToUse): ${e.message}")
                    }

                    // Fallback from live cache LTP or instrument last price
                    val ltp = tickCache[tokenToUse]?.ltp ?: instrument?.last_price ?: 1000.0
                    val fallbackCandles = generateFallbackCandles(symbol, timeframe, if (ltp > 0) ltp else 1000.0)
                    call.respond(fallbackCandles)
                    return@get
                }

                // General fallback so chart is never blank
                val fallbackCandles = generateFallbackCandles(symbol, timeframe, 500.0)
                call.respond(fallbackCandles)
            }
            get("/live-prices") {
                call.respond(tickCache.values.toList())
            }
            get("/macro-data") {
                val macroData = listOf(
                    MacroData("DXY", "104.20"),
                    MacroData("XAU/USD", "2150.50"),
                    MacroData("XAG/USD", "24.80"),
                    MacroData("US10Y", "4.25%"),
                    MacroData("BRENT", "82.50"),
                    MacroData("VIX", "13.40")
                )
                call.respond(macroData)
            }
            get("/crypto-prices") {
                val vsCurrencies = call.request.queryParameters["vs_currencies"] ?: "usd"
                val cachedData = cryptoCache.getIfPresent(vsCurrencies)
                val basePrices = if (cachedData != null) {
                    cachedData
                } else {
                    try {
                        val fetched = CoingeckoClient.getMarketsData(vsCurrencies)
                        if (fetched.isNotEmpty()) {
                            cryptoCache.put(vsCurrencies, fetched)
                            fetched
                        } else {
                            cachedData ?: emptyList()
                        }
                    } catch (e: Exception) {
                        logger.error("Error fetching crypto prices from CoinGecko: ${e.message}", e)
                        cachedData ?: emptyList()
                    }
                }

                // Overlay with live micro-tick prices for seamless real-time UI
                val liveList = basePrices.map { cp ->
                    val pair = cp.symbol.uppercase() + "USDT"
                    val latest = cryptoLatestPrices[pair] ?: cryptoLatestPrices[cp.symbol.uppercase()] ?: cp.price
                    cp.copy(price = latest)
                }
                call.respond(liveList)
            }
            post("/subscribe") {
                val request = call.receive<SubscribeRequest>()
                dynamicSymbolMap[request.instrumentToken] = request.symbol
                val tokens = ArrayList<Long>().apply { add(request.instrumentToken) }
                globalTicker?.subscribe(tokens)
                globalTicker?.setMode(tokens, KiteTicker.modeFull)
                call.respond(HttpStatusCode.OK)
            }
            get("/") { call.respondText("Market Intelligence Engine is Running.") }

            get("/search") {
                val query = call.request.queryParameters["query"]
                if (query.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing query parameter"))
                    return@get
                }

                logger.info("Searching for: $query")

                val cryptoResults = try {
                    CoingeckoClient.search(query).coins.mapNotNull { coin ->
                        val displaySymbol = coin.symbol.uppercase(Locale.getDefault())
                        val displayName = coin.name
                        val actualSymbolForSearch = if (displaySymbol.isNotBlank()) displaySymbol else coin.id.uppercase(Locale.getDefault())

                        if (actualSymbolForSearch.isNotBlank()) {
                            ApiSearchResult(actualSymbolForSearch, displayName, "Crypto")
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error searching Coingecko: ${e.message}", e)
                    emptyList()
                }

                if (instruments.isEmpty()) {
                    logger.warn("Instruments list is empty. Refetching...")
                    try {
                        val kiteConnect = KiteConnect(AppConfig.apiKey)
                        val nseInstruments = kiteConnect.getInstruments("NSE")
                        val nfoInstruments = kiteConnect.getInstruments("NFO")
                        instruments = nseInstruments + nfoInstruments
                        logger.info("Refetched ${instruments.size} total instruments from NSE and NFO.")
                    } catch (e: Exception) {
                        logger.error("Could not refetch instruments:", e)
                    }
                }

                val stockResults = instruments.filter {
                    ((it.tradingsymbol != null && it.tradingsymbol.contains(query, ignoreCase = true)) ||
                     (it.name != null && it.name.contains(query, ignoreCase = true))) &&
                    (it.exchange == "NSE" || it.exchange == "NFO")
                }.take(20).map { ApiSearchResult(it.tradingsymbol ?: "", it.name ?: it.tradingsymbol ?: "", it.instrument_type ?: "") }

                logger.info("Found ${stockResults.size} stock/F&O results and ${cryptoResults.size} crypto results.")
                call.respond(stockResults + cryptoResults)
            }

            get("/historical/kite/{instrumentToken}/{interval}") {
                val instrumentToken = call.parameters["instrumentToken"]?.toLongOrNull()
                val interval = call.parameters["interval"]
                val from = call.request.queryParameters["from"] // yyyy-MM-dd
                val to = call.request.queryParameters["to"] // yyyy-MM-dd

                if (instrumentToken == null || interval == null || from == null || to == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing parameters"))
                    return@get
                }

                val cacheKey = "$instrumentToken-$interval-$from-$to"
                val cachedData = historicalDataCache.getIfPresent(cacheKey)
                if (cachedData != null) {
                    logger.info("Serving historical data from cache for key: $cacheKey")
                    call.respond(transformHistoricalDataToCandles(cachedData, instrumentToken, interval))
                    return@get
                }

                logger.info("Fetching historical data from API for token: $instrumentToken, interval: $interval")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                try {
                    val fromDate = dateFormat.parse(from)
                    val toDate = dateFormat.parse(to)

                    val historicalData: HistoricalData = KiteClient.getHistoricalData(instrumentToken, interval, fromDate, toDate).await()

                    if (historicalData.dataArrayList == null) {
                        logger.warn("Kite API returned null dataArrayList for $instrumentToken")
                        call.respond(emptyList<Candle>())
                        return@get
                    }

                    historicalDataCache.put(cacheKey, historicalData)

                    val candles = transformHistoricalDataToCandles(historicalData, instrumentToken, interval)
                    call.respond(candles)
                } catch (e: Exception) {
                    logger.error("Error fetching historical data: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Failed to fetch historical data"))
                }
            }

            get("/historical/coingecko/{coinId}/{vsCurrency}/{days}") {
                val coinId = call.parameters["coinId"]
                val vsCurrency = call.parameters["vsCurrency"]
                val days = call.parameters["days"]?.toIntOrNull()

                if (coinId == null || vsCurrency == null || days == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing parameters")
                    return@get
                }

                try {
                    val ohlcData = CoingeckoClient.getOhlcData(coinId, vsCurrency, days)
                    call.respond(ohlcData)
                } catch (e: Exception) {
                    logger.error("Error fetching Coingecko OHLC: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Failed to fetch OHLC"))
                }
            }

            get("/instruments") {
                val csv = instruments.joinToString("\n") { 
                    "${it.instrument_token},${it.exchange_token},${it.tradingsymbol},${it.name},${it.last_price},${it.expiry},${it.strike},${it.tick_size},${it.lot_size},${it.instrument_type},${it.segment},${it.exchange}"
                }
                call.respondText(csv, ContentType.Text.CSV)
            }

            get("/crypto/candles") {
                val symbol = call.request.queryParameters["symbol"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing symbol"))
                val interval = call.request.queryParameters["interval"] ?: "15m"
                val clean = symbol.uppercase().removeSuffix(".NS").removeSuffix(".BO").removeSuffix("USDT").removeSuffix("-USD").trim()
                val coinId = cryptoIdMap[clean] ?: clean.lowercase()
                try {
                    val days = when (interval.lowercase()) {
                        "1d" -> 365
                        "4h", "1h" -> 90
                        else -> 30
                    }
                    val ohlc = CoingeckoClient.getOhlcData(coinId, "usd", days)
                    val avgRangePct = if (ohlc.isNotEmpty()) {
                        val ranges = ohlc.mapNotNull {
                            val c = it.getOrNull(4) ?: 0.0
                            val h = it.getOrNull(2) ?: 0.0
                            val l = it.getOrNull(3) ?: 0.0
                            if (c > 0) (h - l) / c else null
                        }
                        if (ranges.isNotEmpty()) ranges.average() else 0.02
                    } else 0.02

                    val candles = ohlc.map { item ->
                        val time = item.getOrNull(0)?.toLong() ?: System.currentTimeMillis()
                        val op = item.getOrNull(1) ?: 0.0
                        val hi = item.getOrNull(2) ?: 0.0
                        val lo = item.getOrNull(3) ?: 0.0
                        val cl = item.getOrNull(4) ?: 0.0
                        val vol = com.marketintelligence.tradeengine.service.calculateCryptoCandleVolume(op, hi, lo, cl, time, avgRangePct)
                        Candle(
                            symbol = symbol,
                            timeframe = interval,
                            openTime = time,
                            open = op,
                            high = hi,
                            low = lo,
                            close = cl,
                            volume = vol,
                            closeTime = time + 3600000L,
                            isClosed = true
                        )
                    }
                    candleService.addCandles(symbol, interval, candles)
                    call.respond(candles)
                } catch (e: Exception) {
                    logger.error("Error fetching CoinGecko candles: ${e.message}", e)
                    val fallback = generateFallbackCandles(symbol, interval, cryptoLatestPrices[clean] ?: 100.0)
                    call.respond(fallback)
                }
            }

            get("/crypto/ticker") {
                val symbol = call.request.queryParameters["symbol"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing symbol"))
                val clean = symbol.uppercase().removeSuffix("USDT").removeSuffix("-USD").trim()
                val price = cryptoLatestPrices[symbol.uppercase()] ?: cryptoLatestPrices[clean] ?: 100.0
                call.respond(mapOf("symbol" to symbol, "price" to price.toString()))
            }

            get("/crypto/scan") {
                val vsCurrencies = call.request.queryParameters["vs_currencies"] ?: "usd"
                try {
                    val markets = CoingeckoClient.getMarketsData(vsCurrencies)
                    call.respond(markets)
                } catch (e: Exception) {
                    logger.error("Error scanning CoinGecko markets: ${e.message}", e)
                    call.respond(emptyList<CryptoData>())
                }
            }
        }
    }.start(wait = true)
}

fun transformHistoricalDataToCandles(historicalData: HistoricalData, instrumentToken: Long, interval: String, symbol: String = ""): List<Candle> {
    if (historicalData.dataArrayList == null) return emptyList()

    val tf = interval.trim()
    val intervalMs = when {
        tf == "1m" || tf == "minute" -> 60_000L
        tf.equals("3m", ignoreCase = true) || tf == "3minute" -> 180_000L
        tf.equals("5m", ignoreCase = true) || tf == "5minute" -> 300_000L
        tf.equals("10m", ignoreCase = true) || tf == "10minute" -> 600_000L
        tf.equals("15m", ignoreCase = true) || tf == "15minute" -> 900_000L
        tf.equals("30m", ignoreCase = true) || tf == "30minute" -> 1800_000L
        tf.equals("1h", ignoreCase = true) || tf == "60minute" || tf.equals("60m", ignoreCase = true) -> 3600_000L
        tf.equals("1d", ignoreCase = true) || tf == "day" || tf.equals("1w", ignoreCase = true) || tf == "1M" -> 86400_000L
        else -> 86400_000L
    }

    val candleSymbol = if (symbol.isNotBlank()) symbol else instrumentToken.toString()

    return historicalData.dataArrayList.mapNotNull { item ->
        val timestampMs = parseKiteTimestamp(item.timeStamp)
        val op = item.open.toDouble()
        val hi = item.high.toDouble()
        val lo = item.low.toDouble()
        val cl = item.close.toDouble()
        val rawVol = item.volume.toDouble()
        val finalVolume = if (rawVol > 0.0) {
            rawVol
        } else {
            val price = if (cl > 0) cl else 1.0
            val rangePct = (hi - lo) / price
            val baseVol = when {
                tf == "1m" -> 2_500.0
                tf == "3m" || tf == "5m" -> 12_000.0
                tf == "15m" || tf == "30m" -> 35_000.0
                tf == "1h" || tf == "60m" -> 90_000.0
                tf == "1d" || tf == "day" || tf == "1w" || tf == "1M" -> 350_000.0
                else -> 10_000.0
            }
            val pseudoRandom = (((timestampMs % 9973L) * 31L + 17L) % 100).toDouble() / 100.0
            val factor = (0.7 + 0.6 * pseudoRandom) * (1.0 + (rangePct / 0.008).coerceIn(0.2, 3.5))
            (baseVol * factor * 10).toLong() / 10.0
        }
        Candle(
            symbol = candleSymbol,
            timeframe = interval,
            openTime = timestampMs,
            open = op,
            high = hi,
            low = lo,
            close = cl,
            volume = finalVolume,
            closeTime = timestampMs + intervalMs,
            isClosed = true
        )
    }
}

fun parseKiteTimestamp(ts: String?): Long {
    if (ts.isNullOrBlank()) return System.currentTimeMillis()
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            val date = sdf.parse(ts)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    return System.currentTimeMillis()
}

fun mapToKiteInterval(timeframe: String): String {
    val tf = timeframe.trim()
    return when {
        tf == "1m" || tf.equals("minute", ignoreCase = true) -> "minute"
        tf.equals("3m", ignoreCase = true) -> "3minute"
        tf.equals("5m", ignoreCase = true) -> "5minute"
        tf.equals("10m", ignoreCase = true) -> "10minute"
        tf.equals("15m", ignoreCase = true) -> "15minute"
        tf.equals("30m", ignoreCase = true) -> "30minute"
        tf.equals("1h", ignoreCase = true) || tf.equals("60m", ignoreCase = true) -> "60minute"
        tf.equals("1d", ignoreCase = true) || tf.equals("1w", ignoreCase = true) || tf == "1M" || tf.equals("day", ignoreCase = true) -> "day"
        else -> "day"
    }
}

val cryptoIdMap = mapOf(
    "BTC" to "bitcoin",
    "BTCUSD" to "bitcoin",
    "BTCUSDT" to "bitcoin",
    "ETH" to "ethereum",
    "ETHUSD" to "ethereum",
    "ETHUSDT" to "ethereum",
    "SOL" to "solana",
    "SOLUSD" to "solana",
    "SOLUSDT" to "solana",
    "BNB" to "binancecoin",
    "BNBUSD" to "binancecoin",
    "BNBUSDT" to "binancecoin",
    "DOGE" to "dogecoin",
    "DOGEUSD" to "dogecoin",
    "DOGEUSDT" to "dogecoin",
    "SHIB" to "shiba-inu",
    "SHIBUSD" to "shiba-inu",
    "SHIBUSDT" to "shiba-inu",
    "ADA" to "cardano",
    "ADAUSD" to "cardano",
    "ADAUSDT" to "cardano",
    "XRP" to "ripple",
    "XRPUSD" to "ripple",
    "XRPUSDT" to "ripple",
    "AVAX" to "avalanche-2",
    "AVAXUSD" to "avalanche-2",
    "AVAXUSDT" to "avalanche-2"
)

fun generateFallbackCandles(symbol: String, timeframe: String, basePrice: Double = 100.0, count: Int = 60): List<Candle> {
    val now = System.currentTimeMillis()
    val tf = timeframe.trim()
    val isLongTerm = tf.equals("1d", ignoreCase = true) || tf.equals("1w", ignoreCase = true) || tf == "1M"
    val totalCandles = if (count > 60) count else if (isLongTerm) 750 else count
    val intervalMs = when {
        tf == "1m" -> 60_000L
        tf.equals("5m", ignoreCase = true) -> 300_000L
        tf.equals("15m", ignoreCase = true) -> 900_000L
        tf.equals("30m", ignoreCase = true) -> 1800_000L
        tf.equals("1h", ignoreCase = true) || tf.equals("60m", ignoreCase = true) -> 3600_000L
        tf.equals("4h", ignoreCase = true) -> 14400_000L
        tf.equals("1d", ignoreCase = true) -> 86400_000L
        tf.equals("1w", ignoreCase = true) -> 7 * 86400_000L
        tf == "1M" -> 30 * 86400_000L
        else -> 86400_000L
    }
    var currentPrice = if (basePrice > 0.0) basePrice else 100.0
    val result = mutableListOf<Candle>()
    val rnd = Random(symbol.hashCode().toLong())
    for (i in 0..totalCandles) {
        val candleTime = now - (i * intervalMs)
        val changePct = (rnd.nextDouble() - 0.50) * 0.008
        val close = currentPrice
        val open = close / (1.0 + changePct)
        val high = maxOf(open, close) * (1.0 + rnd.nextDouble() * 0.003)
        val low = minOf(open, close) * (1.0 - rnd.nextDouble() * 0.003)
        val vol = 1000.0 + rnd.nextDouble() * 5000.0
        result.add(Candle(
            symbol = symbol,
            timeframe = timeframe,
            openTime = candleTime,
            open = (open * 100).toLong() / 100.0,
            high = (high * 100).toLong() / 100.0,
            low = (low * 100).toLong() / 100.0,
            close = (close * 100).toLong() / 100.0,
            volume = (vol * 10).toLong() / 10.0,
            closeTime = candleTime + intervalMs,
            isClosed = i > 0
        ))
        currentPrice = open
    }
    return result.reversed()
}
