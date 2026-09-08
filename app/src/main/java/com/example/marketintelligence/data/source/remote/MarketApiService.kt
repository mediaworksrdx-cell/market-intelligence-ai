package com.example.marketintelligence.data.source.remote

import com.example.marketintelligence.data.model.SearchResult
import com.example.marketintelligence.data.model.SubscribeRequest
import com.example.tradeengine.LivePrice
import com.example.tradeengine.models.Candle
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface MarketApiService {

    @GET("candles")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("timeframe") timeframe: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("type") type: String
    ): List<Candle>

    @GET("live-prices")
    suspend fun getLivePrices(): List<LivePrice>

    @GET("search")
    suspend fun search(@Query("query") query: String): List<SearchResult>

    @POST("subscribe")
    suspend fun subscribeToInstrument(@Body subscribeRequest: SubscribeRequest)

    @GET("historical-data")
    suspend fun getKiteHistoricalData(
        @Query("instrument_token") instrumentToken: Long,
        @Query("interval") interval: String,
        @Query("from") from: String,
        @Query("to") to: String
    ): List<Candle>

    @GET("coingecko-historical-data")
    suspend fun getCoinGeckoHistoricalData(
        @Query("coin_id") coinId: String,
        @Query("vs_currency") vsCurrency: String,
        @Query("days") days: Int
    ): List<List<Double>>
}
