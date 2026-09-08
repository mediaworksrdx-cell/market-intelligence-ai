package com.marketintelligence.ai.ui.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    smcEngine: HardenedSMCEngine = remember { HardenedSMCEngine() }
) {
    if (candles.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("NO MARKET DATA AVAILABLE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        return
    }

    var pan by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var crosshairOffset by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val candleBaseWidth = remember(density) { with(density) { 14.dp.toPx() } }

    val textMeasurer = rememberTextMeasurer()

    var indicators by remember { mutableStateOf<InstitutionalIndicatorOutputs?>(null) }
    var smcAnalysis by remember { mutableStateOf<InstitutionalSMCAnalysis?>(null) }

    LaunchedEffect(candles) {
        withContext(Dispatchers.Default) {
            indicators = indicatorEngine.computeAll(candles)
            smcAnalysis = smcEngine.analyze(candles)
        }
    }

    val renderCandles = if (chartStyle == ChartStyle.HEIKIN_ASHI) indicators?.heikinAshiCandles ?: candles else candles

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newZoom = (zoom * zoomChange).coerceIn(0.2f, 8f)
        zoom = newZoom
        val candleWidth = candleBaseWidth * newZoom
        val maxPan = (renderCandles.size * candleWidth).coerceAtLeast(0f)
        pan = (pan + panChange.x).coerceIn(-maxPan, 0f)
    }

    val inspectedCandleIndex = remember(crosshairOffset, renderCandles, zoom, pan, candleBaseWidth) {
        crosshairOffset?.let { offset ->
            val candleWidth = candleBaseWidth * zoom
            val totalCandles = renderCandles.size
            val startIdx = ((-pan) / candleWidth).toInt().coerceIn(0, totalCandles - 1)
            val relativeIdx = ((offset.x) / candleWidth).toInt()
            (startIdx + relativeIdx).coerceIn(0, totalCandles - 1)
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(symbol.uppercase(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    Text("• $timeframe", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("O: %.2f".format(c.open), color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("H: %.2f".format(c.high), color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("L: %.2f".format(c.low), color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("C: %.2f".format(c.close), color = if (isBull) AppGreen else AppRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text(
                    "${if (isBull) "+" else ""}%.2f (%.2f%%)".format(change, pct),
                    color = if (isBull) AppGreen else AppRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformableState)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { crosshairOffset = it },
                            onDrag = { change, dragAmount ->
                                val candleWidth = candleBaseWidth * zoom
                                val maxPan = (renderCandles.size * candleWidth).coerceAtLeast(0f)
                                pan = (pan + dragAmount.x).coerceIn(-maxPan, 0f)
                                crosshairOffset = change.position
                                change.consume()
                            },
                            onDragEnd = { crosshairOffset = null },
                            onDragCancel = { crosshairOffset = null }
                        )
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                if (renderCandles.isEmpty() || canvasWidth <= 0 || canvasHeight <= 0) return@Canvas

                val candleWidth = candleBaseWidth * zoom

                val startIndex = ((-pan) / candleWidth).toInt().coerceIn(0, renderCandles.size - 1)
                val visibleCount = ((canvasWidth / candleWidth).roundToInt() + 2).coerceAtMost(renderCandles.size - startIndex)
                val endIndex = (startIndex + visibleCount).coerceAtMost(renderCandles.size)

                val visibleCandles = renderCandles.subList(startIndex, endIndex)
                if (visibleCandles.isEmpty()) return@Canvas

                val minPrice = visibleCandles.minOf { it.low } * 0.999
                val maxPrice = visibleCandles.maxOf { it.high } * 1.001
                val priceRange = (maxPrice - minPrice).takeIf { it > 0 } ?: 1.0

                fun priceToY(price: Double): Float {
                    return (canvasHeight - ((price - minPrice) / priceRange * canvasHeight)).toFloat()
                }

                fun indexToX(absoluteIdx: Int): Float {
                    return pan + (absoluteIdx * candleWidth)
                }

                drawPriceAndGrid(minPrice, maxPrice, priceRange, canvasWidth, canvasHeight, textMeasurer)

                if (indicatorConfig.showSMC) {
                    smcAnalysis?.let { smc ->
                        drawSMCOverlays(smc, canvasWidth, ::priceToY, textMeasurer)
                    }
                }

                indicators?.let { ind ->
                    drawIndicatorOverlays(indicatorConfig, ind, visibleCandles, startIndex, candleWidth, ::indexToX, ::priceToY)
                }

                drawPriceSeries(chartStyle, visibleCandles, startIndex, candleWidth, ::indexToX, ::priceToY, canvasHeight)

                if (indicatorConfig.showVolume) {
                    drawVolumeSubHistogram(visibleCandles, startIndex, candleWidth, ::indexToX, canvasHeight)
                }

                crosshairOffset?.let { offset ->
                    drawMagneticCrosshair(offset, activeCandle, canvasWidth, canvasHeight, ::priceToY, textMeasurer)
                }
            }
        }

        if (indicatorConfig.showRSI) {
            indicators?.let { ind ->
                RsiSubPane(candles = renderCandles, rsiData = ind.rsi14, pan = pan, zoom = zoom)
            }
        }
        if (indicatorConfig.showMACD) {
            indicators?.let { ind ->
                MacdSubPane(candles = renderCandles, macdData = ind.macd, pan = pan, zoom = zoom)
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
    canvasHeight: Float,
    textMeasurer: TextMeasurer
) {
    val steps = 5
    val stepPrice = priceRange / steps

    for (i in 0..steps) {
        val p = minPrice + (i * stepPrice)
        val y = (canvasHeight - ((p - minPrice) / priceRange * canvasHeight)).toFloat()

        drawLine(
            color = Color(0xFF20232E),
            start = Offset(0f, y),
            end = Offset(canvasWidth - 65.dp.toPx(), y),
            strokeWidth = 1f
        )

        drawText(
            textMeasurer = textMeasurer,
            text = "%.2f".format(p),
            topLeft = Offset(canvasWidth - 60.dp.toPx(), y - 7.dp.toPx()),
            style = TextStyle(color = Color(0xFF8B92A5), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        )
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

private fun DrawScope.drawVolumeSubHistogram(
    visibleCandles: List<Candle>,
    startIndex: Int,
    candleWidth: Float,
    indexToX: (Int) -> Float,
    canvasHeight: Float
) {
    val maxVol = visibleCandles.maxOfOrNull { it.volume }?.takeIf { it > 0 } ?: 1.0
    val volAreaHeight = canvasHeight * 0.18f

    for (i in visibleCandles.indices) {
        val c = visibleCandles[i]
        val x = indexToX(startIndex + i)
        val barHeight = ((c.volume / maxVol) * volAreaHeight).toFloat().coerceAtLeast(1f)
        val color = if (c.close >= c.open) AppGreen.copy(alpha = 0.35f) else AppRed.copy(alpha = 0.35f)

        drawRect(
            color = color,
            topLeft = Offset(x + candleWidth * 0.1f, canvasHeight - barHeight),
            size = Size(candleWidth * 0.8f, barHeight)
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawMagneticCrosshair(
    offset: Offset,
    activeCandle: Candle?,
    canvasWidth: Float,
    canvasHeight: Float,
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
        end = Offset(offset.x, canvasHeight),
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
    }
}

@Composable
fun RsiSubPane(candles: List<Candle>, rsiData: Map<Long, Double>, pan: Float, zoom: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .background(Color(0xFF0B0C10))
            .border(0.5.dp, Color(0xFF1E212B))
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val candleWidth = 14.dp.toPx() * zoom
            val startIndex = ((-pan) / candleWidth).toInt().coerceIn(0, candles.size - 1)
            val visibleCount = ((size.width / candleWidth).roundToInt() + 2).coerceAtMost(candles.size - startIndex)
            val visibleCandles = candles.subList(startIndex, (startIndex + visibleCount).coerceAtMost(candles.size))

            val y70 = size.height * (1f - (70f / 100f))
            val y30 = size.height * (1f - (30f / 100f))

            drawLine(Color(0xFFEF5350).copy(alpha = 0.5f), Offset(0f, y70), Offset(size.width, y70), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
            drawLine(Color(0xFF26A69A).copy(alpha = 0.5f), Offset(0f, y30), Offset(size.width, y30), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))

            val path = Path()
            var first = true
            for (i in visibleCandles.indices) {
                val rsi = rsiData[visibleCandles[i].openTime] ?: continue
                val x = pan + (startIndex + i) * candleWidth + candleWidth / 2
                val y = size.height * (1f - (rsi.toFloat() / 100f))
                if (first) { path.moveTo(x, y); first = false } else { path.lineTo(x, y) }
            }
            if (!first) drawPath(path, Color(0xFFBA68C8), style = Stroke(width = 1.5.dp.toPx()))
        }
        Text("RSI (14)", color = Color(0xFFBA68C8), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}

@Composable
fun MacdSubPane(candles: List<Candle>, macdData: Map<Long, MacdResult>, pan: Float, zoom: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(85.dp)
            .background(Color(0xFF0B0C10))
            .border(0.5.dp, Color(0xFF1E212B))
            .padding(vertical = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val candleWidth = 14.dp.toPx() * zoom
            val startIndex = ((-pan) / candleWidth).toInt().coerceIn(0, candles.size - 1)
            val visibleCount = ((size.width / candleWidth).roundToInt() + 2).coerceAtMost(candles.size - startIndex)
            val visibleCandles = candles.subList(startIndex, (startIndex + visibleCount).coerceAtMost(candles.size))

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
                val x = pan + (startIndex + i) * candleWidth + candleWidth / 2

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
