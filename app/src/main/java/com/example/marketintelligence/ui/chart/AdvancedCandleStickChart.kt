package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
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
    onUpdateDrawingPoints: ((String, List<ChartPoint>) -> Unit)? = null,
    onUpdateDrawingText: ((String, String) -> Unit)? = null,
    onDrawingDragStart: (() -> Unit)? = null,
    onCancelDrawing: (() -> Unit)? = null,
    onOpenIndicatorSettingsFor: ((IndicatorType) -> Unit)? = null,
    onToggleIndicator: ((IndicatorType) -> Unit)? = null,
    onRemoveIndicator: ((IndicatorType) -> Unit)? = null,
    onRemoveIndicatorConfig: ((IndicatorConfig) -> Unit)? = null
) {
    val effectivePrice = when {
        currentPrice != null && currentPrice > 0.0 -> currentPrice
        chartState.candles.isNotEmpty() -> chartState.candles.last().close
        else -> 0.0
    }
    val candles = remember(chartState.candles, effectivePrice) {
        val list = chartState.candles.toMutableList()
        if (list.isNotEmpty() && effectivePrice > 0.0) {
            val last = list.last()
            list[list.lastIndex] = last.copy(
                close = effectivePrice,
                high = maxOf(last.high, effectivePrice),
                low = minOf(last.low, effectivePrice)
            )
        }
        list
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
    var showTextEditDialog by remember { mutableStateOf(false) }
    var editingTextDrawingId by remember { mutableStateOf<String?>(null) }
    var editingTextValue by remember { mutableStateOf("") }

    // When timeframe changes, anchor chart to real-time latest candles
    LaunchedEffect(timeframe) {
        scrollOffsetFromRight = 0f
    }

    // Pre-compute Heikin-Ashi candles if needed
    val heikinAshiCandles = remember(candles, chartState.chartType) {
        if (chartState.chartType == ChartType.HEIKIN_ASHI) candles.toHeikinAshi() else emptyList()
    }

    // Layout constants (Reduced side margin to 40.dp to maximize charting space)
    val rightMarginDp = 40.dp
    val bottomMarginDp = 22.dp
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

    // Whitespace bars configuration for natural left and right scrolling
    val defaultRightOffsetBars = 6
    val maxFutureOffsetBars = 35f
    val maxPastOffsetBars = 25f

    val viewportState by remember {
        derivedStateOf {
            val baseCount = if (chartCanvasWidthPx > 1800f) 75f else if (chartCanvasWidthPx > 1200f) 60f else 45f
            val vCount = (baseCount / zoom).roundToInt().coerceIn(16, (candles.size + 30).coerceAtLeast(16))
            val maxScroll = (candles.size - vCount + defaultRightOffsetBars + maxPastOffsetBars).coerceAtLeast(0f)
            val minScroll = -maxFutureOffsetBars
            val clampedScroll = scrollOffsetFromRight.coerceIn(minScroll, maxScroll)

            val eIndex = (candles.size + defaultRightOffsetBars - clampedScroll.roundToInt())
            val sIndex = eIndex - vCount
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
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        // ── Dedicated OHLCV Header Strip (Clean light theme) ──
        OHLCVHeaderStrip(
            candle = activeCandle,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            timeframe = timeframe
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val canvasGestureModifier = if (chartState.activeDrawingTool != DrawingToolType.NONE) {
                Modifier.pointerInput(chartState.activeDrawingTool, candles.size, chartState.currentDrawing?.points?.size) {
                    val rightMarginPx = with(density) { rightMarginDp.toPx() }
                    val panelHeight = size.height * panelHeightFraction
                    val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
                    val chartWidth = size.width - rightMarginPx
                    val chartAreaHeight = size.height - panelHeight - bottomMarginPx

                    fun offsetToChartPoint(pos: Offset): ChartPoint? {
                        if (candles.isEmpty() || chartWidth <= 0f || chartAreaHeight <= 0f) return null
                        val safeStart = startIndex.coerceIn(0, candles.size)
                        val safeEnd = endIndex.coerceIn(safeStart, candles.size)
                        val rawMin = if (safeEnd > safeStart) candles.subList(safeStart, safeEnd).minOfOrNull { it.low } ?: 0.0 else 0.0
                        val rawMax = if (safeEnd > safeStart) candles.subList(safeStart, safeEnd).maxOfOrNull { it.high } ?: 0.0 else 0.0
                        val pricePadding = ((rawMax - rawMin) * 0.05).coerceAtLeast(0.01)
                        val priceMin = rawMin - pricePadding
                        val priceMax = rawMax + pricePadding
                        val priceRange = (priceMax - priceMin).takeIf { it > 0 } ?: 1.0
                        val candleWidth = chartWidth / visibleCount
                        val relativeIdx = (pos.x / candleWidth).toInt().coerceIn(0, visibleCount - 1)
                        val candleIndex = (startIndex + relativeIdx).coerceIn(0, candles.size - 1)
                        val price = priceMax - (pos.y / chartAreaHeight) * priceRange
                        val timestamp = candles.getOrNull(candleIndex)?.openTime ?: System.currentTimeMillis()
                        return ChartPoint(timestamp, price)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downOffset = down.position
                        var movedPastSlop = false
                        var currentPt = offsetToChartPoint(downOffset)
                        val dragThreshold = with(density) { 4.dp.toPx() }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                change.consume()
                                if (movedPastSlop) {
                                    val isMultiPoint = chartState.activeDrawingTool !in listOf(
                                        DrawingToolType.HORIZONTAL_LINE,
                                        DrawingToolType.VERTICAL_LINE,
                                        DrawingToolType.TEXT_ANNOTATION
                                    )
                                    if (chartState.currentDrawing == null && isMultiPoint) {
                                        val startPt = offsetToChartPoint(downOffset)
                                        if (startPt != null) {
                                            onAddDrawingPoint?.invoke(startPt)
                                        }
                                    }
                                    if (currentPt != null) {
                                        onAddDrawingPoint?.invoke(currentPt)
                                    }
                                } else {
                                    val tapPt = offsetToChartPoint(change.position) ?: currentPt
                                    if (tapPt != null) {
                                        onAddDrawingPoint?.invoke(tapPt)
                                    }
                                }
                                currentRubberBandPoint = null
                                break
                            } else {
                                val dist = (change.position - downOffset).getDistance()
                                if (dist > dragThreshold) {
                                    movedPastSlop = true
                                    change.consume()
                                    val pt = offsetToChartPoint(change.position)
                                    if (pt != null) {
                                        currentPt = pt
                                        currentRubberBandPoint = pt
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Modifier.pointerInput(localSelectedDrawingId, candles.size, chartState.drawings, zoom) {
                    val rightMarginPx = with(density) { rightMarginDp.toPx() }
                    val panelHeight = size.height * panelHeightFraction
                    val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
                    val chartWidth = size.width - rightMarginPx
                    val chartAreaHeight = size.height - panelHeight - bottomMarginPx

                    val safeStart = startIndex.coerceIn(0, candles.size)
                    val safeEnd = endIndex.coerceIn(safeStart, candles.size)
                    val rawMin = if (safeEnd > safeStart) candles.subList(safeStart, safeEnd).minOfOrNull { it.low } ?: 0.0 else 0.0
                    val rawMax = if (safeEnd > safeStart) candles.subList(safeStart, safeEnd).maxOfOrNull { it.high } ?: 0.0 else 0.0
                    val rawRange = rawMax - rawMin
                    val minPadding = maxOf(rawMax * 0.005, 0.5)
                    val pricePadding = maxOf(rawRange * 0.08, minPadding)
                    val priceMin = rawMin - pricePadding
                    val priceMax = rawMax + pricePadding
                    val priceRange = (priceMax - priceMin).takeIf { it > 0 } ?: 1.0
                    val candleWidth = if (visibleCount > 0) chartWidth / visibleCount else 10f
                    val anchorTolerancePx = with(density) { 16.dp.toPx() }
                    val lineTolerancePx = with(density) { 10.dp.toPx() }
                    val panSlop = with(density) { 4.dp.toPx() }

                    val localMaxScroll = (candles.size - visibleCount + defaultRightOffsetBars + maxPastOffsetBars).coerceAtLeast(0f)
                    val localMinScroll = -maxFutureOffsetBars

                    fun tToX(t: Long): Float {
                        if (candles.isEmpty() || visibleCount <= 0) return 0f
                        var low = 0
                        var high = candles.size - 1
                        var closestIdx = -1
                        while (low <= high) {
                            val mid = (low + high) / 2
                            val midTime = candles[mid].openTime
                            if (midTime == t) { closestIdx = mid; break }
                            else if (midTime < t) low = mid + 1
                            else high = mid - 1
                        }
                        val candleIndex: Float = if (closestIdx != -1) {
                            closestIdx.toFloat()
                        } else {
                            if (t < candles.first().openTime) {
                                val interval = if (candles.size > 1) (candles[1].openTime - candles[0].openTime).coerceAtLeast(1000L) else 60000L
                                val offsetBars = (candles.first().openTime - t).toFloat() / interval
                                -offsetBars
                            } else if (t > candles.last().openTime) {
                                val interval = if (candles.size > 1) (candles.last().openTime - candles[candles.size - 2].openTime).coerceAtLeast(1000L) else 60000L
                                val offsetBars = (t - candles.last().openTime).toFloat() / interval
                                (candles.size - 1) + offsetBars
                            } else {
                                val tLow = candles[high].openTime
                                val tHigh = candles[low].openTime
                                val frac = if (tHigh > tLow) (t - tLow).toFloat() / (tHigh - tLow) else 0f
                                high.toFloat() + frac
                            }
                        }
                        val localIndex = candleIndex - startIndex
                        return localIndex * candleWidth + candleWidth / 2f
                    }

                    fun xToT(x: Float): Long {
                        if (candles.isEmpty() || visibleCount <= 0) return System.currentTimeMillis()
                        val localIndex = (x - candleWidth / 2f) / candleWidth
                        val candleIndex = startIndex + localIndex
                        val roundedIdx = candleIndex.roundToInt()
                        return if (roundedIdx in 0 until candles.size) {
                            candles[roundedIdx].openTime
                        } else if (roundedIdx >= candles.size) {
                            val interval = if (candles.size > 1) (candles.last().openTime - candles[candles.size - 2].openTime).coerceAtLeast(1000L) else 60000L
                            candles.last().openTime + ((roundedIdx - (candles.size - 1)) * interval)
                        } else {
                            val interval = if (candles.size > 1) (candles[1].openTime - candles[0].openTime).coerceAtLeast(1000L) else 60000L
                            candles.first().openTime + (roundedIdx * interval)
                        }
                    }

                    fun pToY(p: Double): Float {
                        if (priceRange <= 0.0) return chartAreaHeight / 2f
                        return chartAreaHeight - ((p - priceMin) / priceRange * chartAreaHeight).toFloat()
                    }

                    fun yToP(y: Float): Double {
                        if (chartAreaHeight <= 0f) return priceMin
                        return priceMax - (y / chartAreaHeight) * priceRange
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startPos = down.position
                        var movedPastSlop = false
                        var prevPos = startPos

                        var activeTargetDrawingId = localSelectedDrawingId
                        val curSelected = chartState.drawings.find { it.id == activeTargetDrawingId }

                        var draggedAnchorIndex: Int? = null
                        var isWholeDrawingDrag = false
                        var initialPoints: List<ChartPoint> = emptyList()

                        if (curSelected != null) {
                            val anchorIdx = findTappedAnchor(curSelected, startPos, anchorTolerancePx, ::tToX, ::pToY, chartWidth, chartAreaHeight)
                            if (anchorIdx != null) {
                                draggedAnchorIndex = anchorIdx
                                initialPoints = curSelected.points
                            } else if (isPointOnDrawing(curSelected, startPos, lineTolerancePx, ::tToX, ::pToY, chartWidth, chartAreaHeight)) {
                                isWholeDrawingDrag = true
                                initialPoints = curSelected.points
                            } else {
                                val other = findTappedDrawing(startPos, chartState.drawings, lineTolerancePx, ::tToX, ::pToY, chartWidth, chartAreaHeight)
                                if (other != null) {
                                    activeTargetDrawingId = other.id
                                    isWholeDrawingDrag = true
                                    initialPoints = other.points
                                }
                            }
                        } else {
                            val tapped = findTappedDrawing(startPos, chartState.drawings, lineTolerancePx, ::tToX, ::pToY, chartWidth, chartAreaHeight)
                            if (tapped != null) {
                                activeTargetDrawingId = tapped.id
                                isWholeDrawingDrag = true
                                initialPoints = tapped.points
                            }
                        }

                        var prevPinchDist = 0f
                        var prevPinchCenter = Offset.Zero
                        var hasDragStarted = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }

                            if (pressedPointers.size >= 2) {
                                val p0 = pressedPointers[0].position
                                val p1 = pressedPointers[1].position
                                val currentPinchDist = (p0 - p1).getDistance()
                                val currentPinchCenter = (p0 + p1) / 2f

                                if (prevPinchDist > 0f && currentPinchDist > 0f) {
                                    val scale = currentPinchDist / prevPinchDist
                                    zoom = (zoom * scale).coerceIn(0.15f, 6.0f)
                                }
                                if (prevPinchCenter != Offset.Zero && candleWidth > 0f) {
                                    val panX = currentPinchCenter.x - prevPinchCenter.x
                                    scrollOffsetFromRight = (scrollOffsetFromRight + panX / candleWidth).coerceIn(localMinScroll, localMaxScroll)
                                }
                                prevPinchDist = currentPinchDist
                                prevPinchCenter = currentPinchCenter
                                crosshairPosition = null
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull() ?: break

                            if (!change.pressed) {
                                if (!movedPastSlop) {
                                    change.consume()
                                    val tapPos = change.position
                                    val tappedDrawing = findTappedDrawing(tapPos, chartState.drawings, lineTolerancePx, ::tToX, ::pToY, chartWidth, chartAreaHeight)
                                    if (tappedDrawing != null) {
                                        if (localSelectedDrawingId == tappedDrawing.id && tappedDrawing.toolType == DrawingToolType.TEXT_ANNOTATION) {
                                            editingTextDrawingId = tappedDrawing.id
                                            editingTextValue = tappedDrawing.text
                                            showTextEditDialog = true
                                        }
                                        localSelectedDrawingId = tappedDrawing.id
                                        selectedIndicatorType = null
                                        onSelectDrawing?.invoke(tappedDrawing.id)
                                    } else {
                                        val tappedIndicator = findTappedIndicator(
                                            tap = tapPos,
                                            indicators = chartState.overlayIndicators,
                                            indicatorResults = chartState.indicatorResults,
                                            startIndex = startIndex,
                                            endIndex = endIndex,
                                            chartWidth = chartWidth,
                                            chartAreaHeight = chartAreaHeight,
                                            priceMin = priceMin,
                                            priceRange = priceRange,
                                            tolerancePx = lineTolerancePx
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
                                            } else if (chartState.cursorMode == CursorMode.CROSSHAIR) {
                                                crosshairPosition = tapPos
                                            } else {
                                                crosshairPosition = null
                                            }
                                        }
                                    }
                                }
                                break
                            } else {
                                val curPos = change.position
                                val totalDist = (curPos - startPos).getDistance()
                                if (!movedPastSlop && totalDist > panSlop) {
                                    movedPastSlop = true
                                    if (activeTargetDrawingId != null && (draggedAnchorIndex != null || isWholeDrawingDrag)) {
                                        if (activeTargetDrawingId != localSelectedDrawingId) {
                                            localSelectedDrawingId = activeTargetDrawingId
                                            onSelectDrawing?.invoke(activeTargetDrawingId)
                                        }
                                        if (!hasDragStarted) {
                                            hasDragStarted = true
                                            onDrawingDragStart?.invoke()
                                        }
                                    } else {
                                        // Chart navigation pan: apply initial displacement so zero movement is discarded
                                        val initialDx = curPos.x - startPos.x
                                        if (candleWidth > 0f) {
                                            scrollOffsetFromRight = (scrollOffsetFromRight + initialDx / candleWidth).coerceIn(localMinScroll, localMaxScroll)
                                        }
                                        if (chartState.cursorMode == CursorMode.CROSSHAIR) {
                                            crosshairPosition = curPos
                                        } else {
                                            crosshairPosition = null
                                        }
                                    }
                                    prevPos = curPos
                                }

                                if (movedPastSlop) {
                                    change.consume()
                                    if (activeTargetDrawingId != null && draggedAnchorIndex != null) {
                                        val targetDrawing = chartState.drawings.find { it.id == activeTargetDrawingId }
                                        if (targetDrawing != null) {
                                            when (targetDrawing.toolType) {
                                                DrawingToolType.HORIZONTAL_LINE -> {
                                                    val newPrice = yToP(curPos.y)
                                                    val t = targetDrawing.points.firstOrNull()?.timestamp ?: xToT(curPos.x)
                                                    onMoveDrawingPoint?.invoke(activeTargetDrawingId, 0, ChartPoint(t, newPrice))
                                                }
                                                DrawingToolType.VERTICAL_LINE -> {
                                                    val newTime = xToT(curPos.x)
                                                    val p = targetDrawing.points.firstOrNull()?.price ?: yToP(curPos.y)
                                                    onMoveDrawingPoint?.invoke(activeTargetDrawingId, 0, ChartPoint(newTime, p))
                                                }
                                                DrawingToolType.CHANNEL -> {
                                                    val p0 = targetDrawing.points.getOrNull(0) ?: ChartPoint(xToT(curPos.x), yToP(curPos.y))
                                                    val p1 = targetDrawing.points.getOrNull(1) ?: p0
                                                    val p2 = targetDrawing.points.getOrNull(2) ?: p0
                                                    val priceOffset = p2.price - p0.price
                                                    when (draggedAnchorIndex) {
                                                        0 -> {
                                                            val newP0 = ChartPoint(xToT(curPos.x), yToP(curPos.y))
                                                            val newP2 = ChartPoint(newP0.timestamp, newP0.price + priceOffset)
                                                            onUpdateDrawingPoints?.invoke(activeTargetDrawingId, listOf(newP0, p1, newP2))
                                                        }
                                                        1 -> {
                                                            val newP1 = ChartPoint(xToT(curPos.x), yToP(curPos.y))
                                                            onUpdateDrawingPoints?.invoke(activeTargetDrawingId, listOf(p0, newP1, p2))
                                                        }
                                                        2 -> {
                                                            val currentPrice = yToP(curPos.y)
                                                            val basePrice = (p0.price + p1.price) / 2.0
                                                            val newOffset = currentPrice - basePrice
                                                            val newP2 = ChartPoint(p0.timestamp, p0.price + newOffset)
                                                            onUpdateDrawingPoints?.invoke(activeTargetDrawingId, listOf(p0, p1, newP2))
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    val newPt = ChartPoint(xToT(curPos.x), yToP(curPos.y))
                                                    onMoveDrawingPoint?.invoke(activeTargetDrawingId, draggedAnchorIndex!!, newPt)
                                                }
                                            }
                                        }
                                    } else if (activeTargetDrawingId != null && isWholeDrawingDrag && initialPoints.isNotEmpty()) {
                                        val dx = curPos.x - startPos.x
                                        val dy = curPos.y - startPos.y
                                        val newPoints = initialPoints.map { pt ->
                                            val startX = tToX(pt.timestamp)
                                            val startY = pToY(pt.price)
                                            val newX = startX + dx
                                            val newY = startY + dy
                                            ChartPoint(xToT(newX), yToP(newY))
                                        }
                                        onUpdateDrawingPoints?.invoke(activeTargetDrawingId, newPoints)
                                    } else {
                                        val dx = curPos.x - prevPos.x
                                        if (candleWidth > 0f) {
                                            scrollOffsetFromRight = (scrollOffsetFromRight + dx / candleWidth).coerceIn(localMinScroll, localMaxScroll)
                                        }
                                        if (chartState.cursorMode == CursorMode.CROSSHAIR) {
                                            crosshairPosition = curPos
                                        } else {
                                            crosshairPosition = null
                                        }
                                    }
                                    prevPos = curPos
                                }
                            }
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        val rightMarginPx = with(density) { rightMarginDp.toPx() }
                        chartCanvasWidthPx = (size.width - rightMarginPx).coerceAtLeast(1f)
                    }
                    .background(Color.White)
                    .pointerHoverIcon(if (chartState.cursorMode == CursorMode.CROSSHAIR) PointerIcon.Crosshair else PointerIcon.Hand)
                    .pointerInput(chartState.cursorMode) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (chartState.cursorMode == CursorMode.CROSSHAIR) {
                                    if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) {
                                        event.changes.firstOrNull()?.position?.let { pos ->
                                            crosshairPosition = pos
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .then(canvasGestureModifier)
            ) {
            if (candles.isEmpty()) return@Canvas

            val rightMarginPx = rightMarginDp.toPx()
            val bottomMarginPx = bottomMarginDp.toPx()
            val chartWidth = size.width - rightMarginPx
            val panelHeight = size.height * panelHeightFraction
            val chartAreaHeight = size.height - panelHeight - bottomMarginPx

            if (chartWidth <= 0 || chartAreaHeight <= 0) return@Canvas

            // ── Viewport calculation (Right-anchored with whitespace by default) ──
            val safeStart = startIndex.coerceIn(0, candles.size)
            val safeEnd = endIndex.coerceIn(safeStart, candles.size)
            val visibleCandles = if (safeEnd > safeStart) candles.subList(safeStart, safeEnd) else emptyList()
            if (visibleCandles.isEmpty()) return@Canvas

            // Price range computed strictly from visible candles in viewport
            var rawMin = visibleCandles.minOf { it.low }
            var rawMax = visibleCandles.maxOf { it.high }

            // Include real-time price in bounds ONLY if the latest candle is visible in the viewport
            val isLatestCandleVisible = safeEnd >= candles.size
            if (isLatestCandleVisible && effectivePrice > 0.0) {
                val isRealisticPrice = effectivePrice in (rawMin * 0.75)..(rawMax * 1.25)
                if (isRealisticPrice) {
                    rawMin = minOf(rawMin, effectivePrice)
                    rawMax = maxOf(rawMax, effectivePrice)
                }
            }

            // Include visible overlay indicators in bounds only if reasonably close to price
            if (chartState.hasOverlayIndicators) {
                val candleRange = (rawMax - rawMin).coerceAtLeast(1.0)
                val allowedMargin = maxOf(candleRange * 0.25, rawMax * 0.01)
                val allowedMin = rawMin - allowedMargin
                val allowedMax = rawMax + allowedMargin
                for (config in chartState.overlayIndicators) {
                    when (val res = chartState.getIndicatorResult(config)) {
                        is List<*> -> {
                            val loopStart = safeStart.coerceAtMost(res.size)
                            val loopEnd = safeEnd.coerceAtMost(res.size)
                            for (i in loopStart until loopEnd) {
                                (res[i] as? Double)?.let { v ->
                                    if (v in allowedMin..allowedMax) {
                                        rawMin = minOf(rawMin, v)
                                        rawMax = maxOf(rawMax, v)
                                    }
                                }
                            }
                        }
                        is BollingerResult -> {
                            val maxLimit = minOf(res.upper.size, res.lower.size)
                            val loopStart = safeStart.coerceAtMost(maxLimit)
                            val loopEnd = safeEnd.coerceAtMost(maxLimit)
                            for (i in loopStart until loopEnd) {
                                res.upper[i]?.let { if (it in allowedMin..allowedMax) rawMax = maxOf(rawMax, it) }
                                res.lower[i]?.let { if (it in allowedMin..allowedMax) rawMin = minOf(rawMin, it) }
                            }
                        }
                        is SupertrendResult -> {
                            val loopStart = safeStart.coerceAtMost(res.values.size)
                            val loopEnd = safeEnd.coerceAtMost(res.values.size)
                            for (i in loopStart until loopEnd) {
                                res.values[i]?.let {
                                    if (it in allowedMin..allowedMax) {
                                        rawMin = minOf(rawMin, it)
                                        rawMax = maxOf(rawMax, it)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Guarantee minimum breathing room (at least 8% padding or 0.5% of price, ensuring clear candle visibility)
            val rawRange = rawMax - rawMin
            val minPadding = maxOf(rawMax * 0.005, 0.5)
            val pricePadding = maxOf(rawRange * 0.08, minPadding)
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
                    timeframe = timeframe,
                    isDarkTheme = false
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
                
                // ── Layer 4: Volume Bars (only shown in overlay mode when sub-panel is disabled) ──
                if (!showVolumePanel && chartState.showVolume) {
                    drawVolumeLayer(
                        candles = candles,
                        visibleStartIndex = startIndex,
                        visibleEndIndex = endIndex,
                        chartAreaHeight = chartAreaHeight,
                        rightMargin = rightMarginPx,
                        volumeAreaHeight = volumeAreaHeight
                    )
                }

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

        // ── Double Right Arrow "Scroll to Real-time" Button ──
        if (kotlin.math.abs(scrollOffsetFromRight) > 2f) {
            Surface(
                onClick = { scrollOffsetFromRight = 0f },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color(0xFFD0D5DD)),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 24.dp)
                    .size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "»",
                        color = Color(0xFF344054),
                        fontSize = 18.sp,
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
                                    if (onRemoveIndicatorConfig != null) {
                                        onRemoveIndicatorConfig(config)
                                    } else {
                                        onToggleIndicator?.invoke(config.type)
                                    }
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
            val stepHint = when (chartState.activeDrawingTool) {
                DrawingToolType.HORIZONTAL_LINE -> "Tap or drag on chart to place price line"
                DrawingToolType.VERTICAL_LINE -> "Tap or drag on chart to place time line"
                DrawingToolType.CHANNEL, DrawingToolType.PITCHFORK, DrawingToolType.FIBONACCI_EXTENSION -> {
                    when (chartState.currentDrawing?.points?.size ?: 0) {
                        0 -> "Step 1/3: Tap first anchor point"
                        1 -> "Step 2/3: Tap second baseline point"
                        else -> "Step 3/3: Tap channel / parallel offset"
                    }
                }
                else -> {
                    if (chartState.currentDrawing == null || chartState.currentDrawing.points.isEmpty()) {
                        "Step 1/2: Tap or drag to start"
                    } else {
                        "Step 2/2: Tap or drag to finish"
                    }
                }
            }

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
                        text = "✏ ${chartState.activeDrawingTool.label} • $stepHint",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (onCancelDrawing != null) {
                        Surface(
                            onClick = onCancelDrawing,
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, Color(0xFF4CAF50))
                        ) {
                            Text(
                                text = "Done",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = onCancelDrawing,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Drawing",
                                tint = Color(0xFFEF5350),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
                            if (onRemoveIndicatorConfig != null) {
                                onRemoveIndicatorConfig.invoke(selectedIndicatorConfig)
                            } else if (onRemoveIndicator != null) {
                                onRemoveIndicator.invoke(selectedIndicatorConfig.type)
                            } else {
                                onToggleIndicator?.invoke(selectedIndicatorConfig.type)
                            }
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
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E222D).copy(alpha = 0.96f),
                border = BorderStroke(1.5.dp, Color(0xFF00E5FF)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color((selectedDrawing.color and 0xFFFFFFFFL).toInt()))
                    )
                    Text(
                        text = selectedDrawing.toolType.label,
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Continue Drawing
                    if (!selectedDrawing.isComplete && selectedDrawing.toolType !in listOf(
                        DrawingToolType.HORIZONTAL_LINE,
                        DrawingToolType.VERTICAL_LINE,
                        DrawingToolType.HORIZONTAL_RAY,
                        DrawingToolType.TEXT_ANNOTATION
                    )) {
                        TextButton(
                            onClick = { onContinueDrawing?.invoke(selectedDrawing) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Continue", tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Continue", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Edit Note for Text Annotation
                    if (selectedDrawing.toolType == DrawingToolType.TEXT_ANNOTATION) {
                        TextButton(
                            onClick = {
                                editingTextDrawingId = selectedDrawing.id
                                editingTextValue = selectedDrawing.text
                                showTextEditDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Edit Note", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Delete Button
                    Button(
                        onClick = {
                            val idToDelete = selectedDrawing.id
                            localSelectedDrawingId = null
                            onSelectDrawing?.invoke(null)
                            onDeleteDrawing?.invoke(idToDelete)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Deselect / Close Button
                    IconButton(
                        onClick = {
                            localSelectedDrawingId = null
                            onSelectDrawing?.invoke(null)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color.LightGray, modifier = Modifier.size(16.dp))
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

        // ── Text Annotation Edit Dialog ──
        if (showTextEditDialog && editingTextDrawingId != null) {
            AlertDialog(
                onDismissRequest = { showTextEditDialog = false },
                title = { Text("Edit Note Annotation", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = editingTextValue,
                        onValueChange = { editingTextValue = it },
                        label = { Text("Note text") },
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF00E5FF),
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = editingTextDrawingId
                            if (id != null) {
                                onUpdateDrawingText?.invoke(id, editingTextValue)
                            }
                            showTextEditDialog = false
                        }
                    ) {
                        Text("Save", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTextEditDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E222D)
            )
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
    indicatorResults: Map<String, Any>,
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
        val result = indicatorResults[config.id] ?: indicatorResults[config.type.name] ?: continue
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

internal fun isPointInPolygon(p: Offset, poly: List<Offset>): Boolean {
    if (poly.size < 3) return false
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val pi = poly[i]
        val pj = poly[j]
        if (((pi.y > p.y) != (pj.y > p.y)) &&
            (p.x < (pj.x - pi.x) * (p.y - pi.y) / (pj.y - pi.y) + pi.x)) {
            inside = !inside
        }
        j = i
    }
    return inside
}

internal fun distToSegment(p: Offset, p1: Offset, p2: Offset): Float {
    val l2 = (p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y)
    if (l2 == 0f) return (p - p1).getDistance()
    val t = (((p.x - p1.x) * (p2.x - p1.x) + (p.y - p1.y) * (p2.y - p1.y)) / l2).coerceIn(0f, 1f)
    val proj = Offset(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
    return (p - proj).getDistance()
}

internal fun findTappedAnchor(
    drawing: DrawingData,
    pos: Offset,
    tolerancePx: Float,
    tToX: (Long) -> Float,
    pToY: (Double) -> Float,
    chartWidth: Float,
    chartAreaHeight: Float
): Int? {
    if (drawing.points.isEmpty()) return null
    when (drawing.toolType) {
        DrawingToolType.HORIZONTAL_LINE -> {
            val y = pToY(drawing.points[0].price)
            if (abs(pos.y - y) <= tolerancePx && pos.x in 0f..chartWidth) {
                return 0
            }
        }
        DrawingToolType.VERTICAL_LINE -> {
            val x = tToX(drawing.points[0].timestamp)
            if (abs(pos.x - x) <= tolerancePx && pos.y in 0f..chartAreaHeight) {
                return 0
            }
        }
        DrawingToolType.CHANNEL -> {
            if (drawing.points.size >= 2) {
                val p0 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p1 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                if ((pos - p0).getDistance() <= tolerancePx) return 0
                if ((pos - p1).getDistance() <= tolerancePx) return 1
                if (drawing.points.size >= 3) {
                    val priceOffset = drawing.points[2].price - drawing.points[0].price
                    val y0p = pToY(drawing.points[0].price + priceOffset)
                    val y1p = pToY(drawing.points[1].price + priceOffset)
                    val pMid = Offset((p0.x + p1.x) / 2f, (y0p + y1p) / 2f)
                    val p0p = Offset(p0.x, y0p)
                    val p1p = Offset(p1.x, y1p)
                    if ((pos - pMid).getDistance() <= tolerancePx ||
                        (pos - p0p).getDistance() <= tolerancePx ||
                        (pos - p1p).getDistance() <= tolerancePx) {
                        return 2
                    }
                }
            }
        }
        DrawingToolType.FIBONACCI -> {
            if (drawing.points.size >= 2) {
                val p0 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p1 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                if ((pos - p0).getDistance() <= tolerancePx) return 0
                if ((pos - p1).getDistance() <= tolerancePx) return 1
            }
        }
        else -> {
            for ((index, pt) in drawing.points.withIndex()) {
                val p = Offset(tToX(pt.timestamp), pToY(pt.price))
                if ((pos - p).getDistance() <= tolerancePx) return index
            }
        }
    }
    return null
}

internal fun isPointOnDrawing(
    drawing: DrawingData,
    pos: Offset,
    tolerancePx: Float,
    tToX: (Long) -> Float,
    pToY: (Double) -> Float,
    chartWidth: Float,
    chartAreaHeight: Float
): Boolean {
    if (drawing.points.isEmpty()) return false
    when (drawing.toolType) {
        DrawingToolType.HORIZONTAL_LINE -> {
            val y = pToY(drawing.points[0].price)
            return abs(pos.y - y) <= tolerancePx && pos.x in 0f..chartWidth
        }
        DrawingToolType.VERTICAL_LINE -> {
            val x = tToX(drawing.points[0].timestamp)
            return abs(pos.x - x) <= tolerancePx && pos.y in 0f..chartAreaHeight
        }
        DrawingToolType.TRENDLINE -> {
            if (drawing.points.size >= 2) {
                val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                return distToSegment(pos, p1, p2) <= tolerancePx
            }
        }
        DrawingToolType.CHANNEL -> {
            if (drawing.points.size >= 2) {
                val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                if (distToSegment(pos, p1, p2) <= tolerancePx) return true
                if (drawing.points.size >= 3) {
                    val priceOffset = drawing.points[2].price - drawing.points[0].price
                    val p1p = Offset(p1.x, pToY(drawing.points[0].price + priceOffset))
                    val p2p = Offset(p2.x, pToY(drawing.points[1].price + priceOffset))
                    if (distToSegment(pos, p1p, p2p) <= tolerancePx) return true
                    val p1m = Offset(p1.x, (p1.y + p1p.y) / 2f)
                    val p2m = Offset(p2.x, (p2.y + p2p.y) / 2f)
                    if (distToSegment(pos, p1m, p2m) <= tolerancePx) return true
                    if (isPointInPolygon(pos, listOf(p1, p2, p2p, p1p))) return true
                }
            }
        }
        DrawingToolType.FIBONACCI -> {
            if (drawing.points.size >= 2) {
                val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                if (distToSegment(pos, p1, p2) <= tolerancePx) return true
                val highPrice = maxOf(drawing.points[0].price, drawing.points[1].price)
                val lowPrice = minOf(drawing.points[0].price, drawing.points[1].price)
                val range = highPrice - lowPrice
                val fibLevels = listOf(0.0, 0.236, 0.382, 0.5, 0.618, 0.786, 1.0)
                for (lvl in fibLevels) {
                    val lvlPrice = highPrice - range * lvl
                    val yLvl = pToY(lvlPrice)
                    if (abs(pos.y - yLvl) <= tolerancePx && pos.x in 0f..chartWidth) return true
                }
                val y382 = pToY(highPrice - range * 0.382)
                val y618 = pToY(highPrice - range * 0.618)
                val topZone = minOf(y382, y618)
                val bottomZone = maxOf(y382, y618)
                if (pos.y in topZone..bottomZone && pos.x in 0f..chartWidth) return true
            }
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
                return pos.x in left..right && pos.y in top..bottom
            }
        }
        DrawingToolType.PRICE_RANGE -> {
            if (drawing.points.size >= 2) {
                val x1 = tToX(drawing.points[0].timestamp)
                val y1 = pToY(drawing.points[0].price)
                val x2 = tToX(drawing.points[1].timestamp)
                val y2 = pToY(drawing.points[1].price)
                val left = minOf(x1, x2) - tolerancePx
                val right = maxOf(x1, x2) + tolerancePx
                val top = minOf(y1, y2) - tolerancePx
                val bottom = maxOf(y1, y2) + tolerancePx
                return pos.x in left..right && pos.y in top..bottom
            }
        }
        DrawingToolType.ARROW -> {
            if (drawing.points.size >= 2) {
                val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                return distToSegment(pos, p1, p2) <= tolerancePx
            }
        }
        DrawingToolType.RAY -> {
            if (drawing.points.size >= 2) {
                val p1 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p2 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                val dx = p2.x - p1.x
                val dy = p2.y - p1.y
                val length = kotlin.math.hypot(dx, dy)
                if (length > 0f) {
                    val multiplier = 10000f / length
                    val rayEnd = Offset(p1.x + dx * multiplier, p1.y + dy * multiplier)
                    return distToSegment(pos, p1, rayEnd) <= tolerancePx
                }
            }
        }
        DrawingToolType.HORIZONTAL_RAY -> {
            if (drawing.points.isNotEmpty()) {
                val x1 = tToX(drawing.points[0].timestamp)
                val y = pToY(drawing.points[0].price)
                return abs(pos.y - y) <= tolerancePx && pos.x in (x1 - tolerancePx)..chartWidth
            }
        }
        DrawingToolType.FIBONACCI_EXTENSION -> {
            if (drawing.points.size >= 2) {
                for (i in 0 until drawing.points.size - 1) {
                    val p1 = Offset(tToX(drawing.points[i].timestamp), pToY(drawing.points[i].price))
                    val p2 = Offset(tToX(drawing.points[i + 1].timestamp), pToY(drawing.points[i + 1].price))
                    if (distToSegment(pos, p1, p2) <= tolerancePx) return true
                }
            }
        }
        DrawingToolType.PITCHFORK -> {
            if (drawing.points.size >= 3) {
                val p0 = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
                val p1 = Offset(tToX(drawing.points[1].timestamp), pToY(drawing.points[1].price))
                val p2 = Offset(tToX(drawing.points[2].timestamp), pToY(drawing.points[2].price))
                val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
                return distToSegment(pos, p0, mid) <= tolerancePx || distToSegment(pos, p1, p2) <= tolerancePx
            }
        }
        DrawingToolType.TEXT_ANNOTATION -> {
            val p = Offset(tToX(drawing.points[0].timestamp), pToY(drawing.points[0].price))
            val text = drawing.text.ifBlank { "Annotation" }
            val estWidth = text.length * 16f + 28f
            val boxLeft = (p.x - estWidth / 2f).coerceIn(4f, (chartWidth - estWidth - 4f).coerceAtLeast(4f))
            val boxTop = (p.y - 44f).coerceIn(4f, (chartAreaHeight - 40f).coerceAtLeast(4f))
            val inBox = pos.x in (boxLeft - tolerancePx)..(boxLeft + estWidth + tolerancePx) &&
                        pos.y in (boxTop - tolerancePx)..(boxTop + 44f + tolerancePx)
            return inBox || (pos - p).getDistance() <= tolerancePx * 2
        }
        DrawingToolType.NONE -> return false
    }
    return false
}

internal fun findTappedDrawing(
    tap: Offset,
    drawings: List<DrawingData>,
    tolerancePx: Float,
    tToX: (Long) -> Float,
    pToY: (Double) -> Float,
    chartWidth: Float,
    chartAreaHeight: Float
): DrawingData? {
    for (drawing in drawings.reversed()) {
        if (isPointOnDrawing(drawing, tap, tolerancePx, tToX, pToY, chartWidth, chartAreaHeight)) {
            return drawing
        }
    }
    return null
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
    val greenColor = Color(0xFF089981)
    val redColor = Color(0xFFF23645)
    val candleColor = if (isBullish) greenColor else redColor
    val labelColor = Color(0xFF6B7280)
    val valueColor = Color(0xFF131722)
    val volColor = Color(0xFF00897B)

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
            color = Color(0xFF6B7280)
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

