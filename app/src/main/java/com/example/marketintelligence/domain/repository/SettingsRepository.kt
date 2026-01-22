package com.example.marketintelligence.domain.repository

import com.example.marketintelligence.domain.model.MarketType
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val selectedScannerEngine: Flow<String>
    val selectedMentorEngine: Flow<String>
    val selectedChartEngine: Flow<String>
    val selectedMarket: Flow<MarketType>
    val isDarkMode: Flow<Boolean>
    val riskProfile: Flow<String>
    val isNotificationsEnabled: Flow<Boolean>

    suspend fun setScannerEngine(name: String)
    suspend fun setMentorEngine(name: String)
    suspend fun setChartEngine(name: String)
    suspend fun setMarket(market: MarketType)
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setRiskProfile(profile: String)
    suspend fun setNotificationsEnabled(enabled: Boolean)
}
