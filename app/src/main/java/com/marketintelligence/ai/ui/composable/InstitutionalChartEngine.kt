package com.marketintelligence.ai.ui.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketintelligence.ai.domain.engine.*
import com.marketintelligence.ai.ui.theme.AppGreen
import com.marketintelligence.ai.ui.theme.AppRed
import com.marketintelligence.tradeengine.models.Candle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ChartStyle {
    CANDLESTICK,
    HOLLOW_CANDLE,
    LINE,
    AREA,
    HEIKIN_ASHI
}

data class ChartIndicatorConfig(
    val showSMC: Boolean = true,
    val showEMA: Boolean = true,
    val showBollinger: Boolean = false,
    val showVWAP: Boolean = true,
    val showSuperTrend: Boolean = false,
    val showVolume: Boolean = true,
    val showRSI: Boolean = false,
    val showMACD: Boolean = false
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun InstitutionalChartEngine(
    symbol: String,
    timeframe: String,
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    chartStyle: ChartStyle = ChartStyle.CANDLESTICK,
    indicatorConfig: ChartIndicatorConfig = ChartIndicatorConfig(),
    indicatorEngine: InstitutionalIndicatorEngine = remember { InstitutionalIndicatorEngine() },
    smcEngine: HardenedSMCEngine = remember { HardenedSMCEngine() },
    currentPrice: Double? = null
) {
    if (candles.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("NO MARKET DATA AVAILABLE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        return
    }

    val effectivePrice = when {
        currentPrice != null && currentPrice > 0.0 -> currentPrice
        candles.isNotEmpty() -> candles.last().close
        else -> 0.0
    }

    val effectiveCandles = remember(candles, effectivePrice) {
        if (candles.isNotEmpty() && effectivePrice > 0.0) {
            val last = candles.last()
            candles.dropLast(1) + last.copy(
                close = effectivePrice,
                high = maxOf(last.high, effectivePrice),
                low = minOf(last.low, effectivePrice)
            )
        } else {
            candles
        }
    }

    var zoom by remember { mutableFloatStateOf(1f) }
    var scrollOffsetFromRight by remember { mutableFloatStateOf(0f) }
    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val candleBaseWidth = remember(density) { with(density) { 14.dp.toPx() } }

    val textMeasurer = rememberTextMeasurer()

    var indicators by remember { mutableStateOf<InstitutionalIndicatorOutputs?>(null) }
    var smcAnalysis by remember { mutableStateOf<InstitutionalSMCAnalysis?>(null) }

    LaunchedEffect(effectiveCandles) {
        withContext(Dispatchers.Default) {
            indicators = indicatorEngine.computeAll(effectiveCandles)
            smcAnalysis = smcEngine.analyze(effectiveCandles)
        }
    }

    val renderCandles = if (chartStyle == ChartStyle.HEIKIN_ASHI) indicators?.heikinAshiCandles ?: effectiveCandles else effectiveCandles

    val inspectedCandleIndex = remember(crosshairOffset, renderCandles, zoom, scrollOffsetFromRight) {
        crosshairOffset?.let { offset ->
            val vCount = (50f / zoom).roundToInt().coerceIn(8, renderCandles.size.coerceAtLeast(8))
            val mScroll = (renderCandles.size - vCount).coerceAtLeast(0).toFloat()
            val cScroll = scrollOffsetFromRight.coerceIn(0f, mScroll)
            val endIdx = (renderCandles.size - cScroll.roundToInt()).coerceIn(vCount.coerceAtMost(renderCandles.size), renderCandles.size)
            val startIdx = (endIdx - vCount).coerceAtLeast(0)
            val cWidth = 350f / vCount // approximate for hit-testing
            val relativeIdx = (offset.x / cWidth.coerceAtLeast(1f)).toInt().coerceIn(0, vCount - 1)
            (startIdx + relativeIdx).coerceIn(0, renderCandles.size - 1)
        }
    }

    val activeCandle = inspectedCandleIndex?.let { renderCandles.getOrNull(it) } ?: renderCandles.lastOrNull()

    Column(modifier = modifier.fillMaxSize().background(Color(0xFF0D0E12))) {
        activeCandle?.let { c ->
            val change = c.close - c.open
            val pct = if (c.open > 0) (change / c.open) * 100.0 else 0.0
            val isBull = change >= 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14161D))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(symbol.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    Text("• $timeframe", color = Color(0xFF00E5FF), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    val rawT = c.openTime
                    val tMs = if (rawT in 1..99_999_999_999L) rawT * 1000L else rawT
                    val isDaily = timeframe.contains("D", ignoreCase = true) || timeframe.contains("W", ignoreCase = true) || timeframe.contains("M", ignoreCase = true)
                    val dateLabel = if (isDaily) SimpleDateFormat("dd MMM", Locale.US).format(Date(tMs)) else SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(tMs))
                    Text(dateLabel, color = Color(0xFF8B949E), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("O: %.2f".format(c.open), color = Color(0xFFD1D4DC), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("H: %.2f".format(c.high), color = Color(0xFFD1D4DC), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("L: %.2f".format(c.low), color = Color(0xFFD1D4DC), fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("C: %.2f".format(c.close), color = if (isBull) AppGreen else AppRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    if (c.volume > 0.0) {
                        Text("V: ${formatEngineVolume(c.volume)}", color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                Text(
                    "${if (isBull) "+" else ""}%.2f (%.2f%%)".format(change, pct),
                    color = if (isBull) AppGreen else AppRed,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(renderCandles.size) {
                        detectTransformGestures { _, panAmount, zoomAmount, _ ->
                            zoom = (zoom * zoomAmount).coerceIn(0.15f, 6.0f)
                            val canvasWidth = size.width.toFloat()
                            val vCount = (50f / zoom).roundToInt().coerceIn(8, renderCandles.size.coerceAtLeast(8))
                            val rightMargin = with(density) { 65.dp.toPx() }
                            val chartWidth = (canvasWidth - rightMargin).coerceAtLeast(10f)
                            val cWidth = chartWidth / vCount
                            val mScroll = (renderCandles.size - vCount).coerceAtLeast(0).toFloat()
                            if (cWidth > 0f) {
                                scrollOffsetFromRight = (scrollOffsetFromRight + panAmount.x / cWidth).coerceIn(0f, mScroll)
                            }
                            if (panAmount.getDistance() > 1.5f || kotlin.math.abs(zoomAmount - 1f) > 0.02f) {
                                crosshairOffset = null
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                crosshairOffset = if (crosshairOffset != null) null else offset
                            },
                            onLongPress = { offset ->
                                crosshairOffset = offset
                            }
                        )
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                if (renderCandles.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) return@Canvas

                val baseCount = 50f
                val visibleCount = (baseCount / zoom).roundToInt().coerceIn(8, renderCandles.size.coerceAtLeast(8))
                val maxScroll = (renderCandles.size - visibleCount).coerceAtLeast(0).toFloat()
                val clampedScroll = scrollOffsetFromRight.coerceIn(0f, maxScroll)

                val endIndex = (renderCandles.size - clampedScroll.roundToInt()).coerceIn(visibleCount.coerceAtMost(renderCandles.size), renderCandles.size)
                val startIndex = (endIndex - visibleCount).coerceAtLeast(0)

                val visibleCandles = renderCandles.subList(startIndex, endIndex)
                if (visibleCandles.isEmpty()) return@Canvas

                val rightMargin = 65.dp.toPx()
                val bottomMargin = 18.dp.toPx()
                val chartWidth = (canvasWidth - rightMargin).coerceAtLeast(0f)
                val chartHeight = (canvasHeight - bottomMargin).coerceAtLeast(0f)

                val candleWidth = chartWidth / visibleCount.coerceAtLeast(1)

                var minPrice = visibleCandles.minOf { it.low } * 0.999
                var maxPrice = visibleCandles.maxOf { it.high } * 1.001
                if (effectivePrice > 0.0) {
                    minPrice = minOf(minPrice, effectivePrice * 0.999)
                    maxPrice = maxOf(maxPrice, effectivePrice * 1.001)
                }
                val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

                fun priceToY(price: Double): Float {
                    return (chartHeight - ((price - minPrice) / priceRange * chartHeight)).toFloat()
                }

                fun indexToX(absoluteIdx: Int): Float {
                    return (absoluteIdx - startIndex) * candleWidth
                }

                drawPriceAndGrid(
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    priceRange = priceRange,
                    canvasWidth = canvasWidth,
                    chartHeight = chartHeight,
                    chartWidth = chartWidth,
                    candles = renderCandles,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    candleWidth = candleWidth,
                    timeframe = timeframe,
                    textMeasurer = textMeasurer
                )

                if (indicatorConfig.showSMC) {
                    smcAnalysis?.let { smc ->
                        drawSMCOverlays(smc, canvasWidth, ::priceToY, textMeasurer)
                    }
                }

                indicators?.let { ind ->
                    drawIndicatorOverlays(indicatorConfig, ind, visibleCandles, startIndex, candleWidth, ::indexToX, ::priceToY)
                }

                drawPriceSeries(chartStyle, visibleCandles, startIndex, candleWidth, ::indexToX, ::priceToY, chartHeight)

                if (effectivePrice > 0.0) {
                    drawCurrentPriceLine(
                        effectivePrice = effectivePrice,
                        priceToY = ::priceToY,
                        canvasWidth = canvasWidth,
                        canvasHeight = chartHeight,
                        textMeasurer = textMeasurer,
                        isBullish = effectiveCandles.lastOrNull()?.let { effectivePrice >= it.open } ?: true
                    )
                }

                crosshairOffset?.let { offset ->
                    drawMagneticCrosshair(offset, activeCandle, canvasWidth, chartHeight, timeframe, ::priceToY, textMeasurer)
                }
            }

            // ── Double Right Arrow "Scroll to Real-time" Button (60% Opacity) ──
            if (scrollOffsetFromRight > 3f) {
                androidx.compose.material3.Surface(
                    onClick = { scrollOffsetFromRight = 0f },
                    shape = CircleShape,
                    color = Color(0xFF1E222D).copy(alpha = 0.60f),
                    border = BorderStroke(1.dp, Color(0xFF4A5268).copy(alpha = 0.60f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
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
        }

        if (indicatorConfig.showVolume) {
            VolumeSubPane(
                candles = renderCandles,
                volumeMaData = indicators?.volumeMa20,
                scrollOffsetFromRight = scrollOffsetFromRight,
                zoom = zoom
            )
        }
        if (indicatorConfig.showRSI) {
            indicators?.let { ind ->
                RsiSubPane(candles = renderCandles, rsiData = ind.rsi14, scrollOffsetFromRight = scrollOffsetFromRight, zoom = zoom)
            }
        }
        if (indicatorConfig.showMACD) {
            indicators?.let { ind ->
                MacdSubPane(candles = renderCandles, macdData = ind.macd, scrollOffsetFromRight = scrollOffsetFromRight, zoom = zoom)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawPriceAndGrid(
    minPrice: Double,
    maxPrice: Double,
    priceRange: Double,
    canvasWidth: Float,
    chartHeight: Float,
    chartWidth: Float,
    candles: List<Candle>,
    startIndex: Int,
    endIndex: Int,
    candleWidth: Float,
    timeframe: String,
    textMeasurer: TextMeasurer
) {
    val steps = 5
    val stepPrice = priceRange / steps

    for (i in 0..steps) {
        val p = minPrice + (i * stepPrice)
        val y = (chartHeight - ((p - minPrice) / priceRange * chartHeight)).toFloat()

        drawLine(
            color = Color(0xFF20232E),
            start = Offset(0f, y),
            end = Offset(chartWidth, y),
            strokeWidth = 1f
        )

        drawText(
            textMeasurer = textMeasurer,
            text = "%.2f".format(p),
            topLeft = Offset(chartWidth + 5.dp.toPx(), y - 7.dp.toPx()),
            style = TextStyle(color = Color(0xFF8B92A5), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
    }

    // Vertical grid lines & time/date labels at bottom
    val visibleCount = endIndex - startIndex
    if (visibleCount <= 0 || candles.isEmpty()) return

    val timeStep = maxOf(1, visibleCount / 5)
    val firstRaw = candles.firstOrNull()?.openTime ?: 0L
    val lastRaw = candles.lastOrNull()?.openTime ?: 0L
    val firstMs = if (firstRaw in 1..99_999_999_999L) firstRaw * 1000L else firstRaw
    val lastMs = if (lastRaw in 1..99_999_999_999L) lastRaw * 1000L else lastRaw
    val avgDurationMs = if (candles.size > 1) {
        abs(lastMs - firstMs) / (candles.size - 1).coerceAtLeast(1)
    } else 86_400_000L

    val isDailyOrHigher = timeframe.equals("1D", ignoreCase = true) ||
        timeframe.equals("D", ignoreCase = true) ||
        timeframe.equals("1W", ignoreCase = true) ||
        timeframe.equals("W", ignoreCase = true) ||
        timeframe.equals("1M", ignoreCase = true) ||
        timeframe.equals("M", ignoreCase = true) ||
        timeframe.contains("day", ignoreCase = true) ||
        timeframe.contains("week", ignoreCase = true) ||
        timeframe.contains("month", ignoreCase = true) ||
        avgDurationMs >= 20 * 3600 * 1000L

    val calFirst = Calendar.getInstance().apply { timeInMillis = firstMs }
    val calLast = Calendar.getInstance().apply { timeInMillis = lastMs }
    val spansMultipleYears = calFirst.get(Calendar.YEAR) != calLast.get(Calendar.YEAR)

    val dateFormat = if (spansMultipleYears) {
        SimpleDateFormat("dd MMM ''yy", Locale.US)
    } else {
        SimpleDateFormat("dd MMM", Locale.US)
    }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    val totalSpanMs = abs(lastMs - firstMs)
    val isMultiDayIntraday = !isDailyOrHigher && totalSpanMs > 24 * 3600 * 1000L
    var prevDayOfYear = -1

    for (i in startIndex until endIndex step timeStep) {
        val localIndex = i - startIndex
        val x = localIndex * candleWidth + candleWidth / 2f
        if (x > chartWidth) break

        drawLine(
            color = Color(0xFF20232E),
            start = Offset(x, 0f),
            end = Offset(x, chartHeight),
            strokeWidth = 1f
        )

        if (i < candles.size) {
            val rawTime = candles[i].openTime
            val timeMs = if (rawTime in 1..99_999_999_999L) rawTime * 1000L else rawTime
            val date = Date(timeMs)
            val currentCal = Calendar.getInstance().apply { time = date }
            val currentDayOfYear = currentCal.get(Calendar.DAY_OF_YEAR)

            val label = when {
                isDailyOrHigher -> dateFormat.format(date)
                isMultiDayIntraday -> {
                    if (prevDayOfYear != currentDayOfYear) dateFormat.format(date)
                    else timeFormat.format(date)
                }
                else -> timeFormat.format(date)
            }
            prevDayOfYear = currentDayOfYear

            val textResult = textMeasurer.measure(
                label,
                TextStyle(color = Color(0xFF8B92A5), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            )
            drawText(
                textResult,
                topLeft = Offset(x - textResult.size.width / 2f, chartHeight + 3.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawSMCOverlays(
    smc: InstitutionalSMCAnalysis,
    canvasWidth: Float,
    priceToY: (Double) -> Float,
    textMeasurer: TextMeasurer
) {
    val eqY = priceToY(smc.equilibriumPrice)
    drawLine(
        color = Color(0xFFFFD700).copy(alpha = 0.5f),
        start = Offset(0f, eqY),
        end = Offset(canvasWidth - 70.dp.toPx(), eqY),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
    )
    drawText(
        textMeasurer = textMeasurer,
        text = "EQ (50%)",
        topLeft = Offset(12.dp.toPx(), eqY - 14.dp.toPx()),
        style = TextStyle(color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    )

    for (fvg in smc.fairValueGaps) {
        if (fvg.state == MitigationState.INVALIDATED) continue
        val topY = priceToY(fvg.top)
        val botY = priceToY(fvg.bottom)
        val fvgColor = if (fvg.direction == SMCDirection.BULLISH) Color(0xFF00E676) else Color(0xFFFF1744)

        val alpha = if (fvg.state == MitigationState.TESTED) 0.12f else 0.22f
        drawRect(
            color = fvgColor.copy(alpha = alpha),
            topLeft = Offset(0f, min(topY, botY)),
            size = Size(canvasWidth - 70.dp.toPx(), abs(topY - botY).coerceAtLeast(2f))
        )
        drawLine(
            color = fvgColor.copy(alpha = 0.6f),
            start = Offset(0f, min(topY, botY)),
            end = Offset(canvasWidth - 70.dp.toPx(), min(topY, botY)),
            strokeWidth = 1f
        )
    }

    for (ob in smc.orderBlocks) {
        if (ob.state == MitigationState.INVALIDATED) continue
        val topY = priceToY(ob.top)
        val botY = priceToY(ob.bottom)
        val obColor = if (ob.direction == SMCDirection.BULLISH) Color(0xFF00B0FF) else Color(0xFFFF9100)

        drawRect(
            color = obColor.copy(alpha = 0.20f),
            topLeft = Offset(0f, min(topY, botY)),
            size = Size(canvasWidth - 70.dp.toPx(), abs(topY - botY).coerceAtLeast(3f))
        )
        drawText(
            textMeasurer = textMeasurer,
            text = if (ob.direction == SMCDirection.BULLISH) "OB (Demand)" else "OB (Supply)",
            topLeft = Offset(14.dp.toPx(), min(topY, botY) + 2.dp.toPx()),
            style = TextStyle(color = obColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        )
    }

    for (sb in smc.structureBreaks) {
        val y = priceToY(sb.price)
        val color = if (sb.isBullish) AppGreen else AppRed
        drawLine(
            color = color.copy(alpha = 0.8f),
            start = Offset(0f, y),
            end = Offset(canvasWidth - 70.dp.toPx(), y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )
        drawText(
            textMeasurer = textMeasurer,
            text = sb.type,
            topLeft = Offset(canvasWidth - 120.dp.toPx(), y - 12.dp.toPx()),
            style = TextStyle(color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        )
    }
}

private fun DrawScope.drawIndicatorOverlays(
    config: ChartIndicatorConfig,
    indicators: InstitutionalIndicatorOutputs,
    visibleCandles: List<Candle>,
    startIndex: Int,
    candleWidth: Float,
    indexToX: (Int) -> Float,
    priceToY: (Double) -> Float
) {
    fun drawIndicatorLine(map: Map<Long, Double>, color: Color, strokeWidth: Float = 1.5f) {
        val path = Path()
        var first = true
        for (i in visibleCandles.indices) {
            val c = visibleCandles[i]
            val v = map[c.openTime] ?: continue
            val x = indexToX(startIndex + i) + candleWidth / 2
            val y = priceToY(v)
            if (first) { path.moveTo(x, y); first = false } else { path.lineTo(x, y) }
        }
        if (!first) drawPath(path, color, style = Stroke(width = strokeWidth.dp.toPx()))
    }

    if (config.showEMA) {
        drawIndicatorLine(indicators.ema9, Color(0xFF00E5FF), 1.2f)
        drawIndicatorLine(indicators.ema20, Color(0xFFFFD600), 1.5f)
        drawIndicatorLine(indicators.ema50, Color(0xFFFF6D00), 1.5f)
        drawIndicatorLine(indicators.ema200, Color(0xFFAA00FF), 1.8f)
    }

    if (config.showVWAP) {
        drawIndicatorLine(indicators.vwap, Color(0xFF00E676), 1.6f)
    }

    if (config.showBollinger) {
        val upperPath = Path()
        val lowerPath = Path()
        val fillPath = Path()
        var first = true
        val upperPoints = mutableListOf<Offset>()
        val lowerPoints = mutableListOf<Offset>()

        for (i in visibleCandles.indices) {
            val c = visibleCandles[i]
            val bb = indicators.bollingerBands[c.openTime] ?: continue
            val x = indexToX(startIndex + i) + candleWidth / 2
            val upY = priceToY(bb.upperBand)
            val lowY = priceToY(bb.lowerBand)

            upperPoints.add(Offset(x, upY))
            lowerPoints.add(Offset(x, lowY))

            if (first) {
                upperPath.moveTo(x, upY)
                lowerPath.moveTo(x, lowY)
                first = false
            } else {
                upperPath.lineTo(x, upY)
                lowerPath.lineTo(x, lowY)
            }
        }

        if (upperPoints.isNotEmpty()) {
            drawPath(upperPath, Color(0xFF2979FF).copy(alpha = 0.6f), style = Stroke(width = 1.dp.toPx()))
            drawPath(lowerPath, Color(0xFF2979FF).copy(alpha = 0.6f), style = Stroke(width = 1.dp.toPx()))

            fillPath.moveTo(upperPoints.first().x, upperPoints.first().y)
            upperPoints.forEach { fillPath.lineTo(it.x, it.y) }
            for (k in lowerPoints.indices.reversed()) fillPath.lineTo(lowerPoints[k].x, lowerPoints[k].y)
            fillPath.close()
            drawPath(fillPath, Color(0xFF2979FF).copy(alpha = 0.08f), style = Fill)
        }
    }
}

private fun DrawScope.drawPriceSeries(
    style: ChartStyle,
    visibleCandles: List<Candle>,
    startIndex: Int,
    candleWidth: Float,
    indexToX: (Int) -> Float,
    priceToY: (Double) -> Float,
    canvasHeight: Float
) {
    when (style) {
        ChartStyle.CANDLESTICK, ChartStyle.HEIKIN_ASHI -> {
            for (i in visibleCandles.indices) {
                val c = visibleCandles[i]
                val x = indexToX(startIndex + i)
                val isBull = c.close >= c.open
                val color = if (isBull) AppGreen else AppRed

                val openY = priceToY(c.open)
                val closeY = priceToY(c.close)
                val highY = priceToY(c.high)
                val lowY = priceToY(c.low)

                drawLine(
                    color = color,
                    start = Offset(x + candleWidth / 2, highY),
                    end = Offset(x + candleWidth / 2, lowY),
                    strokeWidth = 1.dp.toPx()
                )

                val top = min(openY, closeY)
                val height = max(abs(openY - closeY), 1.5f)
                drawRect(
                    color = color,
                    topLeft = Offset(x + candleWidth * 0.1f, top),
                    size = Size(candleWidth * 0.8f, height)
                )
            }
        }
        ChartStyle.HOLLOW_CANDLE -> {
            for (i in visibleCandles.indices) {
                val c = visibleCandles[i]
                val x = indexToX(startIndex + i)
                val isBull = c.close >= c.open
                val color = if (isBull) AppGreen else AppRed

                val openY = priceToY(c.open)
                val closeY = priceToY(c.close)
                val highY = priceToY(c.high)
                val lowY = priceToY(c.low)

                drawLine(color, Offset(x + candleWidth / 2, highY), Offset(x + candleWidth / 2, lowY), strokeWidth = 1.dp.toPx())

                val top = min(openY, closeY)
                val height = max(abs(openY - closeY), 1.5f)
                if (isBull) {
                    drawRect(color, Offset(x + candleWidth * 0.1f, top), Size(candleWidth * 0.8f, height), style = Stroke(width = 1.2.dp.toPx()))
                } else {
                    drawRect(color, Offset(x + candleWidth * 0.1f, top), Size(candleWidth * 0.8f, height))
                }
            }
        }
        ChartStyle.LINE -> {
            val linePath = Path()
            var first = true
            for (i in visibleCandles.indices) {
                val x = indexToX(startIndex + i) + candleWidth / 2
                val y = priceToY(visibleCandles[i].close)
                if (first) { linePath.moveTo(x, y); first = false } else { linePath.lineTo(x, y) }
            }
            drawPath(linePath, Color(0xFF00E5FF), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        ChartStyle.AREA -> {
            val linePath = Path()
            val areaPath = Path()
            var first = true
            var firstX = 0f

            for (i in visibleCandles.indices) {
                val x = indexToX(startIndex + i) + candleWidth / 2
                val y = priceToY(visibleCandles[i].close)
                if (first) {
                    firstX = x
                    linePath.moveTo(x, y)
                    areaPath.moveTo(x, canvasHeight)
                    areaPath.lineTo(x, y)
                    first = false
                } else {
                    linePath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }
            if (!first) {
                val lastX = indexToX(startIndex + visibleCandles.size - 1) + candleWidth / 2
                areaPath.lineTo(lastX, canvasHeight)
                areaPath.close()

                drawPath(
                    areaPath,
                    brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.35f), Color.Transparent)),
                    style = Fill
                )
                drawPath(linePath, Color(0xFF00E5FF), style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}

@Composable
fun VolumeSubPane(
    candles: List<Candle>,
    volumeMaData: Map<Long, Double>?,
    scrollOffsetFromRight: Float,
    zoom: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .background(Color(0xFF0B0C10))
            .border(0.5.dp, Color(0xFF1E212B))
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas
            val baseCount = 50f
            val visibleCount = (baseCount / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
            val maxScroll = (candles.size - visibleCount).coerceAtLeast(0).toFloat()
            val clampedScroll = scrollOffsetFromRight.coerceIn(0f, maxScroll)

            val endIndex = (candles.size - clampedScroll.roundToInt()).coerceIn(visibleCount.coerceAtMost(candles.size), candles.size)
            val startIndex = (endIndex - visibleCount).coerceAtLeast(0)
            val visibleCandles = candles.subList(startIndex, endIndex)
            if (visibleCandles.isEmpty()) return@Canvas

            val rightMargin = 65.dp.toPx()
            val chartWidth = (size.width - rightMargin).coerceAtLeast(0f)
            val candleWidth = chartWidth / visibleCount.coerceAtLeast(1)
            val bodyWidth = candleWidth * 0.75f
            val drawHeight = size.height - 4.dp.toPx()

            var maxVol = visibleCandles.maxOfOrNull { it.volume }?.takeIf { it > 0 } ?: 1.0
            if (volumeMaData != null) {
                val maxMa = visibleCandles.maxOfOrNull { volumeMaData[it.openTime] ?: 0.0 } ?: 0.0
                maxVol = maxOf(maxVol, maxMa)
            }

            // Draw volume bars
            for (i in visibleCandles.indices) {
                val c = visibleCandles[i]
                val x = i * candleWidth
                val barHeight = ((c.volume / maxVol) * drawHeight).toFloat().coerceAtLeast(1f)
                val color = if (c.close >= c.open) AppGreen.copy(alpha = 0.6f) else AppRed.copy(alpha = 0.6f)

                drawRect(
                    color = color,
                    topLeft = Offset(x + (candleWidth - bodyWidth) / 2f, size.height - barHeight),
                    size = Size(bodyWidth, barHeight)
                )
            }

            // Draw Volume MA 20 line if available
            if (volumeMaData != null) {
                val maPath = Path()
                var first = true
                for (i in visibleCandles.indices) {
                    val maVal = volumeMaData[visibleCandles[i].openTime] ?: continue
                    val x = i * candleWidth + candleWidth / 2f
                    val y = size.height - ((maVal / maxVol) * drawHeight).toFloat()
                    if (first) {
                        maPath.moveTo(x, y)
                        first = false
                    } else {
                        maPath.lineTo(x, y)
                    }
                }
                if (!first) {
                    drawPath(maPath, Color(0xFFFFB74D), style = Stroke(width = 1.2.dp.toPx()))
                }
            }
        }
        Text("VOL", color = Color(0xFF8B949E), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawMagneticCrosshair(
    offset: Offset,
    activeCandle: Candle?,
    canvasWidth: Float,
    chartHeight: Float,
    timeframe: String,
    priceToY: (Double) -> Float,
    textMeasurer: TextMeasurer
) {
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(0f, offset.y),
        end = Offset(canvasWidth, offset.y),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
    )
    drawLine(
        color = Color(0xFFB0BEC5),
        start = Offset(offset.x, 0f),
        end = Offset(offset.x, chartHeight),
        strokeWidth = 1f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
    )

    activeCandle?.let { c ->
        val priceText = "%.2f".format(c.close)
        val badgeY = priceToY(c.close)
        drawRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(canvasWidth - 65.dp.toPx(), badgeY - 9.dp.toPx()),
            size = Size(65.dp.toPx(), 18.dp.toPx())
        )
        drawText(
            textMeasurer = textMeasurer,
            text = priceText,
            topLeft = Offset(canvasWidth - 60.dp.toPx(), badgeY - 7.dp.toPx()),
            style = TextStyle(color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )

        // Bottom time / date badge
        val rawTime = c.openTime
        val timeMs = if (rawTime in 1..99_999_999_999L) rawTime * 1000L else rawTime
        val isDaily = timeframe.contains("D", ignoreCase = true) ||
            timeframe.contains("W", ignoreCase = true) ||
            timeframe.contains("M", ignoreCase = true) ||
            timeframe.contains("day", ignoreCase = true) ||
            timeframe.contains("week", ignoreCase = true) ||
            timeframe.contains("month", ignoreCase = true)
        val dateText = if (isDaily) SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(timeMs))
        else SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(timeMs))

        val dateMeasured = textMeasurer.measure(
            dateText,
            TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
        val badgeW = dateMeasured.size.width + 8.dp.toPx()
        val badgeH = dateMeasured.size.height + 4.dp.toPx()
        val badgeX = (offset.x - badgeW / 2f).coerceIn(2f, canvasWidth - badgeW - 2f)
        drawRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(badgeX, chartHeight),
            size = Size(badgeW, badgeH)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = dateText,
            topLeft = Offset(badgeX + 4.dp.toPx(), chartHeight + 2.dp.toPx()),
            style = TextStyle(color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawCurrentPriceLine(
    effectivePrice: Double,
    priceToY: (Double) -> Float,
    canvasWidth: Float,
    canvasHeight: Float,
    textMeasurer: TextMeasurer,
    isBullish: Boolean
) {
    val rawY = priceToY(effectivePrice)
    val clampedY = rawY.coerceIn(2f, canvasHeight - 2f)
    val rightMargin = 65.dp.toPx()
    val chartWidth = (canvasWidth - rightMargin).coerceAtLeast(0f)
    val accentColor = if (isBullish) Color(0xFF00E676) else Color(0xFFFF1744)

    // 1. Soft glow halo line across chart
    drawLine(
        color = accentColor.copy(alpha = 0.28f),
        start = Offset(0f, clampedY),
        end = Offset(chartWidth, clampedY),
        strokeWidth = 3.5.dp.toPx()
    )

    // 2. Core horizontal dashed guideline across chart
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
    drawLine(
        color = accentColor,
        start = Offset(0f, clampedY),
        end = Offset(chartWidth, clampedY),
        strokeWidth = 1.6.dp.toPx(),
        pathEffect = dashEffect
    )

    // 3. High-Visibility Right-Axis Pill Badge (Vibrant Accent Color)
    val priceStr = formatEnginePrice(effectivePrice)
    val textColor = if (isBullish) Color.Black else Color.White
    val priceTextStyle = TextStyle(
        color = textColor,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = FontFamily.Monospace
    )
    val priceMeasured = textMeasurer.measure(priceStr, priceTextStyle)
    val badgePaddingH = 5.dp.toPx()
    val badgePaddingV = 3.dp.toPx()
    val badgeWidth = (priceMeasured.size.width + badgePaddingH * 2 + 7.dp.toPx()).coerceAtLeast(rightMargin - 4.dp.toPx())
    val badgeHeight = priceMeasured.size.height + badgePaddingV * 2
    val badgeLeft = chartWidth + 2.dp.toPx()
    val badgeTop = clampedY - badgeHeight / 2f

    val pillPath = Path().apply {
        addRoundRect(
            RoundRect(
                left = badgeLeft,
                top = badgeTop,
                right = badgeLeft + badgeWidth,
                bottom = badgeTop + badgeHeight,
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        )
    }
    drawPath(pillPath, accentColor)

    // Live beacon dot
    val dotRadius = 2.dp.toPx()
    val dotCenter = Offset(badgeLeft + badgePaddingH + dotRadius, clampedY)
    drawCircle(color = textColor, radius = dotRadius, center = dotCenter)

    // Price text
    val textOffset = Offset(dotCenter.x + dotRadius + 3.dp.toPx(), badgeTop + badgePaddingV)
    drawText(
        textMeasurer = textMeasurer,
        text = priceStr,
        topLeft = textOffset,
        style = priceTextStyle
    )
}

private fun formatEnginePrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(java.util.Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(java.util.Locale.US, "%.2f", price)
        price >= 0.01 -> String.format(java.util.Locale.US, "%.4f", price)
        price < 0.001 -> String.format(java.util.Locale.US, "%.7f", price)
        else -> String.format(java.util.Locale.US, "%.6f", price)
    }
}

private fun formatEngineVolume(volume: Double): String {
    return when {
        volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
        volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
        volume >= 1_000 -> "%.1fK".format(volume / 1_000)
        volume >= 10 -> "%.1f".format(volume)
        volume >= 1 -> "%.2f".format(volume)
        volume > 0 -> "%.3f".format(volume)
        else -> "0"
    }
}

@Composable
fun RsiSubPane(candles: List<Candle>, rsiData: Map<Long, Double>, scrollOffsetFromRight: Float, zoom: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .background(Color(0xFF0B0C10))
            .border(0.5.dp, Color(0xFF1E212B))
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas
            val baseCount = 50f
            val visibleCount = (baseCount / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
            val maxScroll = (candles.size - visibleCount).coerceAtLeast(0).toFloat()
            val clampedScroll = scrollOffsetFromRight.coerceIn(0f, maxScroll)

            val endIndex = (candles.size - clampedScroll.roundToInt()).coerceIn(visibleCount.coerceAtMost(candles.size), candles.size)
            val startIndex = (endIndex - visibleCount).coerceAtLeast(0)
            val visibleCandles = candles.subList(startIndex, endIndex)
            val candleWidth = size.width / visibleCount.coerceAtLeast(1)

            val y70 = size.height * (1f - (70f / 100f))
            val y30 = size.height * (1f - (30f / 100f))

            drawLine(Color(0xFFEF5350).copy(alpha = 0.5f), Offset(0f, y70), Offset(size.width, y70), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            drawLine(Color(0xFF26A69A).copy(alpha = 0.5f), Offset(0f, y30), Offset(size.width, y30), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

            val path = Path()
            var first = true
            for (i in visibleCandles.indices) {
                val rsi = rsiData[visibleCandles[i].openTime] ?: continue
                val x = i * candleWidth + candleWidth / 2f
                val y = size.height * (1f - (rsi.toFloat() / 100f))
                if (first) { path.moveTo(x, y); first = false } else { path.lineTo(x, y) }
            }
            if (!first) drawPath(path, Color(0xFFBA68C8), style = Stroke(width = 1.5.dp.toPx()))
        }
        Text("RSI (14)", color = Color(0xFFBA68C8), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}

@Composable
fun MacdSubPane(candles: List<Candle>, macdData: Map<Long, MacdResult>, scrollOffsetFromRight: Float, zoom: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(Color(0xFF0B0C10))
            .border(0.5.dp, Color(0xFF1E212B))
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas
            val baseCount = 50f
            val visibleCount = (baseCount / zoom).roundToInt().coerceIn(8, candles.size.coerceAtLeast(8))
            val maxScroll = (candles.size - visibleCount).coerceAtLeast(0).toFloat()
            val clampedScroll = scrollOffsetFromRight.coerceIn(0f, maxScroll)

            val endIndex = (candles.size - clampedScroll.roundToInt()).coerceIn(visibleCount.coerceAtMost(candles.size), candles.size)
            val startIndex = (endIndex - visibleCount).coerceAtLeast(0)
            val visibleCandles = candles.subList(startIndex, endIndex)
            val candleWidth = size.width / visibleCount.coerceAtLeast(1)

            val maxVal = visibleCandles.maxOfOrNull {
                val m = macdData[it.openTime]
                if (m != null) max(abs(m.macdLine), max(abs(m.signalLine), abs(m.histogram))) else 1.0
            }?.takeIf { it > 0 } ?: 1.0

            val zeroY = size.height / 2f
            drawLine(Color(0xFF37474F), Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1f)

            val macdPath = Path()
            val signalPath = Path()
            var first = true

            for (i in visibleCandles.indices) {
                val m = macdData[visibleCandles[i].openTime] ?: continue
                val x = i * candleWidth + candleWidth / 2f

                val histHeight = (m.histogram / maxVal * (size.height / 2.2f)).toFloat()
                val histColor = if (m.histogram >= 0) AppGreen.copy(alpha = 0.6f) else AppRed.copy(alpha = 0.6f)
                drawRect(histColor, topLeft = Offset(x - candleWidth * 0.35f, if (m.histogram >= 0) zeroY - histHeight else zeroY), size = Size(candleWidth * 0.7f, abs(histHeight).coerceAtLeast(1f)))

                val macdY = zeroY - (m.macdLine / maxVal * (size.height / 2.2f)).toFloat()
                val sigY = zeroY - (m.signalLine / maxVal * (size.height / 2.2f)).toFloat()

                if (first) {
                    macdPath.moveTo(x, macdY)
                    signalPath.moveTo(x, sigY)
                    first = false
                } else {
                    macdPath.lineTo(x, macdY)
                    signalPath.lineTo(x, sigY)
                }
            }

            if (!first) {
                drawPath(macdPath, Color(0xFF29B6F6), style = Stroke(width = 1.2.dp.toPx()))
                drawPath(signalPath, Color(0xFFFFB74D), style = Stroke(width = 1.2.dp.toPx()))
            }
        }
        Text("MACD (12, 26, 9)", color = Color(0xFF29B6F6), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}
