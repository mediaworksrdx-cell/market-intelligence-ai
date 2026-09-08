package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.ChartType
import com.example.marketintelligence.domain.chart.DrawingToolType
import com.example.marketintelligence.domain.chart.IndicatorConfig
import com.example.marketintelligence.domain.chart.IndicatorType

/**
 * Chart toolbar with dropdown selectors for Timeframe, Chart Type, Indicators, and Drawing Tools.
 */
@Composable
fun ChartToolbar(
    selectedTimeframe: String,
    chartType: ChartType,
    activeDrawingTool: DrawingToolType,
    activeIndicatorCount: Int,
    activeIndicators: List<IndicatorConfig> = emptyList(),
    showVolumeProfile: Boolean = true,
    showFnoOverlay: Boolean = true,
    showSmcOverlay: Boolean = true,
    onTimeframeSelected: (String) -> Unit,
    onChartTypeSelected: (ChartType) -> Unit,
    onToggleIndicator: (IndicatorType) -> Unit = {},
    onOpenIndicatorSettings: () -> Unit = {},
    onDrawingToolSelected: (DrawingToolType) -> Unit = {},
    onClearDrawings: () -> Unit = {},
    onToggleIndicators: () -> Unit = {},
    onToggleDrawingTools: () -> Unit = {},
    onToggleVolumeProfile: () -> Unit = {},
    onToggleFnoOverlay: () -> Unit = {},
    onToggleSmcOverlay: () -> Unit = {},
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val timeframes = listOf("1m", "5m", "15m", "30m", "1H", "4H", "1D", "1W", "1M")

    var timeframeExpanded by remember { mutableStateOf(false) }
    var chartTypeExpanded by remember { mutableStateOf(false) }
    var indicatorsExpanded by remember { mutableStateOf(false) }
    var drawExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 1. Timeframe Dropdown ──
            Box {
                Surface(
                    onClick = { timeframeExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = selectedTimeframe,
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Timeframe",
                            tint = colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = timeframeExpanded,
                    onDismissRequest = { timeframeExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    timeframes.forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = tf,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onTimeframeSelected(tf)
                                timeframeExpanded = false
                            }
                        )
                    }
                }
            }

            // ── 2. Chart Type Dropdown ──
            Box {
                Surface(
                    onClick = { chartTypeExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, colors.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = chartType.label,
                            color = colors.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Chart Type",
                            tint = colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = chartTypeExpanded,
                    onDismissRequest = { chartTypeExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    ChartType.entries.forEach { type ->
                        val isSelected = type == chartType
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = type.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onChartTypeSelected(type)
                                chartTypeExpanded = false
                            }
                        )
                    }
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(colors.outline.copy(alpha = 0.3f))
            )

            // ── 3. Indicators Dropdown ──
            Box {
                Surface(
                    onClick = { indicatorsExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    color = if (activeIndicatorCount > 0) colors.primary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.5.dp,
                        if (activeIndicatorCount > 0) colors.primary.copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Indicators",
                            tint = if (activeIndicatorCount > 0) colors.primary else colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Indicators",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (activeIndicatorCount > 0) colors.primary else colors.onSurface.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                        if (activeIndicatorCount > 0) {
                            Surface(
                                color = colors.primary,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(16.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "$activeIndicatorCount",
                                        color = colors.onPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Indicator",
                            tint = colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = indicatorsExpanded,
                    onDismissRequest = { indicatorsExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    IndicatorType.entries.forEach { indicator ->
                        val isEnabled = activeIndicators.any { it.type == indicator && it.enabled }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = indicator.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isEnabled) colors.primary else colors.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isEnabled) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Active",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onToggleIndicator(indicator)
                            }
                        )
                    }

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Settings & Parameters...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.secondary,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Settings",
                                tint = colors.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            indicatorsExpanded = false
                            if (onOpenIndicatorSettings != {}) {
                                onOpenIndicatorSettings()
                            } else {
                                onToggleIndicators()
                            }
                        }
                    )
                }
            }

            // ── 4. Drawing Tools Dropdown ──
            Box {
                Surface(
                    onClick = { drawExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    color = if (activeDrawingTool != DrawingToolType.NONE) colors.secondary.copy(alpha = 0.12f) else colors.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.5.dp,
                        if (activeDrawingTool != DrawingToolType.NONE) colors.secondary.copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Draw,
                            contentDescription = "Drawing Tools",
                            tint = if (activeDrawingTool != DrawingToolType.NONE) colors.secondary else colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (activeDrawingTool != DrawingToolType.NONE) activeDrawingTool.label else "Draw",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (activeDrawingTool != DrawingToolType.NONE) colors.secondary else colors.onSurface.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Drawing Tool",
                            tint = colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = drawExpanded,
                    onDismissRequest = { drawExpanded = false },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DrawingToolType.entries.forEach { tool ->
                        val isSelected = activeDrawingTool == tool
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (tool == DrawingToolType.NONE) "None (Select/Pan)" else tool.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.secondary else colors.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colors.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onDrawingToolSelected(tool)
                                drawExpanded = false
                            }
                        )
                    }

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Clear All Drawings",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            drawExpanded = false
                            onClearDrawings()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // ── Fullscreen Button ──
        IconButton(
            onClick = onToggleFullScreen,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Fullscreen,
                contentDescription = "Fullscreen",
                tint = colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

