package com.marketintelligence.tradeengine.config

import java.io.File
import java.io.FileInputStream
import java.util.Properties

object AppConfig {
    private val properties = Properties().apply {
        val searchPaths = listOf(
            File("config.properties"),
            File("trade-engine/config.properties"),
            File("../config.properties"),
            File("local.properties"),
            File("../local.properties")
        )
        for (file in searchPaths) {
            if (file.exists() && file.isFile) {
                try {
                    val temp = Properties()
                    FileInputStream(file).use { temp.load(it) }
                    temp.stringPropertyNames().forEach { name ->
                        val value = temp.getProperty(name)
                        if (!value.isNullOrBlank() && getProperty(name).isNullOrBlank()) {
                            setProperty(name, value)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // --- Zerodha API Credentials --- //
    val apiKey: String = properties.getProperty("ZERODHA_API_KEY", System.getenv("ZERODHA_API_KEY") ?: "")
    val apiSecret: String = properties.getProperty("ZERODHA_API_SECRET", System.getenv("ZERODHA_API_SECRET") ?: "")
    val accessToken: String = properties.getProperty("ZERODHA_ACCESS_TOKEN", System.getenv("ZERODHA_ACCESS_TOKEN") ?: "")

    // --- CoinGecko API Credentials --- //
    val coingeckoApiKey: String = properties.getProperty("COINGECKO_API_KEY", System.getenv("COINGECKO_API_KEY") ?: "CG-TDEGaehYzYVRpRvSnWr77Ybp")

    // --- Massive / Polygon API Credentials --- //
    val massiveApiKey: String = properties.getProperty("MASSIVE_API_KEY", System.getenv("MASSIVE_API_KEY") ?: "Lcz6VOZUXzeoxZghYy3V3FfgATTdpyvm")

    // --- Instruments to Track (Token to Symbol Map) --- //
    val symbolMap = mapOf(
        256265L to "NIFTY 50",
        260105L to "BANKNIFTY",
        257801L to "FINNIFTY",
        265L    to "SENSEX",
        738561L to "RELIANCE.NS",
        341249L to "HDFCBANK.NS",
        408065L to "INFY.NS",
        2953217L to "TCS.NS",
        1270529L to "ICICIBANK.NS",
        779521L to "SBIN.NS",
        2714625L to "BHARTIARTL.NS",
        424961L to "ITC.NS",
        2939649L to "LT.NS",
        884737L to "TATAMOTORS.NS"
    )

    val instrumentTokens = symbolMap.keys.toList()

    // --- Ktor Server Configuration --- //
    const val serverPort = 8080
}
