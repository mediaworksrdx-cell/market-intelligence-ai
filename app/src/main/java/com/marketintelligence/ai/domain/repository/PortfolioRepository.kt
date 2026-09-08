package com.marketintelligence.ai.domain.repository

import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.data.source.local.HoldingEntity
import com.marketintelligence.ai.data.source.local.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getCachedHoldings(market: MarketType): Flow<List<HoldingEntity>>
    suspend fun refreshHoldings()
    suspend fun addTransaction(transaction: TransactionEntity)
    suspend fun deleteAllTransactions()
    suspend fun deleteAsset(symbol: String)
}
