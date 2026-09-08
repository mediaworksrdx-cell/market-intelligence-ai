package com.example.tradeengine

import com.example.tradeengine.config.AppConfig
import com.example.tradeengine.models.Candle
import com.example.tradeengine.service.CandleService
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
import kotlinx.coroutines.future.await // Import await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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
    logger.info("====== TradeEngine Starting Up ======")

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
                        val changePercent = if (closePrice != 0.0) (tick.change / closePrice) * 100 else 0.0

                        val livePrice = LivePrice(
                            instrumentToken = tick.instrumentToken,
                            symbol = symbol,
                            ltp = tick.lastTradedPrice,
                            change = tick.change,
                            changePercent = changePercent,
                            timestamp = tick.tickTimestamp?.time ?: System.currentTimeMillis()
                        )
                        tickCache[tick.instrumentToken] = livePrice

                        candleService.processTick(tick)
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

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { json(jsonEncoder) }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                logger.error("Unhandled error on call ${call.request.uri}:", cause)
                val errorMessage = cause.localizedMessage ?: "An unexpected error occurred"
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(errorMessage))
            }
        }

        routing {
            get("/candles") {
                val symbol = call.request.queryParameters["symbol"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing symbol"))
                val timeframe = call.request.queryParameters["timeframe"] ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing timeframe"))
                val from = call.request.queryParameters["from"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing or invalid from timestamp"))
                val to = call.request.queryParameters["to"]?.toLongOrNull() ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing or invalid to timestamp"))
                val instrument = instruments.find { it.tradingsymbol == symbol }
                if (instrument == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Instrument not found"))
                    return@get
                }

                val fromDate = Date(from)
                val toDate = Date(to)

                val historicalData: HistoricalData = KiteClient.getHistoricalData(instrument.instrument_token, timeframe, fromDate, toDate).await()
                val candles = transformHistoricalDataToCandles(historicalData, instrument.instrument_token, timeframe)
                
                candleService.addCandles(symbol, timeframe, candles)

                call.respond(candles)
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
                if (cachedData != null) {
                    call.respond(cachedData)
                    return@get
                }

                val prices = CoingeckoClient.getMarketsData(vsCurrencies)
                cryptoCache.put(vsCurrencies, prices)
                call.respond(prices)
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
                    (it.tradingsymbol.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true)) &&
                    (it.exchange == "NSE" || it.exchange == "NFO")
                }.take(20).map { ApiSearchResult(it.tradingsymbol, it.name, it.instrument_type) }

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

                val ohlcData = CoingeckoClient.getOhlcData(coinId, vsCurrency, days)
                call.respond(ohlcData)
            }

            get("/instruments") {
                val csv = instruments.joinToString("\n") { 
                    "${it.instrument_token},${it.exchange_token},${it.tradingsymbol},${it.name},${it.last_price},${it.expiry},${it.strike},${it.tick_size},${it.lot_size},${it.instrument_type},${it.segment},${it.exchange}"
                }
                call.respondText(csv, ContentType.Text.CSV)
            }
        }
    }.start(wait = true)
}

fun transformHistoricalDataToCandles(historicalData: HistoricalData, instrumentToken: Long, interval: String): List<Candle> {
    if (historicalData.dataArrayList == null) return emptyList()

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
