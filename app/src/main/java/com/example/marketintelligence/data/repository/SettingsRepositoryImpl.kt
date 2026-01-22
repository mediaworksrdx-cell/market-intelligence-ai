package com.example.marketintelligence.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private companion object {
        val SCANNER_ENGINE_KEY = stringPreferencesKey("scanner_engine_preference")
        val MENTOR_ENGINE_KEY = stringPreferencesKey("mentor_engine_preference")
        val CHART_ENGINE_KEY = stringPreferencesKey("chart_engine_preference")
        val MARKET_KEY = stringPreferencesKey("market_preference")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_preference")
        val RISK_PROFILE_KEY = stringPreferencesKey("risk_profile_preference")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled_preference")
    }

    override val selectedScannerEngine: Flow<String> = dataStore.data.map { it[SCANNER_ENGINE_KEY] ?: "Standard (Gemini)" }
    override val selectedMentorEngine: Flow<String> = dataStore.data.map { it[MENTOR_ENGINE_KEY] ?: "Standard (Gemini)" }
    override val selectedChartEngine: Flow<String> = dataStore.data.map { it[CHART_ENGINE_KEY] ?: "Proprietary Engine" }
    
    override val selectedMarket: Flow<MarketType> = dataStore.data.map { 
        val name = it[MARKET_KEY] ?: MarketType.IN.name
        try { MarketType.valueOf(name) } catch(e: Exception) { MarketType.IN }
    }

    override val isDarkMode: Flow<Boolean> = dataStore.data.map { it[DARK_MODE_KEY] ?: true }
    override val riskProfile: Flow<String> = dataStore.data.map { it[RISK_PROFILE_KEY] ?: "CONSERVATIVE" }
    override val isNotificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: true }

    override suspend fun setScannerEngine(name: String) {
        dataStore.edit { it[SCANNER_ENGINE_KEY] = name }
    }
    override suspend fun setMentorEngine(name: String) {
        dataStore.edit { it[MENTOR_ENGINE_KEY] = name }
    }
    override suspend fun setChartEngine(name: String) {
        dataStore.edit { it[CHART_ENGINE_KEY] = name }
    }
    override suspend fun setMarket(market: MarketType) {
        dataStore.edit { it[MARKET_KEY] = market.name }
    }
    override suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }
    override suspend fun setRiskProfile(profile: String) {
        dataStore.edit { it[RISK_PROFILE_KEY] = profile }
    }
    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }
}
