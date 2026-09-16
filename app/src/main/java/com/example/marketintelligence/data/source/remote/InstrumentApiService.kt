package com.example.marketintelligence.data.source.remote

import com.example.marketintelligence.data.model.SearchResult
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming

interface InstrumentApiService {

    @GET("search")
    suspend fun search(@Query("query") query: String): List<SearchResult>

    @GET("instruments")
    @Streaming
    suspend fun downloadInstruments(): Response<ResponseBody>

    @GET("https://api.kite.trade/instruments")
    @Streaming
    suspend fun downloadInstrumentsFromKite(): Response<ResponseBody>
}
