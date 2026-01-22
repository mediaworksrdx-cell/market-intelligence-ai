package com.example.marketintelligence.data.repository

import com.example.marketintelligence.domain.model.StockData
import com.example.marketintelligence.domain.model.OptionChain
import kotlinx.coroutines.flow.Flow

interface MarketDataRepository {
    fun getQuote(symbol: String): Flow<Result<StockData>>
    fun getOptionChain(symbol: String): Flow<Result<OptionChain>>
}
