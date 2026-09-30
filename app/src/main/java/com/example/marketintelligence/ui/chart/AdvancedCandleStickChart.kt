package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.*
import com.example.marketintelligence.ui.chart.layers.*
import com.example.tradeengine.models.Candle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Advanced candlestick chart composable with layered rendering architecture.
 *
 * Layers (rendered bottom to top):
 * 1. Grid → 2. Volume Profile → 3. SMC Overlays → 4. Volume Bars → 5. Candles →
 * 5b. Real-time Price Guideline & Countdown Timer →
 * 6. Indicator Overlays (EMA, VWAP, Bollinger) → 7. F&O Walls →
 * 8. User Drawings (with selection & rubber-band preview) → 9. Crosshair →
 * 10. Sub-Panel Indicators (RSI, MACD, Stochastic, ATR, CVD)
 */
@Composable
fun AdvancedCandleStickChart(
    chartState: ChartState,
    modifier: Modifier = Modifier,
    timeframe: String = "1D",
    currentPrice: Double? = null,
    onAddDrawingPoint: ((ChartPoint) -> Unit)? = null,
    onSelectDrawing: ((String?) -> Unit)? = null,
    onDeleteDrawing: ((String) -> Unit)? = null,
    onContinueDrawing: ((DrawingData) -> Unit)? = null,
    onMoveDrawingPoint: ((String, Int, ChartPoint) -> Unit)? = null,
    onOpenIndicatorSettingsFor: ((IndicatorType) -> Unit)? = null,
    onToggleIndicator: ((IndicatorType) -> Unit)? = null
) {
    val effectivePrice = when {
        currentPrice != null && currentPrice > 0.0 -> currentPrice
        chartState.candles.isNotEmpty() -> chartState.candles.last().close
        else -> 0.0
    }
    val candles = remember(chartState.candles) { chartState.candles.toMutableList() }.also { list ->
        if (list.isNotEmpty() && effectivePrice > 0.0) {
            val last = list.last()
            list[list.lastIndex] = last.copy(
                close = effectivePrice,
                high = maxOf(last.high, effectivePrice),
                low = minOf(last.low, effectivePrice)
            )
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var zoom by remember { mutableFloatStateOf(1f) }
    var scrollOffsetFromRight by remember { mutableFloatStateOf(0f) }
    var crosshairPosition by remember { mutableStateOf<Offset?>(null) }
    var currentRubberBandPoint by remember { mutableStateOf<ChartPoint?>(null) }
    var localSelectedDrawingId by remember(chartState.selectedDrawingId) {
        mutableStateOf(chartState.selectedDrawingId)
    }
    var selectedIndicatorType by remember { mutableStateOf<IndicatorType?>(null) }

    // Pre-compute Heikin-Ashi candles if needed
    val heikinAshiCandles = remember(candles, chartState.chartType) {
        if (chartState.chartType == ChartType.HEIKIN_ASHI) candles.toHeikinAshi() else emptyList()
    }

    // Layout constants
    val rightMarginDp = 56.dp
    val bottomMarginDp = 20.dp
    val showVolumePanel = chartState.showVolume
    val totalPanels = chartState.panelIndicators.size + (if (showVolumePanel) 1 else 0)
    val panelHeightFraction = when (totalPanels) {
        0 -> 0f
        1 -> 0.20f
        2 -> 0.32f
        3 -> 0.42f
        else -> 0.50f
    }
    val volumeHeightFraction = 0.15f

    val selectedDrawing = remember(chartState.drawings, localSelectedDrawingId) {
        chartState.drawings.find { it.id == localSelectedDrawingId }
    }
    val selectedIndicatorConfig = remember(chartState.activeIndicators, selectedIndicatorType) {
        chartState.activeIndicators.find { it.type == selectedIndicatorType }
    }

    var chartCanvasWidthPx by remember { mutableFloatStateOf(0f) }

    val viewportState by remember {
        derivedStateOf {
            val baseCount = 50f
            val vCount = (baseCount / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
            val maxScroll = (candles.size - vCount).coerceAtLeast(0).toFloat()
            val clampedScroll = scrollOffsetFromRight.coerceIn(0f, maxScroll)

            val eIndex = (candles.size - clampedScroll.roundToInt()).coerceIn(vCount.coerceAtMost(candles.size), candles.size)
            val sIndex = (eIndex - vCount).coerceAtLeast(0)
            Triple(vCount, sIndex, eIndex)
        }
    }
    val visibleCount = viewportState.first
    val startIndex = viewportState.second
    val endIndex = viewportState.third

    val activeCandle = remember(crosshairPosition, chartState.cursorMode, startIndex, visibleCount, chartCanvasWidthPx, candles) {
        if (chartState.cursorMode == CursorMode.CROSSHAIR && crosshairPosition != null && candles.isNotEmpty() && chartCanvasWidthPx > 0f) {
            val candleWidth = chartCanvasWidthPx / visibleCount
            val relIdx = (crosshairPosition!!.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
            val candleIdx = (startIndex + relIdx).coerceIn(0, candles.size - 1)
            candles.getOrNull(candleIdx) ?: candles.lastOrNull()
        } else {
            candles.lastOrNull()
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // ── Dedicated OHLCV Header Strip (Institutional style) ──
        OHLCVHeaderStrip(
            candle = activeCandle,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF131722))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            timeframe = timeframe
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        val rightMarginPx = with(density) { rightMarginDp.toPx() }
                        chartCanvasWidthPx = (size.width - rightMarginPx).coerceAtLeast(1f)
                    }
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerHoverIcon(if (chartState.cursorMode == CursorMode.CROSSHAIR) PointerIcon.Crosshair else PointerIcon.Hand)
                // 1. Pan and Zoom (enabled by default so users can always scroll historical candles)
                .pointerInput(chartState.activeDrawingTool, candles.size) {
                    detectTransformGestures { _, panAmount, zoomAmount, _ ->
                        zoom = (zoom * zoomAmount).coerceIn(0.15f, 6.0f)
                        if (chartState.activeDrawingTool == DrawingToolType.NONE || zoomAmount != 1f) {
                            val rightMarginPx = with(density) { rightMarginDp.toPx() }
                            val chartWidth = size.width - rightMarginPx
                            val vCount = (50f / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
                            val cWidth = if (vCount > 0) chartWidth / vCount else 10f
                            val localMaxScroll = (candles.size - vCount).coerceAtLeast(0).toFloat()
                            if (cWidth > 0f && chartState.activeDrawingTool == DrawingToolType.NONE) {
                                scrollOffsetFromRight = (scrollOffsetFromRight + panAmount.x / cWidth).coerceIn(0f, localMaxScroll)
                            }
                            crosshairPosition = null
                        }
                    }
                }
                // 1b. Drag gestures for drawing rubber-band preview & moving anchor points
                .pointerInput(chartState.activeDrawingTool, localSelectedDrawingId, candles.size) {
                    if (chartState.activeDrawingTool != DrawingToolType.NONE || localSelectedDrawingId != null) {
                        var draggedPointIndex by mutableStateOf<Int?>(null)
                        
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (chartState.activeDrawingTool == DrawingToolType.NONE && localSelectedDrawingId != null) {
                                    val drawing = chartState.drawings.find { it.id == localSelectedDrawingId }
                                    if (drawing != null) {
                                        val rightMarginPx = with(density) { rightMarginDp.toPx() }
                                        val chartWidth = size.width - rightMarginPx
                                        val panelHeight = size.height * panelHeightFraction
                                        val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
                                        val chartAreaHeight = size.height - panelHeight - bottomMarginPx
                                        
                                        val vCount = (50f / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
                                        val localMaxScroll = (candles.size - vCount).coerceAtLeast(0).toFloat()
                                        val localClampedScroll = scrollOffsetFromRight.coerceIn(0f, localMaxScroll)
                                        val localEndIdx = (candles.size - localClampedScroll.roundToInt()).coerceIn(vCount.coerceAtMost(candles.size), candles.size)
                                        val localStartIdx = (localEndIdx - vCount).coerceAtLeast(0)
                                        val tolerancePx = with(density) { 36.dp.toPx() }
                                        val rawMin = candles.subList(localStartIdx, localEndIdx).minOfOrNull { it.low } ?: 0.0
                                        val rawMax = candles.subList(localStartIdx, localEndIdx).maxOfOrNull { it.high } ?: 0.0
                                        val pricePadding = ((rawMax - rawMin) * 0.05).coerceAtLeast(0.01)
                                        val priceMin = rawMin - pricePadding
                                        val priceMax = rawMax + pricePadding
                                        val priceRange = (priceMax - priceMin).takeIf { it > 0 } ?: 1.0

                                        for ((index, point) in drawing.points.withIndex()) {
                                            var low = 0
                                            var high = candles.size - 1
                                            var closestIdx = -1
                                            while (low <= high) {
                                                val mid = (low + high) / 2
                                                val midTime = candles[mid].openTime
                                                if (midTime == point.timestamp) { closestIdx = mid; break }
                                                else if (midTime < point.timestamp) low = mid + 1
                                                else high = mid - 1
                                            }
                                            if (closestIdx == -1) {
                                                if (high < 0) closestIdx = 0
                                                else if (low >= candles.size) closestIdx = candles.size - 1
                                                else closestIdx = if ((point.timestamp - candles[high].openTime) <= (candles[low].openTime - point.timestamp)) high else low
                                            }
                                            
                                            val px = (closestIdx - localStartIdx).toFloat() * (chartWidth / vCount) + (chartWidth / vCount) / 2f
                                            val py = chartAreaHeight - ((point.price - priceMin) / priceRange * chartAreaHeight).toFloat()
                                            
                                            val dx = offset.x - px
                                            val dy = offset.y - py
                                            if (dx * dx + dy * dy <= tolerancePx * tolerancePx) {
                                                draggedPointIndex = index
                                                break
                                            }
                                        }
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val rightMarginPx = with(density) { rightMarginDp.toPx() }
                                val chartWidth = size.width - rightMarginPx
                                val panelHeight = size.height * panelHeightFraction
                                val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
                                val chartAreaHeight = size.height - panelHeight - bottomMarginPx

                                if (candles.isNotEmpty() && chartWidth > 0 && chartAreaHeight > 0) {
                                    val vCount = (50f / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
                                    val localMaxScroll = (candles.size - vCount).coerceAtLeast(0).toFloat()
                                    val localClampedScroll = scrollOffsetFromRight.coerceIn(0f, localMaxScroll)
                                    val localEndIdx = (candles.size - localClampedScroll.roundToInt()).coerceIn(vCount.coerceAtMost(candles.size), candles.size)
                                    val localStartIdx = (localEndIdx - vCount).coerceAtLeast(0)
                                    val visibleList = candles.subList(localStartIdx, localEndIdx)

                                    if (visibleList.isNotEmpty()) {
                                        val rawMin = visibleList.minOf { it.low }
                                        val rawMax = visibleList.maxOf { it.high }
                                        val pricePadding = ((rawMax - rawMin) * 0.05).coerceAtLeast(0.01)
                                        val priceMin = rawMin - pricePadding
                                        val priceMax = rawMax + pricePadding
                                        val priceRange = (priceMax - priceMin).takeIf { it > 0 } ?: 1.0
                                        val candleWidth = chartWidth / vCount
                                        val relativeIdx = (change.position.x / candleWidth).toInt().coerceIn(0, visibleList.size - 1)
                                        val candleIndex = (localStartIdx + relativeIdx).coerceIn(0, candles.size - 1)
                                        val price = priceMax - (change.position.y / chartAreaHeight) * priceRange
                                        val timestamp = candles.getOrNull(candleIndex)?.openTime ?: System.currentTimeMillis()
                                        
                                        if (chartState.activeDrawingTool != DrawingToolType.NONE) {
                                            currentRubberBandPoint = ChartPoint(timestamp, price)
                                        } else if (localSelectedDrawingId != null && draggedPointIndex != null) {
                                            onMoveDrawingPoint?.invoke(localSelectedDrawingId!!, draggedPointIndex!!, ChartPoint(timestamp, price))
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                currentRubberBandPoint = null
                                draggedPointIndex = null
                            },
                            onDragCancel = {
                                currentRubberBandPoint = null
                                draggedPointIndex = null
                            }
                        )
                    }
                }
                .pointerInput(chartState.activeDrawingTool, candles.size) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            crosshairPosition = offset
                        },
                        onTap = { offset ->
                            val rightMarginPx = with(density) { rightMarginDp.toPx() }
                            val chartWidth = size.width - rightMarginPx
                            val panelHeight = size.height * panelHeightFraction
                            val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
                            val chartAreaHeight = size.height - panelHeight - bottomMarginPx

                            if (candles.isNotEmpty() && chartWidth > 0 && chartAreaHeight > 0) {
                                val vCount = (50f / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
                                val localMaxScroll = (candles.size - vCount).coerceAtLeast(0).toFloat()
                                val localClampedScroll = scrollOffsetFromRight.coerceIn(0f, localMaxScroll)
                                val localEndIdx = (candles.size - localClampedScroll.roundToInt()).coerceIn(vCount.coerceAtMost(candles.size), candles.size)
                                val localStartIdx = (localEndIdx - vCount).coerceAtLeast(0)
                                val visibleList = candles.subList(localStartIdx, localEndIdx)

                                if (visibleList.isNotEmpty()) {
                                    val rawMin = visibleList.minOf { it.low }
                                    val rawMax = visibleList.maxOf { it.high }
                                    val pricePadding = ((rawMax - rawMin) * 0.05).coerceAtLeast(0.01)
                                    val priceMin = rawMin - pricePadding
                                    val priceMax = rawMax + pricePadding
                                    val priceRange = (priceMax - priceMin).takeIf { it > 0 } ?: 1.0
                                    val candleWidth = chartWidth / vCount
                                    val relativeIdx = (offset.x / candleWidth).toInt().coerceIn(0, visibleList.size - 1)
                                    val candleIndex = (localStartIdx + relativeIdx).coerceIn(0, candles.size - 1)
                                    val price = priceMax - (offset.y / chartAreaHeight) * priceRange
                                    val timestamp = candles.getOrNull(candleIndex)?.openTime ?: System.currentTimeMillis()

                                    if (chartState.activeDrawingTool != DrawingToolType.NONE && onAddDrawingPoint != null) {
                                        onAddDrawingPoint(ChartPoint(timestamp, price))
                                        currentRubberBandPoint = null
                                    } else if (chartState.cursorMode == CursorMode.CROSSHAIR) {
                                        crosshairPosition = offset
                                    } else {
                                        // Check if user tapped near an existing drawing
                                        val candleIntervalMs = if (candles.size > 1) candles[1].openTime - candles[0].openTime else 60000L
                                        val candleStartTime = candles.firstOrNull()?.openTime ?: 0L
                                        val tolerancePx = with(density) { 36.dp.toPx() }

                                        val tappedDrawing = findTappedDrawing(
                                            tap = offset,
                                            drawings = chartState.drawings,
                                            startIndex = localStartIdx,
                                            endIndex = localEndIdx,
                                            candles = candles,
                                            chartWidth = chartWidth,
                                            chartAreaHeight = chartAreaHeight,
                                            priceMin = priceMin,
                                            priceRange = priceRange,
                                            tolerancePx = tolerancePx
                                        )

                                        if (tappedDrawing != null) {
                                            localSelectedDrawingId = tappedDrawing.id
                                            selectedIndicatorType = null
                                            onSelectDrawing?.invoke(tappedDrawing.id)
                                        } else {
                                            // Check if user tapped near an active overlay indicator line
                                            val tappedIndicator = findTappedIndicator(
                                                tap = offset,
                                                indicators = chartState.overlayIndicators,
                                                indicatorResults = chartState.indicatorResults,
                                                startIndex = localStartIdx,
                                                endIndex = localEndIdx,
                                                chartWidth = chartWidth,
                                                chartAreaHeight = chartAreaHeight,
                                                priceMin = priceMin,
                                                priceRange = priceRange,
                                                tolerancePx = tolerancePx
                                            )

                                            if (tappedIndicator != null) {
                                                selectedIndicatorType = tappedIndicator.type
                                                localSelectedDrawingId = null
                                                onSelectDrawing?.invoke(null)
                                            } else {
                                                if (localSelectedDrawingId != null || selectedIndicatorType != null) {
                                                    localSelectedDrawingId = null
                                                    selectedIndicatorType = null
                                                    onSelectDrawing?.invoke(null)
                                                }
                                            }
                                        }
                                    }
                                }
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

            // ── Viewport calculation (Right-anchored by default) ──
            val visibleCandles = candles.subList(startIndex, endIndex)
            if (visibleCandles.isEmpty()) return@Canvas

            // Price range with padding
            var rawMin = visibleCandles.minOf { it.low }
            var rawMax = visibleCandles.maxOf { it.high }
            if (effectivePrice > 0.0) {
                rawMin = minOf(rawMin, effectivePrice)
                rawMax = maxOf(rawMax, effectivePrice)
            }

            // Include visible overlay indicators in bounds so they never clip
            if (chartState.hasOverlayIndicators) {
                for (config in chartState.overlayIndicators) {
                    when (val res = chartState.indicatorResults[config.type]) {
                        is List<*> -> {
                            for (i in startIndex until minOf(endIndex, res.size)) {
                                (res[i] as? Double)?.let { v ->
                                    if (v > 0.0) {
                                        rawMin = minOf(rawMin, v)
                                        rawMax = maxOf(rawMax, v)
                                    }
                                }
                            }
                        }
                        is BollingerResult -> {
                            for (i in startIndex until minOf(endIndex, res.upper.size, res.lower.size)) {
                                res.upper[i]?.let { rawMax = maxOf(rawMax, it) }
                                res.lower[i]?.let { rawMin = minOf(rawMin, it) }
                            }
                        }
                        is SupertrendResult -> {
                            for (i in startIndex until minOf(endIndex, res.values.size)) {
                                res.values[i]?.let {
                                    rawMin = minOf(rawMin, it)
                                    rawMax = maxOf(rawMax, it)
                                }
                            }
                        }
                    }
                }
            }

            val pricePadding = ((rawMax - rawMin) * 0.05).coerceAtLeast(0.01)
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
                    rightMarginPx, bottomMarginPx, textMeasurer,
                    timeframe = timeframe
                )
            }

            // ── Clip chart drawings strictly within the price chart area (prevents SMC/candles from bleeding into volume panel or axis) ──
            val chartAreaWidth = (size.width - rightMarginPx).coerceAtLeast(1f)
            clipRect(left = 0f, top = 0f, right = chartAreaWidth, bottom = chartAreaHeight) {
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

                // ── Layer 3: Smart Money Concepts (SMC Orderblocks & Sweeps) ──
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
                
                // ── Layer 4: Volume Bars ──
                drawVolumeLayer(
                    candles = candles,
                    visibleStartIndex = startIndex,
                    visibleEndIndex = endIndex,
                    chartAreaHeight = chartAreaHeight,
                    rightMargin = rightMarginPx,
                    volumeAreaHeight = volumeAreaHeight
                )

                // ── Layer 5: Candles ──
                drawCandleLayer(
                    candles, heikinAshiCandles, chartState.chartType,
                    startIndex, endIndex, priceMin, priceMax,
                    chartAreaHeight, rightMarginPx
                )

                // ── Layer 6: Indicator Overlays (EMA, VWAP, Bollinger, etc.) ──
                if (chartState.hasOverlayIndicators) {
                    drawIndicatorOverlayLayer(
                        indicators = chartState.overlayIndicators,
                        indicatorResults = chartState.indicatorResults,
                        candles = candles,
                        visibleStartIndex = startIndex,
                        visibleEndIndex = endIndex,
                        priceMin = priceMin,
                        priceMax = priceMax,
                        chartAreaHeight = chartAreaHeight,
                        rightMargin = rightMarginPx
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

                // ── Layer 8: User Drawings (with Selection & Live Rubber-Band Preview) ──
                if (chartState.drawings.isNotEmpty() || chartState.currentDrawing != null) {
                    drawDrawingLayer(
                        drawings = chartState.drawings,
                        currentDrawing = chartState.currentDrawing,
                        selectedDrawingId = localSelectedDrawingId,
                        rubberBandPoint = currentRubberBandPoint,
                        visibleStartIndex = startIndex,
                        visibleEndIndex = endIndex,
                        priceMin = priceMin,
                        priceMax = priceMax,
                        chartAreaHeight = chartAreaHeight,
                        rightMargin = rightMarginPx,
                        candles = candles
                    )
                }
            }

            // ── Layer 5b: Real-Time Current Price Guideline & Live Badge (Rendered on top, extending into right-axis) ──
            clipRect(left = 0f, top = 0f, right = size.width, bottom = chartAreaHeight) {
                drawCurrentPriceLineLayer(
                    candles = candles,
                    priceMin = priceMin,
                    priceMax = priceMax,
                    chartAreaHeight = chartAreaHeight,
                    rightMargin = rightMarginPx,
                    textMeasurer = textMeasurer,
                    timeframe = timeframe,
                    currentPriceOverride = if (effectivePrice > 0.0) effectivePrice else null
                )
            }

            // ── Layer 9: Crosshair ──
            if (chartState.cursorMode == CursorMode.CROSSHAIR) {
                drawCrosshairLayer(
                    crosshairPosition, candles, startIndex, endIndex,
                    priceMin, priceMax, chartAreaHeight, rightMarginPx,
                    textMeasurer,
                    timeframe = timeframe
                )
            }

            // ── Layer 10: Sub-Panel Indicators (Volume, RSI, MACD, Stochastic, ATR, CVD) ──
            val panelTopBase = chartAreaHeight + bottomMarginPx
            val singlePanelH = if (totalPanels > 0) panelHeight / totalPanels else 0f
            var currentPanelY = panelTopBase

            if (showVolumePanel) {
                drawVolumePanelLayer(
                    candles = candles,
                    visibleStartIndex = startIndex,
                    visibleEndIndex = endIndex,
                    panelTop = currentPanelY,
                    panelHeight = singlePanelH,
                    chartWidth = chartCanvasWidthPx,
                    rightMargin = rightMarginPx,
                    textMeasurer = textMeasurer
                )
                currentPanelY += singlePanelH
            }

            if (chartState.hasPanelIndicators) {
                val remainingPanelHeight = panelHeight - (if (showVolumePanel) singlePanelH else 0f)
                drawIndicatorPanelLayer(
                    chartState.panelIndicators, chartState.indicatorResults,
                    startIndex, endIndex,
                    currentPanelY, remainingPanelHeight,
                    rightMarginPx, textMeasurer
                )
            }
        }

        // ── Double Right Arrow "Scroll to Real-time" Button (60% Opacity) ──
        if (scrollOffsetFromRight > 3f) {
            Surface(
                onClick = { scrollOffsetFromRight = 0f },
                shape = CircleShape,
                color = Color(0xFF1E222D).copy(alpha = 0.60f),
                border = BorderStroke(1.dp, Color(0xFF4A5268).copy(alpha = 0.60f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
                    .size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "»",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ── Interactive Overlay Indicator Badges (Touch to customize or remove) ──
        if (chartState.overlayIndicators.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                chartState.overlayIndicators.forEach { config ->
                    val badgeColor = Color((config.color and 0xFFFFFFFFL).toInt())
                    val isSelected = selectedIndicatorType == config.type
                    Surface(
                        onClick = {
                            selectedIndicatorType = if (isSelected) null else config.type
                            localSelectedDrawingId = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF2A2E39) else Color(0xFF131722).copy(alpha = 0.88f),
                        border = BorderStroke(if (isSelected) 1.dp else 0.6.dp, badgeColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 7.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(badgeColor)
                            )
                            Text(
                                text = "${config.type.label} ${config.period}" + (if (config.multiplier != 1.0) " (${config.multiplier})" else ""),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = { onOpenIndicatorSettingsFor?.invoke(config.type) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Edit ${config.type.label}",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    onToggleIndicator?.invoke(config.type)
                                    if (selectedIndicatorType == config.type) selectedIndicatorType = null
                                },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete ${config.type.label}",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Active Drawing Banner Overlay ──
        if (chartState.activeDrawingTool != DrawingToolType.NONE) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E222D).copy(alpha = 0.95f),
                border = BorderStroke(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✏ Drawing: ${chartState.activeDrawingTool.label} (Tap on chart to place)",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ── Selected Indicator Floating Action Bar (Edit / Delete) ──
        if (selectedIndicatorConfig != null && chartState.activeDrawingTool == DrawingToolType.NONE) {
            val indColor = Color((selectedIndicatorConfig.color and 0xFFFFFFFFL).toInt())
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E222D).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, indColor),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(indColor)
                    )
                    Text(
                        text = "${selectedIndicatorConfig.type.label} (${selectedIndicatorConfig.period})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Edit Settings
                    TextButton(
                        onClick = {
                            onOpenIndicatorSettingsFor?.invoke(selectedIndicatorConfig.type)
                            selectedIndicatorType = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Edit Settings", tint = Color(0xFF00E676), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Edit", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            onToggleIndicator?.invoke(selectedIndicatorConfig.type)
                            selectedIndicatorType = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Indicator", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                    }

                    // Close Selection
                    IconButton(
                        onClick = { selectedIndicatorType = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // ── Selected Drawing Floating Actions Bar (Edit / Continue / Delete) ──
        if (selectedDrawing != null && chartState.activeDrawingTool == DrawingToolType.NONE) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E222D).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Selected: ${selectedDrawing.toolType.label}",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Continue Drawing
                    TextButton(
                        onClick = { onContinueDrawing?.invoke(selectedDrawing) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Continue", tint = Color(0xFF00E676), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Continue", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            onDeleteDrawing?.invoke(selectedDrawing.id)
                            localSelectedDrawingId = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                    }

                    // Close Selection
                    IconButton(
                        onClick = {
                            localSelectedDrawingId = null
                            onSelectDrawing?.invoke(null)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }

        // ── Institutional Sub-Pane Headers (Settings info, ⚙ Settings, ✕ Close) ──
        if (chartState.hasPanelIndicators) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalHeight = maxHeight
                val panelIndicators = chartState.panelIndicators
                val panelHeightDp = totalHeight * panelHeightFraction
                val chartAreaHeightDp = totalHeight - panelHeightDp - bottomMarginDp
                val panelTopDp = chartAreaHeightDp + bottomMarginDp
                val singlePanelHeightDp = panelHeightDp / panelIndicators.size

                panelIndicators.forEachIndexed { index, config ->
                    val topOffset = panelTopDp + singlePanelHeightDp * index
                    val color = Color((config.color and 0xFFFFFFFFL).toInt())

                    val settingsDesc = when (config.type) {
                        IndicatorType.RSI -> "RSI (${config.period})"
                        IndicatorType.MACD -> "MACD (${config.period}, ${config.secondaryPeriod}, 9)"
                        IndicatorType.STOCHASTIC -> "STOCH (${config.period}, ${config.secondaryPeriod})"
                        IndicatorType.ATR -> "ATR (${config.period})"
                        IndicatorType.CVD -> "CVD (${config.period})"
                        else -> "${config.type.label} (${config.period})"
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = rightMarginDp)
                            .offset(y = topOffset),
                        color = Color(0xFF131722).copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    onOpenIndicatorSettingsFor?.invoke(config.type)
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                )
                                Text(
                                    text = settingsDesc,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { onOpenIndicatorSettingsFor?.invoke(config.type) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "Edit ${config.type.label} settings",
                                        tint = Color(0xFFB0BEC5),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleIndicator?.invoke(config.type) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close ${config.type.label} sub-pane",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        }
    }
}

/**
 * Detects if a tap occurred near an active overlay indicator line on the chart.
 */
private fun findTappedIndicator(
    tap: Offset,
    indicators: List<IndicatorConfig>,
    indicatorResults: Map<IndicatorType, Any>,
    startIndex: Int,
    endIndex: Int,
    chartWidth: Float,
    chartAreaHeight: Float,
    priceMin: Double,
    priceRange: Double,
    tolerancePx: Float
): IndicatorConfig? {
    val visibleCount = endIndex - startIndex
    if (visibleCount <= 0 || priceRange <= 0.0 || chartWidth <= 0f) return null
    val candleWidth = chartWidth / visibleCount

    val localIdx = (tap.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
    val dataIdx = startIndex + localIdx

    fun pToY(price: Double): Float {
        return chartAreaHeight - ((price - priceMin) / priceRange * chartAreaHeight).toFloat()
    }

    for (config in indicators.reversed()) {
        if (!config.enabled || !config.type.isOverlay) continue
        val result = indicatorResults[config.type] ?: continue
        when (config.type) {
            IndicatorType.SMA, IndicatorType.EMA, IndicatorType.VWAP -> {
                @Suppress("UNCHECKED_CAST")
                val list = result as? List<Double?> ?: continue
                val p = list.getOrNull(dataIdx) ?: continue
                val y = pToY(p)
                if (abs(tap.y - y) <= tolerancePx) return config
            }
            IndicatorType.BOLLINGER_BANDS -> {
                val bb = result as? BollingerResult ?: continue
                val u = bb.upper.getOrNull(dataIdx)
                val m = bb.middle.getOrNull(dataIdx)
                val l = bb.lower.getOrNull(dataIdx)
                if (u != null && abs(tap.y - pToY(u)) <= tolerancePx) return config
                if (m != null && abs(tap.y - pToY(m)) <= tolerancePx) return config
                if (l != null && abs(tap.y - pToY(l)) <= tolerancePx) return config
            }
            IndicatorType.SUPERTREND -> {
                val st = result as? SupertrendResult ?: continue
                val p = st.values.getOrNull(dataIdx) ?: continue
                val y = pToY(p)
                if (abs(tap.y - y) <= tolerancePx) return config
            }
            IndicatorType.ICHIMOKU -> {
                val ich = result as? IchimokuResult ?: continue
                val lines = listOf(ich.tenkanSen, ich.kijunSen, ich.chikouSpan, ich.senkouSpanA, ich.senkouSpanB)
                for (line in lines) {
                    val p = line.getOrNull(dataIdx) ?: continue
                    if (abs(tap.y - pToY(p)) <= tolerancePx) return config
                }
            }
            else -> {}
        }
    }
    return null
}

/**
 * Calculates distance from tap point to existing drawings to support touch-selection.
 */
private fun findTappedDrawing(
    tap: Offset,
    drawings: List<DrawingData>,
    startIndex: Int,
    endIndex: Int,
    candles: List<Candle>,
    chartWidth: Float,
    chartAreaHeight: Float,
    priceMin: Double,
    priceRange: Double,
    tolerancePx: Float
): DrawingData? {
    val visibleCount = endIndex - startIndex
    if (visibleCount <= 0 || priceRange <= 0.0 || candles.isEmpty()) return null

    fun tToX(t: Long): Float {
        var low = 0
        var high = candles.size - 1
        var closestIdx = -1
        
        while (low <= high) {
            val mid = (low + high) / 2
            val midTime = candles[mid].openTime
            if (midTime == t) {
                closestIdx = mid
                break
            } else if (midTime < t) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        
        if (closestIdx == -1) {
            if (high < 0) closestIdx = 0
            else if (low >= candles.size) closestIdx = candles.size - 1
            else {
                val d1 = t - candles[high].openTime
                val d2 = candles[low].openTime - t
                closestIdx = if (d1 <= d2) high else low
            }
        }
        
        val localIndex = closestIdx - startIndex
        val candleWidth = chartWidth / visibleCount
        return localIndex * candleWidth + candleWidth / 2f
    }
    fun pToY(p: Double): Float {
        return chartAreaHeight - ((p - priceMin) / priceRange * chartAreaHeight).toFloat()
    }

    for (drawing in drawings.reversed()) {
        if (drawing.points.isEmpty()) continue
        when (drawing.toolType) {
            DrawingToolType.TRENDLINE, DrawingToolType.FIBONACCI -> {
                if (drawing.points.size >= 2) {
                    val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                    val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                    if (distToSegment(tap, p1, p2) <= tolerancePx) return drawing
                }
            }
            DrawingToolType.HORIZONTAL_LINE -> {
                val y = pToY(drawing.points[0].price)
                if (abs(tap.y - y) <= tolerancePx && tap.x in 0f..chartWidth) return drawing
            }
            DrawingToolType.RECTANGLE -> {
                if (drawing.points.size >= 2) {
                    val x1 = tToX(drawing.points[0].timestamp)
                    val y1 = pToY(drawing.points[0].price)
                    val x2 = tToX(drawing.points[1].timestamp)
                    val y2 = pToY(drawing.points[1].price)
                    val left = minOf(x1, x2) - tolerancePx
                    val right = maxOf(x1, x2) + tolerancePx
                    val top = minOf(y1, y2) - tolerancePx
                    val bottom = maxOf(y1, y2) + tolerancePx
                    if (tap.x in left..right && tap.y in top..bottom) return drawing
                }
            }
            DrawingToolType.CHANNEL -> {
                if (drawing.points.size >= 2) {
                    val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                    val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                    if (distToSegment(tap, p1, p2) <= tolerancePx) return drawing
                }
            }
            DrawingToolType.NONE -> {}
            else -> {}
        }
    }
    return null
}

private fun distToSegment(p: Offset, p1: Offset, p2: Offset): Float {
    val l2 = (p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)
    if (l2 == 0f) return (p - p1).getDistance()
    val t = (((p.x - p1.x) * (p2.x - p1.x) + (p.y - p1.y) * (p2.y - p1.y)) / l2).coerceIn(0f, 1f)
    val proj = Offset(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
    return (p - proj).getDistance()
}

/**
 * Dedicated OHLCV Header Strip positioned cleanly above the chart canvas.
 * Formats volume (e.g. 24.5K, 1.8M) with ample room so it is never truncated,
 * and leaves the chart canvas 100% unobstructed.
 */
@Composable
private fun OHLCVHeaderStrip(
    candle: Candle?,
    modifier: Modifier = Modifier,
    timeframe: String = "1D"
) {
    if (candle == null) return

    val isBullish = candle.close >= candle.open
    // Institutional dark-theme colors (TradingView standard)
    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)
    val candleColor = if (isBullish) greenColor else redColor
    val labelColor = Color(0xFF787B86)
    val valueColor = Color(0xFFD1D4DC)
    val volColor = Color(0xFF00E5FF)

    val isDailyOrHigher = timeframe.equals("1D", ignoreCase = true) ||
        timeframe.equals("D", ignoreCase = true) ||
        timeframe.equals("1W", ignoreCase = true) ||
        timeframe.equals("W", ignoreCase = true) ||
        timeframe.equals("1M", ignoreCase = true) ||
        timeframe.equals("M", ignoreCase = true) ||
        timeframe.contains("day", ignoreCase = true) ||
        timeframe.contains("week", ignoreCase = true) ||
        timeframe.contains("month", ignoreCase = true)

    val timeStr = remember(candle.openTime, isDailyOrHigher) {
        val rawTime = candle.openTime
        val timeMs = if (rawTime in 1..99_999_999_999L) rawTime * 1000L else rawTime
        val date = Date(timeMs)
        val cal = Calendar.getInstance().apply { time = date }
        val nowCal = Calendar.getInstance()
        val isCurYear = cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)

        if (isDailyOrHigher) {
            if (isCurYear) SimpleDateFormat("dd MMM", Locale.US).format(date)
            else SimpleDateFormat("dd MMM ''yy", Locale.US).format(date)
        } else {
            SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(date)
        }
    }
    val change = candle.close - candle.open
    val changePct = if (candle.open > 0) (change / candle.open) * 100.0 else 0.0
    val sign = if (change >= 0) "+" else ""

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Date badge
        Text(
            text = timeStr,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8B949E)
        )

        // Open
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("O", color = labelColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                text = formatCrosshairPrice(candle.open),
                color = valueColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // High
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("H", color = labelColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                text = formatCrosshairPrice(candle.high),
                color = greenColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Low
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("L", color = labelColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                text = formatCrosshairPrice(candle.low),
                color = redColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Close
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("C", color = labelColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                text = formatCrosshairPrice(candle.close),
                color = candleColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Volume (Prioritized & Compact so it fits on all screens)
        if (candle.volume > 0.0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Vol", color = labelColor, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(
                    text = formatVolume(candle.volume),
                    color = volColor,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Change & Change %
        Text(
            text = "$sign${"%.2f".format(change)} ($sign${"%.2f".format(changePct)}%)",
            color = candleColor,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

