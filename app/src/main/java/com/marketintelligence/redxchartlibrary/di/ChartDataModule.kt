package com.marketintelligence.redxchartlibrary.di

import android.content.Context
import com.marketintelligence.redxchartlibrary.data.TickAggregator
import com.marketintelligence.redxchartlibrary.data.local.CandleDao
import com.marketintelligence.redxchartlibrary.data.local.CandleRepository
import com.marketintelligence.redxchartlibrary.data.local.ChartDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChartDataModule {

    @Provides
    @Singleton
    fun provideTickAggregator(): TickAggregator {
        return TickAggregator()
    }

    @Provides
    @Singleton
    fun provideCandleDao(chartDatabase: ChartDatabase): CandleDao {
        return chartDatabase.candleDao()
    }

    @Provides
    @Singleton
    fun provideCandleRepository(candleDao: CandleDao): CandleRepository {
        return CandleRepository(candleDao)
    }

    @Provides
    @Singleton
    fun provideChartDatabase(@ApplicationContext context: Context): ChartDatabase {
        return ChartDatabase.getDatabase(context)
    }
}
