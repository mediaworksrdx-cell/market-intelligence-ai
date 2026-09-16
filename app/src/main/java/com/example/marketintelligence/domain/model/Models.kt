package com.example.marketintelligence.domain.model

import kotlinx.serialization.Serializable

enum class MarketType { IN, US, UAE }

@Serializable
data class MacroData(val symbol: String, val value: String)

@Serializable
data class IndexData(
    val symbol: String,
    val name: String,
    val price: Double,
    val openPrice: Double,
    val change: Double,
    val changePercent: Double,
    val market: MarketType,
    val instrumentToken: Long? = null
)

@Serializable
data class StockData(
    val symbol: String,
    val name: String,
    val price: Double,
    val openPrice: Double,
    val change: Double,
    val changePercent: Double,
    val volume: String,
    val market: MarketType,
    val instrumentToken: Long? = null
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
)

@Serializable
data class Greeks(
    val delta: Double,
    val gamma: Double,
    val theta: Double,
    val vega: Double,
    val rho: Double = 0.0
)

@Serializable
data class StrategyLeg(
    val id: String,
    val type: String, // "BUY", "SELL"
    val instrument: String, // "CALL", "PUT", "FUT"
    val strike: Double,
    val expiry: String,
    val qty: Int,
    val entryPrice: Double,
    val currentPrice: Double
) {
    val pnl: Double
        get() {
            val mult = if (type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
            return (currentPrice - entryPrice) * qty * mult
        }

    val pnlPercent: Double
        get() {
            return if (entryPrice > 0.0) {
                val mult = if (type.equals("BUY", ignoreCase = true)) 1.0 else -1.0
                ((currentPrice - entryPrice) / entryPrice) * 100.0 * mult
            } else 0.0
        }
}

@Serializable
data class OptionStrategy(
    val id: String,
    val name: String,
    val description: String,
    val maxProfit: Double,
    val maxLoss: Double,
    val breakeven: List<Double>,
    val probability: Double,
    val roi: Double,
    val legs: List<String>,
    val payoffPoints: List<PayoffPoint>,
    val todayPayoffPoints: List<PayoffPoint> = emptyList(),
    val rawLegs: List<StrategyLeg> = emptyList()
)

@Serializable
data class PayoffPoint(val price: Double, val pnl: Double)

@Serializable
data class TrainingModule(
    val id: String,
    val title: String,
    val duration: String,
    val locked: Boolean,
    val topics: List<String>
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

enum class BuildupSortOrder {
    OI_GAINERS, OI_LOSERS, PRICE_GAINERS, PRICE_LOSERS, VOLUME, PCR
}

enum class AssetTypeFilter {
    ALL, INDICES, STOCKS
}

@Serializable
data class FnoBuildupStock(
    val symbol: String,
    val name: String,
    val isIndex: Boolean,
    val sector: String,
    val ltp: Double,
    val priceChange: Double,
    val priceChangePct: Double,
    val openInterest: Long,
    val oiChange: Long,
    val oiChangePct: Double,
    val buildupType: BuildupType,
    val volume: Long,
    val pcr: Double = 1.0,
    val basis: Double = 0.0
)

@Serializable
data class Holding(
    val symbol: String,
    val quantity: Double,
    val avgPrice: Double,
    val investedValue: Double,
    val currentValue: Double,
    val totalPnl: Double,
    val todayPnl: Double,
    val market: MarketType
)

@Serializable
data class PersonalizedLearningPath(
    val suggestedCourseOrder: List<String>,
    val welcomeMessage: String
)

@Serializable
data class PerformanceReview(
    val overallFeedback: String,
    val identifiedWeakness: String,
    val suggestedNextLesson: String
)
