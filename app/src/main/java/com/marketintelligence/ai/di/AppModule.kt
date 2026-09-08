package com.marketintelligence.ai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.marketintelligence.ai.data.source.CandleDataSourceImpl
import com.marketintelligence.ai.data.source.CandleRepository
import com.marketintelligence.ai.data.source.HistoricalDataOrchestrator
import com.marketintelligence.ai.data.source.local.AppDatabase
import com.marketintelligence.ai.data.source.local.CandleDao
import com.marketintelligence.ai.data.source.remote.MarketApiService
import com.marketintelligence.tradeengine.engine.CurrentCandleManager
import com.marketintelligence.tradeengine.live.WebSocketClient
import com.marketintelligence.tradeengine.repository.CandleDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val USER_PREFERENCES_NAME = "user_preferences"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {


    @Provides
    @Singleton
    fun provideCandleRepository(candleDao: CandleDao): CandleRepository {
        return CandleRepository(candleDao)
    }

    @Provides
    @Singleton
    fun provideCandleDataSource(candleRepository: CandleRepository): CandleDataSource {
        return CandleDataSourceImpl(candleRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(client: io.ktor.client.HttpClient, coroutineScope: CoroutineScope): WebSocketClient {
        return WebSocketClient(client, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideHistoricalDataOrchestrator(
        marketApiService: MarketApiService,
        candleRepository: CandleRepository
    ): HistoricalDataOrchestrator {
        return HistoricalDataOrchestrator(marketApiService, candleRepository)
    }

    @Provides
    @Singleton
    fun provideCurrentCandleManager(
        candleDataSource: CandleDataSource,
        coroutineScope: CoroutineScope
    ): CurrentCandleManager {
        return CurrentCandleManager("", "1m", candleDataSource, coroutineScope)
    }

}
