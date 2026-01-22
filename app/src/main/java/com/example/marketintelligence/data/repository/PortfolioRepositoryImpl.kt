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
    // Replaced list with MutableStateFlow to emit updates reactively
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    
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
