package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.repository.MarketDataRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FnoAiEngine @Inject constructor(
    private val marketDataRepository: MarketDataRepository
) {

    suspend fun analyze(underlyingSymbol: String): Result<String> {
        val optionChainResult = marketDataRepository.getOptionChain(underlyingSymbol).first()

        return optionChainResult.map { optionChain ->
            val totalCallsOi = optionChain.strikes.sumOf { it.callOI }
            val totalPutsOi = optionChain.strikes.sumOf { it.putOI }
            
            "Analysis complete. Live Spot Price: ${optionChain.underlyingPrice}. Total Calls OI: $totalCallsOi, Total Puts OI: $totalPutsOi."
        }
    }
}
