package com.example.marketintelligence.domain.model

import kotlinx.serialization.Serializable

enum class MarketType { IN, US, UAE }

@Serializable
data class IndexData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val market: MarketType
)

@Serializable
data class StockData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: String,
    val market: MarketType
)

@Serializable
data class CryptoData(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double
)

@Serializable
data class StockQuote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double
)

@Serializable
data class AIAnalysisResult(
    val symbol: String,
    val signal: SignalType,
    val confidence: Int,
    val alphaScore: Int,
    val rrRatio: String,
    val pattern: String?,
    val patternComplexity: String,
    val timeframe: String,
    val entryZone: String?,
    val liquidityZone: String?,
    val stopLoss: Double?,
    val target: List<String>?,
    val risk: RiskLevel,
    val rationale: String,
    val marketStructure: String,
    val timestamp: String
)

enum class SignalType { BULLISH, BEARISH, NEUTRAL }
enum class RiskLevel { LOW, MEDIUM, HIGH }

@Serializable
data class PortfolioItem(
    val symbol: String,
    val name: String,
    val qty: Double,
    val avgPrice: Double,
    val currentPrice: Double,
    val market: MarketType,
    val type: AssetType,
    val aiScore: Int,
    val aiSentiment: String
)

enum class AssetType { STOCK, CRYPTO, INDEX }

@Serializable
data class OptionChainData(
    val strike: Double,
    val callOI: Double,
    val callLTP: Double,
    val callChange: Double,
    val callIV: Double,
    val callGreeks: Greeks,
    val putOI: Double,
    val putLTP: Double,
    val putChange: Double,
    val putIV: Double,
    val putGreeks: Greeks
)

@Serializable
data class OptionChain(
    val underlyingPrice: Double,
    val strikes: List<OptionChainData>
) {
    val calls: List<OptionChainData> get() = strikes
    val puts: List<OptionChainData> get() = strikes
}

@Serializable
data class Greeks(
    val delta: Double,
    val gamma: Double,
    val theta: Double,
    val vega: Double
)

@Serializable
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val time: String,
    val sentiment: Sentiment? = null,
    val read: Boolean,
    val aiResult: AIAnalysisResult? = null,
    val eventData: EconomicEvent? = null,
    val ipoData: IPOData? = null
)

enum class NotificationType { AI_SIGNAL, NEWS, EVENT, IPO }
enum class Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

enum class BuildupType {
    LONG_BUILDUP, SHORT_BUILDUP, SHORT_COVERING, LONG_UNWINDING, NEUTRAL
}

@Serializable
data class OptionStrategy(
    val id: String,
    val name: String,
    val description: String,
    val maxProfit: Double,
    val maxLoss: Double,
    val breakeven: Double,
    val probability: Double,
    val roi: Double,
    val legs: List<String>,
    val payoffPoints: List<PayoffPoint>
)

@Serializable
data class PayoffPoint(val price: Double, val pnl: Double)

// Restored missing models
@Serializable
data class TrainingModule(
    val id: String,
    val title: String,
    val duration: String,
    val locked: Boolean,
    val topics: List<String>
)

@Serializable
data class EconomicEvent(
    val id: String,
    val time: String,
    val currency: String,
    val event: String,
    val impact: String,
    val actual: String,
    val forecast: String,
    val previous: String
)

@Serializable
data class IPOData(
    val symbol: String,
    val name: String,
    val openDate: String,
    val closeDate: String,
    val priceBand: String,
    val gmp: Double,
    val gmpPercent: Double,
    val status: String
)

@Serializable
data class FutureContract(
    val expiry: String,
    val ltp: Double,
    val changePercent: Double,
    val changeOI: String,
    val volume: String,
    val vwap: Double,
    val basis: Double
)

@Serializable
data class FOSymbolData(
    val symbol: String,
    val exchange: String,
    val price: Double,
    val changePercent: Double,
    val pcr: Double,
    val pcrSignal: String,
    val maxPain: Double,
    val iv: Double,
    val ivRank: Double,
    val ivPercentile: Double,
    val trend: String,
    val bias: String,
    val buildup: String,
    val lotSize: Int,
    val contracts: List<FutureContract>
)

@Serializable
data class StrategyDefinition(
    val id: String,
    val name: String,
    val type: String,
    val description: String,
    val risk: String,
    val profitProb: Double,
    val maxProfit: String,
    val maxLoss: String,
    val breakeven: String,
    val roi: String,
    val payoffData: List<PayoffPoint>
)
