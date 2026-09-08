package com.marketintelligence.ai.data.source.remote

import com.marketintelligence.ai.data.model.SearchResult
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
}
