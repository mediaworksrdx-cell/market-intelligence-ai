package com.example.marketintelligence.ui.fno

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.local.MockData
import com.example.marketintelligence.data.util.MarketPriceCatalog
import com.example.marketintelligence.data.util.ZerodhaStockCatalog
import com.example.marketintelligence.domain.engine.Black76QuantEngine
import com.example.marketintelligence.domain.engine.DealerGammaExposureEngine
import com.example.marketintelligence.domain.model.*
import com.example.tradeengine.models.Candle
import com.example.marketintelligence.domain.repository.SettingsRepository
import com.example.redxfnoscanner.domain.MarketRegime
import com.example.redxfnoscanner.domain.MarketRegimeIdentifier
import com.example.redxfnoscanner.domain.OptionChainAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@HiltViewModel
class FnoViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val quantEngine: Black76QuantEngine,
    private val strategyEngine: com.example.marketintelligence.domain.engine.OptionsStrategyEngine,
    private val gexEngine: DealerGammaExposureEngine,
    private val marketRepository: com.example.marketintelligence.domain.repository.MarketRepository,
    private val intelligenceBus: com.example.marketintelligence.domain.engine.LiveIntelligenceBus,
    private val buildupRepository: com.example.marketintelligence.domain.repository.FnoBuildupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val prefs by lazy {
        context.getSharedPreferences("fno_tracked_strategies_prefs", Context.MODE_PRIVATE)
    }
    private val json = Json { ignoreUnknownKeys = true }

    private fun loadSavedStrategies(): List<OptionStrategy> {
        return try {
            val raw = prefs.getString("tracked_strategies", null)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<List<OptionStrategy>>(raw)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistTrackedStrategies(strategies: List<OptionStrategy>) {
        try {
            val serialized = json.encodeToString(strategies)
            prefs.edit().putString("tracked_strategies", serialized).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val _uiState = MutableStateFlow(FnoUiState())
    val uiState = _uiState.asStateFlow()
    
    private val optionChainAnalyzer = OptionChainAnalyzer()
    private val regimeIdentifier = MarketRegimeIdentifier()

    val strategyDefinitions = mapOf(
        StrategyCategory.BULLISH to listOf(
            "Long Call",
            "Bull Call Spread",
            "Bull Put Spread",
            "Covered Call",
            "Call Ratio Backspread",
            "Bull Calendar Spread",
            "Call Diagonal Spread",
            "Long Call Butterfly"
        ),
        StrategyCategory.BEARISH to listOf(
            "Long Put",
            "Bear Call Spread",
            "Bear Put Spread",
            "Protective Put",
            "Put Ratio Backspread",
            "Bear Calendar Spread",
            "Put Diagonal Spread",
            "Long Put Butterfly"
        ),
        StrategyCategory.NEUTRAL to listOf(
            "Short Straddle",
            "Short Strangle",
            "Iron Condor",
            "Iron Butterfly",
            "Jade Lizard",
            "Reverse Jade Lizard",
            "Calendar Spread",
            "Broken Wing Butterfly"
        ),
        StrategyCategory.VOLATILITY to listOf(
            "Long Straddle",
            "Long Strangle",
            "Reverse Iron Condor",
            "Reverse Iron Butterfly"
        )
    )

    init {
        val initialTracked = loadSavedStrategies()
        if (initialTracked.isNotEmpty()) {
            _uiState.update { it.copy(trackedStrategies = initialTracked) }
        }

        // Initialize Buildup Scanner Catalog
        val initialBuildup = buildupRepository.getInitialBuildupList()
        val longCount = initialBuildup.count { it.buildupType == BuildupType.LONG_BUILDUP }
        val shortCount = initialBuildup.count { it.buildupType == BuildupType.SHORT_BUILDUP }
        val coveringCount = initialBuildup.count { it.buildupType == BuildupType.SHORT_COVERING }
        val unwindingCount = initialBuildup.count { it.buildupType == BuildupType.LONG_UNWINDING }

        val initialExpiries = generateAvailableExpiries()
        _uiState.update {
            it.copy(
                buildupStocks = initialBuildup,
                longBuildupCount = longCount,
                shortBuildupCount = shortCount,
                shortCoveringCount = coveringCount,
                longUnwindingCount = unwindingCount,
                availableExpiries = initialExpiries,
                selectedExpiry = initialExpiries.firstOrNull() ?: "",
                lotSize = getLotSize(it.selectedAsset)
            )
        }

        settingsRepository.selectedMarket.onEach { market ->
            val defaultAsset = when(market) {
                MarketType.IN -> "NIFTY 50"
                MarketType.US -> "SPX"
                MarketType.UAE -> "DFMGI"
            }
            _uiState.update { it.copy(selectedMarket = market) }
            onSearchQueryChanged(defaultAsset)
            // Defer initial load to prevent startup crash
            viewModelScope.launch {
                delay(1500) // Safety Delay
                loadAssetData(defaultAsset)
            }
        }.launchIn(viewModelScope)

        // 1. Reactively collect central LiveIntelligenceBus price updates
        viewModelScope.launch {
            intelligenceBus.livePrices.collect { priceMap ->
                val currentSymbol = _uiState.value.selectedAsset
                val upper = currentSymbol.uppercase().trim()
                val clean = upper.removeSuffix(".NS").removeSuffix(".BO")
                val info = priceMap[upper] ?: priceMap[clean]
                if (info != null && info.price > 0.0 && abs(info.price - _uiState.value.spotPrice) > 0.001) {
                    onSpotPriceTick(info.price, info.changePercent)
                }

                // Update Buildup Scanner live items
                val currentBuildup = _uiState.value.buildupStocks
                if (currentBuildup.isNotEmpty()) {
                    var changed = false
                    val updatedList = currentBuildup.map { stock ->
                        val sym = stock.symbol.uppercase().trim()
                        val pInfo = priceMap[sym] ?: priceMap["${sym}.NS"]
                        if (pInfo != null && pInfo.price > 0.0 && abs(pInfo.price - stock.ltp) > 0.01) {
                            changed = true
                            val newLtp = pInfo.price
                            val newPriceChange = pInfo.price - (pInfo.price / (1.0 + pInfo.changePercent / 100.0))
                            val newPriceChangePct = pInfo.changePercent
                            val newType = buildupRepository.classifyBuildup(newPriceChange, stock.oiChange)
                            stock.copy(
                                ltp = round(newLtp * 100.0) / 100.0,
                                priceChange = round(newPriceChange * 100.0) / 100.0,
                                priceChangePct = round(newPriceChangePct * 100.0) / 100.0,
                                buildupType = newType
                            )
                        } else {
                            stock
                        }
                    }
                    if (changed) {
                        _uiState.update { state ->
                            state.copy(
                                buildupStocks = updatedList,
                                longBuildupCount = updatedList.count { it.buildupType == BuildupType.LONG_BUILDUP },
                                shortBuildupCount = updatedList.count { it.buildupType == BuildupType.SHORT_BUILDUP },
                                shortCoveringCount = updatedList.count { it.buildupType == BuildupType.SHORT_COVERING },
                                longUnwindingCount = updatedList.count { it.buildupType == BuildupType.LONG_UNWINDING }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Buildup Scanner Handlers ---
    fun setBuildupFilter(filter: BuildupType?) {
        _uiState.update { it.copy(buildupFilter = filter) }
    }

    fun setBuildupAssetTypeFilter(filter: AssetTypeFilter) {
        _uiState.update { it.copy(buildupAssetTypeFilter = filter) }
    }

    fun setBuildupSectorFilter(sector: String) {
        _uiState.update { it.copy(buildupSectorFilter = sector) }
    }

    fun setBuildupSortOrder(order: BuildupSortOrder) {
        _uiState.update { it.copy(buildupSortOrder = order) }
    }

    fun onBuildupSearchChanged(query: String) {
        _uiState.update { it.copy(buildupSearchQuery = query) }
    }

    fun selectAssetFromScanner(symbol: String) {
        onSearchQueryChanged(symbol)
        loadAssetData(symbol)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getSearchSuggestions(query: String): List<FnoBuildupStock> {
        val q = query.trim()
        if (q.isBlank() || q.length < 2) return emptyList()
        val qUpper = q.uppercase()
        val qClean = qUpper.replace("-", "").replace(" ", "")

        val currentMarket = _uiState.value.selectedMarket

        // Strictly isolate US market
        if (currentMarket == MarketType.US) {
            val usUniverse = listOf(
                FnoBuildupStock("SPX", "S&P 500 Index", true, "US Broad Market", 5890.75, 24.15, 0.41, 11450000L, 345000L, 3.10, BuildupType.LONG_BUILDUP, 4890000L, 1.25, 0.0),
                FnoBuildupStock("NDX", "Nasdaq 100 Index", true, "US Technology", 20520.40, 238.10, 1.17, 8900000L, 420000L, 4.95, BuildupType.LONG_BUILDUP, 3600000L, 1.30, 0.0),
                FnoBuildupStock("NVDA", "NVIDIA Corporation", false, "Semiconductors", 142.50, 3.10, 2.22, 18500000L, 1420000L, 8.30, BuildupType.LONG_BUILDUP, 9500000L, 1.45, 0.0),
                FnoBuildupStock("AAPL", "Apple Inc", false, "Consumer Electronics", 232.80, -1.20, -0.51, 14200000L, 980000L, 7.41, BuildupType.SHORT_BUILDUP, 4100000L, 0.85, 0.0),
                FnoBuildupStock("MSFT", "Microsoft Corporation", false, "Enterprise Cloud", 428.50, 4.85, 1.15, 9800000L, 450000L, 4.80, BuildupType.LONG_BUILDUP, 3800000L, 1.20, 0.0),
                FnoBuildupStock("TSLA", "Tesla Inc", false, "Automotive & AI", 260.40, 8.70, 3.45, 24500000L, 2100000L, 9.38, BuildupType.LONG_BUILDUP, 11200000L, 1.60, 0.0),
                FnoBuildupStock("AMZN", "Amazon.com Inc", false, "E-Commerce & Cloud", 188.60, 1.60, 0.85, 11200000L, -580000L, -4.92, BuildupType.SHORT_COVERING, 3400000L, 1.15, 0.0),
                FnoBuildupStock("GOOGL", "Alphabet Inc", false, "Search & AI", 166.20, -2.10, -1.25, 9400000L, 820000L, 9.55, BuildupType.SHORT_BUILDUP, 2900000L, 0.75, 0.0),
                FnoBuildupStock("META", "Meta Platforms Inc", false, "Social Media & AI", 595.00, 10.50, 1.80, 8100000L, 620000L, 8.28, BuildupType.LONG_BUILDUP, 2700000L, 1.35, 0.0),
                FnoBuildupStock("AMD", "Advanced Micro Devices", false, "Semiconductors", 156.30, -2.95, -1.85, 13400000L, -650000L, -4.62, BuildupType.LONG_UNWINDING, 4800000L, 0.88, 0.0),
                FnoBuildupStock("NFLX", "Netflix Inc", false, "Digital Entertainment", 755.20, 4.90, 0.65, 4200000L, 280000L, 7.14, BuildupType.LONG_BUILDUP, 1800000L, 1.25, 0.0),
                FnoBuildupStock("JPM", "JPMorgan Chase & Co", false, "Investment Banking", 222.40, -0.90, -0.40, 6800000L, -290000L, -4.09, BuildupType.LONG_UNWINDING, 2100000L, 0.90, 0.0)
            )
            return usUniverse.filter {
                it.symbol.contains(qUpper) || it.name.contains(q, ignoreCase = true)
            }.take(15)
        }

        // Strictly isolate UAE market
        if (currentMarket == MarketType.UAE) {
            val uaeUniverse = listOf(
                FnoBuildupStock("DFMGI", "DFM General Index", true, "Broad Market Index", 4485.30, 18.90, 0.42, 480000L, 16000L, 3.45, BuildupType.LONG_BUILDUP, 620000L, 1.20, 0.0),
                FnoBuildupStock("EMAAR", "Emaar Properties PJSC", false, "Real Estate", 8.75, 0.12, 1.39, 12500000L, 780000L, 6.66, BuildupType.LONG_BUILDUP, 4500000L, 1.40, 0.0),
                FnoBuildupStock("FAB", "First Abu Dhabi Bank", false, "Banking", 13.80, -0.14, -1.00, 8900000L, 540000L, 6.46, BuildupType.SHORT_BUILDUP, 2800000L, 0.78, 0.0),
                FnoBuildupStock("DEWA", "Dubai Electricity & Water", false, "Utilities", 2.48, 0.01, 0.40, 14200000L, -420000L, -2.87, BuildupType.SHORT_COVERING, 7200000L, 1.15, 0.0),
                FnoBuildupStock("ALDAR", "Aldar Properties PJSC", false, "Real Estate", 6.85, 0.11, 1.63, 7800000L, 480000L, 6.56, BuildupType.LONG_BUILDUP, 3100000L, 1.32, 0.0),
                FnoBuildupStock("AIRARABIA", "Air Arabia PJSC", false, "Aviation", 2.92, -0.02, -0.68, 5800000L, -240000L, -3.97, BuildupType.LONG_UNWINDING, 3100000L, 0.88, 0.0),
                FnoBuildupStock("ETISALAT", "e& Emirates Telecommunications", false, "Telecom", 18.20, 0.10, 0.55, 4200000L, -130000L, -3.00, BuildupType.SHORT_COVERING, 2200000L, 1.10, 0.0),
                FnoBuildupStock("EMIRATESNBD", "Emirates NBD Bank", false, "Banking", 19.50, -0.20, -1.02, 3400000L, 210000L, 6.58, BuildupType.SHORT_BUILDUP, 1400000L, 0.82, 0.0)
            )
            return uaeUniverse.filter {
                it.symbol.contains(qUpper) || it.name.contains(q, ignoreCase = true)
            }.take(15)
        }

        // INDIA Market: Curated buildup stocks + 3,800+ Zerodha Kite catalog
        val catalog = if (_uiState.value.buildupStocks.isNotEmpty()) {
            _uiState.value.buildupStocks
        } else {
            buildupRepository.getInitialBuildupList()
        }

        val seen = HashSet<String>()
        val results = mutableListOf<FnoBuildupStock>()

        val curatedMatches = catalog.filter { stock ->
            val symClean = stock.symbol.uppercase().replace("-", "").replace(" ", "")
            val nameClean = stock.name.uppercase().replace("-", "").replace(" ", "")
            symClean.contains(qClean) ||
            nameClean.contains(qClean) ||
            stock.symbol.contains(q, ignoreCase = true) ||
            stock.name.contains(q, ignoreCase = true)
        }

        for (stock in curatedMatches) {
            seen.add(stock.symbol.uppercase())
            results.add(stock)
            if (results.size >= 25) return results
        }

        // Search 3,800+ Zerodha Kite catalog
        val zerodhaMatches = ZerodhaStockCatalog.search(context, q, 30)
        for (z in zerodhaMatches) {
            val sym = z.symbol.uppercase()
            if (!seen.contains(sym)) {
                seen.add(sym)
                results.add(ZerodhaStockCatalog.toBuildupStock(z))
                if (results.size >= 30) break
            }
        }

        return results
    }

    fun addAsset() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isNotBlank()) {
            loadAssetData(query)
            _uiState.update { it.copy(searchQuery = "") }
        }
    }
    
    fun setStrategyCategory(category: StrategyCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun getUpcomingExpiry(weeksAhead: Int = 0): String {
        val calendar = Calendar.getInstance()
        var daysToAdd = (Calendar.THURSDAY - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (daysToAdd == 0 && calendar.get(Calendar.HOUR_OF_DAY) >= 15) {
            daysToAdd = 7
        }
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd + (weeksAhead * 7))
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val month = monthNames[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR).toString()
        return "$day $month $year"
    }

    fun getUpcomingMonthlyExpiry(monthsAhead: Int = 0): String {
        val calendar = Calendar.getInstance()
        if (monthsAhead > 0) {
            calendar.add(Calendar.MONTH, monthsAhead)
        }
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.THURSDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val month = monthNames[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR).toString()
        return "$day $month $year"
    }

    fun generateAvailableExpiries(): List<String> {
        val expiries = linkedSetOf<String>()
        for (w in 0..3) {
            expiries.add(getUpcomingExpiry(w))
        }
        for (m in 0..2) {
            expiries.add(getUpcomingMonthlyExpiry(m))
        }
        return expiries.toList()
    }

    fun getWeeklyExpiries(): List<String> = (0..3).map { getUpcomingExpiry(it) }

    fun getMonthlyExpiries(): List<String> = (0..2).map { getUpcomingMonthlyExpiry(it) }

    fun setStrategyExpiry(expiry: String) {
        _uiState.update { state ->
            val updatedLegs = state.activeLegs.map { leg ->
                leg.copy(expiry = expiry)
            }
            state.copy(selectedExpiry = expiry, activeLegs = updatedLegs)
        }
        recalculateLivePnlAndMetrics()
    }

    fun updateLegExpiry(legId: String, expiry: String) {
        _uiState.update { state ->
            val updatedLegs = state.activeLegs.map { leg ->
                if (leg.id == legId) leg.copy(expiry = expiry) else leg
            }
            state.copy(activeLegs = updatedLegs)
        }
        recalculateLivePnlAndMetrics()
    }

    fun setNumberOfLots(lots: Int) {
        val validLots = max(1, lots)
        val baseLot = getLotSize(_uiState.value.selectedAsset)
        _uiState.update { state ->
            val prevLots = state.numberOfLots.coerceAtLeast(1)
            val updatedLegs = state.activeLegs.map { leg ->
                val ratio = max(1, leg.qty / (baseLot * prevLots).coerceAtLeast(1))
                val newQty = ratio * validLots * baseLot
                leg.copy(qty = newQty, lots = validLots, lotSize = baseLot)
            }
            state.copy(numberOfLots = validLots, activeLegs = updatedLegs)
        }
        recalculateLivePnlAndMetrics()
    }

    fun getLotSize(symbol: String = _uiState.value.selectedAsset): Int {
        val clean = symbol.uppercase().trim().removeSuffix(".NS").removeSuffix(".BO").replace("-", "").replace(" ", "")
        return when {
            clean.contains("BANKNIFTY") -> 30 // Official SEBI/NSE Revised Lot
            clean.contains("FINNIFTY") -> 65 // Official SEBI/NSE Revised Lot
            clean.contains("MIDCPNIFTY") -> 120 // Official SEBI/NSE Revised Lot
            clean.contains("NIFTY") -> 65 // Official SEBI/NSE Revised Lot
            clean.contains("SENSEX") -> 20 // Official SEBI/BSE Revised Lot
            clean.contains("BANKEX") -> 30 // Official SEBI/BSE Revised Lot
            // Global US Indices & Equities (Standard US Option contract lot size is 100 shares)
            clean.contains("SPX") || clean.contains("SP500") -> 100
            clean.contains("NDX") || clean.contains("QQQ") || clean.contains("NASDAQ") -> 100
            clean.contains("DJI") || clean.contains("DOW") -> 100
            clean.contains("AAPL") || clean.contains("NVDA") || clean.contains("MSFT") ||
            clean.contains("GOOG") || clean.contains("AMZN") || clean.contains("TSLA") || clean.contains("META") -> 100
            // UAE Indices & Equities
            clean.contains("DFMGI") || clean.contains("ADX") -> 100
            clean.contains("EMAAR") || clean.contains("FAB") || clean.contains("DEWA") || clean.contains("ALDAR") -> 100
            // European & Global Indices
            clean.contains("FTSE") || clean.contains("DAX") || clean.contains("NIKKEI") || clean.contains("HSI") -> 100
            // Commodities
            clean.contains("GOLD") || clean.contains("XAU") -> 100
            clean.contains("SILVER") || clean.contains("XAG") -> 30
            clean.contains("CRUDE") || clean.contains("WTI") || clean.contains("BRENT") -> 100
            clean.contains("NATURALGAS") || clean.contains("NATGAS") -> 1250
            // Cryptocurrencies (Standard 1 unit)
            clean in listOf("BTC", "ETH", "SOL", "BNB", "DOGE", "XRP") ||
            clean.contains("BTC") || clean.contains("ETH") || clean.contains("SOL") -> 1
            // Indian Stocks
            clean.contains("TATASTEEL") -> 5500
            clean.contains("TATAMOTORS") -> 1425
            clean.contains("MARUTI") -> 50
            clean.contains("BAJFINANCE") -> 125
            clean.contains("RELIANCE") -> 250
            clean.contains("HDFCBANK") -> 550
            clean.contains("TCS") -> 175
            clean.contains("ICICIBANK") -> 700
            clean.contains("SBIN") -> 750
            clean.contains("INFY") -> 400
            clean.contains("ITC") -> 1600
            clean.contains("LT") -> 150
            clean.contains("AXISBANK") -> 625
            clean.contains("KOTAKBANK") -> 400
            clean.contains("WIPRO") -> 1500
            clean.contains("HCLTECH") -> 350
            clean.contains("TECHM") -> 600
            clean.contains("BAJAJAUTO") -> 75
            clean.contains("ONGC") -> 3850
            clean.contains("BPCL") -> 1800
            clean.contains("NTPC") -> 1500
            clean.contains("JSWSTEEL") -> 675
            clean.contains("HINDALCO") -> 1400
            clean.contains("SUNPHARMA") -> 350
            clean.contains("DRREDDY") -> 125
            clean.contains("HINDUNILVR") -> 300
            clean.contains("TITAN") -> 175
            clean.contains("BHARTIARTL") -> 475
            else -> {
                val zItem = ZerodhaStockCatalog.findStockBySymbol(context, clean)
                if (zItem != null) {
                    val spot = ZerodhaStockCatalog.getFallbackSpotPrice(zItem)
                    ZerodhaStockCatalog.calculateRealisticLotSize(spot)
                } else {
                    65
                }
            }
        }
    }

    fun getStrikeStepForSymbol(symbol: String, spot: Double): Double {
        val clean = symbol.uppercase().trim().removeSuffix(".NS").removeSuffix(".BO").replace("-", "").replace(" ", "")
        return when {
            clean.contains("BANKNIFTY") || clean.contains("SENSEX") -> 100.0
            clean.contains("NIFTY") || clean.contains("FINNIFTY") -> 50.0
            clean.contains("MIDCPNIFTY") -> 25.0
            clean.contains("SPX") || clean.contains("SP500") -> 25.0
            clean.contains("NDX") || clean.contains("QQQ") || clean.contains("NASDAQ") -> 50.0
            clean.contains("DJI") || clean.contains("DOW") || clean.contains("NIKKEI") -> 100.0
            clean.contains("DFMGI") || clean.contains("ADX") || clean.contains("FTSE") || clean.contains("DAX") -> 25.0
            clean.contains("BTC") -> 500.0
            clean.contains("ETH") -> 50.0
            clean.contains("SOL") -> 5.0
            clean.contains("GOLD") -> 10.0
            clean.contains("SILVER") -> 0.5
            clean.contains("CRUDE") -> 1.0
            clean.contains("TATASTEEL") -> 2.5
            else -> {
                val zItem = ZerodhaStockCatalog.findStockBySymbol(context, clean)
                if (zItem != null) {
                    ZerodhaStockCatalog.calculateRealisticStrikeStep(spot)
                } else if (spot < 50.0) 0.5
                else if (spot < 100.0) 1.0
                else if (spot < 250.0) 2.5
                else if (spot < 500.0) 5.0
                else if (spot < 1000.0) 10.0
                else if (spot < 2500.0) 20.0
                else if (spot < 5000.0) 50.0
                else 100.0
            }
        }
    }

    data class AssetResolution(
        val symbol: String,
        val displayName: String,
        val spotPrice: Double,
        val changePercent: Double,
        val lotSize: Int,
        val strikeStep: Double
    )

    fun resolveAssetInfo(query: String): AssetResolution {
        val qTrimmed = query.trim()
        val qUpper = qTrimmed.uppercase()
        val qClean = qUpper.removeSuffix(".NS").removeSuffix(".BO").replace("-", "").replace(" ", "")

        // 1. Try matching from buildup stocks catalog (checks symbol and name like "Tata Steel")
        val buildupCatalog = if (_uiState.value.buildupStocks.isNotEmpty()) {
            _uiState.value.buildupStocks
        } else {
            buildupRepository.getInitialBuildupList()
        }

        val buildupMatch = buildupCatalog.firstOrNull { stock ->
            val symClean = stock.symbol.uppercase().replace("-", "").replace(" ", "")
            val nameClean = stock.name.uppercase().replace("-", "").replace(" ", "")
            symClean == qClean ||
            nameClean == qClean ||
            stock.symbol.equals(qTrimmed, ignoreCase = true) ||
            stock.name.equals(qTrimmed, ignoreCase = true) ||
            stock.name.contains(qTrimmed, ignoreCase = true) ||
            qTrimmed.contains(stock.name, ignoreCase = true) ||
            (qUpper.contains("TATA") && qUpper.contains("STEEL") && stock.symbol == "TATASTEEL") ||
            (qUpper.contains("TATA") && qUpper.contains("MOTORS") && stock.symbol == "TATAMOTORS")
        }

        if (buildupMatch != null) {
            val spot = buildupMatch.ltp
            val lot = getLotSize(buildupMatch.symbol)
            val step = getStrikeStepForSymbol(buildupMatch.symbol, spot)
            return AssetResolution(
                symbol = buildupMatch.symbol,
                displayName = "${buildupMatch.symbol} • ${buildupMatch.name}",
                spotPrice = spot,
                changePercent = buildupMatch.priceChangePct,
                lotSize = lot,
                strikeStep = step
            )
        }

        // 2. Check in LiveIntelligenceBus
        val liveInfo = intelligenceBus.getLivePrice(qClean) ?: intelligenceBus.getLivePrice(qTrimmed)
        if (liveInfo != null && liveInfo.price > 0.0) {
            val spot = liveInfo.price
            val lot = getLotSize(qClean)
            val step = getStrikeStepForSymbol(qClean, spot)
            return AssetResolution(
                symbol = qClean,
                displayName = qTrimmed,
                spotPrice = spot,
                changePercent = liveInfo.changePercent,
                lotSize = lot,
                strikeStep = step
            )
        }

        // 2b. Check in 3,800+ Zerodha Kite Catalog
        val zStock = ZerodhaStockCatalog.findStockBySymbol(context, qClean) ?: ZerodhaStockCatalog.findStockBySymbol(context, qTrimmed)
        if (zStock != null) {
            val spot = intelligenceBus.getLivePrice(zStock.symbol)?.price ?: ZerodhaStockCatalog.getFallbackSpotPrice(zStock)
            val lot = ZerodhaStockCatalog.calculateRealisticLotSize(spot)
            val step = ZerodhaStockCatalog.calculateRealisticStrikeStep(spot)
            return AssetResolution(
                symbol = zStock.symbol.uppercase(),
                displayName = "${zStock.symbol} • ${zStock.name}",
                spotPrice = spot,
                changePercent = 1.15,
                lotSize = lot,
                strikeStep = step
            )
        }

        // 3. Known dictionary fallbacks
        val spot = when {
            qClean.contains("TATASTEEL") || (qUpper.contains("TATA") && qUpper.contains("STEEL")) -> 154.00
            qClean.contains("TATAMOTORS") || (qUpper.contains("TATA") && qUpper.contains("MOTORS")) -> 985.00
            qClean.contains("BANKNIFTY") -> 54259.95
            qClean.contains("NIFTY") -> 22716.20
            qClean.contains("SENSEX") -> 72529.07
            qClean.contains("FINNIFTY") -> 24648.50
            qClean.contains("MIDCPNIFTY") -> 12280.0
            qClean.contains("RELIANCE") -> 1182.00
            qClean.contains("HDFCBANK") || qClean == "HDFC" -> 722.70
            qClean.contains("TCS") -> 2032.40
            qClean.contains("INFY") || qClean.contains("INFOSYS") -> 1015.40
            qClean.contains("ICICIBANK") || qClean == "ICICI" -> 1292.20
            qClean.contains("SBIN") || qClean == "SBI" || qUpper.contains("STATE BANK") -> 964.70
            qClean.contains("BHARTIARTL") || qClean.contains("AIRTEL") -> 1425.00
            qClean.contains("ITC") -> 438.00
            qClean.contains("LT") || qUpper.contains("LARSEN") -> 3560.00
            qClean.contains("AXISBANK") || qClean == "AXIS" -> 1175.00
            qClean.contains("KOTAKBANK") || qClean == "KOTAK" -> 1775.00
            qClean.contains("MARUTI") -> 12380.00
            qClean.contains("BAJFINANCE") -> 6940.00
            qClean.contains("WIPRO") -> 528.00
            qClean.contains("HCLTECH") -> 1680.00
            qClean.contains("TECHM") -> 1490.00
            qClean.contains("ONGC") -> 295.00
            qClean.contains("BPCL") -> 345.00
            qClean.contains("NTPC") -> 398.00
            qClean.contains("JSWSTEEL") -> 945.00
            qClean.contains("HINDALCO") -> 675.00
            qClean.contains("SUNPHARMA") -> 1695.00
            qClean.contains("DRREDDY") -> 6450.00
            qClean.contains("HINDUNILVR") -> 2465.00
            qClean.contains("TITAN") -> 3485.00
            qClean.contains("SPX") || qClean.contains("SP500") -> 5500.00
            qClean.contains("NDX") || qClean.contains("NASDAQ") || qClean.contains("QQQ") -> 19200.00
            qClean.contains("DJI") || qClean.contains("DOW") -> 39500.00
            qClean.contains("DFMGI") -> 4849.37
            qClean.contains("ADX") -> 9250.00
            qClean.contains("FTSE") -> 8250.00
            qClean.contains("DAX") -> 18500.00
            qClean.contains("NIKKEI") -> 38800.00
            qClean.contains("GOLD") || qClean.contains("XAU") -> 2450.00
            qClean.contains("SILVER") || qClean.contains("XAG") -> 28.50
            qClean.contains("CRUDE") || qClean.contains("WTI") || qClean.contains("BRENT") -> 78.50
            qClean.contains("BTC") || qClean.contains("BITCOIN") -> 68500.00
            qClean.contains("ETH") || qClean.contains("ETHEREUM") -> 3450.00
            qClean.contains("SOL") || qClean.contains("SOLANA") -> 145.00
            else -> MarketPriceCatalog.getFallbackPrice(qClean)
        }

        val lot = getLotSize(qClean)
        val step = getStrikeStepForSymbol(qClean, spot)
        return AssetResolution(
            symbol = qClean,
            displayName = qTrimmed,
            spotPrice = spot,
            changePercent = 1.15,
            lotSize = lot,
            strikeStep = step
        )
    }

    fun selectPredefinedStrategy(strategyName: String) {
        val spot = _uiState.value.spotPrice
        val nearExpiry = _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) }
        val nextExpiry = _uiState.value.availableExpiries.getOrNull(3) ?: getUpcomingMonthlyExpiry(1)
        val step = _uiState.value.strikeStep.coerceAtLeast(1.0)
        val atmStrike = round(spot / step) * step
        val baseLot = getLotSize(_uiState.value.selectedAsset)
        val lot = baseLot * _uiState.value.numberOfLots
        
        val legs = when(strategyName) {
            // --- BULLISH STRATEGIES ---
            "Long Call" -> listOf(
                createLeg("BUY", "CALL", atmStrike, nearExpiry, qty = lot)
            )
            "Bull Call Spread" -> listOf(
                createLeg("BUY", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot)
            )
            "Bull Put Spread" -> listOf(
                createLeg("SELL", "PUT", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )
            "Covered Call" -> listOf(
                createLeg("BUY", "FUT", 0.0, nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot)
            )
            "Call Ratio Backspread" -> listOf(
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot * 2)
            )
            "Bull Calendar Spread" -> listOf(
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike + (step * 2), nextExpiry, qty = lot)
            )
            "Call Diagonal Spread" -> listOf(
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike - step, nextExpiry, qty = lot)
            )
            "Long Call Butterfly" -> listOf(
                createLeg("BUY", "CALL", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot * 2),
                createLeg("BUY", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot)
            )

            // --- BEARISH STRATEGIES ---
            "Long Put" -> listOf(
                createLeg("BUY", "PUT", atmStrike, nearExpiry, qty = lot)
            )
            "Bear Call Spread" -> listOf(
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot)
            )
            "Bear Put Spread" -> listOf(
                createLeg("BUY", "PUT", atmStrike, nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )
            "Protective Put" -> listOf(
                createLeg("BUY", "FUT", 0.0, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike, nearExpiry, qty = lot)
            )
            "Put Ratio Backspread" -> listOf(
                createLeg("SELL", "PUT", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot * 2)
            )
            "Bear Calendar Spread" -> listOf(
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nextExpiry, qty = lot)
            )
            "Put Diagonal Spread" -> listOf(
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike + step, nextExpiry, qty = lot)
            )
            "Long Put Butterfly" -> listOf(
                createLeg("BUY", "PUT", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike, nearExpiry, qty = lot * 2),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )

            // --- NEUTRAL / INCOME STRATEGIES ---
            "Short Straddle" -> listOf(
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike, nearExpiry, qty = lot)
            )
            "Short Strangle" -> listOf(
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )
            "Iron Condor" -> listOf(
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike + (step * 4), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 4), nearExpiry, qty = lot)
            )
            "Iron Butterfly" -> listOf(
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )
            "Jade Lizard" -> listOf(
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike + (step * 4), nearExpiry, qty = lot)
            )
            "Reverse Jade Lizard" -> listOf(
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 4), nearExpiry, qty = lot)
            )
            "Calendar Spread" -> listOf(
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", atmStrike, nextExpiry, qty = lot)
            )
            "Broken Wing Butterfly" -> listOf(
                createLeg("BUY", "CALL", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike, nearExpiry, qty = lot * 2),
                createLeg("BUY", "CALL", atmStrike + (step * 3), nearExpiry, qty = lot)
            )

            // --- VOLATILITY / BREAKOUT STRATEGIES ---
            "Long Straddle" -> listOf(
                createLeg("BUY", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike, nearExpiry, qty = lot)
            )
            "Long Strangle" -> listOf(
                createLeg("BUY", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )
            "Reverse Iron Condor" -> listOf(
                createLeg("BUY", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike + (step * 4), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike - (step * 4), nearExpiry, qty = lot)
            )
            "Reverse Iron Butterfly" -> listOf(
                createLeg("BUY", "CALL", atmStrike, nearExpiry, qty = lot),
                createLeg("SELL", "CALL", atmStrike + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", atmStrike, nearExpiry, qty = lot),
                createLeg("SELL", "PUT", atmStrike - (step * 2), nearExpiry, qty = lot)
            )
            else -> emptyList()
        }
        
        val defaultCustomName = "$strategyName - ${_uiState.value.selectedAsset}"
        _uiState.update {
            it.copy(
                activeLegs = legs,
                selectedStrategyName = strategyName,
                customStrategyName = defaultCustomName,
                selectedStrategy = null,
                isStrategyAnalyzed = false,
                editingStrategyId = null
            )
        }
        recalculateLivePnlAndMetrics()
    }

    fun onCustomStrategyNameChanged(name: String) {
        _uiState.update { it.copy(customStrategyName = name) }
    }
    
    fun calculateMoneyness(strike: Double, spotPrice: Double, instrument: String, strikeStep: Double = 50.0): String {
        if (instrument.equals("FUT", ignoreCase = true)) return "FUT"
        val halfStep = (strikeStep / 2.0).coerceAtLeast(0.5)
        val isAtm = abs(strike - spotPrice) <= halfStep
        if (isAtm) return "ATM"
        val isCall = instrument.equals("CALL", ignoreCase = true) || instrument.equals("CE", ignoreCase = true)
        return if (isCall) {
            if (strike < spotPrice) "ITM" else "OTM"
        } else {
            if (strike > spotPrice) "ITM" else "OTM"
        }
    }

    private fun createLeg(
        type: String,
        instrument: String,
        strike: Double,
        expiry: String = _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) },
        qty: Int = getLotSize(_uiState.value.selectedAsset) * _uiState.value.numberOfLots,
        underlying: String = _uiState.value.selectedAsset
    ): StrategyLeg {
        val spot = _uiState.value.spotPrice
        val step = _uiState.value.strikeStep.coerceAtLeast(1.0)
        val resolvedStrike = if (instrument.equals("FUT", ignoreCase = true)) {
            0.0
        } else {
            round(strike / step) * step
        }
        val currentIv = (_uiState.value.summary.iv).coerceAtLeast(5.0) / 100.0
        val timeYears = 5.0 / 365.0
        val entry = if (instrument == "FUT") {
            spot
        } else {
            val isCall = instrument.equals("CALL", ignoreCase = true)
            val g = quantEngine.calculate(
                forward = spot,
                strike = resolvedStrike,
                rate = 0.065,
                timeToExpiryYears = timeYears,
                volatility = currentIv,
                isCall = isCall
            )
            round(max(0.5, g.price) * 100.0) / 100.0
        }
        val baseLot = getLotSize(underlying)
        val lots = max(1, qty / baseLot.coerceAtLeast(1))
        val moneyness = calculateMoneyness(resolvedStrike, spot, instrument, step)
        return StrategyLeg(
            id = UUID.randomUUID().toString(),
            type = type,
            instrument = instrument,
            strike = resolvedStrike,
            expiry = expiry,
            qty = qty,
            entryPrice = entry,
            currentPrice = entry,
            lotSize = baseLot,
            lots = lots,
            underlying = underlying,
            moneyness = moneyness
        )
    }

    fun addLeg(instrument: String, type: String, strike: Double, expiry: String? = null) {
        val underlying = _uiState.value.selectedAsset
        val baseLot = getLotSize(underlying)
        val totalQty = baseLot * _uiState.value.numberOfLots
        val legExpiry = expiry?.ifBlank { null } ?: _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) }
        val newLeg = createLeg(type, instrument, strike, legExpiry, qty = totalQty, underlying = underlying)
        _uiState.update { it.copy(activeLegs = it.activeLegs + newLeg) }
        recalculateLivePnlAndMetrics()
    }

    fun selectStrikeForDetail(strike: OptionChainData?, side: String? = null) {
        if (strike == null) {
            dismissStrikeDetail()
            return
        }
        val isPut = side?.equals("PUT", ignoreCase = true) == true || side?.equals("PE", ignoreCase = true) == true
        val optionType = if (isPut) "PE" else "CE"
        val optionSide = if (isPut) "PUT" else "CALL"

        _uiState.update {
            it.copy(
                selectedStrikeForDetail = strike,
                selectedOptionSide = optionSide
            )
        }
        loadStrikeChart(
            strike = strike,
            optionType = optionType,
            timeframe = _uiState.value.strikeChartState.selectedTimeframe.ifBlank { "5m" }
        )
    }

    fun onStrikeOptionTypeChanged(optionType: String) {
        val currentStrike = _uiState.value.selectedStrikeForDetail ?: return
        val isPut = optionType.equals("PUT", ignoreCase = true) || optionType.equals("PE", ignoreCase = true)
        val normType = if (isPut) "PE" else "CE"
        val optionSide = if (isPut) "PUT" else "CALL"

        _uiState.update {
            it.copy(selectedOptionSide = optionSide)
        }
        loadStrikeChart(
            strike = currentStrike,
            optionType = normType,
            timeframe = _uiState.value.strikeChartState.selectedTimeframe.ifBlank { "5m" }
        )
    }

    fun onStrikeTimeframeChanged(timeframe: String) {
        val currentStrike = _uiState.value.selectedStrikeForDetail ?: return
        val isPut = _uiState.value.selectedOptionSide.equals("PUT", ignoreCase = true) ||
                    _uiState.value.selectedOptionSide.equals("PE", ignoreCase = true)
        val normType = if (isPut) "PE" else "CE"
        loadStrikeChart(
            strike = currentStrike,
            optionType = normType,
            timeframe = timeframe
        )
    }

    fun retryLoadStrikeChart() {
        val currentStrike = _uiState.value.selectedStrikeForDetail ?: return
        val isPut = _uiState.value.selectedOptionSide.equals("PUT", ignoreCase = true) ||
                    _uiState.value.selectedOptionSide.equals("PE", ignoreCase = true)
        val normType = if (isPut) "PE" else "CE"
        loadStrikeChart(
            strike = currentStrike,
            optionType = normType,
            timeframe = _uiState.value.strikeChartState.selectedTimeframe.ifBlank { "5m" }
        )
    }

    fun loadStrikeChart(
        strike: OptionChainData,
        optionType: String,
        timeframe: String = "5m"
    ) {
        val asset = _uiState.value.selectedAsset
        val expiry = _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) }
        val isCall = !optionType.equals("PE", ignoreCase = true) && !optionType.equals("PUT", ignoreCase = true)
        val normType = if (isCall) "CE" else "PE"

        val ltp = if (isCall) strike.callLTP else strike.putLTP
        val changePct = if (isCall) strike.callChange else strike.putChange
        val iv = if (isCall) strike.callIV else strike.putIV
        val oi = if (isCall) strike.callOI else strike.putOI
        val greeks = if (isCall) strike.callGreeks else strike.putGreeks
        val contractDisplayName = "${asset.uppercase()} ${strike.strike.toInt()} $normType"

        // Immediately set loading state and clear stale candles so switching strikes never retains prior data
        _uiState.update {
            it.copy(
                strikeChartState = StrikeChartState(
                    assetSymbol = asset,
                    strikePrice = strike.strike,
                    optionType = normType,
                    expiry = expiry,
                    contractDisplayName = contractDisplayName,
                    ltp = ltp,
                    changePercent = changePct,
                    iv = iv,
                    oi = oi,
                    greeks = greeks,
                    candles = emptyList(),
                    selectedTimeframe = timeframe,
                    isLoading = true,
                    error = null
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Attempt to fetch from repository or database if live option tokens exist
                val cleanSymbol = "${asset.replace(" ", "")}${strike.strike.toInt()}$normType"
                var fetchedCandles: List<Candle> = emptyList()
                try {
                    fetchedCandles = marketRepository.getHistoricalCandles(cleanSymbol, timeframe)
                } catch (e: Exception) {
                    // Fall back to deterministic mathematical option candle generator
                }

                val finalCandles = if (fetchedCandles.isNotEmpty()) {
                    fetchedCandles
                } else {
                    OptionCandleGenerator.generateOptionCandles(
                        assetSymbol = asset,
                        strike = strike.strike,
                        optionType = normType,
                        expiry = expiry,
                        currentLtp = ltp,
                        changePercent = changePct,
                        iv = iv,
                        timeframe = timeframe
                    )
                }

                _uiState.update { state ->
                    if (state.selectedStrikeForDetail?.strike == strike.strike &&
                        state.strikeChartState.optionType == normType) {
                        state.copy(
                            strikeChartState = state.strikeChartState.copy(
                                candles = finalCandles,
                                isLoading = false,
                                error = if (finalCandles.isEmpty()) "No chart data available for $contractDisplayName" else null
                            )
                        )
                    } else {
                        state
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        strikeChartState = it.strikeChartState.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load option contract chart"
                        )
                    )
                }
            }
        }
    }

    fun dismissStrikeDetail() {
        _uiState.update {
            it.copy(
                selectedStrikeForDetail = null,
                selectedOptionSide = null,
                strikeChartState = StrikeChartState()
            )
        }
    }

    fun addLegFromStrike(strike: Double, instrument: String, type: String = "BUY") {
        val expiry = _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) }
        addLeg(instrument = instrument, type = type, strike = strike, expiry = expiry)
    }

    fun addStraddleFromStrike(strike: Double) {
        val underlying = _uiState.value.selectedAsset
        val baseLot = getLotSize(underlying)
        val totalQty = baseLot * _uiState.value.numberOfLots
        val expiry = _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) }
        val callLeg = createLeg("BUY", "CALL", strike, expiry, qty = totalQty, underlying = underlying)
        val putLeg = createLeg("BUY", "PUT", strike, expiry, qty = totalQty, underlying = underlying)
        _uiState.update { it.copy(activeLegs = it.activeLegs + listOf(callLeg, putLeg)) }
        recalculateLivePnlAndMetrics()
    }

    fun addStrangleFromStrikes(callStrike: Double, putStrike: Double) {
        val underlying = _uiState.value.selectedAsset
        val baseLot = getLotSize(underlying)
        val totalQty = baseLot * _uiState.value.numberOfLots
        val expiry = _uiState.value.selectedExpiry.ifBlank { getUpcomingExpiry(0) }
        val callLeg = createLeg("BUY", "CALL", callStrike, expiry, qty = totalQty, underlying = underlying)
        val putLeg = createLeg("BUY", "PUT", putStrike, expiry, qty = totalQty, underlying = underlying)
        _uiState.update { it.copy(activeLegs = it.activeLegs + listOf(callLeg, putLeg)) }
        recalculateLivePnlAndMetrics()
    }

    fun removeLeg(id: String) {
        _uiState.update { it.copy(activeLegs = it.activeLegs.filter { it.id != id }) }
        recalculateLivePnlAndMetrics()
    }
    
    fun updateLeg(id: String, strike: Double) {
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == id) {
                    val currentIv = (state.summary.iv + state.ivSimulation).coerceAtLeast(5.0) / 100.0
                    val timeYears = 5.0 / 365.0
                    val newPrice = if (leg.instrument == "FUT") {
                        state.spotPrice
                    } else {
                        val isCall = leg.instrument.equals("CALL", ignoreCase = true)
                        val g = quantEngine.calculate(
                            forward = state.spotPrice,
                            strike = strike,
                            rate = 0.065,
                            timeToExpiryYears = timeYears,
                            volatility = currentIv,
                            isCall = isCall
                        )
                        round(max(0.5, g.price) * 100.0) / 100.0
                    }
                    leg.copy(strike = strike, entryPrice = newPrice, currentPrice = newPrice)
                } else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun shiftLegStrike(legId: String, deltaSteps: Int) {
        val step = _uiState.value.strikeStep
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == legId) {
                    val newStrike = max(step, leg.strike + (deltaSteps * step))
                    val currentIv = (state.summary.iv + state.ivSimulation).coerceAtLeast(5.0) / 100.0
                    val timeYears = 5.0 / 365.0
                    val newPrice = if (leg.instrument == "FUT") {
                        state.spotPrice
                    } else {
                        val isCall = leg.instrument.equals("CALL", ignoreCase = true)
                        val g = quantEngine.calculate(
                            forward = state.spotPrice,
                            strike = newStrike,
                            rate = 0.065,
                            timeToExpiryYears = timeYears,
                            volatility = currentIv,
                            isCall = isCall
                        )
                        round(max(0.5, g.price) * 100.0) / 100.0
                    }
                    leg.copy(strike = newStrike, entryPrice = newPrice, currentPrice = newPrice)
                } else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun toggleLegType(legId: String) {
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == legId) {
                    val newType = if (leg.type.equals("BUY", ignoreCase = true)) "SELL" else "BUY"
                    leg.copy(type = newType)
                } else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun toggleLegInstrument(legId: String) {
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == legId) {
                    val newInst = if (leg.instrument.equals("CALL", ignoreCase = true)) "PUT" else "CALL"
                    val currentIv = (state.summary.iv + state.ivSimulation).coerceAtLeast(5.0) / 100.0
                    val timeYears = 5.0 / 365.0
                    val isCall = newInst.equals("CALL", ignoreCase = true)
                    val g = quantEngine.calculate(
                        forward = state.spotPrice,
                        strike = leg.strike,
                        rate = 0.065,
                        timeToExpiryYears = timeYears,
                        volatility = currentIv,
                        isCall = isCall
                    )
                    val newPrice = round(max(0.5, g.price) * 100.0) / 100.0
                    leg.copy(instrument = newInst, entryPrice = newPrice, currentPrice = newPrice)
                } else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun updateLegQty(legId: String, newQty: Int) {
        val lot = getLotSize(_uiState.value.selectedAsset)
        val validQty = max(lot, newQty)
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == legId) leg.copy(qty = validQty) else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun toggleLegEnabled(legId: String) {
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == legId) leg.copy(isEnabled = !leg.isEnabled) else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun duplicateLeg(legId: String) {
        val leg = _uiState.value.activeLegs.firstOrNull { it.id == legId } ?: return
        val newLeg = leg.copy(id = UUID.randomUUID().toString())
        _uiState.update { it.copy(activeLegs = it.activeLegs + newLeg) }
        recalculateLivePnlAndMetrics()
    }

    fun updateLegEntryPrice(legId: String, newEntryPrice: Double) {
        if (newEntryPrice < 0.0) return
        _uiState.update { state ->
            val updated = state.activeLegs.map { leg ->
                if (leg.id == legId) leg.copy(entryPrice = newEntryPrice, currentPrice = newEntryPrice) else leg
            }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun selectAllLegs(enable: Boolean) {
        _uiState.update { state ->
            val updated = state.activeLegs.map { it.copy(isEnabled = enable) }
            state.copy(activeLegs = updated)
        }
        recalculateLivePnlAndMetrics()
    }

    fun recalculateLivePnlAndMetrics() {
        val state = _uiState.value
        var netPrem = 0.0
        var currentVal = 0.0
        var totalPnl = 0.0

        state.activeLegs.forEach { leg ->
            val mult = if (leg.type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
            netPrem += leg.entryPrice * leg.qty * mult
            currentVal += leg.currentPrice * leg.qty * mult
            totalPnl += leg.pnl
        }

        val totalPnlPct = if (abs(netPrem) > 0.01) (totalPnl / abs(netPrem)) * 100.0 else 0.0

        val hasShortLegs = state.activeLegs.any { it.type.equals("SELL", ignoreCase = true) }
        val capReq = if (!hasShortLegs) {
            max(0.0, netPrem)
        } else {
            val shortQty = state.activeLegs.filter { it.type.equals("SELL", ignoreCase = true) }.sumOf { it.qty }
            val baseMargin = shortQty * (state.spotPrice * 0.12)
            val netDebitOutlay = if (netPrem > 0.0) netPrem else 0.0
            baseMargin + netDebitOutlay
        }

        _uiState.update {
            it.copy(
                totalStrategyPnl = round(totalPnl * 10.0) / 10.0,
                totalStrategyPnlPct = round(totalPnlPct * 10.0) / 10.0,
                netPremium = round(netPrem * 10.0) / 10.0,
                currentStrategyValue = round(currentVal * 10.0) / 10.0,
                capitalRequired = round(capReq * 10.0) / 10.0
            )
        }

        if (state.activeLegs.isNotEmpty()) {
            val name = state.customStrategyName.ifBlank {
                state.selectedStrategyName.ifBlank { "Custom Strategy" }
            }
            calculateStrategyMetrics(name)
            _uiState.update { it.copy(isStrategyAnalyzed = true) }
        }
    }

    fun onSpotPriceTick(newSpot: Double, changePct: Double) {
        val state = _uiState.value
        val updatedSummary = state.summary.copy(changePercent = changePct)

        val currentIv = (state.summary.iv + state.ivSimulation).coerceAtLeast(5.0) / 100.0
        val daysRemaining = (5.0 - state.timeSimulation).coerceAtLeast(0.05)
        val timeYears = daysRemaining / 365.0

        val updatedLegs = if (state.activeLegs.isNotEmpty()) {
            state.activeLegs.map { leg ->
                val newLtp = if (leg.instrument == "FUT") {
                    newSpot
                } else {
                    val isCall = leg.instrument.equals("CALL", ignoreCase = true)
                    val g = quantEngine.calculate(
                        forward = newSpot,
                        strike = leg.strike,
                        rate = 0.065,
                        timeToExpiryYears = timeYears,
                        volatility = currentIv,
                        isCall = isCall
                    )
                    round(max(0.5, g.price) * 100.0) / 100.0
                }
                leg.copy(currentPrice = newLtp)
            }
        } else {
            emptyList()
        }

        var netPrem = 0.0
        var currentVal = 0.0
        var totalPnl = 0.0

        updatedLegs.forEach { leg ->
            val mult = if (leg.type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
            netPrem += leg.entryPrice * leg.qty * mult
            currentVal += leg.currentPrice * leg.qty * mult
            totalPnl += leg.pnl
        }

        val totalPnlPct = if (abs(netPrem) > 0.01) (totalPnl / abs(netPrem)) * 100.0 else 0.0

        _uiState.update { current ->
            current.copy(
                spotPrice = newSpot,
                summary = updatedSummary,
                activeLegs = updatedLegs,
                totalStrategyPnl = round(totalPnl * 10.0) / 10.0,
                totalStrategyPnlPct = round(totalPnlPct * 10.0) / 10.0,
                netPremium = round(netPrem * 10.0) / 10.0,
                currentStrategyValue = round(currentVal * 10.0) / 10.0,
                isLiveConnected = true
            )
        }

        if (_uiState.value.isStrategyAnalyzed && _uiState.value.activeLegs.isNotEmpty()) {
            val name = _uiState.value.customStrategyName.ifBlank {
                _uiState.value.selectedStrategyName.ifBlank { "Custom Strategy" }
            }
            calculateStrategyMetrics(name)
        }

        updateTrackedStrategiesLivePrices(newSpot, timeYears, currentIv)
    }

    private fun updateTrackedStrategiesLivePrices(spot: Double, timeYears: Double, currentIv: Double) {
        val currentTracked = _uiState.value.trackedStrategies
        if (currentTracked.isEmpty()) return

        val updatedList = currentTracked.map { strategy ->
            if (strategy.rawLegs.isNotEmpty()) {
                val updatedLegs = strategy.rawLegs.map { leg ->
                    val newPrice = if (leg.instrument == "FUT") {
                        spot
                    } else {
                        val isCall = leg.instrument.equals("CALL", ignoreCase = true)
                        val g = quantEngine.calculate(
                            forward = spot,
                            strike = leg.strike,
                            rate = 0.065,
                            timeToExpiryYears = timeYears,
                            volatility = currentIv,
                            isCall = isCall
                        )
                        round(max(0.5, g.price) * 100.0) / 100.0
                    }
                    leg.copy(currentPrice = newPrice)
                }
                strategy.copy(rawLegs = updatedLegs)
            } else {
                strategy
            }
        }
        _uiState.update { it.copy(trackedStrategies = updatedList) }
    }

    fun onSimulationChanged(ivShift: Double, timeShift: Int) {
        onSimulationChanged(_uiState.value.spotSimulationPct, ivShift, timeShift)
    }

    fun onSimulationChanged(spotShiftPct: Double, ivShift: Double, timeShift: Int) {
        _uiState.update { it.copy(spotSimulationPct = spotShiftPct, ivSimulation = ivShift, timeSimulation = timeShift) }
        val name = _uiState.value.customStrategyName.ifBlank { _uiState.value.selectedStrategy?.name ?: "Custom Strategy" }
        calculateStrategyMetrics(name)
    }

    fun resetSimulation() {
        _uiState.update { it.copy(spotSimulationPct = 0.0, ivSimulation = 0.0, timeSimulation = 0, selectedCrosshairPrice = null) }
        val name = _uiState.value.customStrategyName.ifBlank { _uiState.value.selectedStrategy?.name ?: "Custom Strategy" }
        calculateStrategyMetrics(name)
    }

    fun setWorkspaceTab(tab: OpstraWorkspaceTab) {
        _uiState.update { it.copy(selectedWorkspaceTab = tab) }
    }

    fun setChartPriceRangePct(rangePct: Double) {
        _uiState.update { it.copy(chartPriceRangePct = rangePct) }
    }

    fun setSelectedCrosshairPrice(price: Double?) {
        _uiState.update { it.copy(selectedCrosshairPrice = price) }
    }

    fun requestResetStrategy() {
        if (_uiState.value.activeLegs.isEmpty()) return
        _uiState.update { it.copy(showResetConfirmDialog = true) }
    }

    fun confirmResetStrategy() {
        deleteOrClearStrategy()
        _uiState.update { it.copy(showResetConfirmDialog = false) }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(showResetConfirmDialog = false) }
    }

    fun requestDeleteTrackedStrategy(id: String) {
        _uiState.update { it.copy(showDeleteConfirmDialog = true, pendingDeleteStrategyId = id) }
    }

    fun confirmDeleteTrackedStrategy() {
        val id = _uiState.value.pendingDeleteStrategyId ?: return
        deleteTrackedStrategy(id)
        _uiState.update { it.copy(showDeleteConfirmDialog = false, pendingDeleteStrategyId = null) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false, pendingDeleteStrategyId = null) }
    }

    fun openRenameDialog(id: String, currentName: String) {
        _uiState.update { it.copy(isRenameDialogOpen = true, pendingDeleteStrategyId = id, strategyRenameInput = currentName) }
    }

    fun setRenameInput(name: String) {
        _uiState.update { it.copy(strategyRenameInput = name) }
    }

    fun confirmRenameStrategy() {
        val id = _uiState.value.pendingDeleteStrategyId ?: return
        val newName = _uiState.value.strategyRenameInput.trim()
        if (newName.isNotBlank()) {
            val updated = _uiState.value.trackedStrategies.map {
                if (it.id == id) it.copy(name = newName) else it
            }
            _uiState.update {
                it.copy(
                    trackedStrategies = updated,
                    isRenameDialogOpen = false,
                    pendingDeleteStrategyId = null,
                    selectedStrategyName = if (it.editingStrategyId == id) newName else it.selectedStrategyName
                )
            }
            persistTrackedStrategies(updated)
        }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(isRenameDialogOpen = false, pendingDeleteStrategyId = null) }
    }

    fun duplicateTrackedStrategy(strategyId: String) {
        val strategy = _uiState.value.trackedStrategies.firstOrNull { it.id == strategyId } ?: return
        val duplicated = strategy.copy(
            id = UUID.randomUUID().toString(),
            name = "${strategy.name} (Copy)",
            rawLegs = strategy.rawLegs.map { it.copy(id = UUID.randomUUID().toString()) }
        )
        val updated = listOf(duplicated) + _uiState.value.trackedStrategies
        _uiState.update { it.copy(trackedStrategies = updated) }
        persistTrackedStrategies(updated)
    }
    
    fun analyzeStrategy() {
        val currentName = _uiState.value.customStrategyName.ifBlank {
            _uiState.value.selectedStrategyName.ifBlank { "Custom Strategy" }
        }
        calculateStrategyMetrics(currentName)
        _uiState.update { it.copy(isStrategyAnalyzed = true) }
    }
    
    fun saveStrategy() {
        val state = _uiState.value
        if (state.activeLegs.isEmpty()) return

        val strategyName = state.customStrategyName.ifBlank {
            state.selectedStrategyName.ifBlank { "Custom Strategy" }
        }

        // If not analyzed yet, run analysis so payoff points exist
        if (!state.isStrategyAnalyzed || state.selectedStrategy == null) {
            calculateStrategyMetrics(strategyName)
        }

        val analyzed = _uiState.value.selectedStrategy ?: return
        val finalStrategy = analyzed.copy(
            id = state.editingStrategyId ?: analyzed.id,
            name = strategyName,
            rawLegs = state.activeLegs
        )

        val updatedTracked = if (state.editingStrategyId != null) {
            state.trackedStrategies.map { if (it.id == state.editingStrategyId) finalStrategy else it }
        } else {
            listOf(finalStrategy) + state.trackedStrategies.filter { it.id != finalStrategy.id }
        }

        _uiState.update {
            it.copy(
                trackedStrategies = updatedTracked,
                selectedStrategy = finalStrategy,
                editingStrategyId = finalStrategy.id,
                isStrategyAnalyzed = true
            )
        }
        persistTrackedStrategies(updatedTracked)
    }

    fun editTrackedStrategy(strategy: OptionStrategy) {
        val loadedLegs = if (strategy.rawLegs.isNotEmpty()) {
            strategy.rawLegs
        } else {
            parseLegsFromStrings(strategy.legs)
        }
        _uiState.update {
            it.copy(
                activeLegs = loadedLegs,
                selectedStrategyName = strategy.name,
                customStrategyName = strategy.name,
                editingStrategyId = strategy.id,
                selectedStrategy = strategy,
                isStrategyAnalyzed = true
            )
        }
    }

    fun deleteTrackedStrategy(id: String) {
        val updated = _uiState.value.trackedStrategies.filter { it.id != id }
        val isEditingThis = _uiState.value.editingStrategyId == id
        _uiState.update {
            it.copy(
                trackedStrategies = updated,
                editingStrategyId = if (isEditingThis) null else it.editingStrategyId
            )
        }
        persistTrackedStrategies(updated)
    }

    fun cancelEditMode() {
        _uiState.update {
            it.copy(
                editingStrategyId = null,
                activeLegs = emptyList(),
                selectedStrategy = null,
                customStrategyName = "",
                selectedStrategyName = "",
                isStrategyAnalyzed = false
            )
        }
    }

    fun deleteOrClearStrategy() {
        val editingId = _uiState.value.editingStrategyId
        if (editingId != null) {
            deleteTrackedStrategy(editingId)
        }
        _uiState.update {
            it.copy(
                editingStrategyId = null,
                activeLegs = emptyList(),
                selectedStrategy = null,
                analysisResult = null,
                customStrategyName = "",
                selectedStrategyName = "",
                isStrategyAnalyzed = false,
                totalStrategyPnl = 0.0,
                totalStrategyPnlPct = 0.0,
                netPremium = 0.0,
                currentStrategyValue = 0.0,
                spotSimulationPct = 0.0,
                ivSimulation = 0.0,
                timeSimulation = 0,
                selectedCrosshairPrice = null
            )
        }
    }

    private fun parseLegsFromStrings(legStrings: List<String>): List<StrategyLeg> {
        val spot = _uiState.value.spotPrice
        val expiry = getUpcomingExpiry(0)
        val baseLot = getLotSize(_uiState.value.selectedAsset)
        return legStrings.mapNotNull { legStr ->
            try {
                val parts = legStr.trim().split(" ").filter { it.isNotBlank() }
                var idx = 0
                var qty = baseLot
                if (parts[idx].endsWith("x", ignoreCase = true)) {
                    val multiplier = parts[idx].dropLast(1).toIntOrNull() ?: 1
                    qty = multiplier * baseLot
                    idx++
                }
                val type = parts.getOrNull(idx) ?: "BUY"
                idx++
                val strike = parts.getOrNull(idx)?.toDoubleOrNull() ?: spot
                idx++
                val instrument = parts.getOrNull(idx) ?: "CALL"
                createLeg(type = type, instrument = instrument, strike = strike, expiry = expiry, qty = qty)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun calculateStrategyMetrics(name: String) {
        val state = _uiState.value
        if (state.activeLegs.isEmpty()) {
            _uiState.update { it.copy(selectedStrategy = null, analysisResult = null) }
            return
        }

        val baseLot = getLotSize(state.selectedAsset)
        val daysRemaining = (5.0 - state.timeSimulation).coerceAtLeast(0.05)
        val futPrice = state.summary.contracts.firstOrNull()?.ltp ?: (state.spotPrice * 1.002)

        val analysis = strategyEngine.analyzeStrategy(
            legs = state.activeLegs,
            spotPrice = state.spotPrice,
            impliedVolatility = state.summary.iv.coerceAtLeast(10.0),
            daysToExpiry = daysRemaining,
            interestRate = 0.065,
            lotSize = baseLot,
            numberOfLots = state.numberOfLots,
            spotShiftPct = state.spotSimulationPct,
            ivShiftPct = state.ivSimulation,
            targetDaysToExpiry = daysRemaining,
            futuresPrice = futPrice
        )

        _uiState.update {
            it.copy(
                analysisResult = analysis,
                selectedStrategy = OptionStrategy(
                    id = it.editingStrategyId ?: UUID.randomUUID().toString(),
                    name = name,
                    description = "Institutional Strategy Analytics",
                    maxProfit = analysis.maxProfit,
                    maxLoss = analysis.maxLoss,
                    breakeven = analysis.breakevens,
                    probability = analysis.probabilityOfProfit,
                    roi = if (abs(analysis.maxLoss) > 1.0 && !analysis.isMaxProfitUnlimited && !analysis.isMaxLossUnlimited) {
                        (analysis.maxProfit / abs(analysis.maxLoss)) * 100.0
                    } else 0.0,
                    legs = state.activeLegs.map { l ->
                        val b = if (l.lotSize > 0) l.lotSize else baseLot
                        val lotsMultiplier = if (b > 0 && l.qty > b) "${l.qty / b}x " else ""
                        val mStr = if (l.moneyness.isNotBlank()) " [${l.moneyness}]" else ""
                        val disabledStr = if (!l.isEnabled) " (OFF)" else ""
                        "$lotsMultiplier${l.type} ${l.strike.toInt()} ${l.instrument}$mStr$disabledStr"
                    },
                    payoffPoints = analysis.payoffPoints,
                    todayPayoffPoints = analysis.todayPayoffPoints,
                    rawLegs = state.activeLegs,
                    expiry = state.selectedExpiry.ifBlank { state.activeLegs.firstOrNull()?.expiry ?: getUpcomingExpiry(0) },
                    lotSize = baseLot,
                    lots = state.numberOfLots,
                    capitalRequired = analysis.capitalRequired,
                    underlying = state.selectedAsset,
                    isMaxProfitUnlimited = analysis.isMaxProfitUnlimited,
                    isMaxLossUnlimited = analysis.isMaxLossUnlimited,
                    riskRewardRatio = analysis.riskRewardRatio,
                    estimatedMargin = analysis.estimatedMargin,
                    netCreditDebit = analysis.netCreditDebit
                ),
                strategyGreeks = analysis.totalGreeks,
                totalStrategyPnl = analysis.livePnl,
                totalStrategyPnlPct = analysis.livePnlPercent,
                netPremium = analysis.netCreditDebit,
                currentStrategyValue = analysis.currentStrategyValue,
                capitalRequired = analysis.capitalRequired
            )
        }
    }

    fun loadAssetData(symbol: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            kotlinx.coroutines.delay(200)

            val resolved = resolveAssetInfo(symbol)
            val spot = resolved.spotPrice
            val liveChangePct = resolved.changePercent
            val actualSymbol = resolved.symbol
            val strikeStep = resolved.strikeStep
            val lotSize = resolved.lotSize
            val isIndex = actualSymbol.contains("NIFTY", ignoreCase = true) || actualSymbol.contains("SENSEX", ignoreCase = true) || actualSymbol.contains("SPX", ignoreCase = true) || actualSymbol.contains("DFMGI", ignoreCase = true)

            // Register with live bus so price updates work consistently
            intelligenceBus.updateLivePrice(actualSymbol, spot, spot * liveChangePct / 100.0, liveChangePct)

            val expiries = generateAvailableExpiries()
            val selectedExp = expiries.firstOrNull() ?: getUpcomingExpiry(0)
            _uiState.update { it.copy(
                selectedAsset = resolved.displayName,
                spotPrice = spot,
                lotSize = lotSize,
                availableExpiries = expiries,
                selectedExpiry = if (it.selectedExpiry.isBlank() || !expiries.contains(it.selectedExpiry)) selectedExp else it.selectedExpiry
            ) }

            val nearExpiry = getUpcomingMonthlyExpiry(0)
            val nextExpiry = getUpcomingMonthlyExpiry(1)
            val baseStrike = round(spot / strikeStep) * strikeStep
            val strikeIndices = if (isIndex) (-50..50) else (-25..25)
            val timeYears = 5.0 / 365.0
            
            val domainChain = OptionChain(spot, strikes = strikeIndices.map { i ->
                val strike = baseStrike + (i * strikeStep)
                val dist = abs(strike - spot)
                val callOi = max(10000.0, 180000.0 - (dist * 15.0) + (Random().nextInt(2000)))
                val putOi = max(10000.0, 160000.0 - (dist * 12.0) + (Random().nextInt(2000)))
                val callIv = 14.5 + (i * 0.05)
                val putIv = 15.2 - (i * 0.05)
                val callCalc = quantEngine.calculate(
                    forward = spot,
                    strike = strike,
                    rate = 0.065,
                    timeToExpiryYears = timeYears,
                    volatility = callIv / 100.0,
                    isCall = true
                )
                val putCalc = quantEngine.calculate(
                    forward = spot,
                    strike = strike,
                    rate = 0.065,
                    timeToExpiryYears = timeYears,
                    volatility = putIv / 100.0,
                    isCall = false
                )
                val callLtp = round(max(0.05, callCalc.price) * 100.0) / 100.0
                val putLtp = round(max(0.05, putCalc.price) * 100.0) / 100.0
                OptionChainData(
                    strike = strike,
                    callOI = callOi,
                    callLTP = callLtp,
                    callChange = if (liveChangePct >= 0) 8.5 else -6.2,
                    callIV = callIv,
                    callGreeks = Greeks(callCalc.delta, callCalc.gamma, callCalc.theta, callCalc.vega),
                    putOI = putOi,
                    putLTP = putLtp,
                    putChange = if (liveChangePct >= 0) -7.1 else 9.4,
                    putIV = putIv,
                    putGreeks = Greeks(putCalc.delta, putCalc.gamma, putCalc.theta, putCalc.vega)
                )
            })

            val analyzerChainData = domainChain.strikes.map { s ->
                com.example.redxfnoscanner.data.Option(
                    type = "CE",
                    strikePrice = s.strike,
                    openInterest = s.callOI.toInt(),
                    lastTradedPrice = s.callLTP,
                    changeInOpenInterest = (Random().nextInt(1000)),
                    priceChange = s.callChange,
                    impliedVolatility = s.callIV
                ) to com.example.redxfnoscanner.data.Option(
                    type = "PE",
                    strikePrice = s.strike,
                    openInterest = s.putOI.toInt(),
                    lastTradedPrice = s.putLTP,
                    changeInOpenInterest = (Random().nextInt(1000)),
                    priceChange = s.putChange,
                    impliedVolatility = s.putIV
                )
            }
            
            val analyzerChain = com.example.redxfnoscanner.data.OptionChain(expiryDate = nearExpiry, options = analyzerChainData.flatMap { listOf(it.first, it.second) })
            val pcrValue = round(optionChainAnalyzer.calculatePCR(analyzerChain) * 100.0) / 100.0
            val maxPainValue = optionChainAnalyzer.findMaxPain(analyzerChain)
            val (support, resistance) = optionChainAnalyzer.findSupportAndResistance(analyzerChain)
            val gexProfile = gexEngine.computeGex(domainChain, spot)

            val instGreeksMap = domainChain.strikes.associate {
                it.strike to quantEngine.calculate(
                    forward = spot,
                    strike = it.strike,
                    rate = 0.065,
                    timeToExpiryYears = 5.0 / 365.0,
                    volatility = it.callIV / 100.0,
                    isCall = true
                )
            }

            val participants = listOf(
                ParticipantPositioning("FII (Foreign Inst)", netFutures = 42500, netCalls = 185000, netPuts = 95000, bias = "BULLISH ACCUMULATION"),
                ParticipantPositioning("PRO (Prop Desks)", netFutures = 12200, netCalls = -45000, netPuts = 62000, bias = "LONG GAMMA PINNING"),
                ParticipantPositioning("DII (Domestic Inst)", netFutures = -15400, netCalls = 12000, netPuts = 38000, bias = "PORTFOLIO HEDGING"),
                ParticipantPositioning("CLIENT (Retail)", netFutures = -39300, netCalls = -152000, netPuts = -195000, bias = "NET SHORT BIAS")
            )
            
            val calculatedRegime = when {
                pcrValue >= 1.2 -> "STRONG BULLISH CONFLUENCE"
                pcrValue >= 1.0 -> "MILD BULLISH ACCUMULATION"
                pcrValue >= 0.8 -> "NEUTRAL / GAMMA PINNING"
                else -> "BEARISH PRESSURE / CALL WRITING"
            }

            // Sync with central LiveIntelligenceBus for AI Mentor
            val fnoInfo = com.example.marketintelligence.domain.engine.FnoIntelligence(
                symbol = symbol,
                spotPrice = spot,
                changePercent = liveChangePct,
                regime = calculatedRegime,
                pcr = pcrValue,
                maxPain = maxPainValue,
                callWall = resistance,
                putWall = support,
                institutionalBias = if (pcrValue >= 1.0) "Institutional Put Writing at ${support.toInt()} indicates strong support floor." else "Aggressive Call Writing at ${resistance.toInt()} capping upside momentum."
            )
            intelligenceBus.updateFnoIntelligence(fnoInfo)

            val signalAction = if (pcrValue >= 1.0) "Bull Call Spread" else "Bear Put Spread"
            val targetStrike = if (pcrValue >= 1.0) baseStrike + strikeStep else baseStrike - strikeStep
            val atmStrike = baseStrike
            val otmStrike = targetStrike
            val leg1Type = if (pcrValue >= 1.0) com.example.marketintelligence.data.model.OptionType.CE else com.example.marketintelligence.data.model.OptionType.PE
            val leg2Type = leg1Type
            val legs = listOf(
                com.example.marketintelligence.data.model.OptionLeg("$symbol ${atmStrike.toInt()} ${leg1Type.name}", leg1Type, atmStrike, nearExpiry, com.example.marketintelligence.data.model.TradeAction.BUY, lotSize, 130.0),
                com.example.marketintelligence.data.model.OptionLeg("$symbol ${otmStrike.toInt()} ${leg2Type.name}", leg2Type, otmStrike, nearExpiry, com.example.marketintelligence.data.model.TradeAction.SELL, lotSize, 65.0)
            )
            val signal = com.example.marketintelligence.data.model.AiTradeSignal(
                underlyingSymbol = symbol,
                strategyName = signalAction,
                marketRegime = if (pcrValue >= 1.0) com.example.marketintelligence.data.model.MarketRegime.TRENDING_BULLISH else com.example.marketintelligence.data.model.MarketRegime.TRENDING_BEARISH,
                optionLegs = legs,
                entryPrice = 65.0,
                target = 135.0,
                stopLoss = 32.0,
                confidenceScore = 85.0,
                riskParameters = com.example.marketintelligence.data.model.RiskParameters(1, 3250.0, 16500.0, 3250.0)
            )
            intelligenceBus.postFnoSignal(signal)

            _uiState.update { it.copy(
                isLoading = false,
                spotPrice = spot,
                strikeStep = strikeStep,
                optionChain = domainChain,
                summary = FnoSummary(
                    changePercent = liveChangePct,
                    iv = 14.8,
                    pcr = pcrValue,
                    maxPain = maxPainValue,
                    lotSize = lotSize,
                    trend = if(pcrValue > 1.0) "BULLISH" else "BEARISH",
                    contracts = listOf(
                        FutureContract(nearExpiry, spot + (strikeStep * 0.4), liveChangePct, "+5K", "2M", spot + (strikeStep * 0.38), strikeStep * 0.4),
                        FutureContract(nextExpiry, spot + (strikeStep * 1.2), liveChangePct, "+2K", "1M", spot + (strikeStep * 1.15), strikeStep * 1.2)
                    ),
                    highestCallOIStrike = resistance,
                    highestPutOIStrike = support
                ),
                futuresBuildup = listOf(
                    BuildupData("RELIANCE", "Long Buildup", 1.2, 4.5),
                    BuildupData("HDFCBANK", "Short Covering", 0.8, -2.1),
                    BuildupData("TCS", "Short Buildup", -1.5, 6.2)
                ),
                heatmapData = listOf(
                    HeatmapItem("BANKING", 1.2, 0.35f),
                    HeatmapItem("IT", -0.5, 0.25f),
                    HeatmapItem("OIL & GAS", 0.8, 0.20f),
                    HeatmapItem("PHARMA", 0.3, 0.10f)
                ),
                maxPainHistory = List(10) { spot - (Math.random() * strikeStep * 2).toInt() },
                ivHistory = List(10) { 14.0 + (Math.random() * 4) },
                gexProfile = gexProfile,
                institutionalGreeks = instGreeksMap,
                participantData = participants
            ) }

            if (_uiState.value.activeLegs.isNotEmpty()) {
                recalculateLivePnlAndMetrics()
            }
        }
    }
}
