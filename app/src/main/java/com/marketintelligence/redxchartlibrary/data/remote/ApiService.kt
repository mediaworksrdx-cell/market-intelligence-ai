package com.marketintelligence.redxchartlibrary.data.remote

import com.marketintelligence.redxchartlibrary.model.Candle
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("history")
    suspend fun getHistoricalCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long
    ): List<Candle>
}