package com.example.marketintelligence.di

import com.example.marketintelligence.data.engine.*
import com.example.marketintelligence.domain.engine.Engine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    // --- Chart Engines ---
    @Binds @IntoSet
    abstract fun bindTradingViewChartEngine(engine: TradingViewChartEngine): Engine

    @Binds @IntoSet
    abstract fun bindProprietaryChartEngine(engine: ProprietaryChartEngine): Engine

    // --- Scanner Engines ---
    @Binds @IntoSet
    abstract fun bindGeminiScannerEngine(engine: GeminiScannerEngineImpl): Engine

    @Binds @IntoSet
    abstract fun bindProprietaryScannerEngine(engine: ProprietaryScannerEngineImpl): Engine

    // --- Mentor Engines ---
    @Binds @IntoSet
    abstract fun bindGeminiMentorEngine(engine: GeminiMentorEngineImpl): Engine

    @Binds @IntoSet
    abstract fun bindProprietaryMentorEngine(engine: ProprietaryMentorEngineImpl): Engine
}
