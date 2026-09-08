package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.model.MarketRegime
import com.marketintelligence.ai.domain.repository.MarketDataRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRegimeEngine @Inject constructor(
    private val marketDataRepository: MarketDataRepository
) {
    suspend fun detect(symbol: String): MarketRegime {
        val optionChain = marketDataRepository.getOptionChain(symbol).first().getOrNull() ?: return MarketRegime.RANGING_LOW_VOLATILITY
        val totalCallOi = optionChain.strikes.sumOf { it.callOI.toDouble() }
        val totalPutOi = optionChain.strikes.sumOf { it.putOI.toDouble() }
        val pcr = if (totalCallOi > 0.0) totalPutOi / totalCallOi else 1.0

        return when {
            pcr >= 1.25 -> MarketRegime.TRENDING_BULLISH
            pcr <= 0.75 -> MarketRegime.TRENDING_BEARISH
            pcr in 0.9..1.1 -> MarketRegime.RANGING_LOW_VOLATILITY
            else -> MarketRegime.RANGING_HIGH_VOLATILITY
        }
    }
}
