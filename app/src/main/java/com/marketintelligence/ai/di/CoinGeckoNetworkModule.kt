package com.marketintelligence.ai.di

import com.marketintelligence.ai.data.source.remote.CoinGeckoApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoinGeckoNetworkModule {


    @Provides
    @Singleton
    fun provideCoinGeckoApiService(@Named("CoinGeckoRetrofit") retrofit: Retrofit): CoinGeckoApiService {
        return retrofit.create(CoinGeckoApiService::class.java)
    }
}
