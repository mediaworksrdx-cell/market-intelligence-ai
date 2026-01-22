package com.example.redxaiscanner.domain.usecase

import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import com.example.redxaiscanner.engine.BacktestResult
import com.example.redxaiscanner.engine.BacktestingEngine
import com.example.redxaiscanner.engine.TradeSetup
import kotlinx.coroutines.flow.first

class RunBacktestUseCase(
    private val marketDataRepository: MarketDataRepository,
    private val generateTradeSetupUseCase: GenerateTradeSetupUseCase,
    private val backtestingEngine: BacktestingEngine
) {
    /**
     * Runs a full backtest over a historical period.
     * NOTE: This is a resource-intensive operation.
     *
     * @param symbols The symbols to test.
     * @param startDate The start date for the historical data.
     * @param endDate The end date for the historical data.
     * @return A map of symbols to their list of backtest results.
     */
    suspend fun run(symbols: List<String>, startDate: Long, endDate: Long): Map<String, List<BacktestResult>> {
        // Assume MarketDataRepository has a method to get a historical range.
        // This is a key requirement for backtesting.
        val historicalData = marketDataRepository.getCandles(symbols, Timeframe.ONE_HOUR)/* .getHistoricalCandles(symbols, Timeframe.ONE_HOUR, startDate, endDate) */.first()
        val allResults = mutableMapOf<String, MutableList<BacktestResult>>()

        for (symbol in symbols) {
            val candles = historicalData[symbol] ?: continue
            // We need to simulate the sliding window of a real-time scan
            for (i in 200 until candles.size) { // 200 is the lookback period
                val currentWindow = candles.subList(0, i)
                val futureWindow = candles.subList(i, candles.size)
                
                // This is a simplified call. A real implementation would manage the flow differently.
                val tradeSetups = generateTradeSetupUseCase.getTradeSetups(listOf(symbol))/* .runOnHistoricalData(currentWindow) */.first()

                tradeSetups[symbol]?.forEach { setup ->
                    val result = backtestingEngine.run(setup, futureWindow)
                    if (result.outcome != com.example.redxaiscanner.engine.TradeOutcome.NOT_TRIGGERED) {
                        allResults.getOrPut(symbol) { mutableListOf() }.add(result)
                    }
                }
            }
        }
        return allResults
    }
}