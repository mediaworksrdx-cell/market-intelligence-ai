package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.model.Greeks
import com.example.marketintelligence.domain.model.PayoffPoint
import com.example.marketintelligence.domain.model.StrategyLeg
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class ScenarioRow(
    val spotChangePct: Double,
    val simulatedSpot: Double,
    val todayPnl: Double,
    val expiryPnl: Double,
    val delta: Double
)

data class SensitivityRow(
    val label: String,
    val ivShiftPct: Double,
    val daysElapsed: Int,
    val simulatedValue: Double,
    val pnl: Double
)

data class FuturesAnalysis(
    val futuresPrice: Double,
    val spotPrice: Double,
    val basis: Double,
    val basisPercent: Double,
    val annualizedCostOfCarry: Double,
    val fairValue: Double,
    val isAvailable: Boolean
)

data class StrategyAnalysisResult(
    val payoffPoints: List<PayoffPoint>,
    val todayPayoffPoints: List<PayoffPoint>,
    val breakevens: List<Double>,
    val maxProfit: Double,
    val isMaxProfitUnlimited: Boolean,
    val maxLoss: Double,
    val isMaxLossUnlimited: Boolean,
    val riskRewardRatio: String,
    val probabilityOfProfit: Double,
    val netCreditDebit: Double,
    val totalPremium: Double,
    val currentStrategyValue: Double,
    val livePnl: Double,
    val livePnlPercent: Double,
    val estimatedMargin: Double,
    val capitalRequired: Double,
    val totalGreeks: Greeks,
    val legGreeks: Map<String, Greeks>,
    val scenarioMatrix: List<ScenarioRow>,
    val volatilitySensitivity: List<SensitivityRow>,
    val timeDecaySensitivity: List<SensitivityRow>,
    val futuresAnalysis: FuturesAnalysis?
)

