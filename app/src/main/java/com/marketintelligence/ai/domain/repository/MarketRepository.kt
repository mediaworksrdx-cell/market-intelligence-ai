package com.marketintelligence.ai.domain.repository

import com.marketintelligence.ai.data.model.FnoData
import com.marketintelligence.ai.domain.model.* 
import com.marketintelligence.tradeengine.LivePrice
import com.marketintelligence.tradeengine.models.Candle
import kotlinx.coroutines.flow.Flow

interface MarketRepository {
    fun getIndices(): Flow<List<IndexData>>
    fun getWatchlist(): Flow<List<StockData>>
    fun getCryptos(): Flow<List<CryptoData>>
    suspend fun getFnoData(symbol: String): FnoData
    suspend fun getLiveAnalysis(): AIAnalysisResult
    suspend fun getLivePrices(): List<LivePrice>
    suspend fun getCryptoLivePrices(): List<CryptoData>
    suspend fun search(query: String): List<SearchResult>
    suspend fun getMacroData(): List<MacroData>
    suspend fun addToWatchlist(instrument: SearchResult)
    suspend fun removeFromWatchlist(symbol: String)
    suspend fun getHistoricalCandles(symbol: String, timeframe: String): List<Candle>
    suspend fun subscribeToInstrument(symbol: String, timeframe: String)

    suspend fun getKiteHistoricalData(instrumentToken: Long, interval: String, from: String, to: String): List<Candle>

    suspend fun getCoinGeckoHistoricalData(coinId: String, vsCurrency: String, days: Int): List<List<Double>>
}
