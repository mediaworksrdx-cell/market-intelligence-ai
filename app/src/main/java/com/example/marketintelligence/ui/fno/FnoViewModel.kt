package com.example.marketintelligence.ui.fno

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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@HiltViewModel
class FnoViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val quantEngine: Black76QuantEngine,
    private val gexEngine: DealerGammaExposureEngine
) : ViewModel() {

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

    fun selectPredefinedStrategy(strategyName: String) {
        val spot = _uiState.value.spotPrice
        val nearExpiry = getUpcomingExpiry(0)
        val nextExpiry = getUpcomingMonthlyExpiry(1)
        
        val legs = when(strategyName) {
            // --- BULLISH STRATEGIES ---
            "Long Call" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry)
            )
            "Bull Call Spread" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry),
                createLeg("SELL", "CALL", spot + 200, nearExpiry)
            )
            "Bull Put Spread" -> listOf(
                createLeg("SELL", "PUT", spot, nearExpiry),
                createLeg("BUY", "PUT", spot - 200, nearExpiry)
            )
            "Covered Call" -> listOf(
                createLeg("BUY", "FUT", 0.0, nearExpiry),
                createLeg("SELL", "CALL", spot + 200, nearExpiry)
            )
            "Call Ratio Backspread" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry, qty = 50),
                createLeg("BUY", "CALL", spot + 200, nearExpiry, qty = 100)
            )
            "Bull Calendar Spread" -> listOf(
                createLeg("SELL", "CALL", spot + 200, nearExpiry, qty = 50),
                createLeg("BUY", "CALL", spot + 200, nextExpiry, qty = 50)
            )
            "Call Diagonal Spread" -> listOf(
                createLeg("SELL", "CALL", spot + 200, nearExpiry, qty = 50),
                createLeg("BUY", "CALL", spot - 100, nextExpiry, qty = 50)
            )
            "Long Call Butterfly" -> listOf(
                createLeg("BUY", "CALL", spot - 200, nearExpiry, qty = 50),
                createLeg("SELL", "CALL", spot, nearExpiry, qty = 100),
                createLeg("BUY", "CALL", spot + 200, nearExpiry, qty = 50)
            )

            // --- BEARISH STRATEGIES ---
            "Long Put" -> listOf(
                createLeg("BUY", "PUT", spot, nearExpiry)
            )
            "Bear Call Spread" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry),
                createLeg("BUY", "CALL", spot + 200, nearExpiry)
            )
            "Bear Put Spread" -> listOf(
                createLeg("BUY", "PUT", spot, nearExpiry),
                createLeg("SELL", "PUT", spot - 200, nearExpiry)
            )
            "Protective Put" -> listOf(
                createLeg("BUY", "FUT", 0.0, nearExpiry),
                createLeg("BUY", "PUT", spot, nearExpiry)
            )
            "Put Ratio Backspread" -> listOf(
                createLeg("SELL", "PUT", spot, nearExpiry, qty = 50),
                createLeg("BUY", "PUT", spot - 200, nearExpiry, qty = 100)
            )
            "Bear Calendar Spread" -> listOf(
                createLeg("SELL", "PUT", spot - 200, nearExpiry, qty = 50),
                createLeg("BUY", "PUT", spot - 200, nextExpiry, qty = 50)
            )
            "Put Diagonal Spread" -> listOf(
                createLeg("SELL", "PUT", spot - 200, nearExpiry, qty = 50),
                createLeg("BUY", "PUT", spot + 100, nextExpiry, qty = 50)
            )
            "Long Put Butterfly" -> listOf(
                createLeg("BUY", "PUT", spot + 200, nearExpiry, qty = 50),
                createLeg("SELL", "PUT", spot, nearExpiry, qty = 100),
                createLeg("BUY", "PUT", spot - 200, nearExpiry, qty = 50)
            )

            // --- NEUTRAL / INCOME STRATEGIES ---
            "Short Straddle" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry),
                createLeg("SELL", "PUT", spot, nearExpiry)
            )
            "Short Strangle" -> listOf(
                createLeg("SELL", "CALL", spot + 200, nearExpiry),
                createLeg("SELL", "PUT", spot - 200, nearExpiry)
            )
            "Iron Condor" -> listOf(
                createLeg("SELL", "CALL", spot + 200, nearExpiry),
                createLeg("BUY", "CALL", spot + 400, nearExpiry),
                createLeg("SELL", "PUT", spot - 200, nearExpiry),
                createLeg("BUY", "PUT", spot - 400, nearExpiry)
            )
            "Iron Butterfly" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry),
                createLeg("BUY", "CALL", spot + 200, nearExpiry),
                createLeg("SELL", "PUT", spot, nearExpiry),
                createLeg("BUY", "PUT", spot - 200, nearExpiry)
            )
            "Jade Lizard" -> listOf(
                createLeg("SELL", "PUT", spot - 200, nearExpiry),
                createLeg("SELL", "CALL", spot + 200, nearExpiry),
                createLeg("BUY", "CALL", spot + 400, nearExpiry)
            )
            "Reverse Jade Lizard" -> listOf(
                createLeg("SELL", "CALL", spot + 200, nearExpiry),
                createLeg("SELL", "PUT", spot - 200, nearExpiry),
                createLeg("BUY", "PUT", spot - 400, nearExpiry)
            )
            "Calendar Spread" -> listOf(
                createLeg("SELL", "CALL", spot, nearExpiry),
                createLeg("BUY", "CALL", spot, nextExpiry)
            )
            "Broken Wing Butterfly" -> listOf(
                createLeg("BUY", "CALL", spot - 200, nearExpiry, qty = 50),
                createLeg("SELL", "CALL", spot, nearExpiry, qty = 100),
                createLeg("BUY", "CALL", spot + 300, nearExpiry, qty = 50)
            )

            // --- VOLATILITY / BREAKOUT STRATEGIES ---
            "Long Straddle" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry),
                createLeg("BUY", "PUT", spot, nearExpiry)
            )
            "Long Strangle" -> listOf(
                createLeg("BUY", "CALL", spot + 200, nearExpiry),
                createLeg("BUY", "PUT", spot - 200, nearExpiry)
            )
            "Reverse Iron Condor" -> listOf(
                createLeg("BUY", "CALL", spot + 200, nearExpiry),
                createLeg("SELL", "CALL", spot + 400, nearExpiry),
                createLeg("BUY", "PUT", spot - 200, nearExpiry),
                createLeg("SELL", "PUT", spot - 400, nearExpiry)
            )
            "Reverse Iron Butterfly" -> listOf(
                createLeg("BUY", "CALL", spot, nearExpiry),
                createLeg("SELL", "CALL", spot + 200, nearExpiry),
                createLeg("BUY", "PUT", spot, nearExpiry),
                createLeg("SELL", "PUT", spot - 200, nearExpiry)
            )
            else -> emptyList()
        }
        
        _uiState.update { it.copy(activeLegs = legs) }
        calculateStrategyMetrics(strategyName)
    }
    
    private fun createLeg(type: String, instrument: String, strike: Double, expiry: String, qty: Int = 50): StrategyLeg {
        val spot = _uiState.value.spotPrice
        val diff = abs(strike - spot)
        val entry = if (instrument == "FUT") {
            spot
        } else {
            val baseAtm = 160.0
            max(20.0, baseAtm - (diff * 0.45))
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
        val newLeg = createLeg(type, instrument, strike, getUpcomingExpiry(0))
        _uiState.update { it.copy(activeLegs = it.activeLegs + newLeg) }
        calculateStrategyMetrics("Custom Strategy")
    }

    fun removeLeg(id: String) {
        _uiState.update { it.copy(activeLegs = it.activeLegs.filter { it.id != id }) }
        calculateStrategyMetrics("Custom Strategy")
    }
    
    fun updateLeg(id: String, strike: Double) {
        _uiState.update { it.copy(activeLegs = it.activeLegs.map { leg -> if (leg.id == id) leg.copy(strike = strike) else leg }) }
        calculateStrategyMetrics(_uiState.value.selectedStrategy?.name ?: "Custom")
    }

    fun onSimulationChanged(ivShift: Double, timeShift: Int) {
        _uiState.update { it.copy(ivSimulation = ivShift, timeSimulation = timeShift) }
        calculateStrategyMetrics(_uiState.value.selectedStrategy?.name ?: "Custom")
    }
    
    fun analyzeStrategy() {
        calculateStrategyMetrics(_uiState.value.selectedStrategy?.name ?: "Custom")
    }
    
    fun saveStrategy() {
        val currentStrategy = _uiState.value.selectedStrategy
        if (currentStrategy != null) {
            _uiState.update { it.copy(trackedStrategies = it.trackedStrategies + currentStrategy) }
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

        // Compute net Portfolio Greeks
        state.activeLegs.forEach { leg ->
            val multiplier = if (leg.type == "BUY") 1.0 else -1.0
            val lotMultiplier = leg.qty / 50.0
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
                portfolioTheta += g.theta * multiplier * lotMultiplier * 50.0
                portfolioVega += g.vega * multiplier * lotMultiplier * 50.0
            }
        }

        for (i in -range..range step 20) {
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
                id = UUID.randomUUID().toString(),
                name = name,
                description = "Institutional Build",
                maxProfit = maxProfitValue,
                maxLoss = maxLossValue,
                breakeven = breakevens,
                probability = (prob * 10).toInt() / 10.0,
                roi = if (abs(maxLossValue) > 1.0) abs(maxProfitValue / maxLossValue) * 100 else 0.0,
                legs = state.activeLegs.map { l -> "${if (l.qty > 50) "${l.qty/50}x " else ""}${l.type} ${l.strike.toInt()} ${l.instrument}" },
                payoffPoints = expiryPoints,
                todayPayoffPoints = todayPoints
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
            kotlinx.coroutines.delay(800)
            val spot = if(symbol.contains("NIFTY")) 22500.0 else 5100.0
            val nearExpiry = getUpcomingMonthlyExpiry(0)
            val nextExpiry = getUpcomingMonthlyExpiry(1)
            
            val domainChain = OptionChain(spot, strikes = (-10..10).map { i ->
                val strike = spot + (i * 100)
                OptionChainData(strike = strike, callOI = 150000.0 - abs(i) * 5000, callLTP = 300.0 - (i * 40), callChange = 12.5, callIV = 14.5 + (i * 0.1), callGreeks = Greeks(0.5, 0.001, -12.0, 15.0), putOI = 120000.0 - abs(i) * 4000, putLTP = 300.0 + (i * 40), putChange = -8.2, putIV = 15.2 - (i * 0.1), putGreeks = Greeks(-0.45, 0.001, -11.5, 14.8))
            })

            val analyzerChainData = (-10..10).map { i ->
                val strike = spot + (i * 100)
                com.example.redxfnoscanner.data.Option(type = "CE", strikePrice = strike, openInterest = (150000 - abs(i) * 5000).toInt(), lastTradedPrice = 300.0 - (i * 40), changeInOpenInterest = (Random().nextInt(1000)), priceChange = 12.5, impliedVolatility = 14.5 + (i * 0.1)) to com.example.redxfnoscanner.data.Option(type = "PE", strikePrice = strike, openInterest = (120000 - abs(i) * 4000).toInt(), lastTradedPrice = 300.0 + (i * 40), changeInOpenInterest = (Random().nextInt(1000)), priceChange = -8.2, impliedVolatility = 15.2 - (i * 0.1))
            }
            
            val analyzerChain = com.example.redxfnoscanner.data.OptionChain(expiryDate = nearExpiry, options = analyzerChainData.flatMap { listOf(it.first, it.second) })
            val pcrValue = optionChainAnalyzer.calculatePCR(analyzerChain)
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
            
            _uiState.update { it.copy(
                isLoading = false,
                spotPrice = spot,
                optionChain = domainChain,
                summary = FnoSummary(changePercent = 0.65, iv = 14.8, pcr = pcrValue, maxPain = maxPainValue, lotSize = 50, trend = if(pcrValue > 1.0) "BULLISH" else "BEARISH", contracts = listOf(FutureContract(nearExpiry, spot + 40, 0.68, "+5K", "2M", spot + 38, 40.0), FutureContract(nextExpiry, spot + 120, 0.72, "+2K", "1M", spot + 115, 120.0)), highestCallOIStrike = resistance, highestPutOIStrike = support),
                futuresBuildup = listOf(BuildupData("RELIANCE", "Long Buildup", 1.2, 4.5), BuildupData("HDFCBANK", "Short Covering", 0.8, -2.1), BuildupData("TCS", "Short Buildup", -1.5, 6.2)),
                heatmapData = listOf(HeatmapItem("BANKING", 1.2, 0.35f), HeatmapItem("IT", -0.5, 0.25f), HeatmapItem("OIL & GAS", 0.8, 0.20f), HeatmapItem("PHARMA", 0.3, 0.10f)),
                maxPainHistory = List(10) { spot - (Math.random() * 200).toInt() },
                ivHistory = List(10) { 14.0 + (Math.random() * 4) },
                gexProfile = gexProfile,
                institutionalGreeks = instGreeksMap,
                participantData = participants
            ) }
        }
    }
}
