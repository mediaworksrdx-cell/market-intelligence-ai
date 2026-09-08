package com.example.marketintelligence.data.source.remote

import com.example.marketintelligence.data.model.CoinGeckoHistoricalDataResponse
import com.example.marketintelligence.data.model.CoinGeckoSearchResponse
import com.example.marketintelligence.domain.model.CryptoData
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApiService {
    @GET("coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=false")
    suspend fun getLiveCryptoPrices(): List<CryptoData>

    @GET("search")
    suspend fun search(@Query("query") query: String): CoinGeckoSearchResponse

    @GET("coins/{id}/market_chart")
    suspend fun getCoinGeckoHistoricalData(
        @Path("id") coinId: String,
        @Query("vs_currency") vsCurrency: String,
        @Query("days") days: String
    ): CoinGeckoHistoricalDataResponse
}
