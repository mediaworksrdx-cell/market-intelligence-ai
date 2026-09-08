package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.model.OptionChain
import javax.inject.Inject
import javax.inject.Singleton

data class StrikeGex(
    val strike: Double,
    val callGex: Double,
    val putGex: Double,
    val netGex: Double
)

data class GexProfile(
    val totalNetGex: Double,
    val gammaFlipStrike: Double,
    val callWallStrike: Double,
    val putWallStrike: Double,
    val strikeGexList: List<StrikeGex>,
    val regime: String
)

data class StrikeInput(
    val strike: Double,
    val callOI: Double,
    val callIV: Double,
    val putOI: Double,
    val putIV: Double
)

@Singleton
class DealerGammaExposureEngine @Inject constructor(
    private val quantEngine: Black76QuantEngine
) {
    fun computeGex(
        optionChain: OptionChain,
        spotPrice: Double,
        timeToExpiryDays: Double = 5.0,
        riskFreeRate: Double = 0.065
    ): GexProfile {
        return computeGexFromStrikes(
            optionChain.strikes.map { StrikeInput(it.strike, it.callOI, it.callIV, it.putOI, it.putIV) },
            spotPrice,
            timeToExpiryDays,
            riskFreeRate
        )
    }

    fun computeGex(
        optionChain: com.marketintelligence.ai.domain.model.OptionChain,
        spotPrice: Double,
        timeToExpiryDays: Double = 5.0,
        riskFreeRate: Double = 0.065
    ): GexProfile {
        return computeGexFromStrikes(
            optionChain.strikes.map { StrikeInput(it.strike, it.callOI, it.callIV, it.putOI, it.putIV) },
            spotPrice,
            timeToExpiryDays,
            riskFreeRate
        )
    }

    private fun computeGexFromStrikes(
        strikes: List<StrikeInput>,
        spotPrice: Double,
        timeToExpiryDays: Double,
        riskFreeRate: Double
    ): GexProfile {
        if (strikes.isEmpty()) {
            return GexProfile(0.0, spotPrice, spotPrice, spotPrice, emptyList(), "NEUTRAL")
        }

        val timeYears = (timeToExpiryDays / 365.0).coerceAtLeast(0.001)
        val strikeGexList = mutableListOf<StrikeGex>()

        strikes.forEach { data ->
            val callGreeks = quantEngine.calculate(
                forward = spotPrice,
                strike = data.strike,
                rate = riskFreeRate,
                timeToExpiryYears = timeYears,
                volatility = data.callIV / 100.0,
                isCall = true
            )
            val putGreeks = quantEngine.calculate(
                forward = spotPrice,
                strike = data.strike,
                rate = riskFreeRate,
                timeToExpiryYears = timeYears,
                volatility = data.putIV / 100.0,
                isCall = false
            )

            // Market Maker assumption: Dealers are Net Short Calls -> Positive GEX (Dampening)
            // Dealers are Net Long Puts -> Negative GEX (Expansion)
            val callGex = callGreeks.gamma * data.callOI * (spotPrice * spotPrice) * 0.01 / 10_000_000.0
            val putGex = putGreeks.gamma * data.putOI * (spotPrice * spotPrice) * 0.01 / 10_000_000.0 * -1.0
            val netGex = callGex + putGex

            strikeGexList.add(StrikeGex(data.strike, callGex, putGex, netGex))
        }

        strikeGexList.sortBy { it.strike }
        val totalNetGex = strikeGexList.sumOf { it.netGex }

        val callWallStrike = strikeGexList.maxByOrNull { it.callGex }?.strike ?: spotPrice
        val putWallStrike = strikeGexList.minByOrNull { it.putGex }?.strike ?: spotPrice

        var flipStrike = spotPrice
        for (i in 0 until strikeGexList.size - 1) {
            val s1 = strikeGexList[i]
            val s2 = strikeGexList[i + 1]
            if (s1.netGex * s2.netGex <= 0.0) {
                flipStrike = (s1.strike + s2.strike) / 2.0
                break
            }
        }

        val regime = if (totalNetGex >= 0) {
            "LONG GAMMA (Pinning / Mean-Reverting)"
        } else {
            "SHORT GAMMA (Vol Acceleration / Trend)"
        }

        return GexProfile(
            totalNetGex = totalNetGex,
            gammaFlipStrike = flipStrike,
            callWallStrike = callWallStrike,
            putWallStrike = putWallStrike,
            strikeGexList = strikeGexList,
            regime = regime
        )
    }
}
