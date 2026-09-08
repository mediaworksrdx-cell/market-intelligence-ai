package com.marketintelligence.ai.data.repository

import com.marketintelligence.ai.data.source.local.HoldingDao
import com.marketintelligence.ai.data.source.local.HoldingEntity
import com.marketintelligence.ai.data.source.local.TransactionDao
import com.marketintelligence.ai.data.source.local.TransactionEntity
import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val holdingDao: HoldingDao
) : PortfolioRepository {
    
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    
    override fun getCachedHoldings(market: MarketType): Flow<List<HoldingEntity>> = holdingDao.getAllHoldings()
    
    override suspend fun refreshHoldings() {
        // Holdings can be re-calculated or synced here
    }
    
    override suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }
    
    override suspend fun deleteAllTransactions() {
        transactionDao.deleteAll()
        holdingDao.deleteAll()
    }

    override suspend fun deleteAsset(symbol: String) {
        transactionDao.deleteBySymbol(symbol)
        holdingDao.deleteBySymbol(symbol)
    }
}
