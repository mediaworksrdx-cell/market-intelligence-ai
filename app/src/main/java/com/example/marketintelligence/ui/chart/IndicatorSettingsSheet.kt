package com.example.marketintelligence.ui.chart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.chart.IndicatorConfig
import com.example.marketintelligence.domain.chart.IndicatorType

private val PALETTE_COLORS = listOf(
    0xFFFF9800L, // Trading Orange
    0xFF2962FFL, // Royal Blue
    0xFF00E676L, // Neon Green
    0xFF089981L, // Pine Green
    0xFFE91E63L, // Pink / Magenta
    0xFF9C27B0L, // Purple
    0xFFFF5252L, // Coral Red
    0xFFFFD600L  // Amber Yellow
)

/**
 * Modern, institutional-grade Indicator Settings Sheet modeled after TradingView.
 * Supports:
 * - Live non-dismissing stepper & keyboard adjustments
 * - One-tap quick presets (9, 20, 50, 100, 200) for instant single-take configuration
 * - Direct factory default reset without auto-dismissing
 * - Full support up to 200-day moving averages (200 DMA) and beyond
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorSettingsSheet(
    activeIndicators: List<IndicatorConfig>,
    targetIndicatorType: IndicatorType? = null,
    onToggleIndicator: (IndicatorType) -> Unit,
    onUpdateIndicator: (IndicatorConfig) -> Unit,
    onRemoveIndicator: ((IndicatorType) -> Unit)? = null,
    onAddIndicatorInstance: ((IndicatorType) -> Unit)? = null,
    onRemoveIndicatorConfig: ((IndicatorConfig) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var selectedConfigId by remember(targetIndicatorType, activeIndicators.size) {
        val target = activeIndicators.find { it.type == targetIndicatorType && it.enabled }
            ?: activeIndicators.find { it.type == targetIndicatorType }
            ?: activeIndicators.firstOrNull { it.enabled }
            ?: activeIndicators.firstOrNull()
        mutableStateOf(target?.id)
    }

    var selectedType by remember(targetIndicatorType, selectedConfigId) {
        val cfg = activeIndicators.find { it.id == selectedConfigId }
        mutableStateOf(cfg?.type ?: targetIndicatorType ?: IndicatorType.SMA)
    }

    val currentConfig = activeIndicators.find { it.id == selectedConfigId }
        ?: activeIndicators.find { it.type == selectedType }
        ?: IndicatorConfig(type = selectedType, enabled = false)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color(0xFFE0E3E6))
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
                    .background(Color(0xFFD1D4DC))
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
                        tint = Color(0xFF089981),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${selectedType.label.uppercase()} (${currentConfig.period}) SETTINGS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = Color(0xFF131722)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        "Close",
                        tint = Color(0xFF787B86),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Horizontal Indicator Quick-Picker Chips
            val chipScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipScrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Active instances chips first
                activeIndicators.filter { it.enabled }.forEach { config ->
                    val isSelected = config.id == selectedConfigId
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedConfigId = config.id
                            selectedType = config.type
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color((config.color and 0xFFFFFFFFL).toInt()), CircleShape)
                                )
                                Text(
                                    "${config.type.label} ${config.period}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF089981).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF089981),
                            containerColor = Color(0xFFF1F3F5),
                            labelColor = Color(0xFF131722)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFE0E3E6),
                            selectedBorderColor = Color(0xFF089981),
                            borderWidth = 1.dp
                        )
                    )
                }

                // Available Indicator Types
                IndicatorType.entries.forEach { type ->
                    val hasActiveInstance = activeIndicators.any { it.type == type && it.enabled }
                    val isCurrent = type == selectedType && (selectedConfigId == null || !hasActiveInstance)
                    FilterChip(
                        selected = isCurrent,
                        onClick = {
                            val existing = activeIndicators.find { it.type == type }
                            selectedConfigId = existing?.id
                            selectedType = type
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (hasActiveInstance) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(Color(0xFF089981), CircleShape)
                                    )
                                }
                                Text(
                                    type.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCurrent || hasActiveInstance) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (hasActiveInstance) Color(0xFF089981).copy(alpha = 0.15f) else Color(0xFFF1F3F5),
                            selectedLabelColor = if (hasActiveInstance) Color(0xFF089981) else Color(0xFF131722),
                            containerColor = Color(0xFFF8F9FA),
                            labelColor = Color(0xFF787B86)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isCurrent,
                            borderColor = if (hasActiveInstance) Color(0xFF089981).copy(alpha = 0.5f) else Color(0xFFE0E3E6),
                            selectedBorderColor = if (hasActiveInstance) Color(0xFF089981) else Color(0xFF2962FF),
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Focused Indicator Settings Card
            IndicatorCustomRow(
                type = selectedType,
                config = currentConfig,
                onToggleActive = { onToggleIndicator(selectedType) },
                onLiveUpdate = { updated ->
                    onUpdateIndicator(updated)
                },
                onApply = { updated ->
                    onUpdateIndicator(updated)
                    onDismiss()
                },
                onAddAnotherInstance = onAddIndicatorInstance,
                onRemoveIndicator = onRemoveIndicator,
                onRemoveIndicatorConfig = onRemoveIndicatorConfig,
                onDismiss = onDismiss
            )

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun IndicatorCustomRow(
    type: IndicatorType,
    config: IndicatorConfig,
    onToggleActive: () -> Unit,
    onLiveUpdate: (IndicatorConfig) -> Unit,
    onApply: (IndicatorConfig) -> Unit,
    onAddAnotherInstance: ((IndicatorType) -> Unit)? = null,
    onRemoveIndicator: ((IndicatorType) -> Unit)?,
    onRemoveIndicatorConfig: ((IndicatorConfig) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val defaultPeriod = IndicatorConfig.defaultPeriod(type)
    val defaultMultiplier = IndicatorConfig.defaultMultiplier(type)
    val defaultSecondary = IndicatorConfig.defaultSecondaryPeriod(type)
    val defaultTertiary = IndicatorConfig.defaultTertiaryPeriod(type)

    var currentPeriod by remember(type, config.period) { mutableIntStateOf(config.period) }
    var currentMultiplier by remember(type, config.multiplier) { mutableDoubleStateOf(config.multiplier) }
    var currentSecondaryPeriod by remember(type, config.secondaryPeriod) { mutableIntStateOf(config.secondaryPeriod) }
    var currentTertiaryPeriod by remember(type, config.tertiaryPeriod) { mutableIntStateOf(config.tertiaryPeriod) }
    var selectedColorLong by remember(type, config.color) { mutableLongStateOf(config.color) }
    var resetNotice by remember { mutableStateOf<String?>(null) }

    val currentColor = Color((selectedColorLong and 0xFFFFFFFFL).toInt())
    val hasMultiplier = type == IndicatorType.BOLLINGER_BANDS || type == IndicatorType.SUPERTREND
    val hasSecondary = type == IndicatorType.MACD || type == IndicatorType.STOCHASTIC || type == IndicatorType.ICHIMOKU
    val hasTertiary = type == IndicatorType.MACD || type == IndicatorType.ICHIMOKU

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8F9FA))
            .border(
                1.dp,
                if (config.enabled) Color(0xFF089981).copy(alpha = 0.4f) else Color(0xFFE0E3E6),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        // Row Header
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (config.enabled) currentColor else currentColor.copy(alpha = 0.35f))
                        .border(1.dp, Color.White, CircleShape)
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = type.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (config.enabled) Color(0xFF089981) else Color(0xFF131722),
                            fontFamily = FontFamily.Monospace
                        )
                        if (config.enabled) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF089981),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(Color(0xFF089981).copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Current: $currentPeriod • Default: $defaultPeriod" + if (hasMultiplier) " | Mult: $currentMultiplier (Def: $defaultMultiplier)" else "",
                        fontSize = 9.5.sp,
                        color = Color(0xFF787B86),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Active Switch
            Switch(
                checked = config.enabled,
                onCheckedChange = { onToggleActive() },
                modifier = Modifier.height(22.dp),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF089981).copy(alpha = 0.4f),
                    checkedThumbColor = Color(0xFF089981),
                    uncheckedTrackColor = Color(0xFFE0E3E6),
                    uncheckedThumbColor = Color(0xFFB2B5BE)
                )
            )
        }

        HorizontalDivider(color = Color(0xFFE0E3E6), modifier = Modifier.padding(vertical = 8.dp))

        // Trading Insight Tip
        Text(
            text = getIndicatorTip(type),
            fontSize = 9.5.sp,
            color = Color(0xFF2962FF),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Reset Feedback Notice
        AnimatedVisibility(visible = resetNotice != null) {
            resetNotice?.let { notice ->
                Text(
                    text = "✓ $notice",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF089981),
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // 1. Days / Period Input & Stepper (TradingView Style with single-take input)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Days / Period:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF131722),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Up to 200 DMA",
                        fontSize = 8.5.sp,
                        color = Color(0xFF787B86),
                        fontFamily = FontFamily.Monospace
                    )
                }
                InstitutionalNumericInput(
                    value = currentPeriod,
                    onValueChange = { newVal ->
                        currentPeriod = newVal
                        resetNotice = null
                        onLiveUpdate(config.copy(period = newVal))
                    },
                    onDecrement = {
                        if (currentPeriod > 1) {
                            currentPeriod -= 1
                            resetNotice = null
                            onLiveUpdate(config.copy(period = currentPeriod))
                        }
                    },
                    onIncrement = {
                        if (currentPeriod < 500) {
                            currentPeriod += 1
                            resetNotice = null
                            onLiveUpdate(config.copy(period = currentPeriod))
                        }
                    },
                    canDecrement = currentPeriod > 1,
                    canIncrement = currentPeriod < 500,
                    minValue = 1,
                    maxValue = 500
                )
            }

            Spacer(Modifier.height(6.dp))

            // Quick Preset Chips for Period (Single Take)
            val presets = getPeriodPresets(type)
            QuickPresetRow(
                presets = presets,
                currentValue = currentPeriod,
                onSelectPreset = { preset ->
                    currentPeriod = preset
                    resetNotice = null
                    onLiveUpdate(config.copy(period = preset))
                }
            )
        }

        // 2. Multiplier / StdDev (If applicable)
        if (hasMultiplier) {
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Multiplier / StdDev:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF131722), fontFamily = FontFamily.Monospace)
                    InstitutionalDecimalInput(
                        value = currentMultiplier,
                        onValueChange = { newVal ->
                            currentMultiplier = newVal
                            resetNotice = null
                            onLiveUpdate(config.copy(multiplier = newVal))
                        },
                        onDecrement = {
                            if (currentMultiplier > 0.5) {
                                currentMultiplier = ((currentMultiplier - 0.5) * 10).toInt() / 10.0
                                resetNotice = null
                                onLiveUpdate(config.copy(multiplier = currentMultiplier))
                            }
                        },
                        onIncrement = {
                            if (currentMultiplier < 10.0) {
                                currentMultiplier = ((currentMultiplier + 0.5) * 10).toInt() / 10.0
                                resetNotice = null
                                onLiveUpdate(config.copy(multiplier = currentMultiplier))
                            }
                        },
                        canDecrement = currentMultiplier > 0.5,
                        canIncrement = currentMultiplier < 10.0
                    )
                }

                Spacer(Modifier.height(6.dp))
                QuickMultiplierPresetRow(
                    presets = getMultiplierPresets(type),
                    currentValue = currentMultiplier,
                    onSelectPreset = { preset ->
                        currentMultiplier = preset
                        resetNotice = null
                        onLiveUpdate(config.copy(multiplier = preset))
                    }
                )
            }
        }

        // 3. Secondary Period (If applicable)
        if (hasSecondary) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Slow / Secondary Period:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF131722), fontFamily = FontFamily.Monospace)
                InstitutionalNumericInput(
                    value = currentSecondaryPeriod,
                    onValueChange = { newVal ->
                        currentSecondaryPeriod = newVal
                        resetNotice = null
                        onLiveUpdate(config.copy(secondaryPeriod = newVal))
                    },
                    onDecrement = {
                        if (currentSecondaryPeriod > 1) {
                            currentSecondaryPeriod -= 1
                            resetNotice = null
                            onLiveUpdate(config.copy(secondaryPeriod = currentSecondaryPeriod))
                        }
                    },
                    onIncrement = {
                        if (currentSecondaryPeriod < 500) {
                            currentSecondaryPeriod += 1
                            resetNotice = null
                            onLiveUpdate(config.copy(secondaryPeriod = currentSecondaryPeriod))
                        }
                    },
                    canDecrement = currentSecondaryPeriod > 1,
                    canIncrement = currentSecondaryPeriod < 500
                )
            }
        }

        // 4. Tertiary Period (If applicable)
        if (hasTertiary) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Signal / Tertiary Period:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF131722), fontFamily = FontFamily.Monospace)
                InstitutionalNumericInput(
                    value = currentTertiaryPeriod,
                    onValueChange = { newVal ->
                        currentTertiaryPeriod = newVal
                        resetNotice = null
                        onLiveUpdate(config.copy(tertiaryPeriod = newVal))
                    },
                    onDecrement = {
                        if (currentTertiaryPeriod > 1) {
                            currentTertiaryPeriod -= 1
                            resetNotice = null
                            onLiveUpdate(config.copy(tertiaryPeriod = currentTertiaryPeriod))
                        }
                    },
                    onIncrement = {
                        if (currentTertiaryPeriod < 500) {
                            currentTertiaryPeriod += 1
                            resetNotice = null
                            onLiveUpdate(config.copy(tertiaryPeriod = currentTertiaryPeriod))
                        }
                    },
                    canDecrement = currentTertiaryPeriod > 1,
                    canIncrement = currentTertiaryPeriod < 500
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // 5. Line Color Palette
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Line Color:", fontSize = 11.sp, color = Color(0xFF787B86), fontFamily = FontFamily.Monospace)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PALETTE_COLORS.forEach { colorLong ->
                    val isSelected = selectedColorLong == colorLong
                    val col = Color((colorLong and 0xFFFFFFFFL).toInt())
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(col)
                            .clickable {
                                selectedColorLong = colorLong
                                resetNotice = null
                                onLiveUpdate(config.copy(color = colorLong))
                            }
                            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF131722) else Color(0xFFE0E3E6), CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 6. Action Buttons (Reset Default, Remove, Apply)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Default Reset Button (Does NOT close the sheet)
            TextButton(
                onClick = {
                    val defP = IndicatorConfig.defaultPeriod(type)
                    val defM = IndicatorConfig.defaultMultiplier(type)
                    val defS = IndicatorConfig.defaultSecondaryPeriod(type)
                    val defT = IndicatorConfig.defaultTertiaryPeriod(type)
                    val defC = IndicatorConfig.defaultColor(type)
                    currentPeriod = defP
                    currentMultiplier = defM
                    currentSecondaryPeriod = defS
                    currentTertiaryPeriod = defT
                    selectedColorLong = defC
                    resetNotice = "Reset to factory default: $defP"
                    val resetConfig = config.copy(
                        enabled = true,
                        period = defP,
                        multiplier = defM,
                        secondaryPeriod = defS,
                        tertiaryPeriod = defT,
                        color = defC
                    )
                    onLiveUpdate(resetConfig)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset to Default", modifier = Modifier.size(14.dp), tint = Color(0xFF787B86))
                Spacer(Modifier.width(4.dp))
                Text("Default ($defaultPeriod)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF787B86), fontWeight = FontWeight.SemiBold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (config.enabled && (onRemoveIndicatorConfig != null || onRemoveIndicator != null)) {
                    OutlinedButton(
                        onClick = {
                            if (onRemoveIndicatorConfig != null) {
                                onRemoveIndicatorConfig(config)
                            } else if (onRemoveIndicator != null) {
                                onRemoveIndicator(type)
                            }
                            onDismiss()
                        },
                        border = BorderStroke(1.dp, Color(0xFFF23645).copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFF23645), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", color = Color(0xFFF23645), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Button(
                    onClick = {
                        val updated = config.copy(
                            enabled = true,
                            period = currentPeriod,
                            multiplier = currentMultiplier,
                            secondaryPeriod = currentSecondaryPeriod,
                            tertiaryPeriod = currentTertiaryPeriod,
                            color = selectedColorLong
                        )
                        onApply(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF089981)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Apply", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Add Another Moving Average / Indicator Instance Button
        if (onAddAnotherInstance != null && (type == IndicatorType.SMA || type == IndicatorType.EMA || type == IndicatorType.RSI || type == IndicatorType.MACD || type == IndicatorType.BOLLINGER_BANDS)) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    onAddAnotherInstance(type)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.06f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "+ Add Another ${type.label} Instance (e.g. 50, 200)",
                    color = Color(0xFF0284C7),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Unified, institutional capsule stepper widget with direct editable numeric input.
 * Allows tapping the number directly to type in a single take (e.g., 20 to 100/200),
 * as well as using [+] and [-] stepper buttons.
 */
