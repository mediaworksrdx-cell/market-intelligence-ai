package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.marketintelligence.domain.chart.CursorMode
import com.example.marketintelligence.domain.chart.DrawingToolType
import com.example.marketintelligence.domain.chart.IndicatorConfig
import com.example.marketintelligence.domain.chart.IndicatorType

/**
 * Chart toolbar with dropdown selectors for Timeframe, Chart Type, Indicators, Drawing Tools, and Cursor Mode.
 */
@Composable
fun ChartToolbar(
    selectedTimeframe: String,
    chartType: ChartType,
    activeDrawingTool: DrawingToolType,
    activeIndicatorCount: Int,
    activeIndicators: List<IndicatorConfig> = emptyList(),
    cursorMode: CursorMode = CursorMode.CROSSHAIR,
    showVolume: Boolean = true,
    showVolumeProfile: Boolean = true,
    showFnoOverlay: Boolean = false,
    showSmcOverlay: Boolean = true,
    onTimeframeSelected: (String) -> Unit,
    onChartTypeSelected: (ChartType) -> Unit,
    onCursorModeChanged: (CursorMode) -> Unit = {},
    onToggleIndicator: (IndicatorType) -> Unit = {},
    onOpenIndicatorSettings: () -> Unit = {},
    onOpenIndicatorSettingsFor: ((IndicatorType) -> Unit)? = null,
    onDrawingToolSelected: (DrawingToolType) -> Unit = {},
    onClearDrawings: () -> Unit = {},
    onToggleIndicators: () -> Unit = {},
    onToggleDrawingTools: () -> Unit = {},
    onToggleVolume: () -> Unit = {},
    onToggleVolumeProfile: () -> Unit = {},
    onToggleFnoOverlay: () -> Unit = {},
    onToggleSmcOverlay: () -> Unit = {},
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onToggleFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val timeframes = listOf("1m", "5m", "15m", "30m", "1H", "4H", "1D", "1W", "1M")

    var activeMenu by remember { mutableStateOf(ActiveToolbarMenu.NONE) }

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
                    onClick = { activeMenu = if (activeMenu == ActiveToolbarMenu.TIMEFRAME) ActiveToolbarMenu.NONE else ActiveToolbarMenu.TIMEFRAME },
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
                    expanded = activeMenu == ActiveToolbarMenu.TIMEFRAME,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(colors.surface)
                        .heightIn(max = 280.dp)
                ) {
                    timeframes.forEach { tf ->
                        val isSelected = tf == selectedTimeframe
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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
                                activeMenu = ActiveToolbarMenu.NONE
                            }
                        )
                    }
                }
            }

            // ── 2. Chart Type Dropdown ──
            Box {
                Surface(
                    onClick = { activeMenu = if (activeMenu == ActiveToolbarMenu.CHART_TYPE) ActiveToolbarMenu.NONE else ActiveToolbarMenu.CHART_TYPE },
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
                    expanded = activeMenu == ActiveToolbarMenu.CHART_TYPE,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(colors.surface)
                        .heightIn(max = 280.dp)
                ) {
                    ChartType.entries.forEach { type ->
                        val isSelected = type == chartType
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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
                                activeMenu = ActiveToolbarMenu.NONE
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

            // ── 3. Indicators Dropdown (Compact, Categorized, Styled with Green Highlights & Direct Settings) ──
            Box {
                val activeGreen = Color(0xFF00E676)
                Surface(
                    onClick = { activeMenu = if (activeMenu == ActiveToolbarMenu.INDICATORS) ActiveToolbarMenu.NONE else ActiveToolbarMenu.INDICATORS },
                    shape = RoundedCornerShape(6.dp),
                    color = if (activeIndicatorCount > 0) activeGreen.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.5.dp,
                        if (activeIndicatorCount > 0) activeGreen.copy(alpha = 0.8f) else colors.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (activeIndicatorCount > 0) "Ind ($activeIndicatorCount)" else "Indicators",
                            color = if (activeIndicatorCount > 0) activeGreen else colors.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Indicator",
                            tint = if (activeIndicatorCount > 0) activeGreen else colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = activeMenu == ActiveToolbarMenu.INDICATORS,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(colors.surface)
                        .heightIn(max = 340.dp)
                        .widthIn(min = 195.dp, max = 240.dp)
                ) {
                    // Quick Settings Shortcut
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = activeGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "⚙ Indicator Settings",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeGreen,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        onClick = {
                            activeMenu = ActiveToolbarMenu.NONE
                            onOpenIndicatorSettings()
                        }
                    )

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    // Header: Overlays
                    DropdownMenuItem(
                        enabled = false,
                        onClick = {},
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 1.dp),
                        text = {
                            Text(
                                text = "── OVERLAYS ──",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.onSurface.copy(alpha = 0.4f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )

                    val overlayTypes = IndicatorType.entries.filter { it.isOverlay }
                    overlayTypes.forEach { indicator ->
                        val isEnabled = activeIndicators.any { it.type == indicator && it.enabled }
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = if (isEnabled) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                            text = {
                                Text(
                                    text = indicator.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isEnabled) activeGreen else colors.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isEnabled) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = activeGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            activeMenu = ActiveToolbarMenu.NONE
                                            if (onOpenIndicatorSettingsFor != null) {
                                                onOpenIndicatorSettingsFor(indicator)
                                            } else {
                                                onOpenIndicatorSettings()
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Edit ${indicator.label}",
                                            tint = if (isEnabled) activeGreen else colors.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onToggleIndicator(indicator)
                                // If activating for the first time, open settings so default values are immediately visible/editable
                                if (!isEnabled && onOpenIndicatorSettingsFor != null) {
                                    activeMenu = ActiveToolbarMenu.NONE
                                    onOpenIndicatorSettingsFor(indicator)
                                }
                            }
                        )
                    }

                    // Smart overlays (Volume Bars, Volume Profile, SMC, F&O)
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    // Volume Bars
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = if (showVolume) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                        text = {
                            Text(
                                text = "Volume Bars",
                                fontSize = 11.sp,
                                fontWeight = if (showVolume) FontWeight.Bold else FontWeight.Normal,
                                color = if (showVolume) activeGreen else colors.onSurface,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = if (showVolume) {
                            {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = activeGreen, modifier = Modifier.size(14.dp))
                            }
                        } else null,
                        onClick = {
                            onToggleVolume()
                            activeMenu = ActiveToolbarMenu.NONE
                        }
                    )

                    // Volume Profile (VPVR)
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = if (showVolumeProfile) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                        text = {
                            Text(
                                text = "Volume Profile (VPVR)",
                                fontSize = 11.sp,
                                fontWeight = if (showVolumeProfile) FontWeight.Bold else FontWeight.Normal,
                                color = if (showVolumeProfile) activeGreen else colors.onSurface,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = if (showVolumeProfile) {
                            {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = activeGreen, modifier = Modifier.size(14.dp))
                            }
                        } else null,
                        onClick = {
                            onToggleVolumeProfile()
                            activeMenu = ActiveToolbarMenu.NONE
                        }
                    )

                    val smcEnabled = showSmcOverlay
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = if (smcEnabled) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                        text = {
                            Text(
                                text = "SMC (Orderblocks)",
                                fontSize = 11.sp,
                                fontWeight = if (smcEnabled) FontWeight.Bold else FontWeight.Normal,
                                color = if (smcEnabled) activeGreen else colors.onSurface,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = if (smcEnabled) {
                            {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = activeGreen, modifier = Modifier.size(14.dp))
                            }
                        } else null,
                        onClick = {
                            onToggleSmcOverlay()
                            activeMenu = ActiveToolbarMenu.NONE
                        }
                    )

                    val fnoEnabled = showFnoOverlay
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = if (fnoEnabled) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                        text = {
                            Text(
                                text = "F&O GEX Walls",
                                fontSize = 11.sp,
                                fontWeight = if (fnoEnabled) FontWeight.Bold else FontWeight.Normal,
                                color = if (fnoEnabled) activeGreen else colors.onSurface,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = if (fnoEnabled) {
                            {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = activeGreen, modifier = Modifier.size(14.dp))
                            }
                        } else null,
                        onClick = {
                            onToggleFnoOverlay()
                            activeMenu = ActiveToolbarMenu.NONE
                        }
                    )

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    // Header: Sub-Panels
                    DropdownMenuItem(
                        enabled = false,
                        onClick = {},
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 1.dp),
                        text = {
                            Text(
                                text = "── SUB-PANELS ──",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = colors.onSurface.copy(alpha = 0.4f),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )

                    val panelTypes = IndicatorType.entries.filter { !it.isOverlay }
                    panelTypes.forEach { indicator ->
                        val isEnabled = activeIndicators.any { it.type == indicator && it.enabled }
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = if (isEnabled) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                            text = {
                                Text(
                                    text = indicator.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isEnabled) activeGreen else colors.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isEnabled) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Active",
                                            tint = activeGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            activeMenu = ActiveToolbarMenu.NONE
                                            if (onOpenIndicatorSettingsFor != null) {
                                                onOpenIndicatorSettingsFor(indicator)
                                            } else {
                                                onOpenIndicatorSettings()
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Edit ${indicator.label}",
                                            tint = if (isEnabled) activeGreen else colors.onSurface.copy(alpha = 0.4f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onToggleIndicator(indicator)
                                if (!isEnabled && onOpenIndicatorSettingsFor != null) {
                                    activeMenu = ActiveToolbarMenu.NONE
                                    onOpenIndicatorSettingsFor(indicator)
                                }
                            }
                        )
                    }
                }
            }

            // ── 4. Drawing Tools Dropdown (Styled like Candle Dropdown) ──
            Box {
                Surface(
                    onClick = { activeMenu = if (activeMenu == ActiveToolbarMenu.DRAW) ActiveToolbarMenu.NONE else ActiveToolbarMenu.DRAW },
                    shape = RoundedCornerShape(6.dp),
                    color = if (activeDrawingTool != DrawingToolType.NONE) colors.secondary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        0.5.dp,
                        if (activeDrawingTool != DrawingToolType.NONE) colors.secondary.copy(alpha = 0.6f) else colors.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (activeDrawingTool != DrawingToolType.NONE) activeDrawingTool.label else "Draw",
                            color = if (activeDrawingTool != DrawingToolType.NONE) colors.secondary else colors.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Drawing Tool",
                            tint = if (activeDrawingTool != DrawingToolType.NONE) colors.secondary else colors.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = activeMenu == ActiveToolbarMenu.DRAW,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(colors.surface)
                        .heightIn(max = 280.dp)
                        .widthIn(min = 160.dp, max = 210.dp)
                ) {
                    DrawingToolType.entries.forEach { tool ->
                        val isSelected = activeDrawingTool == tool
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                onDrawingToolSelected(tool)
                                activeMenu = ActiveToolbarMenu.NONE
                            }
                        )
                    }

                    HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))

                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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
                            activeMenu = ActiveToolbarMenu.NONE
                            onClearDrawings()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // ── Undo/Redo Buttons (Visible only when drawing tool is active) ──
        if (activeDrawingTool != DrawingToolType.NONE) {
            IconButton(
                onClick = onUndo,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            IconButton(
                onClick = onRedo,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = colors.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }

        // ── Cursor Mode Toggle (Crosshair vs Pan/Hand) ──
        IconButton(
            onClick = {
                val nextMode = if (cursorMode == CursorMode.CROSSHAIR) CursorMode.HAND else CursorMode.CROSSHAIR
                onCursorModeChanged(nextMode)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (cursorMode == CursorMode.CROSSHAIR) Icons.Default.FilterCenterFocus else Icons.Default.PanTool,
                contentDescription = if (cursorMode == CursorMode.CROSSHAIR) "Crosshair Mode" else "Pan Mode",
                tint = colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
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
                tint = colors.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

enum class ActiveToolbarMenu {
    NONE,
    TIMEFRAME,
    CHART_TYPE,
    INDICATORS,
    DRAW
}

