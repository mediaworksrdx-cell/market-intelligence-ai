package com.example.marketintelligence.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.Holding
import com.example.marketintelligence.ui.theme.*

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
            onDismiss = { viewModel.dismissAddDialog() },
            onConfirm = { symbol, qty, price -> viewModel.confirmAddAsset(symbol, qty, price) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "PORTFOLIO INTELLIGENCE",
                    color = colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "REAL-TIME VALUATION & RISK ENGINE",
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search & Add Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                placeholder = {
                    Text(
                        "Search Holdings or Enter Ticker...",
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                },
                textStyle = TextStyle(
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchQueryChanged("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceVariant,
                    unfocusedContainerColor = colors.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = colors.onSurface,
                    unfocusedTextColor = colors.onSurface,
                    cursorColor = colors.primary
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.showAddDialog(uiState.searchQuery) },
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Position",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Total Portfolio Value Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, colors.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TOTAL PORTFOLIO VALUE",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "₹${"%,.2f".format(uiState.totalCurrentValue)}",
                    color = colors.onSurface,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Invested Column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "INVESTED",
                            color = colors.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "₹${"%,.2f".format(uiState.totalInvestedValue)}",
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
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val totalColor = if (uiState.totalPnl >= 0) AppGreen else AppRed
                        val totalPrefix = if (uiState.totalPnl >= 0) "+" else ""
                        Text(
                            "$totalPrefix₹${"%,.2f".format(uiState.totalPnl)}",
                            color = totalColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "($totalPrefix${"%.2f".format(uiState.totalPnlPercent)}%)",
                            color = totalColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Today P&L Column
                    Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                        Text(
                            "TODAY'S P&L",
                            color = colors.onSurfaceVariant,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val todayColor = if (uiState.todayPnl >= 0) AppGreen else AppRed
                        val todayPrefix = if (uiState.todayPnl >= 0) "+" else ""
                        Text(
                            "$todayPrefix₹${"%,.2f".format(uiState.todayPnl)}",
                            color = todayColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "($todayPrefix${"%.2f".format(uiState.todayPnlPercent)}%)",
                            color = todayColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Asset Allocation Breakdown Bar (Rendered when holdings exist)
        if (uiState.holdings.isNotEmpty() && uiState.totalCurrentValue > 0) {
            Spacer(modifier = Modifier.height(14.dp))
            PortfolioAllocationBar(
                holdings = uiState.holdings,
                totalValue = uiState.totalCurrentValue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Holdings List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HOLDINGS (${displayedHoldings.size})",
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            if (uiState.holdings.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.requestLiquidateAll() },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "LIQUIDATE ALL",
                        color = AppRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Holdings List or Empty State
        if (displayedHoldings.isEmpty()) {
            if (uiState.holdings.isEmpty()) {
                EmptyPortfolioView(onAddClick = { viewModel.showAddDialog() })
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No positions found matching '${uiState.searchQuery}'",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayedHoldings, key = { it.symbol }) { item ->
                    val weight = if (uiState.totalCurrentValue > 0) {
                        (item.currentValue / uiState.totalCurrentValue) * 100.0
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
}

@Composable
fun PortfolioAllocationBar(holdings: List<Holding>, totalValue: Double) {
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
                "ASSET ALLOCATION BREAKDOWN",
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
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.surfaceVariant)
            ) {
                sorted.forEachIndexed { index, holding ->
                    val proportion = if (totalValue > 0) (holding.currentValue / totalValue).toFloat() else 0f
                    if (proportion > 0f) {
                        val segColor = AllocationColors[index % AllocationColors.size]
                        Box(
                            modifier = Modifier
                                .weight(proportion)
                                .fillMaxHeight()
                                .background(segColor)
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
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${"%.1f".format(weightPercent)}%",
                            color = colors.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "${item.quantity} UNITS @ ₹${"%,.2f".format(item.avgPrice)}",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Right Column: Valuation and Total P&L
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${"%,.2f".format(item.currentValue)}",
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
                    "$prefix₹${"%,.2f".format(item.totalPnl)} ($prefix${"%.2f".format(pnlPercent)}%)",
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
fun EmptyPortfolioView(onAddClick: () -> Unit) {
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
                "NO ACTIVE HOLDINGS",
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Your portfolio is empty. Add your equity or crypto trades to unlock live P&L tracking, asset allocation breakdowns, and quantitative intelligence.",
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
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var symbol by remember { mutableStateOf(initialSymbol) }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    val isInputValid = symbol.isNotBlank() &&
            (quantity.toDoubleOrNull() ?: 0.0) > 0.0 &&
            (price.toDoubleOrNull() ?: 0.0) > 0.0

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
            Column {
                Text(
                    "LOG POSITION TO LOCAL REPOSITORY",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("TICKER / SYMBOL (e.g. RELIANCE.NS, BTC)", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("QUANTITY / UNITS", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("PURCHASE / AVG PRICE (₹)", fontSize = 9.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline
                    )
                )
            }
        },
        containerColor = colors.surface,
        confirmButton = {
            Button(
                onClick = { onConfirm(symbol, quantity, price) },
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
