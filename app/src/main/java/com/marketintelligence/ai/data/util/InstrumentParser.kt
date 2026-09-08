package com.marketintelligence.ai.data.util

import android.util.Log
import com.marketintelligence.ai.data.source.local.InstrumentEntity
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
                        instrument_token = row[colMap["instrument_token"]!!].toLong(),
                        exchange = row[colMap["exchange"]!!],
                        tradingsymbol = row[colMap["tradingsymbol"]!!],
                        name = row[colMap["name"]!!].trim('\"'),
                        instrument_type = row[colMap["instrument_type"]!!],
                        segment = row[colMap["segment"]!!],
                        expiry = row[colMap["expiry"]!!].takeIf { it.isNotEmpty() },
                        strike = row[colMap["strike"]!!].toDoubleOrNull(),
                        lot_size = row[colMap["lot_size"]!!].toInt()
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
