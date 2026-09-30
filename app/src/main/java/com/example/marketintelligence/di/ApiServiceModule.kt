package com.example.marketintelligence.di

import com.example.marketintelligence.data.auth.AuthTokenManager
import com.example.marketintelligence.data.source.remote.InstrumentApiService
import com.example.marketintelligence.data.source.remote.MarketApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideMarketApiService(retrofit: Retrofit): MarketApiService {
        return retrofit.create(MarketApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideInstrumentApiService(retrofit: Retrofit): InstrumentApiService {
        return retrofit.create(InstrumentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAarkaApiService(retrofit: Retrofit): com.example.marketintelligence.data.source.remote.AarkaApiService {
        return retrofit.create(com.example.marketintelligence.data.source.remote.AarkaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, authTokenManager: AuthTokenManager): Retrofit {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val token = runCatching { runBlocking { authTokenManager.token.firstOrNull() } }.getOrNull()
                val request = if (!token.isNullOrBlank()) {
                    original.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    original
                }
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(com.marketintelligence.ai.BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
