package com.example.marketintelligence.data.source.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.marketintelligence.domain.model.MarketType

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val quantity: Int,
    val price: Double,
    val type: String, // BUY/SELL
    val timestamp: Long
)

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey val symbol: String,
    val quantity: Int,
    val averagePrice: Double
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val type: String, // "STOCK", "INDEX", or "CRYPTO"
    val price: Double,
    val changePercent: Double,
    val instrumentToken: Long? = null
)
