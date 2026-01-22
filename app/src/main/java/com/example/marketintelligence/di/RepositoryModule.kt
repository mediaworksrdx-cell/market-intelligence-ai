package com.example.marketintelligence.di

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
        impl: com.example.marketintelligence.data.repository.MarketDataRepositoryImpl
    ): com.example.marketintelligence.domain.repository.MarketDataRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: com.example.marketintelligence.data.repository.SettingsRepositoryImpl
    ): com.example.marketintelligence.domain.repository.SettingsRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(
        impl: com.example.marketintelligence.data.repository.PortfolioRepositoryImpl
    ): com.example.marketintelligence.domain.repository.PortfolioRepository
}
