package com.example.redxfnoscanner.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface FnoApiService {
    @GET("v1/fno-data")
    suspend fun getFnoData(@Query("symbol") symbol: String): FnoData
}

object RetrofitClient {
    private const val BASE_URL = "https://api.example.com/"
    private val json = Json { ignoreUnknownKeys = true }

    val instance: FnoApiService by lazy {
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        retrofit.create(FnoApiService::class.java)
    }
}
