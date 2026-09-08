package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.marketintelligence.domain.chart.*
import com.example.marketintelligence.ui.chart.layers.*
import com.example.tradeengine.models.Candle
import kotlin.math.roundToInt

/**
 * Advanced candlestick chart composable with layered rendering architecture.
 *
 * Layers (rendered bottom to top):
 * 1. Grid → 2. Volume → 3. Candles → 4. Indicator Overlays →
 * 5. Drawings → 6. Crosshair → 7. Indicator Panels (below main chart)
 *
 * Supports: 5 chart types, 10 indicators, 5 drawing tools, pinch-zoom, pan, crosshair.
 */
@Composable
fun AdvancedCandleStickChart(
    chartState: ChartState,
    modifier: Modifier = Modifier,
    onAddDrawingPoint: ((ChartPoint) -> Unit)? = null
) {
    val candles = chartState.candles
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var pan by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.2f, 8f)
        pan += panChange.x
    }

    // Pre-compute Heikin-Ashi candles if needed
    val heikinAshiCandles = remember(candles, chartState.chartType) {
        if (chartState.chartType == ChartType.HEIKIN_ASHI) candles.toHeikinAshi() else emptyList()
    }

    // Layout constants
    val rightMarginDp = 56.dp
    val bottomMarginDp = 20.dp
    val panelHeightFraction = if (chartState.hasPanelIndicators) 0.28f else 0f
    val volumeHeightFraction = if (chartState.showVolume) 0.15f else 0f

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (chartState.activeDrawingTool == DrawingToolType.NONE) {
                            pan += dragAmount.x
                            change.consume()
                        }
                    }
                )
            }
            .pointerInput(chartState.activeDrawingTool) {
                detectTapGestures(
                    onPress = { offset ->
                        if (chartState.activeDrawingTool == DrawingToolType.NONE) {
                            crosshairPosition = offset
                        }
                    },
                    onTap = { offset ->
                        if (chartState.activeDrawingTool != DrawingToolType.NONE && onAddDrawingPoint != null) {
                            // Convert screen coordinates to chart coordinates
                            val rightMarginPx = with(density) { rightMarginDp.toPx() }
                            val chartWidth = size.width - rightMarginPx
                            val panelHeight = size.height * panelHeightFraction
                            val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
                            val chartAreaHeight = size.height - panelHeight - bottomMarginPx

                            if (candles.isNotEmpty() && chartWidth > 0 && chartAreaHeight > 0) {
                                val candleWidthWithZoom = (10.dp.toPx() * zoom)
                                val visibleCount = (chartWidth / candleWidthWithZoom).roundToInt().coerceAtLeast(1)
                                val maxPan = ((candles.size - visibleCount) * candleWidthWithZoom).coerceAtLeast(0f)
                                val clampedPan = pan.coerceIn(-maxPan, 0f)
                                val startIndex = ((-clampedPan / candleWidthWithZoom).toInt()).coerceIn(0, candles.size - 1)
                                val endIndex = (startIndex + visibleCount).coerceAtMost(candles.size)

                                val visibleCandles = candles.subList(startIndex, endIndex)
                                if (visibleCandles.isNotEmpty()) {
                                    val priceMin = visibleCandles.minOf { it.low }
                                    val priceMax = visibleCandles.maxOf { it.high }
                                    val priceRange = priceMax - priceMin

                                    val candleWidth = chartWidth / visibleCount
                                    val candleIndex = startIndex + (offset.x / candleWidth).toInt()
                                    val price = priceMax - (offset.y / chartAreaHeight) * priceRange

                                    val timestamp = if (candleIndex in candles.indices) {
                                        candles[candleIndex].openTime
                                    } else {
                                        System.currentTimeMillis()
                                    }
                                    onAddDrawingPoint(ChartPoint(timestamp, price))
                                }
                            }
                        } else {
                            crosshairPosition = null
                        }
                    }
                )
            }
    ) {
        if (candles.isEmpty()) return@Canvas

        val rightMarginPx = rightMarginDp.toPx()
        val bottomMarginPx = bottomMarginDp.toPx()
        val chartWidth = size.width - rightMarginPx
        val panelHeight = size.height * panelHeightFraction
        val chartAreaHeight = size.height - panelHeight - bottomMarginPx

        if (chartWidth <= 0 || chartAreaHeight <= 0) return@Canvas

        // ── Viewport calculation ──
        val candleWidthWithZoom = 10.dp.toPx() * zoom
        val visibleCount = (chartWidth / candleWidthWithZoom).roundToInt().coerceAtLeast(1)
        val maxPan = ((candles.size - visibleCount) * candleWidthWithZoom).coerceAtLeast(0f)
        pan = pan.coerceIn(-maxPan, 0f)

        val startIndex = ((-pan / candleWidthWithZoom).toInt()).coerceIn(0, candles.size - 1)
        val endIndex = (startIndex + visibleCount).coerceAtMost(candles.size)

        val visibleCandles = candles.subList(startIndex, endIndex)
        if (visibleCandles.isEmpty()) return@Canvas

        // Price range with padding
        val rawMin = visibleCandles.minOf { it.low }
        val rawMax = visibleCandles.maxOf { it.high }
        val pricePadding = (rawMax - rawMin) * 0.05
        val priceMin = rawMin - pricePadding
        val priceMax = rawMax + pricePadding

        val volumeAreaHeight = chartAreaHeight * volumeHeightFraction

        // Candle timing for drawings
        val candleIntervalMs = if (candles.size > 1) {
            candles[1].openTime - candles[0].openTime
        } else 60000L
        val candleStartTime = candles.firstOrNull()?.openTime ?: 0L

        // ── Layer 1: Grid ──
        if (chartState.showGrid) {
            drawGridLayer(
                candles, startIndex, endIndex,
                priceMin, priceMax, chartAreaHeight,
                rightMarginPx, bottomMarginPx, textMeasurer
            )
        }

        // ── Layer 2: Volume Profile (VPVR) ──
        if (chartState.showVolumeProfile) {
            val vpData = chartState.volumeProfile ?: IndicatorCalculator.calculateVolumeProfile(
                candles = visibleCandles,
                priceMin = priceMin,
                priceMax = priceMax
            )
            drawVolumeProfileLayer(
                data = vpData,
                priceMin = priceMin,
                priceMax = priceMax,
                chartAreaHeight = chartAreaHeight,
                rightMargin = rightMarginPx,
                textMeasurer = textMeasurer
            )
        }

        // ── Layer 3: Smart Money Concepts (SMC FVGs & Sweeps) ──
        if (chartState.showSmcOverlay) {
            val smc = chartState.smcAnalysis ?: IndicatorCalculator.detectSMC(candles)
            drawSmcLayer(
                smcAnalysis = smc,
                startIndex = startIndex,
                endIndex = endIndex,
                priceMin = priceMin,
                priceMax = priceMax,
                chartAreaHeight = chartAreaHeight,
                rightMargin = rightMarginPx,
                textMeasurer = textMeasurer
            )
        }

        // ── Layer 4: Volume ──
        if (chartState.showVolume) {
            drawVolumeLayer(candles, startIndex, endIndex, chartAreaHeight, rightMarginPx, volumeAreaHeight)
        }

        // ── Layer 5: Candles ──
        drawCandleLayer(
            candles, heikinAshiCandles, chartState.chartType,
            startIndex, endIndex, priceMin, priceMax,
            chartAreaHeight, rightMarginPx
        )

        // ── Layer 6: Indicator Overlays ──
        if (chartState.hasOverlayIndicators) {
            drawIndicatorOverlayLayer(
                chartState.overlayIndicators, chartState.indicatorResults,
                startIndex, endIndex, priceMin, priceMax,
                chartAreaHeight, rightMarginPx
            )
        }

        // ── Layer 7: F&O Dealer Walls & Active Strategy Payoff Zones ──
        if (chartState.showFnoOverlay || chartState.showStrategyOverlay) {
            drawFnoOverlayLayer(
                fnoLevels = if (chartState.showFnoOverlay) chartState.fnoLevels else null,
                strategyOverlay = if (chartState.showStrategyOverlay) chartState.strategyOverlay else null,
                priceMin = priceMin,
                priceMax = priceMax,
                chartAreaHeight = chartAreaHeight,
                rightMargin = rightMarginPx,
                textMeasurer = textMeasurer
            )
        }

        // ── Layer 8: Drawings ──
        if (chartState.drawings.isNotEmpty() || chartState.currentDrawing != null) {
            drawDrawingLayer(
                chartState.drawings, chartState.currentDrawing,
                startIndex, endIndex, priceMin, priceMax,
                chartAreaHeight, rightMarginPx,
                candleStartTime, candleIntervalMs
            )
        }

        // ── Layer 9: Crosshair ──
        drawCrosshairLayer(
            crosshairPosition, candles, startIndex, endIndex,
            priceMin, priceMax, chartAreaHeight, rightMarginPx,
            textMeasurer
        )

        // ── Layer 10: Indicator Panels (RSI, MACD, Stochastic, ATR, CVD) ──
        if (chartState.hasPanelIndicators) {
            drawIndicatorPanelLayer(
                chartState.panelIndicators, chartState.indicatorResults,
                startIndex, endIndex,
                chartAreaHeight + bottomMarginPx, panelHeight,
                rightMarginPx, textMeasurer
            )
        }
    }
}
