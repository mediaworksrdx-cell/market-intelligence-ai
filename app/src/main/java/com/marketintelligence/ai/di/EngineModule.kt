package com.marketintelligence.ai.di

import com.marketintelligence.ai.data.engine.*
import com.marketintelligence.ai.domain.engine.ChartEngine
import com.marketintelligence.ai.domain.engine.Engine
import com.marketintelligence.ai.domain.engine.MentorEngine
import com.marketintelligence.ai.domain.engine.ScannerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds @IntoSet
    abstract fun bindTradingViewChartEngine(engine: TradingViewChartEngine): Engine

    @Binds @IntoSet
    abstract fun bindProprietaryChartEngine(engine: ProprietaryChartEngine): Engine

    @Binds @IntoSet
    abstract fun bindGeminiScannerEngine(engine: GeminiScannerEngineImpl): Engine

    @Binds @IntoSet
    abstract fun bindProprietaryScannerEngine(engine: ProprietaryScannerEngineImpl): Engine

    @Binds @IntoSet
    abstract fun bindGeminiMentorEngine(engine: GeminiMentorEngineImpl): Engine

    @Binds @IntoSet
    abstract fun bindProprietaryMentorEngine(engine: ProprietaryMentorEngineImpl): Engine

    @Binds @IntoSet
    abstract fun bindAarkaAiMentorEngine(engine: AarkaAiMentorEngineImpl): Engine
}
