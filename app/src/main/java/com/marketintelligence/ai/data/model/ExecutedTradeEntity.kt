package com.marketintelligence.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "executed_trades")
data class ExecutedTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val quantity: Double,
    val price: Double,
    val timestamp: Long
)
