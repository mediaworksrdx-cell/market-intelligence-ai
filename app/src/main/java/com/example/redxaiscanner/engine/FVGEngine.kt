package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.math.BigDecimal

class FVGEngine {

    companion object {
        const val VERSION = "1.0.0"
    }

    fun analyze(candles: List<Candle>): List<FVGResult> {
        if (candles.size < 3) {
            return emptyList()
        }

        val results = mutableListOf<FVGResult>()

        for (i in 0..candles.size - 3) {
            val first = candles[i]
            val second = candles[i + 1]
            val third = candles[i + 2]

            if (first.low > third.high) {
                val bodySize = (second.open - second.close).abs()
                if (bodySize > BigDecimal.ZERO) {
                    val fvg = createFVGResult(
                        direction = FVGDireciton.BEARISH,
                        top = first.low,
                        bottom = third.high,
                        strengthCandle = second,
                        patternWindow = listOf(first, second, third),
                        futureCandles = candles.subList(i + 3, candles.size)
                    )
                    results.add(fvg)
                }
            }

            if (first.high < third.low) {
                val bodySize = (second.close - second.open).abs()
                 if (bodySize > BigDecimal.ZERO) {
                    val fvg = createFVGResult(
                        direction = FVGDireciton.BULLISH,
                        top = third.low,
                        bottom = first.high,
                        strengthCandle = second,
                        patternWindow = listOf(first, second, third),
                        futureCandles = candles.subList(i + 3, candles.size)
                    )
                    results.add(fvg)
                }
            }
        }
        return results
    }

    private fun createFVGResult(
        direction: FVGDireciton,
        top: BigDecimal,
        bottom: BigDecimal,
        strengthCandle: Candle,
        patternWindow: List<Candle>,
        futureCandles: List<Candle>
    ): FVGResult {
        
        val bodySize = (strengthCandle.close - strengthCandle.open).abs()
        val totalRange = (strengthCandle.high - strengthCandle.low).abs()
        val strength = if (totalRange > BigDecimal.ZERO) {
            (bodySize / totalRange).toDouble().coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        
        var isMitigated = false
        var mitigationTimestamp = patternWindow.last().timestamp
        for (futureCandle in futureCandles) {
            val touchesTop = futureCandle.high >= bottom && futureCandle.low <= top
            if (touchesTop) {
                isMitigated = true
                mitigationTimestamp = futureCandle.timestamp
                break
            }
        }

        val explanation = ExplanationComponent(
            "Fair Value Gap",
            "Detected $direction FVG",
            mapOf("Strength" to strength.toString())
        )

        return FVGResult(
            direction = direction,
            top = top,
            bottom = bottom,
            strength = strength,
            isMitigated = isMitigated,
            startTimestamp = patternWindow.first().timestamp,
            endTimestamp = mitigationTimestamp,
            explanation = explanation
        )
    }
}
