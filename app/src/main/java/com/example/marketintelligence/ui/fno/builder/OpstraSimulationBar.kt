package com.example.marketintelligence.ui.fno.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun OpstraSimulationBar(
    spotPrice: Double,
    baseIv: Double,
    spotShiftPct: Double,
    ivShift: Double,
    timeShiftDays: Int,
    onSimulationChange: (Double, Double, Int) -> Unit,
    onResetSimulation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val simulatedSpot = spotPrice * (1.0 + spotShiftPct / 100.0)
    val simulatedIv = (baseIv + ivShift).coerceAtLeast(1.0)
    val isShifted = spotShiftPct != 0.0 || ivShift != 0.0 || timeShiftDays != 0

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isShifted) 1.dp else 0.5.dp,
                color = if (isShifted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        "MARKET SCENARIO SIMULATOR",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (isShifted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onResetSimulation() }
                            .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                            Text("Reset Simulation", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 1. Underlying Spot Price Slider
            val spotSign = if (spotShiftPct > 0) "+" else ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("UNDERLYING SPOT SHIFT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    "₹${String.format(Locale.US, "%,.1f", simulatedSpot)} ($spotSign${String.format(Locale.US, "%.1f", spotShiftPct)}%)",
                    color = if (spotShiftPct != 0.0) OpstraCyan else MaterialTheme.colorScheme.onSurface,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = spotShiftPct.toFloat(),
                onValueChange = { onSimulationChange(it.toDouble(), ivShift, timeShiftDays) },
                valueRange = -10f..10f,
                steps = 19,
                modifier = Modifier.height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = OpstraCyan,
                    activeTrackColor = OpstraCyan
                )
            )

            Spacer(Modifier.height(4.dp))

            // 2. Implied Volatility Slider
            val ivSign = if (ivShift > 0) "+" else ""
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("IMPLIED VOLATILITY (IV) SHIFT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${String.format(Locale.US, "%.1f", simulatedIv)}% ($ivSign${String.format(Locale.US, "%.1f", ivShift)}%)",
                    color = if (ivShift != 0.0) OpstraAmber else MaterialTheme.colorScheme.onSurface,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = ivShift.toFloat(),
                onValueChange = { onSimulationChange(spotShiftPct, it.toDouble(), timeShiftDays) },
                valueRange = -20f..20f,
                steps = 39,
                modifier = Modifier.height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = OpstraAmber,
                    activeTrackColor = OpstraAmber
                )
            )

            Spacer(Modifier.height(4.dp))

            // 3. Time to Expiry (Days Decay) Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TIME ELAPSED (DAYS TO DECAY)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    "+$timeShiftDays Days Decay (${maxOf(0, 5 - timeShiftDays)} DTE remaining)",
                    color = if (timeShiftDays > 0) Color(0xFFFF80AB) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Slider(
                value = timeShiftDays.toFloat(),
                onValueChange = { onSimulationChange(spotShiftPct, ivShift, it.roundToInt()) },
                valueRange = 0f..5f,
                steps = 4,
                modifier = Modifier.height(26.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF80AB),
                    activeTrackColor = Color(0xFFFF80AB)
                )
            )
        }
    }
}
