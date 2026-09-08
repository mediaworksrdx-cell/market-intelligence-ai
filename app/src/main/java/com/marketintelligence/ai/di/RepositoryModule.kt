package com.marketintelligence.ai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMarketDataRepository(
        impl: com.marketintelligence.ai.data.repository.MarketDataRepositoryImpl
    ): com.marketintelligence.ai.domain.repository.MarketDataRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: com.marketintelligence.ai.data.repository.SettingsRepositoryImpl
    ): com.marketintelligence.ai.domain.repository.SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(
        impl: com.marketintelligence.ai.data.repository.PortfolioRepositoryImpl
    ): com.marketintelligence.ai.domain.repository.PortfolioRepository

    @Binds
    @Singleton
    abstract fun bindMarketRepository(
        impl: com.marketintelligence.ai.data.repository.MarketRepositoryImpl
    ): com.marketintelligence.ai.domain.repository.MarketRepository

    @Binds
    @Singleton
    abstract fun bindInstrumentRepository(
        impl: com.marketintelligence.ai.data.repository.InstrumentRepositoryImpl
    ): com.marketintelligence.ai.domain.repository.InstrumentRepository
}
