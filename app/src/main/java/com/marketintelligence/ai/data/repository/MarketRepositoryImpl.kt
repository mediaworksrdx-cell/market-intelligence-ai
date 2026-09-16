package com.marketintelligence.ai.data.repository

import android.content.Context
import android.util.Log
import com.marketintelligence.ai.data.local.MockData
import com.marketintelligence.ai.data.model.FnoData
import com.marketintelligence.ai.data.source.local.InstrumentDao
import com.marketintelligence.ai.data.source.local.WatchlistDao
import com.marketintelligence.ai.data.source.local.WatchlistEntity
import com.marketintelligence.ai.data.source.remote.CoinGeckoApiService
import com.marketintelligence.ai.data.source.remote.MarketApiService
import com.marketintelligence.ai.data.model.SubscribeRequest
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.domain.repository.MarketRepository
import com.marketintelligence.tradeengine.models.Candle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val TAG = "MarketRepo"

@OptIn(InternalSerializationApi::class, ExperimentalCoroutinesApi::class)
class MarketRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: MarketApiService,
    private val coinGeckoApiService: CoinGeckoApiService,
    private val instrumentDao: InstrumentDao,
    private val watchlistDao: WatchlistDao
) : MarketRepository {

    private var lastFetchTime = 0L
    private var cachedCryptoPrices: List<CryptoData> = emptyList()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("market_repo_prefs", Context.MODE_PRIVATE)
            val isSeeded = prefs.getBoolean("has_seeded_hub_v9", false)
            if (!isSeeded) {
                watchlistDao.clearAll()
                val initialEntities = mutableListOf<WatchlistEntity>()

                // India, US, and UAE Indices
                val allIndices = MockData.INDICES_IN + MockData.INDICES_US + MockData.INDICES_UAE
                allIndices.forEach {
                    initialEntities.add(
                        WatchlistEntity(
                            symbol = it.symbol,
                            name = it.name,
                            type = "INDEX",
                            price = it.price,
                            changePercent = it.changePercent,
                            instrumentToken = it.instrumentToken
                        )
                    )
                }

                // India, US, and UAE Stocks
                val allStocks = MockData.WATCHLIST_INITIAL + MockData.WATCHLIST_US + MockData.WATCHLIST_UAE
                allStocks.forEach {
                    initialEntities.add(
                        WatchlistEntity(
                            symbol = it.symbol,
                            name = it.name,
                            type = "STOCK",
                            price = it.price,
                            changePercent = it.changePercent,
                            instrumentToken = it.instrumentToken
                        )
                    )
                }

                MockData.CRYPTO_INITIAL.forEach {
                    initialEntities.add(
                        WatchlistEntity(
                            symbol = it.symbol,
                            name = it.name,
                            type = "CRYPTO",
                            price = it.price,
                            changePercent = it.changePercent,
                            instrumentToken = 0L
                        )
                    )
                }

                watchlistDao.insertAll(initialEntities)
                prefs.edit().putBoolean("has_seeded_hub_v9", true).apply()
            }
            // Always ensure TCS token is corrected to 2953217L
            try {
                watchlistDao.insert(
                    WatchlistEntity(
                        symbol = "TCS.NS",
                        name = "Tata Consultancy",
                        type = "STOCK",
                        price = 2200.0,
                        changePercent = -2.48,
                        instrumentToken = 2953217L
                    )
                )
            } catch (_: Exception) {}
        }
    }

    override fun getIndices(): Flow<List<IndexData>> {
        return watchlistDao.getIndices().map { list ->
            list.map { it.toIndexData() }
        }
    }

    override fun getWatchlist(): Flow<List<StockData>> {
        return watchlistDao.getStocks().map { list ->
            list.map { it.toStockData() }
        }
    }

    override fun getCryptos(): Flow<List<CryptoData>> {
        return watchlistDao.getCryptos().map { list ->
            list.map { it.toCryptoData() }
        }
    }

    override suspend fun getFnoData(symbol: String): FnoData {
        return withContext(Dispatchers.IO) {
            FnoData(
                symbol = symbol,
                spotPrice = 48000.0,
                futures = listOf(),
                options = listOf()
            )
        }
    }

    override suspend fun getLiveAnalysis(): AIAnalysisResult {
        return withContext(Dispatchers.IO) {
            throw IllegalStateException("Live analysis unavailable: no active market intelligence feed")
        }
    }

    override suspend fun getLivePrices(): List<com.marketintelligence.tradeengine.LivePrice> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getLivePrices()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get live prices", e)
                emptyList()
            }
        }
    }

    override suspend fun getCryptoLivePrices(): List<CryptoData> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFetchTime > 2000L || cachedCryptoPrices.isEmpty()) {
                var fetched: List<CryptoData> = emptyList()
                // 1. Primary: fetch from Trade Engine backend (/crypto-prices) which holds live CoinGecko authenticated & cached prices
                try {
                    val serverPrices = apiService.getCryptoPrices()
                    if (serverPrices.isNotEmpty()) {
                        fetched = serverPrices
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Trade engine crypto-prices unavailable, falling back to direct CoinGecko: ${e.message}")
                }

                // 2. Secondary fallback: direct CoinGecko API
                if (fetched.isEmpty()) {
                    try {
                        val apiPrices = coinGeckoApiService.getLiveCryptoPrices()
                        if (apiPrices.isNotEmpty()) {
                            fetched = apiPrices
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Direct CoinGecko fetch failed: ${e.message}")
                    }
                }

                if (fetched.isNotEmpty()) {
                    cachedCryptoPrices = fetched
                    lastFetchTime = currentTime
                }
            }
            if (cachedCryptoPrices.isEmpty()) {
                MockData.CRYPTO_INITIAL
            } else {
                cachedCryptoPrices
            }
        }
    }

    override suspend fun search(query: String): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank()) return@withContext emptyList()

            val allIndices = MockData.INDICES_IN + MockData.INDICES_US + MockData.INDICES_UAE
            val indexMatches = allIndices.filter {
                it.symbol.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
            }.map {
                SearchResult(it.symbol, it.name, "INDEX")
            }

            val cryptoMatches = MockData.CRYPTO_INITIAL.filter {
                it.symbol.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
            }.map {
                SearchResult(it.symbol, it.name, "CRYPTO")
            }

            val allStocks = MockData.WATCHLIST_INITIAL + MockData.WATCHLIST_US + MockData.WATCHLIST_UAE
            val mockStocks = allStocks.filter {
                it.symbol.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
            }.map {
                SearchResult(it.symbol, it.name, "STOCK")
            }

            val catalogMatches = com.marketintelligence.ai.data.util.MarketPriceCatalog.getAllKnownAssets().filter {
                it.symbol.contains(q, ignoreCase = true) || it.name.contains(q, ignoreCase = true)
            }

            val apiResults = try {
                apiService.search(q).map { SearchResult(it.symbol, it.name, it.type) }
            } catch (e: Exception) {
                emptyList()
            }

            val daoMatches = try {
                instrumentDao.search(q).map {
                    val instType = when {
                        it.segment.contains("INDICES", ignoreCase = true) -> "INDEX"
                        it.instrument_type == "EQ" -> "STOCK"
                        else -> "STOCK"
                    }
                    SearchResult(it.tradingsymbol, it.name, instType)
                }
            } catch (e: Exception) {
                emptyList()
            }

            (indexMatches + cryptoMatches + mockStocks + catalogMatches + apiResults + daoMatches)
                .distinctBy { it.symbol.uppercase() }
        }
    }

    override suspend fun getMacroData(): List<MacroData> {
        return withContext(Dispatchers.IO) {
            delay(700)
            listOf(
                MacroData("USDINR", "83.45"),
                MacroData("CRUDEOIL", "78.23"),
                MacroData("GOLD", "2350.10")
            )
        }
    }

    private fun getTokenForSymbol(symbol: String): Long {
        val clean = symbol.removeSuffix(".NS").removeSuffix(".BO").trim().uppercase()
        return when (clean) {
            "NIFTY 50", "NIFTY" -> 256265L
            "BANKNIFTY", "BANK NIFTY" -> 260105L
            "FINNIFTY", "FIN NIFTY" -> 257801L
            "SENSEX" -> 265L
            "MIDCPNIFTY" -> 257033L
            "NIFTY NEXT 50" -> 256521L
            "NIFTY IT" -> 257289L
            "RELIANCE" -> 738561L
            "HDFCBANK" -> 341249L
            "INFY" -> 408065L
            "TCS" -> 2953217L
            "ICICIBANK" -> 1270529L
            "BHARTIARTL" -> 2714625L
            "TATAMOTORS" -> 884737L
            "ITC" -> 424961L
            "SBIN" -> 779521L
            "LT" -> 2939649L
            else -> 0L
        }
    }

    private fun getDefaultPriceAndChange(symbol: String): Pair<Double, Double> {
        val clean = symbol.removeSuffix(".NS").removeSuffix(".BO").trim().uppercase()
        return when (clean) {
            "NIFTY 50", "NIFTY" -> 22500.0 to 0.67
            "SENSEX" -> 74000.0 to 0.61
            "BANKNIFTY", "BANK NIFTY" -> 48000.0 to 1.04
            "FINNIFTY", "FIN NIFTY" -> 21500.0 to 0.47
            "MIDCPNIFTY" -> 12250.0 to 0.85
            "NIFTY NEXT 50" -> 68500.0 to 0.52
            "NIFTY IT" -> 35600.0 to -0.35
            "RELIANCE" -> 2950.0 to 1.0
            "HDFCBANK" -> 1550.0 to -0.6
            "INFY" -> 1450.0 to 1.0
            "TCS" -> 2200.0 to -2.48
            "ICICIBANK" -> 1120.0 to 0.8
            "SBIN" -> 830.0 to 1.2
            "BHARTIARTL" -> 1410.0 to 0.4
            "TATAMOTORS" -> 980.0 to -0.3
            "ITC" -> 435.0 to 0.2
            "LT" -> 3540.0 to 0.85
            "BTC" -> 67450.0 to 2.14
            "ETH" -> 3520.0 to 1.62
            "DOGE" -> 0.125 to 3.85
            "SOL" -> 148.50 to 4.21
            "BNB" -> 585.0 to -0.45
            "SHIB" -> 0.0000185 to 2.90
            "ADA" -> 0.48 to 1.15
            "XRP" -> 0.52 to -0.80
            else -> com.marketintelligence.ai.data.util.MarketPriceCatalog.getFallbackPrice(symbol) to 0.0
        }
    }

    override suspend fun addToWatchlist(instrument: SearchResult) {
        val type = when (instrument.type.uppercase()) {
            "INDEX" -> "INDEX"
            "CRYPTO" -> "CRYPTO"
            else -> "STOCK"
        }
        val defaultCrypto = if (type == "CRYPTO") MockData.CRYPTO_INITIAL.find { it.symbol.equals(instrument.symbol, ignoreCase = true) } else null
        val (defPrice, defChange) = getDefaultPriceAndChange(instrument.symbol)
        val token = getTokenForSymbol(instrument.symbol)
        val watchlistEntity = WatchlistEntity(
            symbol = instrument.symbol.uppercase(),
            name = instrument.name,
            type = type,
            price = defaultCrypto?.price ?: defPrice,
            changePercent = defaultCrypto?.changePercent ?: defChange,
            instrumentToken = token
        )
        watchlistDao.insert(watchlistEntity)
    }

    override suspend fun removeFromWatchlist(symbol: String) {
        watchlistDao.delete(symbol)
    }

    override suspend fun getHistoricalCandles(symbol: String, timeframe: String): List<Candle> {
        val to = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -30)
        val from = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        val clean = symbol.removeSuffix(".NS").removeSuffix(".BO").trim().uppercase()
        val instrument = try { instrumentDao.search(symbol).firstOrNull { it.tradingsymbol == symbol } } catch (e: Exception) { null }
        val instrumentToken = instrument?.instrument_token ?: getTokenForSymbol(clean).takeIf { it != 0L } ?: when(clean) {
            "NIFTY 50", "NIFTY" -> 256265L
            "BANKNIFTY", "BANK NIFTY" -> 260105L
            else -> 256265L
        }

        val kiteInterval = mapToKiteInterval(timeframe)
        return getKiteHistoricalData(instrumentToken, kiteInterval, from, to)
    }

    override suspend fun subscribeToInstrument(symbol: String, timeframe: String) {
        withContext(Dispatchers.IO) {
            val instrument = try { instrumentDao.search(symbol).firstOrNull { it.tradingsymbol == symbol } } catch (e: Exception) { null }
            val instrumentToken = instrument?.instrument_token ?: getTokenForSymbol(symbol)
            if (instrumentToken != 0L) {
                apiService.subscribeToInstrument(SubscribeRequest(symbol, instrumentToken))
            }
        }
    }

    override suspend fun getKiteHistoricalData(instrumentToken: Long, interval: String, from: String, to: String): List<Candle> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.getKiteHistoricalData(instrumentToken, interval, from, to)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get historical data for $instrumentToken", e)
                emptyList()
            }
        }
    }

    override suspend fun getCoinGeckoHistoricalData(coinId: String, vsCurrency: String, days: Int): List<List<Double>> {
        return withContext(Dispatchers.IO) {
            try {
                coinGeckoApiService.getCoinGeckoHistoricalData(coinId, vsCurrency, days.toString()).prices
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get historical data for $coinId", e)
                emptyList()
            }
        }
    }

    private fun mapToKiteInterval(timeframe: String): String {
        return when (timeframe.lowercase()) {
            "1m", "minute" -> "minute"
            "3m", "3minute" -> "3minute"
            "5m", "5minute" -> "5minute"
            "10m", "10minute" -> "10minute"
            "15m", "15minute" -> "15minute"
            "30m", "30minute" -> "30minute"
            "1h", "60minute", "60m" -> "60minute"
            "1d", "day" -> "day"
            else -> "60minute" // Default to 1H if unknown
        }
    }

    private fun mapInstrumentType(type: String, symbol: String): String {
        return when {
            type == "EQ" -> "STOCK"
            type == "INDEX" || symbol.contains("NIFTY") -> "INDEX"
            else -> "STOCK" // Default to stock for safety
        }
    }
}

