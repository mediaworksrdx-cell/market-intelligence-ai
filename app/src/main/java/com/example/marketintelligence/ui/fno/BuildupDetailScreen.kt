package com.example.marketintelligence.ui.fno

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildupDetailScreen(
    viewModel: FnoViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedSymbol by remember { mutableStateOf<String?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val totalScanned = uiState.buildupStocks.size
    val longCount = uiState.longBuildupCount
    val shortCount = uiState.shortBuildupCount
    val coveringCount = uiState.shortCoveringCount
    val unwindingCount = uiState.longUnwindingCount

    // Filter and sort stocks
    val filteredStocks = remember(
        uiState.buildupStocks,
        uiState.buildupFilter,
        uiState.buildupAssetTypeFilter,
        uiState.buildupSectorFilter,
        uiState.buildupSortOrder,
        uiState.buildupSearchQuery
    ) {
        uiState.buildupStocks
            .filter { stock ->
                val matchesSearch = uiState.buildupSearchQuery.isBlank() ||
                        stock.symbol.contains(uiState.buildupSearchQuery, ignoreCase = true) ||
                        stock.name.contains(uiState.buildupSearchQuery, ignoreCase = true)

                val matchesRegime = uiState.buildupFilter == null || stock.buildupType == uiState.buildupFilter

                val matchesAssetType = when (uiState.buildupAssetTypeFilter) {
                    AssetTypeFilter.ALL -> true
                    AssetTypeFilter.INDICES -> stock.isIndex
                    AssetTypeFilter.STOCKS -> !stock.isIndex
                }

                val matchesSector = uiState.buildupSectorFilter == "ALL" ||
                        stock.sector.equals(uiState.buildupSectorFilter, ignoreCase = true)

                matchesSearch && matchesRegime && matchesAssetType && matchesSector
            }
            .sortedWith(
                when (uiState.buildupSortOrder) {
                    BuildupSortOrder.OI_GAINERS -> compareByDescending { it.oiChangePct }
                    BuildupSortOrder.OI_LOSERS -> compareBy { it.oiChangePct }
                    BuildupSortOrder.PRICE_GAINERS -> compareByDescending { it.priceChangePct }
                    BuildupSortOrder.PRICE_LOSERS -> compareBy { it.priceChangePct }
                    BuildupSortOrder.VOLUME -> compareByDescending { it.volume }
                    BuildupSortOrder.PCR -> compareByDescending { it.pcr }
                }
            )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        // --- 1. Top Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "DERIVATIVES",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        "BUILDUP SCANNER",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    "Real-time Institutional Open Interest & Price Positioning",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Live Indicator Pulse
            Surface(
                color = if (uiState.isLiveConnected) AppGreen.copy(0.15f) else AppRed.copy(0.15f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(0.5.dp, if (uiState.isLiveConnected) AppGreen.copy(0.5f) else AppRed.copy(0.5f), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (uiState.isLiveConnected) AppGreen else AppRed, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (uiState.isLiveConnected) "LIVE" else "PAUSED",
                        color = if (uiState.isLiveConnected) AppGreen else AppRed,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- 2. Market Breadth Regime Summary Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.3f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "F&O MARKET BREADTH ($totalScanned ASSETS)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    val sentiment = when {
                        longCount > shortCount * 1.5 -> "STRONG BULLISH INFLOW"
                        longCount > shortCount -> "MILD BULLISH BIAS"
                        shortCount > longCount * 1.5 -> "STRONG BEARISH BUILDUP"
                        shortCount > longCount -> "MILD BEARISH BIAS"
                        else -> "BALANCED ROTATION"
                    }
                    val sentimentColor = if (longCount >= shortCount) AppGreen else AppRed
                    Text(sentiment, color = sentimentColor, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                }

                Spacer(Modifier.height(6.dp))

                // Multi-color segmented ratio bar
                if (totalScanned > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(3.dp))
                    ) {
                        if (longCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(longCount.toFloat())
                                    .fillMaxHeight()
                                    .background(AppGreen, RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
                            )
                        }
                        if (coveringCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(coveringCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFF00E5FF))
                            )
                        }
                        if (unwindingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(unwindingCount.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFFFB300))
                            )
                        }
                        if (shortCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(shortCount.toFloat())
                                    .fillMaxHeight()
                                    .background(AppRed, RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BreadthMetric("LONG BUILDUP", longCount, AppGreen)
                    BreadthMetric("SHORT COVERING", coveringCount, Color(0xFF00E5FF))
                    BreadthMetric("LONG UNWINDING", unwindingCount, Color(0xFFFFB300))
                    BreadthMetric("SHORT BUILDUP", shortCount, AppRed)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 3. Search & Sort Row ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Input Box
            BasicTextField(
                value = uiState.buildupSearchQuery,
                onValueChange = { viewModel.onBuildupSearchChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (uiState.buildupSearchQuery.isEmpty()) {
                                Text(
                                    "Filter by symbol (e.g. NIFTY, RELIANCE)...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                                    fontSize = 10.5.sp
                                )
                            }
                            innerTextField()
                        }
                        if (uiState.buildupSearchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.onBuildupSearchChanged("") }
                            )
                        }
                    }
                }
            )

            Spacer(Modifier.width(8.dp))

            // Sort Selector Button & Dropdown
            Box {
                Surface(
                    onClick = { showSortMenu = true },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        @Suppress("DEPRECATION")
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when (uiState.buildupSortOrder) {
                                BuildupSortOrder.OI_GAINERS -> "% OI ↑"
                                BuildupSortOrder.OI_LOSERS -> "% OI ↓"
                                BuildupSortOrder.PRICE_GAINERS -> "% Price ↑"
                                BuildupSortOrder.PRICE_LOSERS -> "% Price ↓"
                                BuildupSortOrder.VOLUME -> "Volume"
                                BuildupSortOrder.PCR -> "PCR"
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = { Text("Highest % OI Gainers", fontSize = 11.sp) },
                        onClick = { viewModel.setBuildupSortOrder(BuildupSortOrder.OI_GAINERS); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Highest % OI Losers", fontSize = 11.sp) },
                        onClick = { viewModel.setBuildupSortOrder(BuildupSortOrder.OI_LOSERS); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Top Price Gainers", fontSize = 11.sp) },
                        onClick = { viewModel.setBuildupSortOrder(BuildupSortOrder.PRICE_GAINERS); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Top Price Losers", fontSize = 11.sp) },
                        onClick = { viewModel.setBuildupSortOrder(BuildupSortOrder.PRICE_LOSERS); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Traded Volume", fontSize = 11.sp) },
                        onClick = { viewModel.setBuildupSortOrder(BuildupSortOrder.VOLUME); showSortMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Highest Put-Call Ratio (PCR)", fontSize = 11.sp) },
                        onClick = { viewModel.setBuildupSortOrder(BuildupSortOrder.PCR); showSortMenu = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 4. Primary Regime Filter Chips ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RegimeFilterChip(
                label = "ALL (${uiState.buildupStocks.size})",
                isSelected = uiState.buildupFilter == null,
                color = MaterialTheme.colorScheme.primary,
                onClick = { viewModel.setBuildupFilter(null) }
            )
            RegimeFilterChip(
                label = "🟢 LONG BUILDUP ($longCount)",
                isSelected = uiState.buildupFilter == BuildupType.LONG_BUILDUP,
                color = AppGreen,
                onClick = { viewModel.setBuildupFilter(BuildupType.LONG_BUILDUP) }
            )
            RegimeFilterChip(
                label = "🔴 SHORT BUILDUP ($shortCount)",
                isSelected = uiState.buildupFilter == BuildupType.SHORT_BUILDUP,
                color = AppRed,
                onClick = { viewModel.setBuildupFilter(BuildupType.SHORT_BUILDUP) }
            )
            RegimeFilterChip(
                label = "🔵 SHORT COVERING ($coveringCount)",
                isSelected = uiState.buildupFilter == BuildupType.SHORT_COVERING,
                color = Color(0xFF00E5FF),
                onClick = { viewModel.setBuildupFilter(BuildupType.SHORT_COVERING) }
            )
            RegimeFilterChip(
                label = "🟠 LONG UNWINDING ($unwindingCount)",
                isSelected = uiState.buildupFilter == BuildupType.LONG_UNWINDING,
                color = Color(0xFFFFB300),
                onClick = { viewModel.setBuildupFilter(BuildupType.LONG_UNWINDING) }
            )
        }

        Spacer(Modifier.height(6.dp))

        // --- 5. Secondary Segment & Sector Filter Strip ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Asset Type Segment
            AssetTypeFilter.values().forEach { type ->
                val isSelected = uiState.buildupAssetTypeFilter == type
                Surface(
                    onClick = { viewModel.setBuildupAssetTypeFilter(type) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(0.3f),
                        RoundedCornerShape(4.dp)
                    )
                ) {
                    Text(
                        type.name,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(0.3f))
            )

            // Sector Filters
            listOf("ALL", "Banking", "IT", "Auto", "Energy", "Metals", "Pharma", "FMCG", "Industrials").forEach { sector ->
                val isSelected = uiState.buildupSectorFilter.equals(sector, ignoreCase = true)
                Surface(
                    onClick = { viewModel.setBuildupSectorFilter(sector) },
                    color = if (isSelected) MaterialTheme.colorScheme.secondary.copy(0.2f) else Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(0.2f),
                        RoundedCornerShape(4.dp)
                    )
                ) {
                    Text(
                        sector,
                        color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 6. Results Counter Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SHOWING ${filteredStocks.size} CONTRACTS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "TAP CONTRACT FOR ACTION",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                fontSize = 7.5.sp
            )
        }

        Spacer(Modifier.height(4.dp))

        // --- 7. LazyColumn of Buildup Contracts ---
        if (filteredStocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FilterListOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No F&O contracts match the current filters.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredStocks, key = { it.symbol }) { stock ->
                    BuildupStockCard(
                        stock = stock,
                        isExpanded = expandedSymbol == stock.symbol,
                        onToggleExpand = {
                            expandedSymbol = if (expandedSymbol == stock.symbol) null else stock.symbol
                        },
                        onSelectAsset = {
                            viewModel.selectAssetFromScanner(stock.symbol)
                            onNavigateBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BreadthMetric(title: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
        Text(
            "$count",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RegimeFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) color.copy(0.18f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(
            0.5.dp,
            if (isSelected) color else Color.Transparent,
            RoundedCornerShape(6.dp)
        )
    ) {
        Text(
            label,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun BuildupStockCard(
    stock: FnoBuildupStock,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSelectAsset: () -> Unit
) {
    val regimeColor = when (stock.buildupType) {
        BuildupType.LONG_BUILDUP -> AppGreen
        BuildupType.SHORT_BUILDUP -> AppRed
        BuildupType.SHORT_COVERING -> Color(0xFF00E5FF)
        BuildupType.LONG_UNWINDING -> Color(0xFFFFB300)
        BuildupType.NEUTRAL -> Color.Gray
    }

    val regimeLabel = when (stock.buildupType) {
        BuildupType.LONG_BUILDUP -> "LONG BUILDUP"
        BuildupType.SHORT_BUILDUP -> "SHORT BUILDUP"
        BuildupType.SHORT_COVERING -> "SHORT COVERING"
        BuildupType.LONG_UNWINDING -> "LONG UNWINDING"
        BuildupType.NEUTRAL -> "NEUTRAL"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() }
            .border(0.5.dp, regimeColor.copy(0.35f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Row 1: Symbol + Sector + Regime Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stock.symbol,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            stock.sector.uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Regime Badge
                Surface(
                    color = regimeColor.copy(0.12f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, regimeColor, RoundedCornerShape(4.dp))
                ) {
                    Text(
                        regimeLabel,
                        color = regimeColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Row 2: Price vs Open Interest Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price Details
                Column {
                    Text(
                        "₹${"%,.2f".format(stock.ltp)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    val priceSign = if (stock.priceChange >= 0) "+" else ""
                    val priceColor = if (stock.priceChange >= 0) AppGreen else AppRed
                    Text(
                        "$priceSign${"%.2f".format(stock.priceChange)} ($priceSign${"%.2f".format(stock.priceChangePct)}%)",
                        color = priceColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Open Interest Details
                Column(horizontalAlignment = Alignment.End) {
                    val formattedOi = formatCompactNumber(stock.openInterest)
                    Text(
                        "OI: $formattedOi",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    val oiSign = if (stock.oiChangePct >= 0) "+" else ""
                    Text(
                        "ΔOI: $oiSign${"%.2f".format(stock.oiChangePct)}%",
                        color = regimeColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Row 3: Secondary Metrics Bar (Volume, PCR, Basis)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Vol: ${formatCompactNumber(stock.volume)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "PCR: ${"%.2f".format(stock.pcr)}",
                    color = if (stock.pcr >= 1.0) AppGreen else AppRed,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val basisSign = if (stock.basis >= 0) "+" else ""
                Text(
                    "Basis: $basisSign${"%.1f".format(stock.basis)} pts",
                    color = if (stock.basis >= 0) AppGreen else Color(0xFFFFB300),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Expandable Action Panel
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.2f), thickness = 0.5.dp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stock.name,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = onSelectAsset,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Analytics, null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Analyze in F&O Hub", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactNumber(number: Long): String {
    return when {
        number >= 10_000_000 -> "${"%.2f".format(number / 10_000_000.0)} Cr"
        number >= 100_000 -> "${"%.2f".format(number / 100_000.0)} L"
        number >= 1_000 -> "${"%.1f".format(number / 1_000.0)} K"
        else -> number.toString()
    }
}