@Composable
private fun InstitutionalNumericInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
    minValue: Int = 1,
    maxValue: Int = 500,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFD1D4DC))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            // Decrement Button [-]
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable(enabled = canDecrement, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (canDecrement) Color(0xFF131722) else Color(0xFFB2B5BE),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E3E6))
            )

            // Centered Editable Text Input
            Box(
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { newStr ->
                        val filtered = newStr.filter { it.isDigit() }
                        text = filtered
                        val parsed = filtered.toIntOrNull()
                        if (parsed != null && parsed in minValue..maxValue) {
                            onValueChange(parsed)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF089981),
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = text.toIntOrNull()?.coerceIn(minValue, maxValue) ?: value
                            text = parsed.toString()
                            onValueChange(parsed)
                            focusManager.clearFocus()
                        }
                    ),
                    cursorBrush = SolidColor(Color(0xFF089981))
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E3E6))
            )

            // Increment Button [+]
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .clickable(enabled = canIncrement, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (canIncrement) Color(0xFF131722) else Color(0xFFB2B5BE),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Decimal stepper widget for Multiplier / StdDev.
 */
@Composable
private fun InstitutionalDecimalInput(
    value: Double,
    onValueChange: (Double) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    canDecrement: Boolean = true,
    canIncrement: Boolean = true,
    minValue: Double = 0.1,
    maxValue: Double = 10.0,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(String.format(java.util.Locale.US, "%.1f", value)) }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFFFFFF),
        border = BorderStroke(1.dp, Color(0xFFD1D4DC))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable(enabled = canDecrement, onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = if (canDecrement) Color(0xFF131722) else Color(0xFFB2B5BE),
                    modifier = Modifier.size(14.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E3E6))
            )

            Box(
                modifier = Modifier
                    .widthIn(min = 52.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { newStr ->
                        text = newStr
                        val parsed = newStr.toDoubleOrNull()
                        if (parsed != null && parsed in minValue..maxValue) {
                            onValueChange(parsed)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF089981),
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = text.toDoubleOrNull()?.coerceIn(minValue, maxValue) ?: value
                            text = String.format(java.util.Locale.US, "%.1f", parsed)
                            onValueChange(parsed)
                            focusManager.clearFocus()
                        }
                    ),
                    cursorBrush = SolidColor(Color(0xFF089981))
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E3E6))
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .clickable(enabled = canIncrement, onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = if (canIncrement) Color(0xFF131722) else Color(0xFFB2B5BE),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * One-tap TradingView style quick preset chips for instant period switching.
 */
@Composable
private fun QuickPresetRow(
    presets: List<Int>,
    currentValue: Int,
    onSelectPreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Presets:",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF787B86),
            fontFamily = FontFamily.Monospace
        )
        presets.forEach { preset ->
            val isSelected = preset == currentValue
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color(0xFF089981).copy(alpha = 0.15f) else Color(0xFFFFFFFF),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF089981) else Color(0xFFD1D4DC)
                ),
                modifier = Modifier.clickable { onSelectPreset(preset) }
            ) {
                Text(
                    text = "$preset",
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color(0xFF089981) else Color(0xFF131722),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickMultiplierPresetRow(
    presets: List<Double>,
    currentValue: Double,
    onSelectPreset: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Presets:",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF787B86),
            fontFamily = FontFamily.Monospace
        )
        presets.forEach { preset ->
            val isSelected = kotlin.math.abs(preset - currentValue) < 0.05
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Color(0xFF089981).copy(alpha = 0.15f) else Color(0xFFFFFFFF),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF089981) else Color(0xFFD1D4DC)
                ),
                modifier = Modifier.clickable { onSelectPreset(preset) }
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", preset),
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = if (isSelected) Color(0xFF089981) else Color(0xFF131722),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private fun getPeriodPresets(type: IndicatorType): List<Int> = when (type) {
    IndicatorType.SMA, IndicatorType.EMA -> listOf(9, 20, 50, 100, 200)
    IndicatorType.BOLLINGER_BANDS -> listOf(10, 20, 50, 100)
    IndicatorType.RSI -> listOf(7, 14, 21, 28, 50)
    IndicatorType.ATR -> listOf(7, 14, 21, 28)
    IndicatorType.STOCHASTIC -> listOf(5, 9, 14, 21)
    IndicatorType.SUPERTREND -> listOf(7, 10, 14, 20)
    IndicatorType.ICHIMOKU -> listOf(9, 10, 20)
    IndicatorType.ADX -> listOf(7, 14, 21, 28)
    IndicatorType.CCI -> listOf(14, 20, 50, 100)
    IndicatorType.WILLIAMS_R -> listOf(7, 14, 21, 28)
    IndicatorType.MFI -> listOf(7, 14, 21, 28)
    IndicatorType.CVD -> listOf(7, 14, 28)
    else -> listOf(10, 20, 50, 100, 200)
}

private fun getMultiplierPresets(type: IndicatorType): List<Double> = when (type) {
    IndicatorType.BOLLINGER_BANDS -> listOf(1.5, 2.0, 2.5, 3.0)
    IndicatorType.SUPERTREND -> listOf(1.0, 1.5, 2.0, 3.0)
    else -> listOf(1.0, 2.0, 3.0)
}

private fun getIndicatorTip(type: IndicatorType): String = when (type) {
    IndicatorType.SMA -> "• SMA: Simple unweighted mean. Standard for macro trend, 50/100/200 DMA Golden & Death Crosses."
    IndicatorType.EMA -> "• EMA: Exponential weighted to recent price action. Best for swift momentum & trend-following (9/20/50/200)."
    IndicatorType.BOLLINGER_BANDS -> "• Bollinger: Volatility channel (SMA ± StdDev). Squeezes signal breakout impulses."
    IndicatorType.VWAP -> "• VWAP: Volume-weighted average price benchmark used by institutional execution algorithms."
    IndicatorType.SUPERTREND -> "• Supertrend: ATR-based trailing trend filter. Green = bullish trend; Red = bearish trend."
    IndicatorType.ICHIMOKU -> "• Ichimoku: Equilibrium cloud structure identifying support, resistance, and momentum."
    IndicatorType.PARABOLIC_SAR -> "• Parabolic SAR: Trailing stop and reverse price level indicator."
    IndicatorType.RSI -> "• RSI: Relative Strength Index oscillator (0-100). 70 = overbought, 30 = oversold."
    IndicatorType.MACD -> "• MACD: Moving Average Convergence Divergence trend and momentum histogram."
    IndicatorType.STOCHASTIC -> "• Stochastic: Momentum oscillator comparing closing price to high-low range."
    IndicatorType.ATR -> "• ATR: Pure volatility measure across high-low and previous close true ranges."
    IndicatorType.CVD -> "• CVD: Cumulative Volume Delta tracking institutional buying vs selling pressure."
    IndicatorType.ADX -> "• ADX: Average Directional Index measuring overall trend strength (>25 = strong trend)."
    IndicatorType.OBV -> "• OBV: On-Balance Volume measuring volume flow preceding price movement."
    IndicatorType.CCI -> "• CCI: Commodity Channel Index identifying cyclical highs and lows."
    IndicatorType.WILLIAMS_R -> "• Williams %R: Momentum oscillator measuring overbought/oversold levels."
    IndicatorType.MFI -> "• MFI: Volume-weighted Money Flow Index measuring capital inflow and outflow."
}
