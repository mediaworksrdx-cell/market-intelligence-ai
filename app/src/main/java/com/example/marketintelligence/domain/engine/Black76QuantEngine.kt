package com.example.marketintelligence.domain.engine

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class InstitutionalGreeks(
    val price: Double,
    val delta: Double,
    val gamma: Double,
    val theta: Double,
    val vega: Double,
    val vanna: Double,
    val charm: Double,
    val volga: Double
)

@Singleton
class Black76QuantEngine @Inject constructor() {

    fun normalCdf(x: Double): Double {
        val b1 = 0.319381530
        val b2 = -0.356563782
        val b3 = 1.781477937
        val b4 = -1.821255978
        val b5 = 1.330274429
        val p = 0.2316419
        val c = 0.39894228
        if (x >= 0.0) {
            val t = 1.0 / (1.0 + p * x)
            return (1.0 - c * exp(-x * x / 2.0) * t *
                    (t * (t * (t * (t * b5 + b4) + b3) + b2) + b1))
        } else {
            val t = 1.0 / (1.0 - p * x)
            return (c * exp(-x * x / 2.0) * t *
                    (t * (t * (t * (t * b5 + b4) + b3) + b2) + b1))
        }
    }

    fun normalPdf(x: Double): Double = (1.0 / sqrt(2.0 * PI)) * exp(-0.5 * x * x)

    fun calculate(
        forward: Double,
        strike: Double,
        rate: Double,
        timeToExpiryYears: Double,
        volatility: Double,
        isCall: Boolean
    ): InstitutionalGreeks {
        val vol = volatility.coerceAtLeast(0.01)
        val t = timeToExpiryYears.coerceAtLeast(0.0001)
        val sqrtT = sqrt(t)
        val d1 = (ln(forward / strike) + 0.5 * vol * vol * t) / (vol * sqrtT)
        val d2 = d1 - vol * sqrtT
        val discount = exp(-rate * t)
        val pdfD1 = normalPdf(d1)

        val price = if (isCall) {
            discount * (forward * normalCdf(d1) - strike * normalCdf(d2))
        } else {
            discount * (strike * normalCdf(-d2) - forward * normalCdf(-d1))
        }

        val delta = if (isCall) discount * normalCdf(d1) else -discount * normalCdf(-d1)
        val gamma = (discount * pdfD1) / (forward * vol * sqrtT)
        val vega = forward * discount * pdfD1 * sqrtT / 100.0 // per 1% vol change
        val theta = -(forward * discount * pdfD1 * vol) / (2 * sqrtT) / 365.0 // per day

        // Second-order Institutional Greeks
        val vanna = -discount * pdfD1 * (d2 / vol)
        val charm = discount * pdfD1 * (rate / (vol * sqrtT) - d2 / (2 * t))
        val volga = vega * (d1 * d2 / vol)

        return InstitutionalGreeks(
            price = max(0.0, price),
            delta = delta,
            gamma = gamma,
            theta = theta,
            vega = vega,
            vanna = vanna,
            charm = charm,
            volga = volga
        )
    }

    fun calculateProbabilityOfProfit(
        spot: Double,
        breakevens: List<Double>,
        volatility: Double,
        timeToExpiryYears: Double,
        isDebitOrBreakout: Boolean
    ): Double {
        if (breakevens.isEmpty()) return if (isDebitOrBreakout) 40.0 else 65.0
        val vol = volatility.coerceAtLeast(0.05)
        val t = timeToExpiryYears.coerceAtLeast(0.001)

        return if (breakevens.size == 1) {
            val be = breakevens[0]
            val d2 = (ln(spot / be) - 0.5 * vol * vol * t) / (vol * sqrt(t))
            val prob = normalCdf(d2) * 100.0
            prob.coerceIn(10.0, 90.0)
        } else {
            val lowerBe = breakevens.minOrNull() ?: spot
            val upperBe = breakevens.maxOrNull() ?: spot
            val d2Lower = (ln(spot / lowerBe) - 0.5 * vol * vol * t) / (vol * sqrt(t))
            val d2Upper = (ln(spot / upperBe) - 0.5 * vol * vol * t) / (vol * sqrt(t))
            val inRangeProb = abs(normalCdf(d2Lower) - normalCdf(d2Upper)) * 100.0
            if (isDebitOrBreakout) {
                (100.0 - inRangeProb).coerceIn(15.0, 85.0)
            } else {
                inRangeProb.coerceIn(20.0, 85.0)
            }
        }
    }
}
