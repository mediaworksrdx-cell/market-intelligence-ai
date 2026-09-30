package com.example.marketintelligence.ui.fno

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.marketintelligence.data.local.MockData
import com.example.marketintelligence.domain.engine.Black76QuantEngine
import com.example.marketintelligence.domain.engine.DealerGammaExposureEngine
import com.example.marketintelligence.domain.model.*
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

        _uiState.update {
            it.copy(
                buildupStocks = initialBuildup,
                longBuildupCount = longCount,
                shortBuildupCount = shortCount,
                shortCoveringCount = coveringCount,
                longUnwindingCount = unwindingCount
            )
        }

        settingsRepository.selectedMarket.onEach { market ->
            val defaultAsset = when(market) {
                MarketType.IN -> "NIFTY 50"
                MarketType.US -> "SPX"
                MarketType.UAE -> "DFMGI"
            }
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

        // 2. Continuous real-time active micro-tick loop (ensures live P&L moves dynamically)
        viewModelScope.launch(Dispatchers.IO) {
            val rnd = java.util.Random()
            while (isActive) {
                delay(2000)
                try {
                    val currentSymbol = _uiState.value.selectedAsset
                    if (currentSymbol.isNotBlank()) {
                        val currentSpot = _uiState.value.spotPrice
                        if (currentSpot > 0.0) {
                            val deltaPct = (rnd.nextDouble() - 0.495) * 0.0004
                            val tickedPrice = round((currentSpot * (1.0 + deltaPct)) * 100.0) / 100.0
                            val currentChangePct = _uiState.value.summary.changePercent + (deltaPct * 10.0)
                            val roundedChange = round(currentChangePct * 100.0) / 100.0
                            
                            intelligenceBus.updateLivePrice(currentSymbol, tickedPrice, (tickedPrice - currentSpot), roundedChange)
                            onSpotPriceTick(tickedPrice, roundedChange)
                        }
                    }

                    // Dynamically micro-tick 1-2 random stocks in Buildup Scanner
                    val currentBuildup = _uiState.value.buildupStocks
                    if (currentBuildup.isNotEmpty()) {
                        val pickIndex = rnd.nextInt(currentBuildup.size)
                        val target = currentBuildup[pickIndex]
                        val dPct = (rnd.nextDouble() - 0.49) * 0.0006
                        val newPrice = round((target.ltp * (1.0 + dPct)) * 100.0) / 100.0
                        val newPriceChange = round((target.priceChange + (newPrice - target.ltp)) * 100.0) / 100.0
                        val newPriceChangePct = round((target.priceChangePct + (dPct * 10.0)) * 100.0) / 100.0
                        val newType = buildupRepository.classifyBuildup(newPriceChange, target.oiChange)
                        val updatedStock = target.copy(
                            ltp = newPrice,
                            priceChange = newPriceChange,
                            priceChangePct = newPriceChangePct,
                            buildupType = newType
                        )
                        val newList = currentBuildup.toMutableList().also { it[pickIndex] = updatedStock }
                        _uiState.update { s ->
                            s.copy(
                                buildupStocks = newList,
                                longBuildupCount = newList.count { it.buildupType == BuildupType.LONG_BUILDUP },
                                shortBuildupCount = newList.count { it.buildupType == BuildupType.SHORT_BUILDUP },
                                shortCoveringCount = newList.count { it.buildupType == BuildupType.SHORT_COVERING },
                                longUnwindingCount = newList.count { it.buildupType == BuildupType.LONG_UNWINDING }
                            )
                        }
                    }
                } catch (e: Exception) { Log.e("FnoVM", "Error: ${e.message}") }
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

    fun addAsset() {
        loadAssetData(_uiState.value.searchQuery)
    }
    
    fun setStrategyCategory(category: StrategyCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    private fun getUpcomingExpiry(weeksAhead: Int = 0): String {
        val calendar = Calendar.getInstance()
        var daysToAdd = (Calendar.THURSDAY - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
        if (daysToAdd == 0 && calendar.get(Calendar.HOUR_OF_DAY) >= 15) {
            daysToAdd = 7
        }
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd + (weeksAhead * 7))
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthNames = arrayOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val month = monthNames[calendar.get(Calendar.MONTH)]
        val year = (calendar.get(Calendar.YEAR) % 100).toString().padStart(2, '0')
        return "$day$month$year"
    }

    private fun getUpcomingMonthlyExpiry(monthsAhead: Int = 0): String {
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
        val year = (calendar.get(Calendar.YEAR) % 100).toString().padStart(2, '0')
        return "$day$month$year"
    }

    fun getLotSize(symbol: String = _uiState.value.selectedAsset): Int {
        val clean = symbol.uppercase().trim().removeSuffix(".NS").removeSuffix(".BO")
        return when {
            clean.contains("BANKNIFTY") -> 15
            clean.contains("FINNIFTY") -> 40
            clean.contains("NIFTY") -> 25
            clean.contains("SENSEX") -> 10
            clean.contains("RELIANCE") -> 250
            clean.contains("HDFCBANK") -> 550
            clean.contains("TCS") -> 175
            clean.contains("ICICIBANK") -> 700
            clean.contains("SBIN") -> 750
            clean.contains("INFY") -> 400
            clean.contains("SPX") -> 100
            clean.contains("DFMGI") -> 100
            clean in listOf("BTC", "ETH", "SOL", "BNB", "DOGE", "XRP") -> 1
            else -> 250
        }
    }

    fun getStrikeStepForSymbol(symbol: String, spot: Double): Double {
        val clean = symbol.uppercase().trim().removeSuffix(".NS").removeSuffix(".BO")
        return when {
            clean.contains("BANKNIFTY") || clean.contains("SENSEX") -> 100.0
            clean.contains("NIFTY") || clean.contains("FINNIFTY") -> 50.0
            clean.contains("TCS") -> 50.0
            clean.contains("RELIANCE") || clean.contains("INFY") || clean.contains("ICICIBANK") -> 20.0
            clean.contains("SBIN") || clean.contains("HDFCBANK") -> 10.0
            spot > 5000 -> 100.0
            spot > 2000 -> 50.0
            spot > 1000 -> 20.0
            spot > 500 -> 10.0
            else -> 5.0
        }
    }

    fun selectPredefinedStrategy(strategyName: String) {
        val spot = _uiState.value.spotPrice
        val nearExpiry = getUpcomingExpiry(0)
        val nextExpiry = getUpcomingMonthlyExpiry(1)
        val step = _uiState.value.strikeStep
        val lot = getLotSize(_uiState.value.selectedAsset)
        
        val legs = when(strategyName) {
            // --- BULLISH STRATEGIES ---
            "Long Call" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry, qty = lot)
            )
            "Bull Call Spread" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry, qty = lot),
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot)
            )
            "Bull Put Spread" -> listOf(
                createLeg("SELL", "PUT", spot, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot - (step * 2), nearExpiry, qty = lot)
            )
            "Covered Call" -> listOf(
                createLeg("BUY", "FUT", 0.0, nearExpiry, qty = lot),
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot)
            )
            "Call Ratio Backspread" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot + (step * 2), nearExpiry, qty = lot * 2)
            )
            "Bull Calendar Spread" -> listOf(
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot + (step * 2), nextExpiry, qty = lot)
            )
            "Call Diagonal Spread" -> listOf(
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot - step, nextExpiry, qty = lot)
            )
            "Long Call Butterfly" -> listOf(
                createLeg("BUY", "CALL", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot * 2),
                createLeg("BUY", "CALL", spot + (step * 2), nearExpiry, qty = lot)
            )

            // --- BEARISH STRATEGIES ---
            "Long Put" -> listOf(
                createLeg("BUY", "PUT", spot, nearExpiry, qty = lot)
            )
            "Bear Call Spread" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot + (step * 2), nearExpiry, qty = lot)
            )
            "Bear Put Spread" -> listOf(
                createLeg("BUY", "PUT", spot, nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot)
            )
            "Protective Put" -> listOf(
                createLeg("BUY", "FUT", 0.0, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot, nearExpiry, qty = lot)
            )
            "Put Ratio Backspread" -> listOf(
                createLeg("SELL", "PUT", spot, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot - (step * 2), nearExpiry, qty = lot * 2)
            )
            "Bear Calendar Spread" -> listOf(
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot - (step * 2), nextExpiry, qty = lot)
            )
            "Put Diagonal Spread" -> listOf(
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot + step, nextExpiry, qty = lot)
            )
            "Long Put Butterfly" -> listOf(
                createLeg("BUY", "PUT", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot, nearExpiry, qty = lot * 2),
                createLeg("BUY", "PUT", spot - (step * 2), nearExpiry, qty = lot)
            )

            // --- NEUTRAL / INCOME STRATEGIES ---
            "Short Straddle" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot, nearExpiry, qty = lot)
            )
            "Short Strangle" -> listOf(
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot)
            )
            "Iron Condor" -> listOf(
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot + (step * 4), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot - (step * 4), nearExpiry, qty = lot)
            )
            "Iron Butterfly" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot, nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot - (step * 2), nearExpiry, qty = lot)
            )
            "Jade Lizard" -> listOf(
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot + (step * 4), nearExpiry, qty = lot)
            )
            "Reverse Jade Lizard" -> listOf(
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("BUY", "PUT", spot - (step * 4), nearExpiry, qty = lot)
            )
            "Calendar Spread" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot),
                createLeg("BUY", "CALL", spot, nextExpiry, qty = lot)
            )
            "Broken Wing Butterfly" -> listOf(
                createLeg("BUY", "CALL", spot - (step * 2), nearExpiry, qty = lot),
                createLeg("SELL", "CALL", spot, nearExpiry, qty = lot * 2),
                createLeg("BUY", "CALL", spot + (step * 3), nearExpiry, qty = lot)
            )

            // --- VOLATILITY / BREAKOUT STRATEGIES ---
            "Long Straddle" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry),
                createLeg("BUY", "PUT", spot, nearExpiry)
            )
            "Long Strangle" -> listOf(
                createLeg("BUY", "CALL", spot + (step * 2), nearExpiry),
                createLeg("BUY", "PUT", spot - (step * 2), nearExpiry)
            )
            "Reverse Iron Condor" -> listOf(
                createLeg("BUY", "CALL", spot + (step * 2), nearExpiry),
                createLeg("SELL", "CALL", spot + (step * 4), nearExpiry),
                createLeg("BUY", "PUT", spot - (step * 2), nearExpiry),
                createLeg("SELL", "PUT", spot - (step * 4), nearExpiry)
            )
            "Reverse Iron Butterfly" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry),
                createLeg("SELL", "CALL", spot + (step * 2), nearExpiry),
                createLeg("BUY", "PUT", spot, nearExpiry),
                createLeg("SELL", "PUT", spot - (step * 2), nearExpiry)
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
    
    private fun createLeg(type: String, instrument: String, strike: Double, expiry: String, qty: Int = getLotSize(_uiState.value.selectedAsset)): StrategyLeg {
        val spot = _uiState.value.spotPrice
        val currentIv = (_uiState.value.summary.iv).coerceAtLeast(5.0) / 100.0
        val timeYears = 5.0 / 365.0
        val entry = if (instrument == "FUT") {
            spot
        } else {
            val isCall = instrument.equals("CALL", ignoreCase = true)
            val g = quantEngine.calculate(
                forward = spot,
                strike = strike,
                rate = 0.065,
                timeToExpiryYears = timeYears,
                volatility = currentIv,
                isCall = isCall
            )
            round(max(0.5, g.price) * 100.0) / 100.0
        }
        return StrategyLeg(
            id = UUID.randomUUID().toString(),
            type = type,
            instrument = instrument,
            strike = strike,
            expiry = expiry,
            qty = qty,
            entryPrice = entry,
            currentPrice = entry
        )
    }

    fun addLeg(instrument: String, type: String, strike: Double) {
        val lot = getLotSize(_uiState.value.selectedAsset)
        val newLeg = createLeg(type, instrument, strike, getUpcomingExpiry(0), qty = lot)
        _uiState.update { it.copy(activeLegs = it.activeLegs + newLeg) }
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

        _uiState.update {
            it.copy(
                totalStrategyPnl = round(totalPnl * 10.0) / 10.0,
                totalStrategyPnlPct = round(totalPnlPct * 10.0) / 10.0,
                netPremium = round(netPrem * 10.0) / 10.0,
                currentStrategyValue = round(currentVal * 10.0) / 10.0
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
        _uiState.update { it.copy(ivSimulation = ivShift, timeSimulation = timeShift) }
        val name = _uiState.value.customStrategyName.ifBlank { _uiState.value.selectedStrategy?.name ?: "Custom Strategy" }
        calculateStrategyMetrics(name)
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
                customStrategyName = "",
                selectedStrategyName = "",
                isStrategyAnalyzed = false,
                totalStrategyPnl = 0.0,
                totalStrategyPnlPct = 0.0,
                netPremium = 0.0,
                currentStrategyValue = 0.0
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
            _uiState.update { it.copy(selectedStrategy = null) }
            return
        }

        val spot = state.spotPrice
        val range = (spot * 0.06).toInt()
        val expiryPoints = mutableListOf<PayoffPoint>()
        val todayPoints = mutableListOf<PayoffPoint>()
        val currentIv = (state.summary.iv + state.ivSimulation).coerceAtLeast(5.0) / 100.0
        val daysRemaining = (5.0 - state.timeSimulation).coerceAtLeast(0.05)
        val timeYears = daysRemaining / 365.0

        var portfolioDelta = 0.0
        var portfolioGamma = 0.0
        var portfolioTheta = 0.0
        var portfolioVega = 0.0
        val baseLot = getLotSize(state.selectedAsset).toDouble()

        // Compute net Portfolio Greeks
        state.activeLegs.forEach { leg ->
            val multiplier = if (leg.type == "BUY") 1.0 else -1.0
            val lotMultiplier = leg.qty / baseLot
            if (leg.instrument == "FUT") {
                portfolioDelta += 1.0 * multiplier * lotMultiplier
            } else {
                val isCall = leg.instrument == "CALL"
                val g = quantEngine.calculate(
                    forward = spot,
                    strike = leg.strike,
                    rate = 0.065,
                    timeToExpiryYears = timeYears,
                    volatility = currentIv,
                    isCall = isCall
                )
                portfolioDelta += g.delta * multiplier * lotMultiplier
                portfolioGamma += g.gamma * multiplier * lotMultiplier
                portfolioTheta += g.theta * multiplier * lotMultiplier * baseLot
                portfolioVega += g.vega * multiplier * lotMultiplier * baseLot
            }
        }

        val step = max(5, (state.strikeStep / 2).toInt())
        for (i in -range..range step step) {
            val price = spot + i
            var totalPnlExpiry = 0.0
            var totalPnlToday = 0.0
            
            state.activeLegs.forEach { leg ->
                val multiplier = if(leg.type == "BUY") 1 else -1
                val profitExpiry = when (leg.instrument) {
                    "CALL" -> max(0.0, price - leg.strike) - leg.entryPrice
                    "PUT" -> max(0.0, leg.strike - price) - leg.entryPrice
                    else -> price - leg.entryPrice
                }
                totalPnlExpiry += profitExpiry * leg.qty * multiplier

                // Exact Black-76 theoretical value for Today's T+0 curve
                val priceToday = if (leg.instrument == "FUT") {
                    price
                } else {
                    val isCall = leg.instrument == "CALL"
                    val gToday = quantEngine.calculate(
                        forward = price,
                        strike = leg.strike,
                        rate = 0.065,
                        timeToExpiryYears = timeYears,
                        volatility = currentIv,
                        isCall = isCall
                    )
                    gToday.price
                }
                val profitToday = priceToday - leg.entryPrice
                totalPnlToday += profitToday * leg.qty * multiplier
            }
            expiryPoints.add(PayoffPoint(price, totalPnlExpiry))
            todayPoints.add(PayoffPoint(price, totalPnlToday))
        }
        
        val breakevens = mutableListOf<Double>()
        for (i in 0 until expiryPoints.size - 1) {
            val p1 = expiryPoints[i]
            val p2 = expiryPoints[i+1]
            if ((p1.pnl < 0 && p2.pnl >= 0) || (p1.pnl >= 0 && p2.pnl < 0)) {
                breakevens.add(p1.price)
            }
        }
        
        val maxProfitValue = expiryPoints.maxOf { it.pnl }
        val maxLossValue = expiryPoints.minOf { it.pnl }
        val isDebit = name.contains("Long") || name.contains("Reverse") || name.contains("Backspread")
        val prob = quantEngine.calculateProbabilityOfProfit(
            spot = spot,
            breakevens = breakevens,
            volatility = currentIv,
            timeToExpiryYears = timeYears,
            isDebitOrBreakout = isDebit
        )

        _uiState.update { it.copy(
            selectedStrategy = OptionStrategy(
                id = it.editingStrategyId ?: UUID.randomUUID().toString(),
                name = name,
                description = "Institutional Build",
                maxProfit = maxProfitValue,
                maxLoss = maxLossValue,
                breakeven = breakevens,
                probability = (prob * 10).toInt() / 10.0,
                roi = if (abs(maxLossValue) > 1.0) abs(maxProfitValue / maxLossValue) * 100 else 0.0,
                legs = state.activeLegs.map { l -> val b = baseLot.toInt(); "${if (b > 0 && l.qty > b) "${l.qty / b}x " else ""}${l.type} ${l.strike.toInt()} ${l.instrument}" },
                payoffPoints = expiryPoints,
                todayPayoffPoints = todayPoints,
                rawLegs = state.activeLegs
            ),
            strategyGreeks = Greeks(
                delta = (portfolioDelta * 100).toInt() / 100.0,
                gamma = (portfolioGamma * 10000).toInt() / 10000.0,
                theta = (portfolioTheta * 10).toInt() / 10.0,
                vega = (portfolioVega * 10).toInt() / 10.0
            )
        ) }
    }

    private fun loadAssetData(symbol: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedAsset = symbol) }
            kotlinx.coroutines.delay(400)

            // 1. Get live price from LiveIntelligenceBus or fallbacks
            val liveInfo = intelligenceBus.getLivePrice(symbol)
            val spot = liveInfo?.price ?: when {
                symbol.contains("BANKNIFTY", ignoreCase = true) -> 54259.95
                symbol.contains("NIFTY", ignoreCase = true) -> 22716.20
                symbol.contains("SENSEX", ignoreCase = true) -> 72529.07
                symbol.contains("FINNIFTY", ignoreCase = true) -> 24648.50
                symbol.contains("RELIANCE", ignoreCase = true) -> 1182.00
                symbol.contains("HDFCBANK", ignoreCase = true) -> 722.70
                symbol.contains("TCS", ignoreCase = true) -> 2032.40
                symbol.contains("INFY", ignoreCase = true) -> 1015.40
                symbol.contains("SPX", ignoreCase = true) -> 5485.88
                symbol.contains("DFMGI", ignoreCase = true) -> 4849.37
                else -> 22716.20
            }
            val rawChangePct = liveInfo?.changePercent ?: -0.45
            val liveChangePct = round(rawChangePct * 100.0) / 100.0

            val nearExpiry = getUpcomingMonthlyExpiry(0)
            val nextExpiry = getUpcomingMonthlyExpiry(1)

            val isIndex = symbol.contains("NIFTY", ignoreCase = true) || symbol.contains("SENSEX", ignoreCase = true) || symbol.contains("SPX", ignoreCase = true) || symbol.contains("DFMGI", ignoreCase = true)
            val strikeStep = getStrikeStepForSymbol(symbol, spot)
            val lotSize = getLotSize(symbol)
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
