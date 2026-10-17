package com.example.marketintelligence.ui.fno.builder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.engine.StrategyAnalysisResult
import com.example.marketintelligence.domain.model.StrategyLeg
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

val OpstraGreen = Color(0xFF00E676)
val OpstraRed = Color(0xFFFF5252)
val OpstraCyan = Color(0xFF00E5FF)
val OpstraAmber = Color(0xFFFFD54F)

@Composable
fun OpstraPayoffChart(
    analysisResult: StrategyAnalysisResult?,
    spotPrice: Double,
    legs: List<StrategyLeg>,
    rangePct: Double = 0.15,
    onRangeChange: (Double) -> Unit = {},
    spotSimulationPct: Double = 0.0,
    modifier: Modifier = Modifier
) {
    if (analysisResult == null || analysisResult.payoffPoints.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "No strategy legs added. Add legs to render institutional payoff chart.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        return
    }

    val expiryPoints = analysisResult.payoffPoints
    val todayPoints = analysisResult.todayPayoffPoints
    val breakevens = analysisResult.breakevens

    val simulatedSpot = spotPrice * (1.0 + spotSimulationPct / 100.0)
    var crosshairPrice by remember(analysisResult, spotPrice, spotSimulationPct) { mutableStateOf<Double?>(null) }
    val activeTargetPrice = crosshairPrice ?: if (abs(spotSimulationPct) > 0.01) simulatedSpot else spotPrice

    // Range filtering
    val centerPrice = if (abs(spotSimulationPct) > 0.01) simulatedSpot else spotPrice
    val lowerBound = centerPrice * (1.0 - rangePct)
    val upperBound = centerPrice * (1.0 + rangePct)
    val filteredExpiry = expiryPoints.filter { it.price in lowerBound..upperBound }.ifEmpty { expiryPoints }
    val filteredToday = todayPoints.filter { it.price in lowerBound..upperBound }.ifEmpty { todayPoints }

    val minPrice = filteredExpiry.minOf { it.price }
    val maxPrice = filteredExpiry.maxOf { it.price }
    val priceSpan = (maxPrice - minPrice).coerceAtLeast(1.0).toFloat()

    val allPnlValues = filteredExpiry.map { it.pnl } + filteredToday.map { it.pnl }
    val minPnl = min(-100.0, allPnlValues.minOrNull() ?: -100.0)
    val maxPnl = max(100.0, allPnlValues.maxOrNull() ?: 100.0)
    val pnlSpan = (maxPnl - minPnl).coerceAtLeast(1.0).toFloat()

    val selectedExpPnl = activeTargetPrice.let { p ->
        filteredExpiry.minByOrNull { abs(it.price - p) }?.pnl
    }
    val selectedTodayPnl = activeTargetPrice.let { p ->
        filteredToday.minByOrNull { abs(it.price - p) }?.pnl
    }

    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Chart Header with Range Selector & Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Legend: Expiry
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp, 3.dp).background(OpstraGreen, RoundedCornerShape(1.dp)))
                    Text("Expiry", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                // Legend: T+0 Today
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp, 3.dp).background(OpstraAmber, RoundedCornerShape(1.dp)))
                    Text("T+0 Today", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                // Legend: Spot
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp, 2.dp).background(OpstraCyan, RoundedCornerShape(1.dp)))
                    Text("Spot", color = OpstraCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Range Chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0.05 to "±5%", 0.10 to "±10%", 0.15 to "±15%", 0.20 to "±20%").forEach { (pct, label) ->
                    val isSelected = abs(rangePct - pct) < 0.01
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else surfaceVariantColor,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onRangeChange(pct) }
                            .border(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else outlineColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 8.5.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Interactive HUD Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hudPrice = activeTargetPrice
                val diffFromSpot = hudPrice - spotPrice
                val diffPct = if (spotPrice > 0.0) (diffFromSpot / spotPrice) * 100.0 else 0.0
                val sign = if (diffFromSpot >= 0) "+" else ""

                Column {
                    Text("TARGET PRICE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", hudPrice)} ($sign${String.format(Locale.US, "%.2f", diffPct)}%)",
                        color = if (abs(diffFromSpot) > 0.01) OpstraCyan else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("T+0 P&L", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    val pnlT0 = selectedTodayPnl ?: (todayPoints.minByOrNull { abs(it.price - activeTargetPrice) }?.pnl ?: 0.0)
                    val t0Color = if (pnlT0 >= 0.0) OpstraGreen else OpstraRed
                    val t0Sign = if (pnlT0 >= 0.0) "+" else ""
                    Text(
                        "$t0Sign₹${String.format(Locale.US, "%,.1f", pnlT0)}",
                        color = t0Color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("EXPIRY P&L", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                    val pnlExp = selectedExpPnl ?: (expiryPoints.minByOrNull { abs(it.price - activeTargetPrice) }?.pnl ?: 0.0)
                    val expColor = if (pnlExp >= 0.0) OpstraGreen else OpstraRed
                    val expSign = if (pnlExp >= 0.0) "+" else ""
                    Text(
                        "$expSign₹${String.format(Locale.US, "%,.1f", pnlExp)}",
                        color = expColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Main Payoff Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val priceAtX = minPrice + (offset.x / size.width).coerceIn(0f, 1f) * priceSpan
                                crosshairPrice = priceAtX
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val priceAtX = minPrice + (change.position.x / size.width).coerceIn(0f, 1f) * priceSpan
                                crosshairPrice = priceAtX
                            },
                            onDragEnd = { /* keep last crosshair or clear */ },
                            onDragCancel = { crosshairPrice = null }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val priceAtX = minPrice + (offset.x / size.width).coerceIn(0f, 1f) * priceSpan
                            crosshairPrice = priceAtX
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                fun priceToX(price: Double): Float =
                    (canvasW * ((price - minPrice) / priceSpan)).toFloat().coerceIn(0f, canvasW)

                fun pnlToY(pnl: Double): Float =
                    (canvasH * (1.0 - (pnl - minPnl) / pnlSpan)).toFloat().coerceIn(0f, canvasH)

                val zeroY = pnlToY(0.0)

                // 1. Shaded Profit and Loss Zones
                val profitZonePath = Path()
                val lossZonePath = Path()

                profitZonePath.moveTo(0f, zeroY)
                lossZonePath.moveTo(0f, zeroY)

                filteredExpiry.forEach { pt ->
                    val x = priceToX(pt.price)
                    val y = pnlToY(pt.pnl)
                    if (pt.pnl >= 0.0) {
                        profitZonePath.lineTo(x, y)
                    } else {
                        profitZonePath.lineTo(x, zeroY)
                    }
                    if (pt.pnl < 0.0) {
                        lossZonePath.lineTo(x, y)
                    } else {
                        lossZonePath.lineTo(x, zeroY)
                    }
                }
                profitZonePath.lineTo(canvasW, zeroY)
                profitZonePath.close()

                lossZonePath.lineTo(canvasW, zeroY)
                lossZonePath.close()

                drawPath(profitZonePath, OpstraGreen.copy(alpha = 0.12f))
                drawPath(lossZonePath, OpstraRed.copy(alpha = 0.12f))

                // 2. Zero-profit horizontal reference line
                drawLine(
                    color = outlineColor.copy(alpha = 0.6f),
                    start = Offset(0f, zeroY),
                    end = Offset(canvasW, zeroY),
                    strokeWidth = 1.dp.toPx()
                )

                // 3. Grid lines (horizontal for key P&L levels)
                val pnlSteps = listOf(maxPnl * 0.5, minPnl * 0.5)
                pnlSteps.forEach { level ->
                    val y = pnlToY(level)
                    drawLine(
                        color = outlineColor.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(canvasW, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }

                // 4. Strike markers on bottom / zero line
                legs.filter { it.isEnabled }.forEach { leg ->
                    val sx = priceToX(leg.strike)
                    if (sx in 0f..canvasW) {
                        drawLine(
                            color = outlineColor.copy(alpha = 0.35f),
                            start = Offset(sx, 0f),
                            end = Offset(sx, canvasH),
                            strokeWidth = 0.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                        drawCircle(
                            color = if (leg.type.equals("BUY", ignoreCase = true)) OpstraGreen else OpstraRed,
                            radius = 2.5.dp.toPx(),
                            center = Offset(sx, zeroY)
                        )
                    }
                }

                // 5. Expiry Payoff Curve (Solid Green / Cyan)
                val expPath = Path()
                filteredExpiry.forEachIndexed { i, pt ->
                    val x = priceToX(pt.price)
                    val y = pnlToY(pt.pnl)
                    if (i == 0) expPath.moveTo(x, y) else expPath.lineTo(x, y)
                }
                drawPath(expPath, OpstraGreen, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))

                // 6. T+0 Theoretical Curve (Amber Gold, dashed/smooth)
                if (filteredToday.isNotEmpty()) {
                    val todayPath = Path()
                    filteredToday.forEachIndexed { i, pt ->
                        val x = priceToX(pt.price)
                        val y = pnlToY(pt.pnl)
                        if (i == 0) todayPath.moveTo(x, y) else todayPath.lineTo(x, y)
                    }
                    drawPath(
                        todayPath,
                        OpstraAmber,
                        style = Stroke(
                            width = 1.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // 7. Breakeven Markers
                breakevens.forEach { be ->
                    val bx = priceToX(be)
                    if (bx in 0f..canvasW) {
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(bx, zeroY)
                        )
                        drawCircle(
                            color = OpstraAmber,
                            radius = 2.5.dp.toPx(),
                            center = Offset(bx, zeroY)
                        )
                    }
                }

                // 8. Spot Price Marker (Vertical dashed Cyan line)
                val spotX = priceToX(spotPrice)
                val isSimulated = abs(spotSimulationPct) > 0.01
                if (spotX in 0f..canvasW) {
                    drawLine(
                        color = OpstraCyan.copy(alpha = if (isSimulated) 0.4f else 1f),
                        start = Offset(spotX, 0f),
                        end = Offset(spotX, canvasH),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawCircle(
                        color = OpstraCyan.copy(alpha = if (isSimulated) 0.4f else 1f),
                        radius = 4.dp.toPx(),
                        center = Offset(spotX, zeroY)
                    )
                }

                // 8b. Simulated Target Price Marker (Glowing vertical line at simulatedSpot)
                if (isSimulated) {
                    val simX = priceToX(simulatedSpot)
                    if (simX in 0f..canvasW) {
                        drawLine(
                            color = OpstraCyan,
                            start = Offset(simX, 0f),
                            end = Offset(simX, canvasH),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
                        )
                        drawCircle(
                            color = OpstraCyan,
                            radius = 5.dp.toPx(),
                            center = Offset(simX, zeroY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(simX, zeroY)
                        )
                    }
                }

                // 9. Interactive Touch Crosshair
                crosshairPrice?.let { cPrice ->
                    val cx = priceToX(cPrice)
                    if (cx in 0f..canvasW) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.8f),
                            start = Offset(cx, 0f),
                            end = Offset(cx, canvasH),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                        selectedExpPnl?.let { ep ->
                            val ey = pnlToY(ep)
                            drawCircle(OpstraGreen, radius = 5.dp.toPx(), center = Offset(cx, ey))
                        }
                        selectedTodayPnl?.let { tp ->
                            val ty = pnlToY(tp)
                            drawCircle(OpstraAmber, radius = 4.dp.toPx(), center = Offset(cx, ty))
                        }
                    }
                }
            }
        }

        // Bottom Breakevens & Spot Chips Row
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Breakevens listing
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Breakevens:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                if (breakevens.isEmpty()) {
                    Text("None", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp, fontWeight = FontWeight.Medium)
                } else {
                    breakevens.forEach { be ->
                        Surface(
                            color = OpstraAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(3.dp),
                            modifier = Modifier.border(0.5.dp, OpstraAmber.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        ) {
                            Text(
                                "₹${be.toInt()}",
                                color = OpstraAmber,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Spot / Simulated Target indicator
            Surface(
                color = OpstraCyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(3.dp),
                modifier = Modifier.border(0.5.dp, OpstraCyan.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
            ) {
                val label = if (abs(spotSimulationPct) > 0.01) {
                    val sign = if (spotSimulationPct > 0) "+" else ""
                    "Target: ₹${String.format(Locale.US, "%,.1f", simulatedSpot)} ($sign${String.format(Locale.US, "%.1f", spotSimulationPct)}%)"
                } else {
                    "Spot: ₹${String.format(Locale.US, "%,.1f", spotPrice)}"
                }
                Text(
                    label,
                    color = OpstraCyan,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
