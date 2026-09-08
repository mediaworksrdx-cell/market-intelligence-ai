package com.example.marketintelligence.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.marketintelligence.data.source.local.InstrumentDao
import com.example.marketintelligence.data.source.local.InstrumentEntity
import com.example.marketintelligence.data.source.remote.InstrumentApiService
import com.example.marketintelligence.data.util.InstrumentParser
import com.example.marketintelligence.domain.repository.InstrumentRepository
import com.example.marketintelligence.domain.repository.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "InstrumentRepo"

class InstrumentRepositoryImpl @Inject constructor(
    private val instrumentDao: InstrumentDao,
    private val apiService: InstrumentApiService,
    private val dataStore: DataStore<Preferences>
) : InstrumentRepository {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override fun getSyncStatus(): Flow<SyncStatus> = _syncStatus

    private val LAST_SYNC_KEY = longPreferencesKey("last_instrument_sync")

    override suspend fun syncInstrumentsIfNeeded() {
        withContext(Dispatchers.IO) {
            try {
                val dbCount = instrumentDao.getCount()
                val lastSync = dataStore.data.first()[LAST_SYNC_KEY] ?: 0L
                val currentTime = System.currentTimeMillis()
                val twentyFourHours = 24 * 60 * 60 * 1000L

                Log.d(TAG, "Sync check - DB Count: $dbCount, Last Sync: $lastSync")

                if (currentTime - lastSync > twentyFourHours || dbCount == 0) {
                    _syncStatus.emit(SyncStatus.Syncing)

                    val response = apiService.downloadInstruments()
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            val instruments = InstrumentParser.parseCsv(body.byteStream())
                            if (instruments.isNotEmpty()) {
                                instrumentDao.refreshInstruments(instruments)
                                dataStore.edit { it[LAST_SYNC_KEY] = currentTime }
                                _syncStatus.value = SyncStatus.Success
                            } else {
                                _syncStatus.value = SyncStatus.Error("Zero instruments parsed")
                            }
                        } else {
                            _syncStatus.value = SyncStatus.Error("Null response body")
                        }
                    } else {
                        _syncStatus.value = SyncStatus.Error("HTTP ${response.code()}")
                    }
                } else {
                    _syncStatus.value = SyncStatus.Success
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.Error(e.message ?: "Unknown Error")
            }
        }
    }

    override suspend fun search(query: String): List<InstrumentEntity> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank()) emptyList()
            else instrumentDao.search(query)
        }
    }
}
