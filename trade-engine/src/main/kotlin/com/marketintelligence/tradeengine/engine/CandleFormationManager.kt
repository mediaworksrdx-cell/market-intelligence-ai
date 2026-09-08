package com.marketintelligence.tradeengine.engine

import com.marketintelligence.tradeengine.models.Candle
import com.marketintelligence.tradeengine.repository.CandleDataSource
import com.marketintelligence.tradeengine.util.TimeFrame
import com.marketintelligence.tradeengine.util.toTimeFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CandleFormationManager(
    private val symbol: String,
    private val timeframe: String,
    private val candleDataSource: CandleDataSource,
    private val coroutineScope: CoroutineScope
) {
    private val timeFrameMillis = timeframe.toTimeFrame().toMillis()

    fun start() {
        coroutineScope.launch {
            while (isActive) {
                val latestCandle = candleDataSource.getLatestCandle(symbol, timeframe)
                val now = System.currentTimeMillis()

                if (latestCandle == null) {
                    // No candles exist, let the CurrentCandleManager create the first one on the next tick.
                    delay(timeFrameMillis) // Wait for the next candle interval
                    continue
                }

                val nextCandleOpenTime = latestCandle.openTime + timeFrameMillis

                if (now >= nextCandleOpenTime) {
                    // Time to form a new candle
                    if (!latestCandle.isClosed) {
                        candleDataSource.closeCandle(latestCandle.openTime, symbol, timeframe)
                    }

                    // Create a new candle with the close of the last one.
                    val newCandle = Candle(
                        symbol = symbol,
                        timeframe = timeframe,
                        openTime = nextCandleOpenTime,
                        open = latestCandle.close,
                        high = latestCandle.close, // Start with the last close
                        low = latestCandle.close,
                        close = latestCandle.close,
                        volume = 0.0,
                        closeTime = nextCandleOpenTime + timeFrameMillis,
                        isClosed = false
                    )
                    candleDataSource.insertCandles(listOf(newCandle))
                }

                // Calculate delay until the next candle formation time
                val delayTime = nextCandleOpenTime - now
                if (delayTime > 0) {
                    delay(delayTime)
                } else {
                    delay(100L)
                }
            }
        }
    }
}
