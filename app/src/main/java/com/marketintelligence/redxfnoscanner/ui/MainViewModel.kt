package com.marketintelligence.redxfnoscanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marketintelligence.redxfnoscanner.data.FnoData
import com.marketintelligence.redxfnoscanner.data.FnoRepository
import com.marketintelligence.redxfnoscanner.domain.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel : ViewModel() {

    private val fnoRepository = FnoRepository()
    private val marketRegimeIdentifier = MarketRegimeIdentifier()
    private val optionChainAnalyzer = OptionChainAnalyzer()
    private val strategySelector = StrategySelector()
    private val riskManager = RiskManager()
    private val tradeSignalGenerator = TradeSignalGenerator()
    private val strategyBuilder = StrategyBuilder()
    private val payoffCalculator = PayoffCalculator()

    private var fnoData: FnoData? = null
    private var priceSimulationJob: Job? = null

    // Original state flows
    private val _marketRegime = MutableStateFlow<MarketRegime?>(null)
    val marketRegime: StateFlow<MarketRegime?> = _marketRegime

    private val _recommendedStrategy = MutableStateFlow<OptionStrategy?>(null)
    val recommendedStrategy: StateFlow<OptionStrategy?> = _recommendedStrategy

    private val _tradeSignal = MutableStateFlow<TradeSignal?>(null)
    val tradeSignal: StateFlow<TradeSignal?> = _tradeSignal

    // State flows for Practice Mode
    private val _practicePosition = MutableStateFlow<Position?>(null)
    val practicePosition: StateFlow<Position?> = _practicePosition

    private val _payoffChartData = MutableStateFlow<List<PayoffPoint>>(emptyList())
    val payoffChartData: StateFlow<List<PayoffPoint>> = _payoffChartData

    private val _liveSpotPrice = MutableStateFlow<Double?>(null)
    val liveSpotPrice: StateFlow<Double?> = _liveSpotPrice

    private val _livePnl = MutableStateFlow<Double?>(null)
    val livePnl: StateFlow<Double?> = _livePnl

    fun fetchFnoData(symbol: String) {
        viewModelScope.launch {
            try {
                // Store data for practice mode
                fnoData = fnoRepository.getFnoData(symbol)
                val regime = fnoData?.let { marketRegimeIdentifier.identifyRegime(it, optionChainAnalyzer) }
                _marketRegime.value = regime

                val strategy = regime?.let { fnoData?.let { it1 ->
                    strategySelector.selectStrategy(it,
                        it1, optionChainAnalyzer)
                } }
                _recommendedStrategy.value = strategy

                if (strategy != null) {
                    _tradeSignal.value = fnoData?.let {
                        tradeSignalGenerator.generateSignal(strategy, it, riskManager)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun setupPracticeStrategy(strategy: OptionStrategy) {
        priceSimulationJob?.cancel() // Stop any previous simulation
        val data = fnoData ?: return
        val position = strategyBuilder.build(strategy, data)
        _practicePosition.value = position

        if (position != null) {
            _payoffChartData.value = payoffCalculator.calculatePayoffRange(position)
            startPriceSimulation(data.spotPrice.price, position)
        }
    }

    private fun startPriceSimulation(initialPrice: Double, position: Position) {
        _liveSpotPrice.value = initialPrice
        priceSimulationJob = viewModelScope.launch {
            while (isActive) {
                delay(2000) // Update every 2 seconds
                val currentPrice = _liveSpotPrice.value ?: initialPrice
                val change = Random.nextDouble(-0.5, 0.5)
                val newPrice = currentPrice + change
                _liveSpotPrice.value = newPrice
                _livePnl.value = payoffCalculator.calculatePnlAtPrice(position, newPrice)
            }
        }
    }
}
