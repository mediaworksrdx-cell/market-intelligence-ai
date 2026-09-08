package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.IndicatorConfig
import com.example.marketintelligence.domain.chart.IndicatorType

/**
 * Bottom sheet for toggling indicators on/off and configuring their parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorSettingsSheet(
    activeIndicators: List<IndicatorConfig>,
    onToggleIndicator: (IndicatorType) -> Unit,
    onUpdateIndicator: (IndicatorConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Header ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INDICATORS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = colors.onBackground
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = colors.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Overlay Indicators Section ──
            Text(
                "OVERLAY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            val overlayTypes = IndicatorType.entries.filter { it.isOverlay }
            overlayTypes.forEach { type ->
                val config = activeIndicators.find { it.type == type }
                val isActive = config?.enabled == true

                IndicatorRow(
                    type = type,
                    isActive = isActive,
                    config = config,
                    onToggle = { onToggleIndicator(type) },
                    onUpdateConfig = onUpdateIndicator
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Panel Indicators Section ──
            Text(
                "SUB-PANELS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface.copy(alpha = 0.4f),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))

            val panelTypes = IndicatorType.entries.filter { !it.isOverlay }
            panelTypes.forEach { type ->
                val config = activeIndicators.find { it.type == type }
                val isActive = config?.enabled == true

                IndicatorRow(
                    type = type,
                    isActive = isActive,
                    config = config,
                    onToggle = { onToggleIndicator(type) },
                    onUpdateConfig = onUpdateIndicator
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun IndicatorRow(
    type: IndicatorType,
    isActive: Boolean,
    config: IndicatorConfig?,
    onToggle: () -> Unit,
    onUpdateConfig: (IndicatorConfig) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val indicatorColor = Color(IndicatorConfig.defaultColor(type))
    var showPeriodEditor by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Color indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) indicatorColor else indicatorColor.copy(alpha = 0.3f))
            )

            Column {
                Text(
                    type.label,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isActive) colors.onBackground else colors.onSurface.copy(alpha = 0.5f),
                    fontFamily = FontFamily.Monospace
                )
                if (isActive && config != null) {
                    Text(
                        "Period: ${config.period}",
                        fontSize = 9.sp,
                        color = colors.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { showPeriodEditor = !showPeriodEditor }
                    )
                }
            }
        }

        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            modifier = Modifier.height(20.dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = indicatorColor.copy(alpha = 0.5f),
                checkedThumbColor = indicatorColor
            )
        )
    }

    // Period editor
    if (showPeriodEditor && config != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 26.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Period:", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace)
            listOf(5, 9, 14, 20, 26, 50, 100, 200).forEach { period ->
                val isSelected = config.period == period
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) indicatorColor.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onUpdateConfig(config.copy(period = period)) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "$period",
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) indicatorColor else colors.onSurface.copy(alpha = 0.4f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
