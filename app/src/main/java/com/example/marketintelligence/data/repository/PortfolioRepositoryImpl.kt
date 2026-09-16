package com.example.marketintelligence.data.repository

import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.data.source.local.HoldingEntity
import com.example.marketintelligence.data.source.local.TransactionEntity
import com.example.marketintelligence.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor() : PortfolioRepository {
    private val defaultTransactions = listOf(
        TransactionEntity(id = 1, symbol = "RELIANCE.NS", quantity = 15, price = 1285.0, type = "BUY", timestamp = System.currentTimeMillis() - 7 * 86400000L),
        TransactionEntity(id = 2, symbol = "HDFCBANK.NS", quantity = 30, price = 1690.0, type = "BUY", timestamp = System.currentTimeMillis() - 5 * 86400000L),
        TransactionEntity(id = 3, symbol = "INFY.NS", quantity = 20, price = 1815.0, type = "BUY", timestamp = System.currentTimeMillis() - 3 * 86400000L),
        TransactionEntity(id = 4, symbol = "TCS.NS", quantity = 10, price = 4120.0, type = "BUY", timestamp = System.currentTimeMillis() - 4 * 86400000L),
        TransactionEntity(id = 5, symbol = "BTC", quantity = 1, price = 76500.0, type = "BUY", timestamp = System.currentTimeMillis() - 10 * 86400000L),
        TransactionEntity(id = 6, symbol = "ETH", quantity = 5, price = 2450.0, type = "BUY", timestamp = System.currentTimeMillis() - 8 * 86400000L),
        TransactionEntity(id = 7, symbol = "AAPL", quantity = 10, price = 220.0, type = "BUY", timestamp = System.currentTimeMillis() - 6 * 86400000L),
        TransactionEntity(id = 8, symbol = "NVDA", quantity = 15, price = 115.0, type = "BUY", timestamp = System.currentTimeMillis() - 2 * 86400000L)
    )

    // Initialized with default seed so user portfolio immediately reflects live market quotes
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(defaultTransactions)
    
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = _transactions.asStateFlow()
    override fun getCachedHoldings(market: MarketType): Flow<List<HoldingEntity>> = MutableStateFlow(emptyList())
    override suspend fun refreshHoldings() {}
    
    override suspend fun addTransaction(transaction: TransactionEntity) {
        val current = _transactions.value.toMutableList()
        current.add(transaction)
        _transactions.value = current
    }
    
    override suspend fun deleteAllTransactions() {
        _transactions.value = emptyList()
    }

    override suspend fun deleteAsset(symbol: String) {
        val current = _transactions.value.toMutableList()
        current.removeAll { it.symbol == symbol }
        _transactions.value = current
    }
}
