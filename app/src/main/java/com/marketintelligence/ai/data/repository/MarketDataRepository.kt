package com.marketintelligence.ai.data.repository

import com.marketintelligence.ai.domain.model.StockData
import com.marketintelligence.ai.domain.model.OptionChain
import kotlinx.coroutines.flow.Flow

interface MarketDataRepository {
    fun getQuote(symbol: String): Flow<Result<StockData>>
    fun getOptionChain(symbol: String): Flow<Result<OptionChain>>
}
