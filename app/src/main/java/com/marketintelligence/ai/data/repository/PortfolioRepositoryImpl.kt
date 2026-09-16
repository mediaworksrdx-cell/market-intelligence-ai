package com.marketintelligence.ai.data.repository

import com.marketintelligence.ai.data.source.local.HoldingDao
import com.marketintelligence.ai.data.source.local.HoldingEntity
import com.marketintelligence.ai.data.source.local.TransactionDao
import com.marketintelligence.ai.data.source.local.TransactionEntity
import com.marketintelligence.ai.domain.model.MarketType
import com.marketintelligence.ai.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val holdingDao: HoldingDao
) : PortfolioRepository {

    private val defaultTransactions = listOf(
        TransactionEntity(id = 1, symbol = "RELIANCE.NS", quantity = 15.0, price = 1285.0, type = "BUY", timestamp = System.currentTimeMillis() - 7 * 86400000L),
        TransactionEntity(id = 2, symbol = "HDFCBANK.NS", quantity = 30.0, price = 1690.0, type = "BUY", timestamp = System.currentTimeMillis() - 5 * 86400000L),
        TransactionEntity(id = 3, symbol = "INFY.NS", quantity = 20.0, price = 1815.0, type = "BUY", timestamp = System.currentTimeMillis() - 3 * 86400000L),
        TransactionEntity(id = 4, symbol = "TCS.NS", quantity = 10.0, price = 4120.0, type = "BUY", timestamp = System.currentTimeMillis() - 4 * 86400000L),
        TransactionEntity(id = 5, symbol = "BTC", quantity = 1.0, price = 76500.0, type = "BUY", timestamp = System.currentTimeMillis() - 10 * 86400000L),
        TransactionEntity(id = 6, symbol = "ETH", quantity = 5.0, price = 2450.0, type = "BUY", timestamp = System.currentTimeMillis() - 8 * 86400000L),
        TransactionEntity(id = 7, symbol = "AAPL", quantity = 10.0, price = 220.0, type = "BUY", timestamp = System.currentTimeMillis() - 6 * 86400000L),
        TransactionEntity(id = 8, symbol = "NVDA", quantity = 15.0, price = 115.0, type = "BUY", timestamp = System.currentTimeMillis() - 2 * 86400000L)
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = transactionDao.getAllTransactions().firstOrNull()
                if (list.isNullOrEmpty()) {
                    defaultTransactions.forEach { transactionDao.insert(it) }
                }
            } catch (_: Exception) {}
        }
    }
    
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
