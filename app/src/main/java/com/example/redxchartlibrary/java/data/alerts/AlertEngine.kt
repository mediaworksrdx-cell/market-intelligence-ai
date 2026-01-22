package com.example.redxchartlibrary.data.alerts

import com.example.redxchartlibrary.data.indicators.IndicatorResult
import com.example.redxchartlibrary.model.Candle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class AlertEngine {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val _alertEvents = MutableSharedFlow<AlertEvent>()
    val alertEvents: SharedFlow<AlertEvent> = _alertEvents

    private val activeConditions = mutableListOf<AlertCondition>()
    private var lastCandle: Candle? = null

    fun addCondition(condition: AlertCondition) {
        if (activeConditions.none { it.id == condition.id }) {
            activeConditions.add(condition)
        }
    }

    fun removeCondition(conditionId: String) {
        activeConditions.removeAll { it.id == conditionId }
    }

    // This is the entry point for new data.
    fun processData(latestCandle: Candle, indicatorResults: Map<String, IndicatorResult>) {
        coroutineScope.launch {
            evaluateConditions(latestCandle, indicatorResults)
            lastCandle = latestCandle
        }
    }

    private suspend fun evaluateConditions(
        candle: Candle,
        indicators: Map<String, IndicatorResult>
    ) {
        val previousClose = lastCandle?.close ?: candle.close

        for (condition in activeConditions) {
            when (condition) {
                is AlertCondition.PriceCrosses -> {
                    if (!condition.hasTriggered) {
                        if ((previousClose < condition.price && candle.close >= condition.price) ||
                            (previousClose > condition.price && candle.close <= condition.price)) {
                            
                            condition.hasTriggered = true // Prevent re-triggering
                            _alertEvents.emit(
                                AlertEvent(
                                    System.currentTimeMillis(),
                                    condition,
                                    "Price crossed ${condition.price}"
                                )
                            )
                        }
                    }
                }
                is AlertCondition.IndicatorValue -> {
                    // TODO: Implement indicator alert logic
                }
                is AlertCondition.TrendlineCross -> {
                    // TODO: Implement trendline cross alert logic
                }
            }
        }
    }

    fun checkForAlerts(updatedCandles: List<Candle>) {
        // Dummy implementation
    }
}