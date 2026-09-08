package com.marketintelligence.tradeengine

import com.marketintelligence.tradeengine.config.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Serializable
data class CryptoData(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("current_price") val price: Double,
    @SerialName("price_change_percentage_24h") val changePercent: Double,
    @SerialName("market_cap") val marketCap: Long
)

@Serializable
data class SearchResult(
    val coins: List<Coin>
)

@Serializable
data class Coin(
    val id: String,
    val name: String,
    val symbol: String
)

object CoingeckoClient {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    // Rate Limiter: Enforce at least 3 seconds between any requests
    private val rateLimitMutex = Mutex()
    private var lastRequestTimeMs = 0L
    private const val MIN_REQUEST_INTERVAL_MS = 3000L

    private suspend fun awaitRateLimit() = rateLimitMutex.withLock {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTimeMs
        if (elapsed < MIN_REQUEST_INTERVAL_MS) {
            val waitTime = MIN_REQUEST_INTERVAL_MS - elapsed
            delay(waitTime)
        }
        lastRequestTimeMs = System.currentTimeMillis()
    }

    private fun HttpRequestBuilder.attachAuth() {
        val key = AppConfig.coingeckoApiKey
        if (key.isNotBlank()) {
            if (key.startsWith("CG-")) {
                header("x-cg-demo-api-key", key)
            } else {
                header("x-cg-pro-api-key", key)
            }
        }
    }

    suspend fun getMarketsData(vsCurrency: String): List<CryptoData> {
        awaitRateLimit()
        val encodedVs = java.net.URLEncoder.encode(vsCurrency, "UTF-8")
        val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=$encodedVs&order=market_cap_desc&per_page=100&page=1&sparkline=false"
        return client.get(url) {
            attachAuth()
        }.body()
    }

    suspend fun getOhlcData(coinId: String, vsCurrency: String, days: Int): List<List<Double>> {
        awaitRateLimit()
        val encodedCoin = java.net.URLEncoder.encode(coinId, "UTF-8")
        val encodedVs = java.net.URLEncoder.encode(vsCurrency, "UTF-8")
        val url = "https://api.coingecko.com/api/v3/coins/$encodedCoin/ohlc?vs_currency=$encodedVs&days=$days"
        return client.get(url) {
            attachAuth()
        }.body()
    }

    suspend fun search(query: String): SearchResult {
        awaitRateLimit()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.coingecko.com/api/v3/search?query=$encodedQuery"
        return client.get(url) {
            attachAuth()
        }.body()
    }
}
