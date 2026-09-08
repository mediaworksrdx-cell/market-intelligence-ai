package com.marketintelligence.ai.data.source.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "instruments",
    indices = [
        Index(value = ["tradingsymbol"]),
        Index(value = ["name"]),
        Index(value = ["instrument_type"]),
        Index(value = ["segment"])
    ]
)
data class InstrumentEntity(
    @PrimaryKey val instrument_token: Long,
    val exchange: String,
    val tradingsymbol: String,
    val name: String,
    val instrument_type: String,
    val segment: String,
    val expiry: String?,
    val strike: Double?,
    val lot_size: Int
)