fun WatchlistEntity.toStockData(): StockData {
    return StockData(
        symbol = this.symbol,
        name = this.name,
        price = this.price,
        openPrice = this.price, // Initialize openPrice with the same value as price
        change = 0.0,
        changePercent = this.changePercent,
        volume = "",
        market = com.marketintelligence.ai.data.util.MarketPriceCatalog.getMarketType(this.symbol),
        instrumentToken = this.instrumentToken
    )
}

fun WatchlistEntity.toIndexData(): IndexData {
    return IndexData(
        symbol = this.symbol,
        name = this.name,
        price = this.price,
        openPrice = this.price, // Initialize openPrice with the same value as price
        change = 0.0,
        changePercent = this.changePercent,
        market = com.marketintelligence.ai.data.util.MarketPriceCatalog.getMarketType(this.symbol),
        instrumentToken = this.instrumentToken
    )
}

fun WatchlistEntity.toCryptoData(): CryptoData {
    val defaultMatch = MockData.CRYPTO_INITIAL.find { it.symbol.equals(this.symbol, ignoreCase = true) }
    return CryptoData(
        id = this.symbol.lowercase(),
        symbol = this.symbol,
        name = this.name.ifBlank { defaultMatch?.name ?: this.symbol },
        price = if (this.price > 0) this.price else (defaultMatch?.price ?: 0.0),
        changePercent = if (this.price > 0) this.changePercent else (defaultMatch?.changePercent ?: 0.0),
        marketCap = defaultMatch?.marketCap ?: 0L
    )
}
