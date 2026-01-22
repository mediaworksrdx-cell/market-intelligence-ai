package com.example.marketintelligence.domain.repository

import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.data.source.local.HoldingEntity
import com.example.marketintelligence.data.source.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getCachedHoldings(market: MarketType): Flow<List<HoldingEntity>>
    suspend fun refreshHoldings()
    suspend fun addTransaction(transaction: TransactionEntity)
    suspend fun deleteAllTransactions()
    suspend fun deleteAsset(symbol: String)
}
