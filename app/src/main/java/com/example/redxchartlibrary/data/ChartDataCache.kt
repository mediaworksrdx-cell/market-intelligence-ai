package com.example.redxchartlibrary.data

import com.example.redxchartlibrary.model.Candle

/**
 * A simple in-memory cache for storing candle data to avoid redundant network fetches.
 * The key is a composite string, e.g., "BTCUSD-1m".
 */
class ChartDataCache {
    private val cache = mutableMapOf<String, List<Candle>>()

    fun get(symbol: String, timeframe: String): List<Candle>? {
        return cache[generateKey(symbol, timeframe)]
    }

    fun put(symbol: String, timeframe: String, data: List<Candle>) {
        cache[generateKey(symbol, timeframe)] = data
    }

    private fun generateKey(symbol: String, timeframe: String) = "$symbol-$timeframe"
}