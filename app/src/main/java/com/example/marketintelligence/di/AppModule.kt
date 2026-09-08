package com.example.marketintelligence.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.marketintelligence.data.source.CandleDataSourceImpl
import com.example.marketintelligence.data.source.CandleRepository
import com.example.marketintelligence.data.source.HistoricalDataOrchestrator
import com.example.marketintelligence.data.source.local.AppDatabase
import com.example.marketintelligence.data.source.local.CandleDao
import com.example.marketintelligence.data.source.remote.MarketApiService
import com.example.tradeengine.engine.CurrentCandleManager
import com.example.tradeengine.live.WebSocketClient
import com.example.tradeengine.repository.CandleDataSource
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
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    fun provideCandleDao(appDatabase: AppDatabase): CandleDao {
        return appDatabase.candleDao()
    }

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
    fun provideCurrentCandleManager(
        candleDataSource: CandleDataSource,
        coroutineScope: CoroutineScope
    ): CurrentCandleManager {
        // Note: This provides a new instance for each injection. You may want to scope this
        // depending on your specific needs (e.g., to a ViewModel's lifecycle).
        return CurrentCandleManager("", "1m", candleDataSource, coroutineScope)
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
