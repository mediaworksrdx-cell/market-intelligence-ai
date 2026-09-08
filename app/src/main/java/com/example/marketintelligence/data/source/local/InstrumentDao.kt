package com.example.marketintelligence.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface InstrumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(instruments: List<InstrumentEntity>)

    @Query("SELECT * FROM instruments WHERE UPPER(tradingsymbol) LIKE UPPER(:query) || '%' OR UPPER(name) LIKE UPPER(:query) || '%' LIMIT 100")
    suspend fun search(query: String): List<InstrumentEntity>

    @Query("SELECT COUNT(*) FROM instruments")
    suspend fun getCount(): Int

    @Query("DELETE FROM instruments")
    suspend fun clear()

    @Transaction
    suspend fun refreshInstruments(instruments: List<InstrumentEntity>) {
        clear()
        insertAll(instruments)
    }
}
