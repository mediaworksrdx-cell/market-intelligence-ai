package com.example.marketintelligence.data.repository

import com.example.marketintelligence.domain.model.StockData
import com.example.marketintelligence.domain.model.OptionChain
import com.example.marketintelligence.domain.repository.MarketDataRepository
import com.example.marketintelligence.data.source.remote.TwelveDataApiService
import com.example.marketintelligence.data.source.remote.RealTimeDataService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MarketDataRepositoryImpl @Inject constructor(
    private val apiService: TwelveDataApiService,
    private val realTimeService: RealTimeDataService
) : MarketDataRepository {
    override fun getQuote(symbol: String): Flow<Result<StockData>> = flow {
        try {
            val response = apiService.getQuote(symbol)
            emit(Result.success(StockData(
                symbol = response.symbol,
                name = response.symbol,
                price = response.price,
                openPrice = response.open,
                change = response.change,
                changePercent = response.percentChange,
                volume = response.volume,
                market = com.example.marketintelligence.domain.model.MarketType.IN
            )))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getOptionChain(symbol: String): Flow<Result<OptionChain>> = flow {
        emit(Result.failure(NotImplementedError("Option chain not available")))
    }
}