@Singleton
class OptionsStrategyEngine @Inject constructor(
    private val quantEngine: Black76QuantEngine
) {

    fun analyzeStrategy(
        legs: List<StrategyLeg>,
        spotPrice: Double,
        impliedVolatility: Double,
        daysToExpiry: Double,
        interestRate: Double = 0.065,
        lotSize: Int = 65,
        numberOfLots: Int = 1,
        spotShiftPct: Double = 0.0,
        ivShiftPct: Double = 0.0,
        targetDaysToExpiry: Double? = null,
        futuresPrice: Double? = null
    ): StrategyAnalysisResult {
        val enabledLegs = legs.filter { it.isEnabled }
        val effectiveLotSize = if (lotSize > 0) lotSize else 65

        if (enabledLegs.isEmpty()) {
            return emptyResult(spotPrice, futuresPrice, daysToExpiry, interestRate)
        }

        val baseSpot = spotPrice.coerceAtLeast(1.0)
        val simulatedSpot = baseSpot * (1.0 + spotShiftPct / 100.0)
        val simulatedIv = (impliedVolatility + ivShiftPct).coerceIn(1.0, 250.0) / 100.0
        val effectiveDte = (targetDaysToExpiry ?: daysToExpiry).coerceAtLeast(0.01)
        val timeYears = effectiveDte / 365.0

        // 1. Determine Grid Range for Payoff
        val strikes = enabledLegs.map { it.strike }
        val minStrike = min(baseSpot, strikes.minOrNull() ?: baseSpot)
        val maxStrike = max(baseSpot, strikes.maxOrNull() ?: baseSpot)
        val lowerBound = max(1.0, minStrike * 0.85)
        val upperBound = maxStrike * 1.15
        val gridStep = max(5.0, (upperBound - lowerBound) / 160.0)

        // 2. Compute Payoff Points (Expiry & T+0 Today)
        val expiryPoints = mutableListOf<PayoffPoint>()
        val todayPoints = mutableListOf<PayoffPoint>()
        var currentS = lowerBound
        while (currentS <= upperBound) {
            val (expPnl, todayPnl) = computePayoffsAtPrice(
                price = currentS,
                legs = enabledLegs,
                timeYears = timeYears,
                iv = simulatedIv,
                rate = interestRate
            )
            expiryPoints.add(PayoffPoint(round(currentS * 100.0) / 100.0, round(expPnl * 100.0) / 100.0))
            todayPoints.add(PayoffPoint(round(currentS * 100.0) / 100.0, round(todayPnl * 100.0) / 100.0))
            currentS += gridStep
        }

        // 3. Breakevens
        val breakevens = mutableListOf<Double>()
        for (i in 0 until expiryPoints.size - 1) {
            val p1 = expiryPoints[i]
            val p2 = expiryPoints[i + 1]
            if ((p1.pnl < 0.0 && p2.pnl >= 0.0) || (p1.pnl >= 0.0 && p2.pnl < 0.0)) {
                val dPnl = p2.pnl - p1.pnl
                val zeroCrossing = if (dPnl != 0.0) {
                    p1.price + (0.0 - p1.pnl) / dPnl * (p2.price - p1.price)
                } else {
                    p1.price
                }
                breakevens.add(round(zeroCrossing * 100.0) / 100.0)
            }
        }

        // 4. Asymptotic Slope & Unlimited Profit/Loss Detection
        val farHigh = max(baseSpot, maxStrike) * 10.0
        val farHighPayoff1 = computePayoffsAtPrice(farHigh, enabledLegs, timeYears, simulatedIv, interestRate).first
        val farHighPayoff2 = computePayoffsAtPrice(farHigh + 1000.0, enabledLegs, timeYears, simulatedIv, interestRate).first
        val upperSlope = farHighPayoff2 - farHighPayoff1

        val isMaxProfitUnlimited = upperSlope > 0.5
        val isMaxLossUnlimited = upperSlope < -0.5

        val gridMaxProfit = expiryPoints.maxOf { it.pnl }
        val gridMaxLoss = expiryPoints.minOf { it.pnl }

        val maxProfit = if (isMaxProfitUnlimited) Double.POSITIVE_INFINITY else gridMaxProfit
        val maxLoss = if (isMaxLossUnlimited) Double.NEGATIVE_INFINITY else gridMaxLoss

        // 5. Risk to Reward Ratio
        val rrRatio = when {
            isMaxProfitUnlimited && !isMaxLossUnlimited -> "Unlimited"
            !isMaxProfitUnlimited && isMaxLossUnlimited -> "Unlimited Loss"
            isMaxProfitUnlimited && isMaxLossUnlimited -> "Open"
            maxProfit > 0.0 && maxLoss < 0.0 -> {
                val ratio = maxProfit / abs(maxLoss)
                if (ratio >= 1.0) {
                    "1 : ${String.format(Locale.US, "%.2f", ratio)}"
                } else {
                    "${String.format(Locale.US, "%.2f", 1.0 / ratio)} : 1"
                }
            }
            else -> "N/A"
        }

        // 6. Net Premium & Cash Flow
        var premiumPaid = 0.0
        var premiumReceived = 0.0
        var currentStrategyVal = 0.0
        var liveTotalPnl = 0.0

        enabledLegs.forEach { leg ->
            val isBuy = leg.type.equals("BUY", ignoreCase = true)
            val legCost = leg.entryPrice * leg.qty
            if (isBuy) {
                premiumPaid += legCost
            } else {
                premiumReceived += legCost
            }
            val curLegVal = leg.currentPrice * leg.qty
            if (isBuy) {
                currentStrategyVal += curLegVal
            } else {
                currentStrategyVal -= curLegVal
            }
            liveTotalPnl += leg.pnl
        }
        val netCreditDebit = premiumReceived - premiumPaid // > 0 = Net Credit, < 0 = Net Debit
        val totalPremium = abs(netCreditDebit)
        val initialOutlay = if (netCreditDebit < 0.0) abs(netCreditDebit) else premiumReceived.coerceAtLeast(1.0)
        val livePnlPercent = if (initialOutlay > 0.0) (liveTotalPnl / initialOutlay) * 100.0 else 0.0

        // 7. Probability of Profit (POP) Calculation via Normal CDF
        val pop = calculateProbabilityOfProfit(
            spot = baseSpot,
            breakevens = breakevens,
            volatility = simulatedIv,
            timeYears = timeYears,
            isMaxProfitUnlimited = isMaxProfitUnlimited,
            isMaxLossUnlimited = isMaxLossUnlimited,
            expiryPoints = expiryPoints
        )

        // 8. Leg Greeks and Portfolio Greeks
        var totalDelta = 0.0
        var totalGamma = 0.0
        var totalTheta = 0.0
        var totalVega = 0.0
        var totalRho = 0.0
        val legGreeksMap = mutableMapOf<String, Greeks>()

        enabledLegs.forEach { leg ->
            val isBuy = leg.type.equals("BUY", ignoreCase = true)
            val mult = if (isBuy) 1.0 else -1.0
            val lots = leg.qty / effectiveLotSize.toDouble()

            if (leg.instrument.equals("FUT", ignoreCase = true)) {
                val legG = Greeks(delta = 1.0 * mult, gamma = 0.0, theta = 0.0, vega = 0.0, rho = 0.0)
                legGreeksMap[leg.id] = legG
                totalDelta += 1.0 * mult * lots
            } else {
                val isCall = leg.instrument.equals("CALL", ignoreCase = true)
                val g = quantEngine.calculate(
                    forward = simulatedSpot,
                    strike = leg.strike,
                    rate = interestRate,
                    timeToExpiryYears = timeYears,
                    volatility = simulatedIv,
                    isCall = isCall
                )
                val legG = Greeks(
                    delta = round(g.delta * mult * 1000.0) / 1000.0,
                    gamma = round(g.gamma * mult * 10000.0) / 10000.0,
                    theta = round(g.theta * mult * leg.qty * 10.0) / 10.0,
                    vega = round(g.vega * mult * leg.qty * 10.0) / 10.0,
                    rho = round(g.rho * mult * leg.qty * 10.0) / 10.0
                )
                legGreeksMap[leg.id] = legG

                totalDelta += g.delta * mult * lots
                totalGamma += g.gamma * mult * lots
                totalTheta += g.theta * mult * leg.qty
                totalVega += g.vega * mult * leg.qty
                totalRho += g.rho * mult * leg.qty
            }
        }

        val totalGreeks = Greeks(
            delta = round(totalDelta * 100.0) / 100.0,
            gamma = round(totalGamma * 10000.0) / 10000.0,
            theta = round(totalTheta * 10.0) / 10.0,
            vega = round(totalVega * 10.0) / 10.0,
            rho = round(totalRho * 10.0) / 10.0
        )

        // 9. Margin & Capital Required (SPAN + Exposure Margin Estimation)
        val (estimatedMargin, capitalRequired) = calculateEstimatedMargin(
            legs = enabledLegs,
            spot = baseSpot,
            netCreditDebit = netCreditDebit
        )

        // 10. Scenario Analysis Matrix (-5% to +5% spot changes)
        val scenarioShifts = listOf(-5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0)
        val scenarioMatrix = scenarioShifts.map { pct ->
            val scenSpot = baseSpot * (1.0 + pct / 100.0)
            val (expP, todP) = computePayoffsAtPrice(
                price = scenSpot,
                legs = enabledLegs,
                timeYears = timeYears,
                iv = simulatedIv,
                rate = interestRate
            )
            // Approximate delta at simulated spot
            var scenDelta = 0.0
            enabledLegs.forEach { leg ->
                val mult = if (leg.type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
                val lots = leg.qty / effectiveLotSize.toDouble()
                if (leg.instrument.equals("FUT", ignoreCase = true)) {
                    scenDelta += 1.0 * mult * lots
                } else {
                    val g = quantEngine.calculate(
                        forward = scenSpot,
                        strike = leg.strike,
                        rate = interestRate,
                        timeToExpiryYears = timeYears,
                        volatility = simulatedIv,
                        isCall = leg.instrument.equals("CALL", ignoreCase = true)
                    )
                    scenDelta += g.delta * mult * lots
                }
            }
            ScenarioRow(
                spotChangePct = pct,
                simulatedSpot = round(scenSpot * 100.0) / 100.0,
                todayPnl = round(todP * 10.0) / 10.0,
                expiryPnl = round(expP * 10.0) / 10.0,
                delta = round(scenDelta * 100.0) / 100.0
            )
        }

        // 11. Sensitivity Table: Volatility
        val ivShifts = listOf(-10.0, -5.0, 0.0, 5.0, 10.0)
        val volSensitivity = ivShifts.map { ivShift ->
            val testIv = (simulatedIv + ivShift / 100.0).coerceAtLeast(0.01)
            val (_, pnlToday) = computePayoffsAtPrice(
                price = simulatedSpot,
                legs = enabledLegs,
                timeYears = timeYears,
                iv = testIv,
                rate = interestRate
            )
            SensitivityRow(
                label = "${if (ivShift >= 0) "+" else ""}$ivShift% IV",
                ivShiftPct = ivShift,
                daysElapsed = 0,
                simulatedValue = round((currentStrategyVal + pnlToday - liveTotalPnl) * 10.0) / 10.0,
                pnl = round(pnlToday * 10.0) / 10.0
            )
        }

        // 12. Sensitivity Table: Time Decay
        val decayDays = listOf(0, 1, 2, 3, 5)
        val timeSensitivity = decayDays.map { days ->
            val testDte = (effectiveDte - days).coerceAtLeast(0.001)
            val testTimeYears = testDte / 365.0
            val (_, pnlToday) = computePayoffsAtPrice(
                price = simulatedSpot,
                legs = enabledLegs,
                timeYears = testTimeYears,
                iv = simulatedIv,
                rate = interestRate
            )
            SensitivityRow(
                label = "+$days Days Decay",
                ivShiftPct = 0.0,
                daysElapsed = days,
                simulatedValue = round((currentStrategyVal + pnlToday - liveTotalPnl) * 10.0) / 10.0,
                pnl = round(pnlToday * 10.0) / 10.0
            )
        }

        // 13. Futures Analysis
        val futuresAnalysis = if (futuresPrice != null && futuresPrice > 0.0) {
            val basis = futuresPrice - baseSpot
            val basisPercent = (basis / baseSpot) * 100.0
            val annualizedCarry = if (effectiveDte > 0.0) {
                basisPercent * (365.0 / effectiveDte)
            } else 0.0
            val fairValue = baseSpot * exp(interestRate * timeYears)
            FuturesAnalysis(
                futuresPrice = futuresPrice,
                spotPrice = baseSpot,
                basis = round(basis * 100.0) / 100.0,
                basisPercent = round(basisPercent * 100.0) / 100.0,
                annualizedCostOfCarry = round(annualizedCarry * 100.0) / 100.0,
                fairValue = round(fairValue * 100.0) / 100.0,
                isAvailable = true
            )
        } else null

        return StrategyAnalysisResult(
            payoffPoints = expiryPoints,
            todayPayoffPoints = todayPoints,
            breakevens = breakevens,
            maxProfit = maxProfit,
            isMaxProfitUnlimited = isMaxProfitUnlimited,
            maxLoss = maxLoss,
            isMaxLossUnlimited = isMaxLossUnlimited,
            riskRewardRatio = rrRatio,
            probabilityOfProfit = round(pop * 10.0) / 10.0,
            netCreditDebit = round(netCreditDebit * 100.0) / 100.0,
            totalPremium = round(totalPremium * 100.0) / 100.0,
            currentStrategyValue = round(currentStrategyVal * 100.0) / 100.0,
            livePnl = round(liveTotalPnl * 100.0) / 100.0,
            livePnlPercent = round(livePnlPercent * 100.0) / 100.0,
            estimatedMargin = round(estimatedMargin * 100.0) / 100.0,
            capitalRequired = round(capitalRequired * 100.0) / 100.0,
            totalGreeks = totalGreeks,
            legGreeks = legGreeksMap,
            scenarioMatrix = scenarioMatrix,
            volatilitySensitivity = volSensitivity,
            timeDecaySensitivity = timeSensitivity,
            futuresAnalysis = futuresAnalysis
        )
    }

    private fun computePayoffsAtPrice(
        price: Double,
        legs: List<StrategyLeg>,
        timeYears: Double,
        iv: Double,
        rate: Double
    ): Pair<Double, Double> {
        var totalExpiryPnl = 0.0
        var totalTodayPnl = 0.0

        legs.forEach { leg ->
            val isBuy = leg.type.equals("BUY", ignoreCase = true)
            val mult = if (isBuy) 1.0 else -1.0

            // Expiry Payoff
            val expiryUnitGain = when (leg.instrument.uppercase()) {
                "CALL" -> max(0.0, price - leg.strike) - leg.entryPrice
                "PUT" -> max(0.0, leg.strike - price) - leg.entryPrice
                "FUT" -> price - leg.entryPrice
                else -> max(0.0, price - leg.strike) - leg.entryPrice
            }
            totalExpiryPnl += expiryUnitGain * leg.qty * mult

            // Today T+0 Theoretical Payoff
            val todayUnitGain = if (leg.instrument.equals("FUT", ignoreCase = true)) {
                price - leg.entryPrice
            } else {
                val isCall = leg.instrument.equals("CALL", ignoreCase = true)
                val g = quantEngine.calculate(
                    forward = price,
                    strike = leg.strike,
                    rate = rate,
                    timeToExpiryYears = timeYears,
                    volatility = iv,
                    isCall = isCall
                )
                g.price - leg.entryPrice
            }
            totalTodayPnl += todayUnitGain * leg.qty * mult
        }

        return Pair(totalExpiryPnl, totalTodayPnl)
    }

    private fun calculateProbabilityOfProfit(
        spot: Double,
        breakevens: List<Double>,
        volatility: Double,
        timeYears: Double,
        isMaxProfitUnlimited: Boolean,
        isMaxLossUnlimited: Boolean,
        expiryPoints: List<PayoffPoint>
    ): Double {
        if (expiryPoints.isEmpty()) return 50.0

        val v = volatility.coerceAtLeast(0.02)
        val t = timeYears.coerceAtLeast(0.0001)
        val sqrtT = sqrt(t)

        fun probPriceGreaterThan(k: Double): Double {
            if (k <= 0.0) return 1.0
            val d2 = (ln(spot / k) - 0.5 * v * v * t) / (v * sqrtT)
            return quantEngine.normalCdf(d2)
        }

        if (breakevens.isEmpty()) {
            val samplePnl = expiryPoints.firstOrNull()?.pnl ?: 0.0
            return if (samplePnl >= 0.0) 95.0 else 5.0
        }

        val sortedBe = breakevens.sorted()
        return if (sortedBe.size == 1) {
            val be = sortedBe[0]
            val isBullish = isMaxProfitUnlimited || (expiryPoints.lastOrNull()?.pnl ?: 0.0) > 0.0
            val prob = if (isBullish) {
                probPriceGreaterThan(be) * 100.0
            } else {
                (1.0 - probPriceGreaterThan(be)) * 100.0
            }
            prob.coerceIn(5.0, 95.0)
        } else if (sortedBe.size == 2) {
            val lowerBe = sortedBe[0]
            val upperBe = sortedBe[1]
            val probBetween = abs(probPriceGreaterThan(lowerBe) - probPriceGreaterThan(upperBe)) * 100.0
            val midPrice = (lowerBe + upperBe) / 2.0
            val midPnl = expiryPoints.minByOrNull { abs(it.price - midPrice) }?.pnl ?: 0.0
            val isCreditRange = midPnl >= 0.0
            val prob = if (isCreditRange) probBetween else (100.0 - probBetween)
            prob.coerceIn(5.0, 95.0)
        } else {
            // Multi-wing strategy (e.g. Iron Condor, Butterflies)
            val sortedPoints = expiryPoints.sortedBy { it.price }
            var totalProb = 0.0
            for (i in 0 until sortedPoints.size - 1) {
                val p1 = sortedPoints[i]
                val p2 = sortedPoints[i + 1]
                if (p1.pnl >= 0.0 || p2.pnl >= 0.0) {
                    val pGreaterP1 = probPriceGreaterThan(p1.price)
                    val pGreaterP2 = probPriceGreaterThan(p2.price)
                    totalProb += abs(pGreaterP1 - pGreaterP2)
                }
            }
            (totalProb * 100.0).coerceIn(5.0, 95.0)
        }
    }

    private fun calculateEstimatedMargin(
        legs: List<StrategyLeg>,
        spot: Double,
        netCreditDebit: Double
    ): Pair<Double, Double> {
        val hasShortOptions = legs.any { it.type.equals("SELL", ignoreCase = true) }
        val hasFutures = legs.any { it.instrument.equals("FUT", ignoreCase = true) }

        if (!hasShortOptions && !hasFutures) {
            // Debit strategy only: Required capital = net premium paid
            val required = abs(min(0.0, netCreditDebit))
            return Pair(0.0, required)
        }

        // Short option or futures margin estimation:
        // Indian NSE SPAN + Exposure Margin rule of thumb: ~13-15% of underlying contract value per short lot
        var totalShortMargin = 0.0

        val shortCalls = legs.filter { it.type.equals("SELL", ignoreCase = true) && it.instrument.equals("CALL", ignoreCase = true) }
        val longCalls = legs.filter { it.type.equals("BUY", ignoreCase = true) && it.instrument.equals("CALL", ignoreCase = true) }
        val shortPuts = legs.filter { it.type.equals("SELL", ignoreCase = true) && it.instrument.equals("PUT", ignoreCase = true) }
        val longPuts = legs.filter { it.type.equals("BUY", ignoreCase = true) && it.instrument.equals("PUT", ignoreCase = true) }

        // Analyze Calls
        shortCalls.forEach { sc ->
            val matchingLc = longCalls.firstOrNull { it.strike > sc.strike && it.qty >= sc.qty }
            if (matchingLc != null) {
                // Hedged spread (Credit Spread): Max margin is the difference between strikes
                val spreadMargin = (matchingLc.strike - sc.strike) * sc.qty
                totalShortMargin += spreadMargin
            } else {
                // Naked short call
                val nakedMargin = (0.14 * spot + sc.entryPrice) * sc.qty
                totalShortMargin += nakedMargin
            }
        }

        // Analyze Puts
        shortPuts.forEach { sp ->
            val matchingLp = longPuts.firstOrNull { it.strike < sp.strike && it.qty >= sp.qty }
            if (matchingLp != null) {
                // Hedged put spread
                val spreadMargin = (sp.strike - matchingLp.strike) * sp.qty
                totalShortMargin += spreadMargin
            } else {
                // Naked short put
                val nakedMargin = (0.14 * spot + sp.entryPrice) * sp.qty
                totalShortMargin += nakedMargin
            }
        }

        // Futures legs
        legs.filter { it.instrument.equals("FUT", ignoreCase = true) }.forEach { fut ->
            totalShortMargin += (0.15 * spot) * fut.qty
        }

        val estimatedMargin = max(0.0, totalShortMargin)
        // Capital required is estimated margin minus net credit received (or plus debit paid)
        val capitalRequired = max(estimatedMargin, estimatedMargin - netCreditDebit)

        return Pair(estimatedMargin, capitalRequired)
    }

    private fun emptyResult(
        spot: Double,
        futuresPrice: Double?,
        daysToExpiry: Double,
        rate: Double
    ): StrategyAnalysisResult {
        val futAnalysis = if (futuresPrice != null && futuresPrice > 0.0) {
            val basis = futuresPrice - spot
            FuturesAnalysis(
                futuresPrice = futuresPrice,
                spotPrice = spot,
                basis = basis,
                basisPercent = (basis / spot) * 100.0,
                annualizedCostOfCarry = (basis / spot) * (365.0 / daysToExpiry.coerceAtLeast(1.0)) * 100.0,
                fairValue = spot * exp(rate * (daysToExpiry / 365.0)),
                isAvailable = true
            )
        } else null

        return StrategyAnalysisResult(
            payoffPoints = emptyList(),
            todayPayoffPoints = emptyList(),
            breakevens = emptyList(),
            maxProfit = 0.0,
            isMaxProfitUnlimited = false,
            maxLoss = 0.0,
            isMaxLossUnlimited = false,
            riskRewardRatio = "N/A",
            probabilityOfProfit = 0.0,
            netCreditDebit = 0.0,
            totalPremium = 0.0,
            currentStrategyValue = 0.0,
            livePnl = 0.0,
            livePnlPercent = 0.0,
            estimatedMargin = 0.0,
            capitalRequired = 0.0,
            totalGreeks = Greeks(0.0, 0.0, 0.0, 0.0, 0.0),
            legGreeks = emptyMap(),
            scenarioMatrix = emptyList(),
            volatilitySensitivity = emptyList(),
            timeDecaySensitivity = emptyList(),
            futuresAnalysis = futAnalysis
        )
    }
}
