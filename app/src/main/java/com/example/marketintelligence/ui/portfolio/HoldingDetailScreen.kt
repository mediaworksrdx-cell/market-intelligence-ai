package com.example.marketintelligence.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.Holding
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed

@Composable
fun HoldingDetailScreen(
    symbol: String,
    viewModel: PortfolioViewModel = hiltViewModel(),
    onNavigateToAnalysis: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Safety dialog for liquidation
    uiState.holdingToLiquidate?.let { sym ->
        ConfirmLiquidateDialog(
            symbol = sym,
            onDismiss = { viewModel.dismissLiquidateAsset() },
            onConfirm = {
                viewModel.confirmLiquidateAsset()
                onNavigateBack()
            }
        )
    }

    val holding = uiState.allHoldings.find { it.symbol.equals(symbol, ignoreCase = true) }
        ?: uiState.holdings.find { it.symbol.equals(symbol, ignoreCase = true) }
        ?: Holding(
            symbol = symbol,
            quantity = 0.0,
            avgPrice = 0.0,
            investedValue = 0.0,
            currentValue = 0.0,
            totalPnl = 0.0,
            todayPnl = 0.0,
            market = uiState.selectedMarket,
            currency = if (symbol.contains("BTC") || symbol.contains("ETH") || symbol.contains("SOL")) "USD" else "INR",
            currencySymbol = if (symbol.contains("BTC") || symbol.contains("ETH") || symbol.contains("SOL")) "$" else "₹"
        )

    val currentPrice = if (holding.quantity > 0.0) holding.currentValue / holding.quantity else holding.avgPrice
    val pnlPercent = if (holding.investedValue > 0.0) (holding.totalPnl / holding.investedValue) * 100.0 else 0.0
    val totalCategoryVal = if (holding.currency == "USD") uiState.usdCurrentValue else uiState.inrCurrentValue
    val weightPercent = if (totalCategoryVal > 0.0) {
        (holding.currentValue / totalCategoryVal) * 100.0
    } else 0.0

    val totalColor = if (holding.totalPnl >= 0) AppGreen else AppRed
    val totalPrefix = if (holding.totalPnl >= 0) "+" else ""
    val todayColor = if (holding.todayPnl >= 0) AppGreen else AppRed
    val todayPrefix = if (holding.todayPnl >= 0) "+" else ""
    val currSym = holding.currencySymbol

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    holding.symbol.uppercase(),
                    color = colors.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "CATEGORY: ${holding.assetCategory.replace("_", " ")} • CURRENCY: ${holding.currency} ($currSym)",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 1: Valuation & Price
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "CURRENT VALUATION (${holding.currency})",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$currSym${"%,.2f".format(holding.currentValue)}",
                        color = colors.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        holding.currency,
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                val qtyFormatted = if (holding.quantity % 1.0 == 0.0) "${holding.quantity.toInt()}" else "${holding.quantity}"
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailMetric("QTY / UNITS", qtyFormatted, Modifier.weight(1f))
                    DetailMetric("AVG PRICE", "$currSym${"%,.2f".format(holding.avgPrice)}", Modifier.weight(1f))
                    DetailMetric("LTP (MARKET)", "$currSym${"%,.2f".format(currentPrice)}", Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: P&L Analysis
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, totalColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "RETURN & P&L INSIGHTS",
                    color = colors.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailMetric(
                        "TOTAL RETURN",
                        "$totalPrefix$currSym${"%,.2f".format(holding.totalPnl)}\n($totalPrefix${"%.2f".format(pnlPercent)}%)",
                        Modifier.weight(1f),
                        totalColor
                    )
                    DetailMetric(
                        "DAY'S P&L",
                        "$todayPrefix$currSym${"%,.2f".format(holding.todayPnl)}",
                        Modifier.weight(1f),
                        todayColor
                    )
                    DetailMetric(
                        "INVESTED BASIS",
                        "$currSym${"%,.2f".format(holding.investedValue)}",
                        Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PORTFOLIO WEIGHT",
                        color = colors.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${"%.2f".format(weightPercent)}% OF ${if (holding.currency == "USD") "USD" else "INR"} HOLDINGS",
                        color = colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (weightPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Deep Dive Analysis & Liquidate
        Button(
            onClick = { onNavigateToAnalysis(holding.symbol) },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "OPEN TECHNICAL & QUANT CHARTS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { viewModel.requestLiquidateAsset(holding.symbol) },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppRed.copy(alpha = 0.5f))
        ) {
            Text(
                "LIQUIDATE / CLOSE POSITION",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DetailMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            label,
            color = colors.onSurfaceVariant,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            color = if (valueColor != Color.Unspecified) valueColor else colors.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}
