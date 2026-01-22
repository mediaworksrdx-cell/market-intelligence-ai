package com.example.redxaiscanner.engine

import com.example.redxaiscanner.domain.model.Candle
import java.security.MessageDigest

data class DataIntegrityReport(
    val isValid: Boolean,
    val issues: List<String>,
    val dataHash: String
)

/**
 * A governance engine responsible for validating the integrity of market data
 * before it is passed to any analytical engines.
 */
class DataIntegrityEngine {
    companion object {
        const val VERSION = "1.0.0"
    }

    fun validate(candles: List<Candle>): DataIntegrityReport {
        val issues = mutableListOf<String>()

        if (candles.isEmpty()) {
            issues.add("Data series is empty.")
        }

        candles.forEachIndexed { index, candle ->
            // Check for impossible prices
            if (candle.low > candle.high || candle.open > candle.high || candle.close > candle.high) {
                issues.add("Invalid OHLC data at timestamp ${candle.timestamp}")
            }
            // Check for zero or negative volume (unless in a market where this is possible)
            if (candle.volume < java.math.BigDecimal.ZERO) {
                issues.add("Negative volume at timestamp ${candle.timestamp}")
            }
        }

        // Generate a SHA-256 hash of the candle data to create a deterministic signature
        val hash = generateHash(candles)

        return DataIntegrityReport(isValid = issues.isEmpty(), issues = issues, dataHash = hash)
    }

    private fun generateHash(candles: List<Candle>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = candles.joinToString { "${it.timestamp}${it.close}" }.toByteArray()
        val hashBytes = digest.digest(input)
        return hashBytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}