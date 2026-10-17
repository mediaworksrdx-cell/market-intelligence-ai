package com.marketintelligence.ai.ui.portfolio

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.data.util.MarketPriceCatalog
import com.marketintelligence.ai.domain.model.Holding
import com.marketintelligence.ai.ui.theme.*

private val AllocationColors = listOf(
    Color(0xFF00E676), // Emerald Green
    Color(0xFF2979FF), // Vivid Blue
    Color(0xFFFF9100), // Vibrant Amber
    Color(0xFFE040FB), // Electric Purple
    Color(0xFF00E5FF), // Cyan
    Color(0xFFFF5252)  // Coral Red
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onHoldingClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Dialogs
    if (uiState.isAddDialogVisible) {
        AddAssetDialog(
            initialSymbol = uiState.prefillSymbol,
            initialCategory = uiState.prefillCategory,
            onDismiss = { viewModel.dismissAddDialog() },
            onConfirm = { symbol, qty, price, category, currency ->
                viewModel.confirmAddAsset(symbol, qty, price, category, currency)
            }
        )
    }

    uiState.holdingToLiquidate?.let { symbol ->
        ConfirmLiquidateDialog(
            symbol = symbol,
            onDismiss = { viewModel.dismissLiquidateAsset() },
            onConfirm = { viewModel.confirmLiquidateAsset() }
        )
    }

    if (uiState.isLiquidateAllDialogVisible) {
        ConfirmLiquidateAllDialog(
            onDismiss = { viewModel.dismissLiquidateAll() },
            onConfirm = { viewModel.confirmLiquidateAll() }
        )
    }

    val displayedHoldings = remember(uiState.holdings, uiState.searchQuery) {
        if (uiState.searchQuery.isBlank()) uiState.holdings
        else uiState.holdings.filter { it.symbol.contains(uiState.searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "PORTFOLIO INTELLIGENCE",
                        color = colors.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "INDEPENDENT INR & USD VALUATION ENGINE",
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Currency Badges
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "₹ INR",
                            color = Color(0xFF00E676),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Surface(
                        color = Color(0xFFFF9100).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "$ USD",
                            color = Color(0xFFFF9100),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Currency / Asset Category Segmented Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PortfolioFilterChip(
                    text = "ALL HOLDINGS (${uiState.allHoldings.size})",
                    isSelected = uiState.selectedTab == PortfolioTab.ALL,
                    onClick = { viewModel.onTabSelected(PortfolioTab.ALL) }
                )
                PortfolioFilterChip(
                    text = "🇮🇳 INDIAN STOCKS (₹) (${uiState.indianStocksCount})",
                    isSelected = uiState.selectedTab == PortfolioTab.INDIAN_STOCKS,
                    onClick = { viewModel.onTabSelected(PortfolioTab.INDIAN_STOCKS) }
                )
                PortfolioFilterChip(
                    text = "🪙 CRYPTO ($) (${uiState.cryptoCount})",
                    isSelected = uiState.selectedTab == PortfolioTab.CRYPTO,
                    onClick = { viewModel.onTabSelected(PortfolioTab.CRYPTO) }
                )
                PortfolioFilterChip(
                    text = "🇺🇸 US STOCKS ($) (${uiState.usStocksCount})",
                    isSelected = uiState.selectedTab == PortfolioTab.US_STOCKS,
                    onClick = { viewModel.onTabSelected(PortfolioTab.US_STOCKS) }
                )
            }
        }

        // Search & Add Bar
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    textStyle = TextStyle(
                        color = colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.primary),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (uiState.searchQuery.isEmpty()) {
                                    Text(
                                        "Search ${uiState.selectedTab.label.lowercase()} or enter ticker...",
                                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                innerTextField()
                            }
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = {
                        val category = when (uiState.selectedTab) {
                            PortfolioTab.CRYPTO -> "CRYPTO"
                            PortfolioTab.US_STOCKS -> "US_STOCK"
                            else -> "INDIAN_STOCK"
                        }
                        viewModel.showAddDialog(uiState.searchQuery, category)
                    },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Position",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Summary Presentation
        item {
            if (uiState.selectedTab == PortfolioTab.ALL) {
                // Dual Currency Segregated Summary
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // INR Card
                    CurrencySummaryCard(
                        title = "INDIAN STOCKS PORTFOLIO (₹ INR)",
                        currencySymbol = "₹",
                        currentValue = uiState.inrCurrentValue,
                        investedValue = uiState.inrInvestedValue,
                        totalPnl = uiState.inrTotalPnl,
                        pnlPercent = uiState.inrPnlPercent,
                        todayPnl = uiState.inrTodayPnl,
                        todayPnlPercent = uiState.inrTodayPnlPercent,
                        accentColor = Color(0xFF00E676)
                    )

                    // USD Card - PURE USD, ALWAYS IN USD
                    CurrencySummaryCard(
                        title = "CRYPTO & US ASSETS PORTFOLIO ($ USD)",
                        currencySymbol = "$",
                        currentValue = uiState.usdCurrentValue,
                        investedValue = uiState.usdInvestedValue,
                        totalPnl = uiState.usdTotalPnl,
                        pnlPercent = uiState.usdPnlPercent,
                        todayPnl = uiState.usdTodayPnl,
                        todayPnlPercent = uiState.usdTodayPnlPercent,
                        accentColor = Color(0xFFFF9100)
                    )
                }
            } else {
                // Single Currency Tab Summary Card
                val title = when (uiState.selectedTab) {
                    PortfolioTab.INDIAN_STOCKS -> "INDIAN EQUITIES & F&O PORTFOLIO (₹ INR)"
                    PortfolioTab.CRYPTO -> "CRYPTO PORTFOLIO ($ USD / USDT)"
                    PortfolioTab.US_STOCKS -> "US & GLOBAL EQUITIES ($ USD)"
                    else -> ""
                }
                val accent = if (uiState.displayCurrencySymbol == "₹") Color(0xFF00E676) else Color(0xFFFF9100)

                CurrencySummaryCard(
                    title = title,
                    currencySymbol = uiState.displayCurrencySymbol,
                    currentValue = uiState.totalCurrentValue,
                    investedValue = uiState.totalInvestedValue,
                    totalPnl = uiState.totalPnl,
                    pnlPercent = uiState.totalPnlPercent,
                    todayPnl = uiState.todayPnl,
                    todayPnlPercent = uiState.todayPnlPercent,
                    accentColor = accent
                )
            }
        }

        // Asset Allocation Breakdown Bar (Rendered on filtered view)
        if (uiState.selectedTab != PortfolioTab.ALL && displayedHoldings.isNotEmpty() && uiState.totalCurrentValue > 0) {
            item {
                PortfolioAllocationBar(
                    holdings = displayedHoldings,
                    totalValue = uiState.totalCurrentValue,
                    currencySymbol = uiState.displayCurrencySymbol
                )
            }
        }

        // Holdings List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "POSITIONS (${displayedHoldings.size})",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                if (uiState.allHoldings.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.requestLiquidateAll() },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "LIQUIDATE ALL",
                            color = AppRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }
        }

        // Holdings Items or Empty State
        if (displayedHoldings.isEmpty()) {
            item {
                if (uiState.allHoldings.isEmpty()) {
                    EmptyPortfolioView(
                        categoryName = uiState.selectedTab.label,
                        onAddClick = {
                            val category = when (uiState.selectedTab) {
                                PortfolioTab.CRYPTO -> "CRYPTO"
                                PortfolioTab.US_STOCKS -> "US_STOCK"
                                else -> "INDIAN_STOCK"
                            }
                            viewModel.showAddDialog(category = category)
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No positions found matching '${uiState.searchQuery}'",
                            color = colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(displayedHoldings, key = { it.symbol }) { item ->
                val weight = if (item.currency == "INR" && uiState.inrCurrentValue > 0) {
                    (item.currentValue / uiState.inrCurrentValue) * 100.0
                } else if (item.currency == "USD" && uiState.usdCurrentValue > 0) {
                    (item.currentValue / uiState.usdCurrentValue) * 100.0
                } else 0.0

                HoldingItemView(
                    item = item,
                    weightPercent = weight,
                    onClick = onHoldingClick,
                    onLiquidate = { viewModel.requestLiquidateAsset(item.symbol) }
                )
            }
        }
    }
}

@Composable
fun CurrencySummaryCard(
    title: String,
    currencySymbol: String,
    currentValue: Double,
    investedValue: Double,
    totalPnl: Double,
    pnlPercent: Double,
    todayPnl: Double,
    todayPnlPercent: Double,
    accentColor: Color
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = colors.onSurfaceVariant,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    color = accentColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        currencySymbol,
                        color = accentColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "$currencySymbol${"%,.2f".format(currentValue)}",
                color = colors.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Invested Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "INVESTED",
                        color = colors.onSurfaceVariant,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "$currencySymbol${"%,.2f".format(investedValue)}",
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Total P&L Column
                Column(modifier = Modifier.weight(1.1f)) {
                    Text(
                        "TOTAL P&L",
                        color = colors.onSurfaceVariant,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val totalColor = if (totalPnl >= 0) AppGreen else AppRed
                    val totalPrefix = if (totalPnl >= 0) "+" else ""
                    Text(
                        "$totalPrefix$currencySymbol${"%,.2f".format(totalPnl)}",
                        color = totalColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "($totalPrefix${"%.2f".format(pnlPercent)}%)",
                        color = totalColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Today P&L Column
                Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                    Text(
                        "TODAY'S P&L",
                        color = colors.onSurfaceVariant,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val todayColor = if (todayPnl >= 0) AppGreen else AppRed
                    val todayPrefix = if (todayPnl >= 0) "+" else ""
                    Text(
                        "$todayPrefix$currencySymbol${"%,.2f".format(todayPnl)}",
                        color = todayColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "($todayPrefix${"%.2f".format(todayPnlPercent)}%)",
                        color = todayColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PortfolioFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isSelected) colors.primary else colors.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PortfolioAllocationBar(
    holdings: List<Holding>,
    totalValue: Double,
    currencySymbol: String
) {
    val colors = MaterialTheme.colorScheme
    val sorted = remember(holdings, totalValue) {
        holdings.sortedByDescending { it.currentValue }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "ASSET ALLOCATION BREAKDOWN ($currencySymbol)",
                color = colors.onSurfaceVariant,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Multi-segment horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surfaceVariant)
            ) {
                sorted.forEachIndexed { index, holding ->
                    val weight = if (totalValue > 0) (holding.currentValue / totalValue).toFloat().coerceIn(0f, 1f) else 0f
                    if (weight > 0.01f) {
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(AllocationColors[index % AllocationColors.size])
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend labels for top holdings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                sorted.take(4).forEachIndexed { index, holding ->
                    val pct = if (totalValue > 0) (holding.currentValue / totalValue) * 100.0 else 0.0
                    val segColor = AllocationColors[index % AllocationColors.size]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(segColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${holding.symbol.removeSuffix(".NS").removeSuffix(".BO")} ${"%.1f".format(pct)}%",
                            color = colors.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingItemView(
    item: Holding,
    weightPercent: Double,
    onClick: (String) -> Unit,
    onLiquidate: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val badgeText = when (item.assetCategory) {
        "CRYPTO" -> "CRYPTO • $"
        "INDIAN_STOCK" -> "NSE • ₹"
        "US_STOCK" -> "US • $"
        "UAE_STOCK" -> "UAE • AED"
        else -> item.currency
    }

    val badgeColor = when (item.assetCategory) {
        "CRYPTO" -> Color(0xFFFF9100)      // Amber Gold
        "INDIAN_STOCK" -> Color(0xFF00E676) // Emerald Green
        "US_STOCK" -> Color(0xFF2979FF)     // Electric Blue
        else -> colors.primary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item.symbol) }
            .border(0.5.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Ticker, Market badge, Units & Avg Price
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.symbol.uppercase(),
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Asset Category Badge
                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            badgeText,
                            color = badgeColor,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (weightPercent > 0.0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${"%.1f".format(weightPercent)}%",
                                color = colors.onSurfaceVariant,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))

                // Quantity and Native Purchase Price - STRICTLY IN NATIVE CURRENCY
                val qtyFormatted = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "${item.quantity}"
                Text(
                    "$qtyFormatted UNITS @ ${item.currencySymbol}${"%,.2f".format(item.avgPrice)}",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Right Column: Valuation and Total P&L - STRICTLY IN NATIVE CURRENCY (USD IN USD, INR IN INR)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.currencySymbol}${"%,.2f".format(item.currentValue)}",
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(2.dp))
                val pnlColor = if (item.totalPnl >= 0) AppGreen else AppRed
                val prefix = if (item.totalPnl >= 0) "+" else ""
                val pnlPercent = if (item.investedValue > 0) (item.totalPnl / item.investedValue) * 100.0 else 0.0
                Text(
                    "$prefix${item.currencySymbol}${"%,.2f".format(item.totalPnl)} ($prefix${"%.2f".format(pnlPercent)}%)",
                    color = pnlColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Liquidate button
            IconButton(
                onClick = onLiquidate,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Liquidate ${item.symbol}",
                    tint = AppRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyPortfolioView(
    categoryName: String = "ALL",
    onAddClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .border(0.5.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "NO ACTIVE $categoryName POSITIONS",
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Record your equity or crypto positions to track currency-specific valuations, multi-currency P&L, and institutional intelligence.",
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RECORD FIRST TRANSACTION", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddAssetDialog(
    initialSymbol: String,
    initialCategory: String = "INDIAN_STOCK",
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, quantity: String, price: String, category: String, currency: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var symbol by remember { mutableStateOf(initialSymbol) }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    val indianSuggestions = listOf("RELIANCE", "TCS", "HDFCBANK", "INFY", "TATAMOTORS", "ITC")
    val cryptoSuggestions = listOf("BTC", "ETH", "SOL", "BNB", "DOGE", "XRP")
    val usSuggestions = listOf("AAPL", "NVDA", "TSLA", "MSFT", "GOOGL")

    val activeSuggestions = when (selectedCategory) {
        "CRYPTO" -> cryptoSuggestions
        "US_STOCK" -> usSuggestions
        else -> indianSuggestions
    }

    // USD MUST BE IN USD!
    val currency = when (selectedCategory) {
        "CRYPTO" -> "USD"
        "US_STOCK" -> "USD"
        else -> "INR"
    }

    val currencySymbol = if (currency == "INR") "₹" else "$"

    // Live catalog quote lookup for fast auto-fill
    val marketPrice = remember(symbol, selectedCategory) {
        if (symbol.isNotBlank()) MarketPriceCatalog.getFallbackPrice(symbol) else 0.0
    }

    val qtyNum = quantity.toDoubleOrNull() ?: 0.0
    val priceNum = price.toDoubleOrNull() ?: 0.0
    val totalCost = qtyNum * priceNum

    val isInputValid = symbol.isNotBlank() && qtyNum > 0.0 && priceNum > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "RECORD TRANSACTION",
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "SELECT ASSET CATEGORY & CURRENCY",
                    color = colors.onSurfaceVariant,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                // Category selector pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryPill(
                        label = "🇮🇳 Indian (₹)",
                        isSelected = selectedCategory == "INDIAN_STOCK",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedCategory = "INDIAN_STOCK"
                            if (symbol in cryptoSuggestions || symbol in usSuggestions) symbol = ""
                        }
                    )
                    CategoryPill(
                        label = "🪙 Crypto ($)",
                        isSelected = selectedCategory == "CRYPTO",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedCategory = "CRYPTO"
                            if (symbol in indianSuggestions || symbol in usSuggestions) symbol = ""
                        }
                    )
                    CategoryPill(
                        label = "🇺🇸 US ($)",
                        isSelected = selectedCategory == "US_STOCK",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedCategory = "US_STOCK"
                            if (symbol in indianSuggestions || symbol in cryptoSuggestions) symbol = ""
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Suggestion chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeSuggestions.forEach { sug ->
                        Surface(
                            color = if (symbol == sug) colors.primary.copy(alpha = 0.2f) else colors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                symbol = sug
                                val p = MarketPriceCatalog.getFallbackPrice(sug)
                                if (p > 0.0) price = "$p"
                            }
                        ) {
                            Text(
                                sug,
                                color = if (symbol == sug) colors.primary else colors.onSurfaceVariant,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Ticker Input
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { input ->
                        val upper = input.uppercase().trim()
                        symbol = upper
                        // Auto-switch category based on input
                        if (upper in cryptoSuggestions || upper.endsWith("USDT") || upper in listOf("BTC", "ETH", "SOL", "DOGE")) {
                            selectedCategory = "CRYPTO"
                        } else if (upper in usSuggestions || upper in listOf("AAPL", "MSFT", "GOOGL", "NVDA")) {
                            selectedCategory = "US_STOCK"
                        }
                    },
                    label = { Text("TICKER / SYMBOL", fontSize = 9.sp) },
                    placeholder = {
                        Text(
                            if (selectedCategory == "CRYPTO") "e.g. BTC, ETH, SOL"
                            else if (selectedCategory == "INDIAN_STOCK") "e.g. RELIANCE, TCS"
                            else "e.g. AAPL, NVDA",
                            fontSize = 9.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )

                // Market price hint & autofill
                if (marketPrice > 0.0) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Market Price: $currencySymbol${"%,.2f".format(marketPrice)} $currency",
                            color = colors.primary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "USE MARKET PRICE",
                            color = colors.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { price = "$marketPrice" }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("QUANTITY / UNITS (Decimals allowed)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )

                Spacer(Modifier.height(6.dp))

                // Purchase Price Input - STRICTLY IN ASSET'S NATIVE CURRENCY
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("PURCHASE PRICE ($currencySymbol $currency)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )

                // Cost preview - STRICTLY IN ASSET'S NATIVE CURRENCY (NO FORCED CONVERSIONS)
                if (totalCost > 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                "ESTIMATED TOTAL COST:",
                                color = colors.onSurfaceVariant,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$currencySymbol${"%,.2f".format(totalCost)} $currency",
                                color = colors.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        containerColor = colors.surface,
        confirmButton = {
            Button(
                onClick = { onConfirm(symbol, quantity, price, selectedCategory, currency) },
                enabled = isInputValid,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "CONFIRM TRADE",
                    color = colors.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    )
}

@Composable
fun CategoryPill(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (isSelected) colors.primary else colors.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
fun ConfirmLiquidateDialog(symbol: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "LIQUIDATE POSITION",
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to remove $symbol from your portfolio? All recorded transaction history for this asset will be permanently deleted.",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        },
        containerColor = colors.surface,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("LIQUIDATE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    )
}

@Composable
fun ConfirmLiquidateAllDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "RESET PORTFOLIO",
                color = AppRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Are you sure you want to liquidate your ENTIRE portfolio? All positions and transaction history will be wiped. This action cannot be undone.",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        },
        containerColor = colors.surface,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("WIPE ALL", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    )
}
