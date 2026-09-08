package com.marketintelligence.ai.di

import com.marketintelligence.ai.data.source.remote.InstrumentApiService
import com.marketintelligence.ai.data.source.remote.MarketApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
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

}
