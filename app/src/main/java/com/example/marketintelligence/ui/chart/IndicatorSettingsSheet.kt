package com.example.marketintelligence.ui.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
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

private val PALETTE_COLORS = listOf(
    0xFFFFD600L, // Amber Yellow
    0xFF00E5FFL, // Cyan
    0xFF00E676L, // Neon Green
    0xFFBA68C8L, // Purple
    0xFF2979FFL, // Royal Blue
    0xFFFF5252L, // Coral Red
    0xFFFF9800L, // Orange
    0xFFFFFFFFL  // White
)

/**
 * Modern, institutional-grade Indicator Settings Sheet allowing custom
 * period/days, multiplier, secondary period, and line color customization.
 * Pre-populates with standard default values upon click.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorSettingsSheet(
    activeIndicators: List<IndicatorConfig>,
    targetIndicatorType: IndicatorType? = null,
    onToggleIndicator: (IndicatorType) -> Unit,
    onUpdateIndicator: (IndicatorConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedType by remember(targetIndicatorType) {
        mutableStateOf(targetIndicatorType ?: activeIndicators.firstOrNull { it.enabled }?.type ?: IndicatorType.SMA)
    }

    val currentConfig = activeIndicators.find { it.type == selectedType }
        ?: IndicatorConfig(type = selectedType, enabled = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = colors.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.onSurface.copy(alpha = 0.2f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${selectedType.label.uppercase()} SETTINGS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = colors.onBackground
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        "Close",
                        tint = colors.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Horizontal Quick-Picker Chips for all indicators
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IndicatorType.entries.forEach { type ->
                    val isCurrent = type == selectedType
                    val isEnabled = activeIndicators.find { it.type == type }?.enabled == true
                    FilterChip(
                        selected = isCurrent,
                        onClick = { selectedType = type },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF00E676), CircleShape)
                                    )
                                }
                                Text(
                                    type.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent || isEnabled) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (isEnabled) Color(0xFF00E676).copy(alpha = 0.25f) else colors.primaryContainer,
                            selectedLabelColor = if (isEnabled) Color(0xFF00E676) else colors.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isCurrent,
                            borderColor = if (isEnabled) Color(0xFF00E676).copy(alpha = 0.6f) else colors.outline.copy(alpha = 0.2f),
                            selectedBorderColor = if (isEnabled) Color(0xFF00E676) else colors.primary,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Focused Single Indicator Settings Card (Clean, direct, non-cluttered)
            IndicatorCustomRow(
                type = selectedType,
                config = currentConfig,
                isExpanded = true,
                onToggleExpand = { /* always expanded in focused view */ },
                onToggleActive = { onToggleIndicator(selectedType) },
                onSaveConfig = { updated ->
                    onUpdateIndicator(updated)
                    onDismiss()
                }
            )

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun IndicatorCustomRow(
    type: IndicatorType,
    config: IndicatorConfig,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleActive: () -> Unit,
    onSaveConfig: (IndicatorConfig) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val currentColor = Color((config.color and 0xFFFFFFFFL).toInt())
    var currentPeriod by remember(config.type, config.period) { mutableStateOf(config.period) }
    var currentMultiplier by remember(config.type, config.multiplier) { mutableStateOf(config.multiplier) }
    var currentSecondaryPeriod by remember(config.type, config.secondaryPeriod) { mutableStateOf(config.secondaryPeriod) }
    var currentTertiaryPeriod by remember(config.type, config.tertiaryPeriod) { mutableStateOf(config.tertiaryPeriod) }
    var selectedColorLong by remember(config.type, config.color) { mutableStateOf(config.color) }

    val hasMultiplier = type == IndicatorType.BOLLINGER_BANDS || type == IndicatorType.SUPERTREND
    val hasSecondary = type == IndicatorType.MACD || type == IndicatorType.STOCHASTIC || type == IndicatorType.ICHIMOKU
    val hasTertiary = type == IndicatorType.MACD || type == IndicatorType.ICHIMOKU

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isExpanded) colors.surfaceVariant.copy(alpha = 0.4f) else Color.Transparent)
            .border(
                0.5.dp,
                if (config.enabled) Color(0xFF00E676).copy(alpha = 0.5f) else colors.outline.copy(alpha = 0.15f),
                RoundedCornerShape(8.dp)
            )
    ) {
        // Main Row Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Color Dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (config.enabled) currentColor else currentColor.copy(alpha = 0.3f))
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = type.label,
                            fontWeight = if (config.enabled) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (config.enabled) Color(0xFF00E676) else colors.onSurface,
                            fontFamily = FontFamily.Monospace
                        )
                        if (config.enabled) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Days/Period: ${config.period}" + if (hasMultiplier) " | Mult: ${config.multiplier}" else "",
                        fontSize = 9.sp,
                        color = colors.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.height(20.dp),
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF00E676).copy(alpha = 0.5f),
                        checkedThumbColor = Color(0xFF00E676)
                    )
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = colors.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded Custom Settings Panel
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            ) {
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 6.dp))

                // Trading Insight Tip
                Text(
                    text = getIndicatorTip(type),
                    fontSize = 9.sp,
                    color = colors.primary.copy(alpha = 0.85f),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // 1. Days / Period Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Days / Period:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontFamily = FontFamily.Monospace)
                    InstitutionalStepper(
                        valueText = "$currentPeriod",
                        canDecrement = currentPeriod > 1,
                        onDecrement = {
                            if (currentPeriod > 1) {
                                currentPeriod -= 1
                                onSaveConfig(config.copy(period = currentPeriod))
                            }
                        },
                        onIncrement = {
                            currentPeriod += 1
                            onSaveConfig(config.copy(period = currentPeriod))
                        }
                    )
                }

                // 2. Multiplier Stepper (If applicable)
                if (hasMultiplier) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Multiplier / StdDev:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontFamily = FontFamily.Monospace)
                        InstitutionalStepper(
                            valueText = "$currentMultiplier",
                            canDecrement = currentMultiplier > 0.5,
                            onDecrement = {
                                if (currentMultiplier > 0.5) {
                                    currentMultiplier = ((currentMultiplier - 0.5) * 10).toInt() / 10.0
                                    onSaveConfig(config.copy(multiplier = currentMultiplier))
                                }
                            },
                            onIncrement = {
                                currentMultiplier = ((currentMultiplier + 0.5) * 10).toInt() / 10.0
                                onSaveConfig(config.copy(multiplier = currentMultiplier))
                            }
                        )
                    }
                }

                // 3. Secondary Period (If applicable)
                if (hasSecondary) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Signal / Secondary Period:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontFamily = FontFamily.Monospace)
                        InstitutionalStepper(
                            valueText = "$currentSecondaryPeriod",
                            canDecrement = currentSecondaryPeriod > 1,
                            onDecrement = {
                                if (currentSecondaryPeriod > 1) {
                                    currentSecondaryPeriod -= 1
                                    onSaveConfig(config.copy(secondaryPeriod = currentSecondaryPeriod))
                                }
                            },
                            onIncrement = {
                                currentSecondaryPeriod += 1
                                onSaveConfig(config.copy(secondaryPeriod = currentSecondaryPeriod))
                            }
                        )
                    }
                }

                if (hasTertiary) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tertiary / Signal Period:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, fontFamily = FontFamily.Monospace)
                        InstitutionalStepper(
                            valueText = "$currentTertiaryPeriod",
                            canDecrement = currentTertiaryPeriod > 1,
                            onDecrement = {
                                if (currentTertiaryPeriod > 1) {
                                    currentTertiaryPeriod -= 1
                                    onSaveConfig(config.copy(tertiaryPeriod = currentTertiaryPeriod))
                                }
                            },
                            onIncrement = {
                                currentTertiaryPeriod += 1
                                onSaveConfig(config.copy(tertiaryPeriod = currentTertiaryPeriod))
                            }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 4. Color Palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Line Color:", fontSize = 10.sp, color = colors.onSurface.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PALETTE_COLORS.forEach { colorLong ->
                            val isSelected = selectedColorLong == colorLong
                            val col = Color((colorLong and 0xFFFFFFFFL).toInt())
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable {
                                        selectedColorLong = colorLong
                                        onSaveConfig(config.copy(color = colorLong))
                                    }
                                    .border(if (isSelected) 1.5.dp else 0.dp, Color.White, CircleShape)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 5. Action Buttons (Apply & Reset Default)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val defaultP = IndicatorConfig.defaultPeriod(type)
                            val defaultM = IndicatorConfig.defaultMultiplier(type)
                            val defaultS = IndicatorConfig.defaultSecondaryPeriod(type)
                            val defaultT = IndicatorConfig.defaultTertiaryPeriod(type)
                            val defaultC = IndicatorConfig.defaultColor(type)
                            currentPeriod = defaultP
                            currentMultiplier = defaultM
                            currentSecondaryPeriod = defaultS
                            currentTertiaryPeriod = defaultT
                            selectedColorLong = defaultC
                            onSaveConfig(
                                config.copy(
                                    period = defaultP,
                                    multiplier = defaultM,
                                    secondaryPeriod = defaultS,
                                    tertiaryPeriod = defaultT,
                                    color = defaultC
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Default", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(Modifier.width(6.dp))

                    Button(
                        onClick = {
                            onSaveConfig(
                                config.copy(
                                    enabled = true,
                                    period = currentPeriod,
                                    multiplier = currentMultiplier,
                                    secondaryPeriod = currentSecondaryPeriod,
                                    tertiaryPeriod = currentTertiaryPeriod,
                                    color = selectedColorLong
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Apply", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun getIndicatorTip(type: IndicatorType): String = when (type) {
    IndicatorType.SMA -> "• SMA: Unweighted mean. Best for macro trend & 50/200 Golden Cross."
    IndicatorType.EMA -> "• EMA: Weighted to recent candles. Reacts faster; best for day trading & scalping (9/21)."
    IndicatorType.BOLLINGER_BANDS -> "• Bollinger: Volatility bands. Squeezes signal breakout; StdDev 2.5 filters crypto chop."
    IndicatorType.VWAP -> "• VWAP: Institutional volume-weighted benchmark for fair value."
    IndicatorType.SUPERTREND -> "• Supertrend: ATR trend filter. Green = bullish trend; Red = bearish trend."
    IndicatorType.ICHIMOKU -> "• Ichimoku: Cloud equilibrium. Standard 9/26/52; 10/30/60 for 24/7 crypto."
    IndicatorType.RSI -> "• RSI: Momentum oscillator. 70 = overbought, 30 = oversold (80/20 for crypto)."
    IndicatorType.MACD -> "• MACD: Trend & momentum divergence (Fast 12, Slow 26, Signal 9)."
    IndicatorType.STOCHASTIC -> "• Stochastic: Close position vs range (%K 14, %D 3)."
    IndicatorType.ATR -> "• ATR: Pure volatility measure (default 14)."
    IndicatorType.CVD -> "• CVD: Cumulative Volume Delta (institutional net buyer vs seller flow)."
    else -> "• Technical Indicator: Quantitative trend and momentum measurement."
}

/**
 * Unified, institutional capsule stepper widget.
 * Features crisp borders, vertical dividers, 32dp touch targets,
 * and high-contrast monospace numeric typography.
 */
@Composable
private fun InstitutionalStepper(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF161922),
        border = BorderStroke(1.dp, Color(0xFF2A2E39))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            // Decrement Button [-]
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable(enabled = canDecrement, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (canDecrement) Color(0xFFE6EDF3) else Color(0xFF484F58),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2A2E39))
            )

            // Centered Value Text
            Box(
                modifier = Modifier
                    .widthIn(min = 44.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00E676)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF2A2E39))
            )

            // Increment Button [+]
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = Color(0xFFE6EDF3),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

