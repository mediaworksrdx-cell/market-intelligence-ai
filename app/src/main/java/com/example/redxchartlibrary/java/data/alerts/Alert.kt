package com.example.redxchartlibrary.data.alerts

import com.example.redxchartlibrary.data.local.DrawingTool
import com.example.redxchartlibrary.state.Indicator

// Represents the specific condition for an alert to trigger.
// This is extensible for FVG, SMC, etc.
sealed class AlertCondition {
    abstract val id: String
    
    data class PriceCrosses(val price: Float) : AlertCondition() {
        override val id: String = "price_cross_$price"
        var hasTriggered: Boolean = false // State to prevent rapid re-triggering
    }
    
    data class IndicatorValue(val indicator: Indicator, val targetValue: Float, val crossesAbove: Boolean) : AlertCondition() {
        override val id: String = "indicator_${indicator.name}_${targetValue}"
        var hasTriggered: Boolean = false
    }

    data class TrendlineCross(val trendline: DrawingTool.Trendline) : AlertCondition() {
        override val id: String = "trendline_${trendline.hashCode()}"
        var hasTriggered: Boolean = false
    }
}

// Represents an event that is fired when a condition is met.
data class AlertEvent(
    val timestamp: Long,
    val condition: AlertCondition,
    val message: String
)