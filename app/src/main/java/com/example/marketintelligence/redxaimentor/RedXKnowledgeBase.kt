package com.example.marketintelligence.redxaimentor

object RedXKnowledgeBase {

    data class KnowledgeEntry(
        val id: String,
        val title: String,
        val content: String,
        val keywords: List<String>
    )

    val entries = listOf(
        KnowledgeEntry(
            id = "KB-CORE-001",
            title = "Market First Principles",
            content = """
                * Price exists due to **order execution**.
                * Two forces only: **Buy orders / Sell orders**.
                * Directional movement = **liquidity consumption**.
                * Charts are **order interaction footprints**.
            """.trimIndent(),
            keywords = listOf("market", "first principles", "order", "liquidity", "charts")
        ),
        KnowledgeEntry(
            id = "KB-CORE-002",
            title = "Candlesticks (Price Language)",
            content = """
                **Definition**
                * Time-compressed record of trading activity.

                **Data Points**
                * Open, High, Low, Close.

                **Interpretation**
                * Shows **control**, not prediction.
            """.trimIndent(),
            keywords = listOf("candlestick", "candle", "price language", "open", "high", "low", "close")
        ),
        KnowledgeEntry(
            id = "KB-CORE-003",
            title = "Candlestick Anatomy",
            content = """
                **Body**
                * Open–Close range.
                * Large body = conviction.
                * Small body = balance.

                **Wicks**
                * Rejected prices.
                * Upper wick = sell-side absorption.
                * Lower wick = buy-side absorption.
                * Institutional meaning: **liquidity capture**.
            """.trimIndent(),
            keywords = listOf("anatomy", "body", "wick", "absorption", "liquidity capture")
        ),
        KnowledgeEntry(
            id = "KB-CORE-004",
            title = "Candle Psychology",
            content = """
                * Bullish: Close > Open → buyer control.
                * Bearish: Close < Open → seller control.
                * Color ≠ trend.
            """.trimIndent(),
            keywords = listOf("psychology", "bullish", "bearish", "control")
        ),
        KnowledgeEntry(
            id = "KB-CORE-005",
            title = "Candle Formation Reality",
            content = """
                * Aggregation of:
                  * Market orders
                  * Stop executions
                  * Limit absorption
                * Candles = **evidence**, not signals.
            """.trimIndent(),
            keywords = listOf("formation", "market orders", "stop executions", "limit absorption", "evidence")
        ),
        KnowledgeEntry(
            id = "KB-CORE-006",
            title = "Candlestick Structures",
            content = """
                * Momentum candle: imbalance.
                * Rejection candle: failed auction.
                * Indecision candle: liquidity buildup.
            """.trimIndent(),
            keywords = listOf("structures", "momentum", "rejection", "indecision", "imbalance")
        ),
        KnowledgeEntry(
            id = "KB-CORE-007",
            title = "Trend (Structural State)",
            content = """
                * Uptrend: HH + HL.
                * Downtrend: LH + LL.
                * Exists due to **position building**.
            """.trimIndent(),
            keywords = listOf("trend", "uptrend", "downtrend", "hh", "hl", "lh", "ll", "position building")
        ),
        KnowledgeEntry(
            id = "KB-CORE-008",
            title = "Trendlines (Institutional Context)",
            content = """
                * Represent **liquidity zones**.
                * Rules:
                  * ≥2 touches (3 confirmation).
                  * Drawn from swing points.
                * Break ≠ reversal.
            """.trimIndent(),
            keywords = listOf("trendline", "liquidity zones", "swing points")
        ),
        KnowledgeEntry(
            id = "KB-CORE-009",
            title = "Moving Averages",
            content = """
                **Concept**
                * Average accepted price.

                **Key Levels**
                * 20 MA: short-term flow.
                * 50 MA: swing structure.
                * 200 MA: institutional bias.

                **Meaning**
                * Above = premium.
                * Below = discount.
                * Dynamic S/R.
            """.trimIndent(),
            keywords = listOf("moving average", "ma", "20 ma", "50 ma", "200 ma", "premium", "discount")
        ),
        KnowledgeEntry(
            id = "KB-CORE-010",
            title = "RSI",
            content = """
                **Measures**
                * Momentum velocity.

                **Zones**
                * > 70 overbought.
                * <30 oversold.

                **Institutional Use**
                * Divergence = momentum loss.
                * Must align with structure.
            """.trimIndent(),
            keywords = listOf("rsi", "momentum", "overbought", "oversold", "divergence")
        ),
        KnowledgeEntry(
            id = "KB-CORE-011",
            title = "Bollinger Bands",
            content = """
                **Components**
                * Mean + volatility envelope.

                **Insights**
                * Expansion = volatility injection.
                * Compression = breakout prep.
                * Band riding = trend continuation.
            """.trimIndent(),
            keywords = listOf("bollinger", "bands", "volatility", "expansion", "compression")
        ),
        KnowledgeEntry(
            id = "KB-CORE-012",
            title = "Support & Resistance",
            content = """
                * Liquidity pools, not lines.
                * Formed by:
                  * Stop clustering
                  * Pending orders
                * Used for **execution**, not prediction.
            """.trimIndent(),
            keywords = listOf("support", "resistance", "liquidity pools", "stop clustering", "pending orders")
        ),
        KnowledgeEntry(
            id = "KB-CORE-013",
            title = "Smart Money Concepts (SMC)",
            content = """
                **Smart Money**
                * Large-capital participants.
                * Require liquidity.

                **Market Structure**
                * BOS: continuation.
                * CHoCH: potential reversal.
            """.trimIndent(),
            keywords = listOf("smc", "smart money", "bos", "choch", "market structure")
        ),
        KnowledgeEntry(
            id = "KB-CORE-014",
            title = "Fair Value Gap (FVG)",
            content = """
                **Definition**
                * Inefficient price delivery.
                * Rapid displacement, no balance.

                **Purpose**
                * Rebalancing zone.
                * Institutional re-entry area.
            """.trimIndent(),
            keywords = listOf("fvg", "fair value gap", "inefficient", "displacement", "rebalancing")
        ),
        KnowledgeEntry(
            id = "KB-CORE-015",
            title = "Liquidity",
            content = """
                * Primary market driver.
                * Found at:
                  * Equal highs/lows
                  * Trendlines
                  * Ranges
                * Price seeks liquidity.
            """.trimIndent(),
            keywords = listOf("liquidity", "equal highs", "equal lows", "driver")
        ),
        KnowledgeEntry(
            id = "KB-CORE-016",
            title = "Order Blocks",
            content = """
                * Last opposing candle before impulse.
                * Represents institutional positioning.
                * High-probability reaction zones.
            """.trimIndent(),
            keywords = listOf("order block", "ob", "impulse", "reaction zone")
        ),
        KnowledgeEntry(
            id = "KB-CORE-017",
            title = "Concept Integration Model",
            content = """
                * Candles → execution evidence.
                * Wicks → rejection/liquidity.
                * Trend → intent.
                * Indicators → behavior metrics.
                * SMC → manipulation logic.
                * FVG → imbalance.
                * Liquidity → destination.
            """.trimIndent(),
            keywords = listOf("integration", "model", "logic")
        ),
        KnowledgeEntry(
            id = "KB-CORE-018",
            title = "Institutional Axioms",
            content = """
                * Price is non-random.
                * Indicators do not move price.
                * Liquidity drives movement.
                * Candles reveal intent.
            """.trimIndent(),
            keywords = listOf("axioms", "institutional", "intent")
        ),
        KnowledgeEntry(
            id = "KB-CORE-019",
            title = "Learning Sequence",
            content = """
                1. Candlestick anatomy
                2. Market structure
                3. Support & resistance
                4. Trend & trendlines
                5. Moving averages
                6. RSI & Bollinger Bands
                7. Liquidity
                8. Smart Money Concepts
                9. FVG & Order Blocks
            """.trimIndent(),
            keywords = listOf("learning", "sequence", "curriculum")
        )
    )
}
