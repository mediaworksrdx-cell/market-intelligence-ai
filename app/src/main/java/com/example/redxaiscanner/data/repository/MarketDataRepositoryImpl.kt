package com.example.redxaiscanner.data.repository

import com.example.redxaiscanner.data.datasource.local.CandleDao
import com.example.redxaiscanner.data.mapper.toCandle
import com.example.redxaiscanner.data.mapper.toCandleEntities
import com.example.redxaiscanner.domain.model.Candle
import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

// A simple placeholder for your remote data source, e.g., a Retrofit service
interface MarketApiService {
    suspend fun getCandles(symbol: String, timeframe: String): com.example.redxaiscanner.data.datasource.remote.MarketDataDto
}


class MarketDataRepositoryImpl(
    private val dao: CandleDao,
    private val api: MarketApiService,
    private val cacheSize: Int = 200
) : MarketDataRepository {

    override fun getCandles(
        symbols: List<String>,
        timeframe: Timeframe
    ): Flow<Map<String, List<Candle>>> {
        
        val flows = symbols.map { symbol ->
            flow<Unit> {
                try {
                    val remoteData = api.getCandles(symbol, timeframe.interval)
                    val entities = remoteData.toCandleEntities(timeframe.interval)
                    dao.upsertAll(entities)
                    dao.trimCache(symbol, timeframe.interval, cacheSize)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.onStart {
                // Initial trigger
            }.combine(dao.getCandles(symbol, timeframe.interval)) { _, cachedEntities ->
                symbol to cachedEntities.map { it.toCandle() }
            }
        }
        
        if (flows.isEmpty()) return flow { emit(emptyMap()) }
        
        return combine(flows) { arrayOfPairs ->
            arrayOfPairs.toMap()
        }
    }
}
