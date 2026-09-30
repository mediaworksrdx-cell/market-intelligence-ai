package com.example.marketintelligence.data.util

import android.util.Log
import com.example.marketintelligence.data.source.local.InstrumentEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object InstrumentParser {
    private const val TAG = "InstrumentParser"

    fun parseCsv(inputStream: InputStream): List<InstrumentEntity> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val instruments = mutableListOf<InstrumentEntity>()
        
        val header = reader.readLine() ?: return emptyList()
        val columns = header.split(",").map { it.trim().trim('\"') }
        val colMap = columns.withIndex().associate { it.value to it.index }
        
        Log.d(TAG, "CSV Headers detected: $columns")

        reader.forEachLine { line ->
            // Zerodha CSV format is consistent, standard split is usually fine
            val row = line.split(",")
            
            if (row.size >= columns.size) {
                try {
                    val entity = InstrumentEntity(
                        instrument_token = row[colMap["instrument_token"] ?: return@forEachLine].toLong(),
                        exchange = row[colMap["exchange"] ?: return@forEachLine],
                        tradingsymbol = row[colMap["tradingsymbol"] ?: return@forEachLine],
                        name = row[colMap["name"] ?: return@forEachLine].trim('\"'),
                        instrument_type = row[colMap["instrument_type"] ?: return@forEachLine],
                        segment = row[colMap["segment"] ?: return@forEachLine],
                        expiry = row[colMap["expiry"] ?: return@forEachLine].takeIf { it.isNotEmpty() },
                        strike = row[colMap["strike"] ?: return@forEachLine].toDoubleOrNull(),
                        lot_size = row[colMap["lot_size"] ?: return@forEachLine].toInt()
                    )
                    
                    val isEquity = entity.instrument_type == "EQ" && (entity.exchange == "NSE" || entity.exchange == "BSE")
                    val isFnO = (entity.segment == "NFO-FUT" || entity.segment == "NFO-OPT") && 
                               (entity.instrument_type == "FUTSTK" || entity.instrument_type == "OPTSTK")
                    val isIndex = entity.instrument_type == "INDEX"
                    
                    if (isEquity || isFnO || isIndex) {
                        instruments.add(entity)
                    }
                } catch (e: Exception) {
                    // Skip invalid rows
                }
            }
        }
        Log.d(TAG, "Successfully parsed ${instruments.size} valid instruments")
        return instruments
    }
}
