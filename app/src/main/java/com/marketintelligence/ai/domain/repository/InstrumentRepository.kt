package com.marketintelligence.ai.domain.repository

import com.marketintelligence.ai.data.source.local.InstrumentEntity
import kotlinx.coroutines.flow.Flow

interface InstrumentRepository {
    suspend fun syncInstrumentsIfNeeded()
    suspend fun search(query: String): List<InstrumentEntity>
    fun getSyncStatus(): Flow<SyncStatus>
}

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
