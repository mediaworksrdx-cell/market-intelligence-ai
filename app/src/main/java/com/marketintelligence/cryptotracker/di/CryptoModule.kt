package com.marketintelligence.cryptotracker.di

import com.marketintelligence.cryptotracker.engine.*
import com.marketintelligence.cryptotracker.scanner.CryptoMarketScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CryptoModule {

    @Provides
    @Singleton
    fun provideSMCEngine(): CryptoSMCEngine = CryptoSMCEngine()

    @Provides
    @Singleton
    fun provideFVGEngine(): CryptoFVGEngine = CryptoFVGEngine()

    @Provides
    @Singleton
    fun provideRSIEngine(): CryptoRSIEngine = CryptoRSIEngine()

    @Provides
    @Singleton
    fun provideRiskEngine(): CryptoRiskEngine = CryptoRiskEngine()

    @Provides
    @Singleton
    fun provideConfluenceEngine(
        smcEngine: CryptoSMCEngine,
        fvgEngine: CryptoFVGEngine,
        rsiEngine: CryptoRSIEngine,
        riskEngine: CryptoRiskEngine
    ): CryptoConfluenceEngine = CryptoConfluenceEngine(
        smcEngine = smcEngine,
        fvgEngine = fvgEngine,
        rsiEngine = rsiEngine,
        riskEngine = riskEngine
    )

    @Provides
    @Singleton
    fun provideMarketScanner(
        confluenceEngine: CryptoConfluenceEngine
    ): CryptoMarketScanner = CryptoMarketScanner(confluenceEngine)
}
