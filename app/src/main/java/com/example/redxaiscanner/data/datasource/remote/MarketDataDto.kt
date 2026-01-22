package com.example.redxaiscanner.data.datasource.remote

// This is a simplified example. The actual structure will depend on your API provider.
// It often comes as a list of lists/arrays.
data class MarketDataDto(
    val symbol: String,
    // e.g., [ [timestamp, open, high, low, close, volume], ... ]
    val candles: List<List<String>>
)