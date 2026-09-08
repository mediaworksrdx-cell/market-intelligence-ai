package com.example.marketintelligence.di

import com.example.redxchartlibrary.data.ChartDataCache
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
