package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.model.MarketRegime
import com.example.marketintelligence.domain.repository.MarketDataRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRegimeEngine @Inject constructor(
    private val marketDataRepository: MarketDataRepository
) {
    suspend fun detect(symbol: String): MarketRegime {
        val optionChain = marketDataRepository.getOptionChain(symbol).first().getOrNull()
        return if ((optionChain?.underlyingPrice ?: 0.0) > 23000) {
            MarketRegime.TRENDING_BULLISH
        } else {
            MarketRegime.RANGING_HIGH_VOLATILITY
        }
    }
}
