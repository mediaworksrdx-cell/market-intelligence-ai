package com.marketintelligence.ai.di

import com.marketintelligence.redxchartlibrary.data.ChartDataCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @Provides
    @Singleton
    fun provideChartDataCache(): ChartDataCache {
        return ChartDataCache()
    }
}
