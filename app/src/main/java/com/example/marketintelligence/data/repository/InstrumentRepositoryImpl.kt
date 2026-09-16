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

                    var parsedInstruments: List<InstrumentEntity>? = null

                    // 1. Primary backend download attempt
                    try {
                        val response = apiService.downloadInstruments()
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) {
                                val parsed = InstrumentParser.parseCsv(body.byteStream())
                                if (parsed.isNotEmpty()) {
                                    parsedInstruments = parsed
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Primary instruments download skipped: ${e.message}")
                    }

                    // 2. Direct Kite public endpoint fallback
                    if (parsedInstruments == null) {
                        try {
                            val kiteResponse = apiService.downloadInstrumentsFromKite()
                            if (kiteResponse.isSuccessful) {
                                val body = kiteResponse.body()
                                if (body != null) {
                                    val parsed = InstrumentParser.parseCsv(body.byteStream())
                                    if (parsed.isNotEmpty()) {
                                        parsedInstruments = parsed
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Direct Kite download fallback skipped: ${e.message}")
                        }
                    }

                    if (!parsedInstruments.isNullOrEmpty()) {
                        instrumentDao.refreshInstruments(parsedInstruments)
                        dataStore.edit { it[LAST_SYNC_KEY] = currentTime }
                        _syncStatus.value = SyncStatus.Success
                    } else if (dbCount > 0) {
                        Log.i(TAG, "Retaining existing $dbCount cached instruments")
                        _syncStatus.value = SyncStatus.Success
                    } else {
                        instrumentDao.refreshInstruments(getDefaultInstruments())
                        _syncStatus.value = SyncStatus.Success
                    }
                } else {
                    _syncStatus.value = SyncStatus.Success
                }
            } catch (e: Exception) {
                Log.w(TAG, "Instrument sync error gracefully handled: ${e.message}")
                val dbCount = try { instrumentDao.getCount() } catch (_: Exception) { 0 }
                if (dbCount > 0) {
                    _syncStatus.value = SyncStatus.Success
                } else {
                    try {
                        instrumentDao.refreshInstruments(getDefaultInstruments())
                        _syncStatus.value = SyncStatus.Success
                    } catch (_: Exception) {
                        _syncStatus.value = SyncStatus.Idle
                    }
                }
            }
        }
    }

    private fun getDefaultInstruments(): List<InstrumentEntity> = listOf(
        // India Indices & Stocks
        InstrumentEntity(256265, "NSE", "NIFTY 50", "NIFTY 50 Index", "INDEX", "INDICES", null, null, 50),
        InstrumentEntity(260105, "NSE", "BANKNIFTY", "Nifty Bank Index", "INDEX", "INDICES", null, null, 15),
        InstrumentEntity(257801, "NSE", "FINNIFTY", "Nifty Financial Services", "INDEX", "INDICES", null, null, 25),
        InstrumentEntity(265, "BSE", "SENSEX", "BSE SENSEX", "INDEX", "INDICES", null, null, 10),
        InstrumentEntity(288009, "NSE", "MIDCPNIFTY", "NIFTY Midcap 50", "INDEX", "INDICES", null, null, 50),
        InstrumentEntity(738561, "NSE", "RELIANCE", "Reliance Industries Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(2953217, "NSE", "TCS", "Tata Consultancy Services Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(341249, "NSE", "HDFCBANK", "HDFC Bank Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(408065, "NSE", "INFY", "Infosys Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(1270529, "NSE", "ICICIBANK", "ICICI Bank Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(2714625, "NSE", "BHARTIARTL", "Bharti Airtel Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(884737, "NSE", "TATAMOTORS", "Tata Motors Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(424961, "NSE", "ITC", "ITC Ltd", "EQ", "NSE", null, null, 1),
        InstrumentEntity(779521, "NSE", "SBIN", "State Bank of India", "EQ", "NSE", null, null, 1),
        InstrumentEntity(2939649, "NSE", "LT", "Larsen & Toubro Ltd", "EQ", "NSE", null, null, 1),

        // US Indices & Stocks
        InstrumentEntity(900001, "US", "SPX", "S&P 500 Index", "INDEX", "INDICES", null, null, 1),
        InstrumentEntity(900002, "US", "NDX", "NASDAQ 100 Index", "INDEX", "INDICES", null, null, 1),
        InstrumentEntity(900003, "US", "DJI", "Dow Jones Industrial", "INDEX", "INDICES", null, null, 1),
        InstrumentEntity(900004, "NASDAQ", "AAPL", "Apple Inc.", "EQ", "US", null, null, 1),
        InstrumentEntity(900005, "NASDAQ", "NVDA", "NVIDIA Corporation", "EQ", "US", null, null, 1),
        InstrumentEntity(900006, "NASDAQ", "MSFT", "Microsoft Corporation", "EQ", "US", null, null, 1),
        InstrumentEntity(900007, "NASDAQ", "GOOGL", "Alphabet Inc.", "EQ", "US", null, null, 1),
        InstrumentEntity(900008, "NASDAQ", "AMZN", "Amazon.com Inc.", "EQ", "US", null, null, 1),
        InstrumentEntity(900009, "NASDAQ", "TSLA", "Tesla Inc.", "EQ", "US", null, null, 1),
        InstrumentEntity(900010, "NASDAQ", "META", "Meta Platforms Inc.", "EQ", "US", null, null, 1),

        // UAE Indices & Stocks
        InstrumentEntity(910001, "DFM", "DFMGI", "DFM General Index", "INDEX", "INDICES", null, null, 1),
        InstrumentEntity(910002, "ADX", "ADX", "Abu Dhabi Securities Exchange", "INDEX", "INDICES", null, null, 1),
        InstrumentEntity(910003, "DFM", "EMAAR", "Emaar Properties PJSC", "EQ", "UAE", null, null, 1),
        InstrumentEntity(910004, "DFM", "DEWA", "Dubai Electricity & Water", "EQ", "UAE", null, null, 1),
        InstrumentEntity(910005, "ADX", "FAB", "First Abu Dhabi Bank", "EQ", "UAE", null, null, 1),
        InstrumentEntity(910006, "ADX", "ALDAR", "Aldar Properties", "EQ", "UAE", null, null, 1),
        InstrumentEntity(910007, "DFM", "EMIRATESNBD", "Emirates NBD Bank", "EQ", "UAE", null, null, 1),

        // Crypto
        InstrumentEntity(920001, "CRYPTO", "BTC", "Bitcoin", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920002, "CRYPTO", "ETH", "Ethereum", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920003, "CRYPTO", "SOL", "Solana", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920004, "CRYPTO", "DOGE", "Dogecoin", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920005, "CRYPTO", "BNB", "Binance Coin", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920006, "CRYPTO", "SHIB", "Shiba Inu", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920007, "CRYPTO", "XRP", "XRP Ripple", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920008, "CRYPTO", "ADA", "Cardano", "CRYPTO", "CRYPTO", null, null, 1),
        InstrumentEntity(920009, "CRYPTO", "AVAX", "Avalanche", "CRYPTO", "CRYPTO", null, null, 1)
    )

    override suspend fun search(query: String): List<InstrumentEntity> {
        return withContext(Dispatchers.IO) {
            if (query.isBlank()) emptyList()
            else instrumentDao.search(query)
        }
    }
}
