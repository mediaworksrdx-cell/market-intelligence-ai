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

    // Dedicated high-contrast colors for white background to prevent invisible icons in dark themes
    val btnBgInactive = Color(0xFFF2F4F7)
    val btnBorderInactive = Color(0xFFD0D5DD)
    val iconColorInactive = Color(0xFF344054)
    val activeGreen = Color(0xFF089981)
    val activeBlue = Color(0xFF0284C7)
    val dropdownBg = Color.White
    val dropdownText = Color(0xFF1D2939)

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
                    color = btnBgInactive,
                    border = BorderStroke(0.8.dp, btnBorderInactive)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = selectedTimeframe,
                            color = activeGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Timeframe",
                            tint = iconColorInactive,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = activeMenu == ActiveToolbarMenu.TIMEFRAME,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(dropdownBg)
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
                                    color = if (isSelected) activeGreen else dropdownText,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = activeGreen,
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

            // ── 2. Chart Type Icon Dropdown (Compact Candlestick Icon) ──
            Box {
                val isChartTypeOpen = activeMenu == ActiveToolbarMenu.CHART_TYPE
                Surface(
                    onClick = { activeMenu = if (isChartTypeOpen) ActiveToolbarMenu.NONE else ActiveToolbarMenu.CHART_TYPE },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isChartTypeOpen) Color(0xFFE0F2FE) else btnBgInactive,
                    border = BorderStroke(0.8.dp, if (isChartTypeOpen) activeBlue else btnBorderInactive)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Chart Type (${chartType.label})",
                            tint = if (isChartTypeOpen) activeBlue else iconColorInactive,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isChartTypeOpen,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(dropdownBg)
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
                                    color = if (isSelected) activeBlue else dropdownText,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = activeBlue,
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

            // ── 3. Indicators Icon Dropdown (Compact fx/ShowChart Icon with Badge) ──
            Box {
                val isIndOpen = activeMenu == ActiveToolbarMenu.INDICATORS
                val hasIndicators = activeIndicatorCount > 0
                Surface(
                    onClick = { activeMenu = if (isIndOpen) ActiveToolbarMenu.NONE else ActiveToolbarMenu.INDICATORS },
                    shape = RoundedCornerShape(6.dp),
                    color = if (hasIndicators) Color(0xFFE8F5E9) else if (isIndOpen) Color(0xFFE0F2FE) else btnBgInactive,
                    border = BorderStroke(
                        0.8.dp,
                        if (hasIndicators) activeGreen else if (isIndOpen) activeBlue else btnBorderInactive
                    )
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Indicators",
                            tint = if (hasIndicators) activeGreen else if (isIndOpen) activeBlue else iconColorInactive,
                            modifier = Modifier.size(18.dp)
                        )
                        if (hasIndicators) {
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(6.dp)
                                    .background(activeGreen, RoundedCornerShape(3.dp))
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isIndOpen,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(dropdownBg)
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

                    HorizontalDivider(color = btnBorderInactive.copy(alpha = 0.5f))

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
                                color = dropdownText.copy(alpha = 0.5f),
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
                                    color = if (isEnabled) activeGreen else dropdownText,
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
                                            tint = if (isEnabled) activeGreen else iconColorInactive,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onToggleIndicator(indicator)
                            }
                        )
                    }

                    // Smart overlays (Volume Bars, Volume Profile, SMC, F&O)
                    HorizontalDivider(color = btnBorderInactive.copy(alpha = 0.5f))

                    // Volume Bars
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = if (showVolume) Modifier.background(activeGreen.copy(alpha = 0.10f)) else Modifier,
                        text = {
                            Text(
                                text = "Volume Bars",
                                fontSize = 11.sp,
                                fontWeight = if (showVolume) FontWeight.Bold else FontWeight.Normal,
                                color = if (showVolume) activeGreen else dropdownText,
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
                                color = if (showVolumeProfile) activeGreen else dropdownText,
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
                                color = if (smcEnabled) activeGreen else dropdownText,
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
                                color = if (fnoEnabled) activeGreen else dropdownText,
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

                    HorizontalDivider(color = btnBorderInactive.copy(alpha = 0.5f))

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
                                color = dropdownText.copy(alpha = 0.5f),
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
                                    color = if (isEnabled) activeGreen else dropdownText,
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
                                            tint = if (isEnabled) activeGreen else iconColorInactive,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onToggleIndicator(indicator)
                            }
                        )
                    }
                }
            }

            // ── 4. Drawing Tools Icon Dropdown (Compact Edit/Pencil Icon) ──
            Box {
                val isDrawActive = activeDrawingTool != DrawingToolType.NONE
                Surface(
                    onClick = { activeMenu = if (activeMenu == ActiveToolbarMenu.DRAW) ActiveToolbarMenu.NONE else ActiveToolbarMenu.DRAW },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDrawActive) Color(0xFFE0F2FE) else btnBgInactive,
                    border = BorderStroke(
                        0.8.dp,
                        if (isDrawActive) activeBlue else btnBorderInactive
                    )
                ) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Drawing Tools (${if (isDrawActive) activeDrawingTool.label else "None"})",
                            tint = if (isDrawActive) activeBlue else iconColorInactive,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = activeMenu == ActiveToolbarMenu.DRAW,
                    onDismissRequest = { activeMenu = ActiveToolbarMenu.NONE },
                    modifier = Modifier
                        .background(dropdownBg)
                        .heightIn(max = 320.dp)
                        .widthIn(min = 180.dp, max = 230.dp)
                ) {
                    DropdownMenuItem(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = "Palette",
                                    tint = activeBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "🎨 All Drawing Tools",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeBlue,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        onClick = {
                            activeMenu = ActiveToolbarMenu.NONE
                            onToggleDrawingTools()
                        }
                    )

                    HorizontalDivider(color = btnBorderInactive.copy(alpha = 0.5f))

                    DrawingToolType.entries.forEach { tool ->
                        val isSelected = activeDrawingTool == tool
                        DropdownMenuItem(
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            text = {
                                Text(
                                    text = if (tool == DrawingToolType.NONE) "None (Select/Pan)" else tool.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) activeBlue else dropdownText,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = activeBlue,
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

                    HorizontalDivider(color = btnBorderInactive.copy(alpha = 0.5f))

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

            // ── 5. Quick Palette Button (Brush next to Draw) ──
            Surface(
                onClick = onToggleDrawingTools,
                shape = RoundedCornerShape(6.dp),
                color = if (activeDrawingTool != DrawingToolType.NONE) Color(0xFFE0F2FE) else btnBgInactive,
                border = BorderStroke(
                    0.8.dp,
                    if (activeDrawingTool != DrawingToolType.NONE) activeBlue else btnBorderInactive
                )
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Brush,
                        contentDescription = "Drawing Tools Palette",
                        tint = if (activeDrawingTool != DrawingToolType.NONE) activeBlue else iconColorInactive,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(4.dp))

        // ── Undo/Redo Buttons (Visible only when drawing tool is active) ──
        if (activeDrawingTool != DrawingToolType.NONE) {
            Surface(
                onClick = onUndo,
                shape = RoundedCornerShape(6.dp),
                color = btnBgInactive,
                border = BorderStroke(0.8.dp, btnBorderInactive)
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = iconColorInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

            Surface(
                onClick = onRedo,
                shape = RoundedCornerShape(6.dp),
                color = btnBgInactive,
                border = BorderStroke(0.8.dp, btnBorderInactive)
            ) {
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = iconColorInactive,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
        }

        // ── Cursor Mode Toggle (Crosshair vs Pan/Hand) ──
        Surface(
            onClick = {
                val nextMode = if (cursorMode == CursorMode.CROSSHAIR) CursorMode.HAND else CursorMode.CROSSHAIR
                onCursorModeChanged(nextMode)
            },
            shape = RoundedCornerShape(6.dp),
            color = if (cursorMode == CursorMode.CROSSHAIR) Color(0xFFE8F5E9) else btnBgInactive,
            border = BorderStroke(
                0.8.dp,
                if (cursorMode == CursorMode.CROSSHAIR) activeGreen else btnBorderInactive
            )
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (cursorMode == CursorMode.CROSSHAIR) Icons.Default.FilterCenterFocus else Icons.Default.PanTool,
                    contentDescription = if (cursorMode == CursorMode.CROSSHAIR) "Crosshair Mode" else "Pan Mode",
                    tint = if (cursorMode == CursorMode.CROSSHAIR) activeGreen else iconColorInactive,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // ── Fullscreen Button ──
        Surface(
            onClick = onToggleFullScreen,
            shape = RoundedCornerShape(6.dp),
            color = btnBgInactive,
            border = BorderStroke(0.8.dp, btnBorderInactive)
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = iconColorInactive,
                    modifier = Modifier.size(20.dp)
                )
            }
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

