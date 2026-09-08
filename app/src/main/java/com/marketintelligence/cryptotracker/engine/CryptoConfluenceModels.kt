package com.marketintelligence.cryptotracker.engine

/**
 * Trade setup direction.
 */
enum class SetupDirection { LONG, SHORT }

/**
 * Setup quality grade based on scoring criteria.
 */
enum class SetupGrade { A_PLUS, A, B, WATCH, IGNORE }

/**
 * Current state of the evaluated trade setup.
 */
enum class SetupStatus { ACTIVE, INVALIDATED, TRIGGERED, EXPIRED }

/**
 * Detailed breakdown of the confluence scoring mechanism.
 */
data class ScoreBreakdown(
    val smcStructure: Int,     // 0–25
    val liquidity: Int,        // 0–20
    val fvg: Int,              // 0–20
    val rsi: Int,              // 0–15
    val htfAlignment: Int,     // 0–10
    val riskReward: Int,       // 0–10
    val total: Int             // 0–100
) {
    companion object {
        /**
         * Factory method to safely instantiate a ScoreBreakdown, ensuring factors are clamped to their maximum allowable limits.
         */
        fun fromFactors(
            smcStructure: Int,
            liquidity: Int,
            fvg: Int,
            rsi: Int,
            htfAlignment: Int,
            riskReward: Int
        ): ScoreBreakdown {
            val clampedSmc = smcStructure.coerceIn(0, 25)
            val clampedLiquidity = liquidity.coerceIn(0, 20)
            val clampedFvg = fvg.coerceIn(0, 20)
            val clampedRsi = rsi.coerceIn(0, 15)
            val clampedHtf = htfAlignment.coerceIn(0, 10)
            val clampedRr = riskReward.coerceIn(0, 10)
            
            val total = clampedSmc + clampedLiquidity + clampedFvg + clampedRsi + clampedHtf + clampedRr
            
            return ScoreBreakdown(
                smcStructure = clampedSmc,
                liquidity = clampedLiquidity,
                fvg = clampedFvg,
                rsi = clampedRsi,
                htfAlignment = clampedHtf,
                riskReward = clampedRr,
                total = total
            )
        }
    }
}

/**
 * Extension function to convert a ScoreBreakdown to its respective SetupGrade.
 */
fun ScoreBreakdown.toGrade(): SetupGrade = when {
    total >= 80 -> SetupGrade.A_PLUS
    total >= 70 -> SetupGrade.A
    total >= 60 -> SetupGrade.B
    total >= 50 -> SetupGrade.WATCH
    else -> SetupGrade.IGNORE
}

/**
 * A fully constructed, high-probability trade setup combining SMC, Liquidity, FVG, and RSI factors.
 */
data class CryptoSetup(
    val symbol: String,
    val timeframe: String,
    val direction: SetupDirection,
    val htfBias: StructureState,
    val smcAnalysis: CryptoSMCAnalysis,
    val fvg: EnrichedFVG?,
    val rsiAnalysis: CryptoRSIAnalysis,
    val score: ScoreBreakdown,
    val grade: SetupGrade,
    val entry: Double,
    val stopLoss: Double,
    val tp1: Double,
    val tp2: Double,
    val tp3: Double,
    val riskRewardRatio: Double,
    val riskPercent: Double,
    val status: SetupStatus,
    val createdAt: Long,
    val invalidatedAt: Long? = null
)
