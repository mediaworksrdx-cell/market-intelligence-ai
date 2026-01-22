package com.example.redxchartlibrary.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.redxchartlibrary.data.indicators.IndicatorResult
import com.example.redxchartlibrary.data.local.Drawing
import com.example.redxchartlibrary.data.local.DrawingTool

// --- Enums and Sealed Classes for State ---

sealed class Indicator {
    abstract val name: String
    data class SMA(val period: Int = 20) : Indicator() { override val name = "SMA($period)" }
    data class EMA(val period: Int = 20) : Indicator() { override val name = "EMA($period)" }
    data class RSI(val period: Int = 14) : Indicator() { override val name = "RSI($period)" }
    data class MACD(val fastPeriod: Int = 12, val slowPeriod: Int = 26, val signalPeriod: Int = 9) : Indicator() {
        override val name = "MACD($fastPeriod, $slowPeriod, $signalPeriod)"
    }
}

sealed class Overlay {
    data class HorizontalLine(val price: Float, val color: Color, val label: String) : Overlay()
}

// --- Main Chart State Class ---

class ChartState {
    // Interaction State
    var zoom by mutableStateOf(1f)
    var pan by mutableStateOf(0f)
    var crosshairPosition by mutableStateOf<Offset?>(null)

    // Drawing State
    var drawingMode by mutableStateOf(DrawingMode.NONE)
    val drawings = mutableStateListOf<Drawing>()
    var currentDrawing by mutableStateOf<DrawingTool?>(null)

    // Configuration State
    var indicators by mutableStateOf<List<Indicator>>(emptyList())
    var overlays by mutableStateOf<List<Overlay>>(emptyList())

    // Calculated/Derived State
    val indicatorResults = mutableMapOf<String, IndicatorResult>()

    fun addIndicator(indicator: Indicator) {
        indicators = indicators + indicator
    }

    fun removeIndicator(indicator: Indicator) {
        indicators = indicators - indicator
        indicatorResults.remove(indicator.name)
    }

    fun addOverlay(overlay: Overlay) {
        overlays = overlays + overlay
    }
}