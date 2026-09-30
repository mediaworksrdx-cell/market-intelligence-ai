package com.marketintelligence.ai.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : SettingsRepository {

    private val selectedScannerEngineKey = stringPreferencesKey("selected_scanner_engine")
    private val selectedMentorEngineKey = stringPreferencesKey("selected_mentor_engine")
    private val selectedChartEngineKey = stringPreferencesKey("selected_chart_engine")
    private val selectedMarketKey = stringPreferencesKey("selected_market")
    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")
    private val riskProfileKey = stringPreferencesKey("risk_profile")
    private val isNotificationsEnabledKey = booleanPreferencesKey("is_notifications_enabled")

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val selectedScannerEngine: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[selectedScannerEngineKey] ?: "Default"
        }

    override val selectedMentorEngine: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[selectedMentorEngineKey] ?: "Aarka AI"
        }

    override val selectedChartEngine: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[selectedChartEngineKey] ?: "Proprietary Engine"
        }

    override val selectedMarket: Flow<MarketType> = preferencesFlow
        .map { preferences ->
            val raw = preferences[selectedMarketKey] ?: MarketType.IN.name
            runCatching { MarketType.valueOf(raw) }.getOrDefault(MarketType.IN)
        }

    override val isDarkMode: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[isDarkModeKey] ?: true
        }

    override val riskProfile: Flow<String> = preferencesFlow
        .map { preferences ->
            preferences[riskProfileKey] ?: "CONSERVATIVE"
        }

    override val isNotificationsEnabled: Flow<Boolean> = preferencesFlow
        .map { preferences ->
            preferences[isNotificationsEnabledKey] ?: true
        }

    override suspend fun setScannerEngine(engineName: String) {
        context.dataStore.edit { it[selectedScannerEngineKey] = engineName }
    }

    override suspend fun setMentorEngine(engineName: String) {
        context.dataStore.edit { it[selectedMentorEngineKey] = engineName }
    }

    override suspend fun setChartEngine(engineName: String) {
        context.dataStore.edit { it[selectedChartEngineKey] = engineName }
    }

    override suspend fun setMarket(market: MarketType) {
        context.dataStore.edit { it[selectedMarketKey] = market.name }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[isDarkModeKey] = enabled }
    }

    override suspend fun setRiskProfile(profile: String) {
        context.dataStore.edit { it[riskProfileKey] = profile }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[isNotificationsEnabledKey] = enabled }
    }
}
