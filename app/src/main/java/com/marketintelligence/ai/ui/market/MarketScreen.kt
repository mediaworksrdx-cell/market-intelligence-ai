package com.marketintelligence.ai.ui.market

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class DetailedSectionType {
    INDICES,
    WATCHLIST,
    CRYPTO
}

fun formatMarketPrice(price: Double): String {
    return when {
        price <= 0.0 -> "0.00"
        price < 0.001 -> String.format(java.util.Locale.US, "%.7f", price)
        price < 1.0 -> String.format(java.util.Locale.US, "%.4f", price)
        else -> String.format(java.util.Locale.US, "%,.2f", price)
    }
}

fun formatMarketCap(cap: Long): String {
    return when {
        cap >= 1_000_000_000_000L -> String.format(java.util.Locale.US, "$%.2fT", cap / 1_000_000_000_000.0)
        cap >= 1_000_000_000L -> String.format(java.util.Locale.US, "$%.2fB", cap / 1_000_000_000.0)
        cap >= 1_000_000L -> String.format(java.util.Locale.US, "$%.2fM", cap / 1_000_000.0)
        cap > 0L -> "$$cap"
        else -> "--"
    }
}

@Composable
fun IntradayRangeBar(
    low: Double,
    high: Double,
    current: Double,
    modifier: Modifier = Modifier
) {
    val progress = if (high > low) {
        ((current - low) / (high - low)).toFloat().coerceIn(0f, 1f)
    } else 0.5f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("L: ${formatMarketPrice(low)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("Day Range", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text("H: ${formatMarketPrice(high)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceAtLeast(0.04f))
                    .height(4.dp)
                    .background(
                        if (progress >= 0.5f) AppGreen else AppRed,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    marketViewModel: MarketViewModel = hiltViewModel(),
    onNavigateToAnalysis: (String, String) -> Unit = { _, _ -> },
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState by marketViewModel.uiState.collectAsState()
    val searchResults by marketViewModel.searchResults.collectAsState()

    var activeDetailedSection by remember { mutableStateOf<DetailedSectionType?>(null) }
    var isIndicesExpanded by remember { mutableStateOf(false) }
    var isWatchlistExpanded by remember { mutableStateOf(false) }
    var isCryptoExpanded by remember { mutableStateOf(false) }

    // Start and stop polling when the screen is displayed
    DisposableEffect(Unit) {
        marketViewModel.startPollingCryptoPrices()
        marketViewModel.startListeningForLivePrices()

        onDispose {
            marketViewModel.stopPollingCryptoPrices()
            marketViewModel.stopListeningForLivePrices()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
    ) {
        item {
            MacroTicker(uiState.macroData)
        }

        item {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "INTELLIGENCE HUB",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                
                TextButton(
                    onClick = { marketViewModel.toggleEditMode() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (uiState.isEditMode) "SAVE" else "EDIT",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            MarketToggle(
                selectedMarket = uiState.selectedMarket,
                onMarketSelected = { marketViewModel.onMarketSelected(it) }
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(2.dp))
            // Clickable Search Bar
            if (uiState.searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSearch() } 
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Search & Add (NIFTY, RELIANCE, BTC...)", 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                SearchResults(searchResults = searchResults, onResultClick = { symbol, type -> onNavigateToAnalysis(symbol, type) })
            }
        }
        
        item {
            SectionHeader(
                title = "Core Indices",
                isDetailed = isIndicesExpanded,
                onToggleInlineDetail = { isIndicesExpanded = !isIndicesExpanded },
                onOpenFullDetail = { activeDetailedSection = DetailedSectionType.INDICES }
            )
            IndicesGrid(
                indices = uiState.indices,
                isEditMode = uiState.isEditMode,
                isDetailed = isIndicesExpanded,
                onRemove = { marketViewModel.removeIndex(it) },
                onIndexClick = onNavigateToAnalysis
            )
        }
        
        item {
            SectionHeader(
                title = "Strategic Watchlist",
                isDetailed = isWatchlistExpanded,
                onToggleInlineDetail = { isWatchlistExpanded = !isWatchlistExpanded },
                onOpenFullDetail = { activeDetailedSection = DetailedSectionType.WATCHLIST }
            )
            WatchlistContent(
                stocks = uiState.watchlist,
                isEditMode = uiState.isEditMode,
                isDetailed = isWatchlistExpanded,
                onRemove = { marketViewModel.removeStock(it) },
                onStockClick = onNavigateToAnalysis
            )
        }
        
        item {
            SectionHeader(
                title = "Crypto Intelligence",
                isDetailed = isCryptoExpanded,
                onToggleInlineDetail = { isCryptoExpanded = !isCryptoExpanded },
                onOpenFullDetail = { activeDetailedSection = DetailedSectionType.CRYPTO }
            )
            CryptoContent(
                cryptos = uiState.cryptos,
                isEditMode = uiState.isEditMode,
                isDetailed = isCryptoExpanded,
                onRemove = { marketViewModel.removeCrypto(it) },
                onCryptoClick = onNavigateToAnalysis
            )
        }
    }

    if (activeDetailedSection != null) {
        MarketDetailedViewSheet(
            sectionType = activeDetailedSection!!,
            indices = uiState.indices,
            watchlist = uiState.watchlist,
            cryptos = uiState.cryptos,
            selectedMarket = uiState.selectedMarket,
            onDismiss = { activeDetailedSection = null },
            onNavigateToAnalysis = { symbol, type ->
                activeDetailedSection = null
                onNavigateToAnalysis(symbol, type)
            }
        )
    }
}

@Composable
fun SearchResults(searchResults: List<SearchResult>, onResultClick: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
    ) {
        items(searchResults) { result ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResultClick(result.symbol, result.type) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(result.symbol, fontWeight = FontWeight.Bold)
                    Text(result.name, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(result.type, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun MacroTicker(macroData: List<MacroData>) {
    val items = if (macroData.isEmpty()) {
        listOf("DXY: --", "XAU/USD: --", "XAG/USD: --", "US10Y: --", "BRENT: --", "VIX: --")
    } else {
        macroData.map { "${it.symbol}: ${it.value}" }
    }
    val tickerItems = items + items
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(items) {
        while(true) {
            scrollState.animateScrollBy(
                value = 150f,
                animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
            )
            if (scrollState.firstVisibleItemIndex >= items.size) {
                scrollState.scrollToItem(0)
            }
        }
    }

    LazyRow(
        state = scrollState,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        userScrollEnabled = false
    ) {
        items(tickerItems) { item ->
            val parts = item.split(": ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                Spacer(Modifier.width(4.dp))
                Text(parts[0], color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(parts.getOrNull(1) ?: "--", color = MaterialTheme.colorScheme.onBackground, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    isDetailed: Boolean = false,
    onToggleInlineDetail: () -> Unit = {},
    onOpenFullDetail: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 13.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                title.uppercase(), 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Inline Toggle Chip (EXPAND / COMPACT)
            Surface(
                onClick = onToggleInlineDetail,
                shape = RoundedCornerShape(6.dp),
                color = if (isDetailed) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(0.5.dp, if (isDetailed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isDetailed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isDetailed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        if (isDetailed) "COMPACT" else "EXPAND",
                        color = if (isDetailed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // DETAILED VIEW Full Modal Trigger Button
            Surface(
                onClick = onOpenFullDetail,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "DETAILED VIEW", 
                        color = MaterialTheme.colorScheme.primary, 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun IndicesGrid(
    indices: List<IndexData>,
    isEditMode: Boolean,
    isDetailed: Boolean = false,
    onRemove: (String) -> Unit,
    onIndexClick: (String, String) -> Unit
) {
    if (indices.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No indices tracked. Search symbols above to add.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    if (isDetailed) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            indices.forEach { indexData ->
                DetailedIndexCard(
                    data = indexData,
                    isEditMode = isEditMode,
                    onRemove = onRemove,
                    onClick = { onIndexClick(indexData.symbol, "INDEX") }
                )
            }
        }
    } else {
        Column {
            val rows = indices.chunked(2)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { index, data ->
                        IndexTile(
                            data = data,
                            isEditMode = isEditMode,
                            modifier = Modifier.weight(1f).clickable { if(!isEditMode) onIndexClick(data.symbol, "INDEX") },
                            onRemove = onRemove
                        )
                        if (index == 0) Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun IndexTile(data: IndexData, isEditMode: Boolean, modifier: Modifier, onRemove: (String) -> Unit) {
    val isPositive = data.changePercent >= 0
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(72.dp).border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(data.symbol.uppercase(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(data.name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(36.dp).height(16.dp)) {
                        Sparkline(isPositive = isPositive)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(
                        formatMarketPrice(data.price),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "${if (isPositive) "+" else ""}${"%.2f".format(data.changePercent)}%",
                        color = if (isPositive) AppGreen else AppRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            if (isEditMode) {
                IconButton(
                    onClick = { onRemove(data.symbol) },
                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp).padding(4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun DetailedIndexCard(
    data: IndexData,
    isEditMode: Boolean,
    onRemove: (String) -> Unit,
    onClick: () -> Unit
) {
    val isPositive = data.changePercent >= 0
    val estHigh = max(data.price, data.openPrice) + abs(data.price - data.openPrice) * 0.45 + (data.price * 0.003)
    val estLow = min(data.price, data.openPrice) - abs(data.price - data.openPrice) * 0.45 - (data.price * 0.003)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { if (!isEditMode) onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            data.symbol.uppercase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                data.market.name,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        data.name.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatMarketPrice(data.price),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${if (isPositive) "+" else ""}${formatMarketPrice(data.change)} pts",
                                color = if (isPositive) AppGreen else AppRed,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (isPositive) AppGreen else AppRed).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${if (isPositive) "+" else ""}${"%.2f".format(data.changePercent)}%",
                                    color = if (isPositive) AppGreen else AppRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (isEditMode) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onRemove(data.symbol) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            IntradayRangeBar(low = estLow, high = estHigh, current = data.price)

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Open: ${formatMarketPrice(data.openPrice)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "High: ${formatMarketPrice(estHigh)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Low: ${formatMarketPrice(estLow)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun WatchlistContent(
    stocks: List<StockData>,
    isEditMode: Boolean,
    isDetailed: Boolean = false,
    onRemove: (String) -> Unit,
    onStockClick: (String, String) -> Unit
) {
    if (stocks.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Watchlist is empty. Search symbols above to add.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    if (isDetailed) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stocks.forEach { stock ->
                DetailedStockCard(
                    stock = stock,
                    isEditMode = isEditMode,
                    onRemove = onRemove,
                    onClick = { onStockClick(stock.symbol, "STOCK") }
                )
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                stocks.forEach { stock ->
                    AssetRow(
                        symbol = stock.symbol,
                        name = stock.name,
                        price = stock.price,
                        changePercent = stock.changePercent,
                        isEditMode = isEditMode,
                        onRemove = onRemove,
                        onClick = { onStockClick(stock.symbol, "STOCK") }
                    )
                    if (stocks.last() != stock) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedStockCard(
    stock: StockData,
    isEditMode: Boolean,
    onRemove: (String) -> Unit,
    onClick: () -> Unit
) {
    val isPositive = stock.changePercent >= 0
    val estHigh = max(stock.price, stock.openPrice) + abs(stock.price - stock.openPrice) * 0.45 + (stock.price * 0.003)
    val estLow = min(stock.price, stock.openPrice) - abs(stock.price - stock.openPrice) * 0.45 - (stock.price * 0.003)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { if (!isEditMode) onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stock.symbol,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "VOL: ${stock.volume}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        stock.name.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatMarketPrice(stock.price),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${if (isPositive) "+" else ""}${formatMarketPrice(stock.change)}",
                                color = if (isPositive) AppGreen else AppRed,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (isPositive) AppGreen else AppRed).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${if (isPositive) "+" else ""}${"%.2f".format(stock.changePercent)}%",
                                    color = if (isPositive) AppGreen else AppRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    if (isEditMode) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onRemove(stock.symbol) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            IntradayRangeBar(low = estLow, high = estHigh, current = stock.price)

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Open: ${formatMarketPrice(stock.openPrice)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "High: ${formatMarketPrice(estHigh)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Low: ${formatMarketPrice(estLow)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun CryptoContent(
    cryptos: List<CryptoData>,
    isEditMode: Boolean,
    isDetailed: Boolean = false,
    onRemove: (String) -> Unit,
    onCryptoClick: (String, String) -> Unit
) {
    if (cryptos.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cryptos tracked. Search symbols above to add.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    if (isDetailed) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cryptos.forEach { crypto ->
                DetailedCryptoCard(
                    crypto = crypto,
                    isEditMode = isEditMode,
                    onRemove = onRemove,
                    onClick = { onCryptoClick(crypto.symbol, "CRYPTO") }
                )
            }
        }
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                cryptos.forEach { crypto ->
                    AssetRow(
                        symbol = crypto.symbol,
                        name = crypto.name,
                        price = crypto.price,
                        changePercent = crypto.changePercent,
                        isEditMode = isEditMode,
                        onRemove = onRemove,
                        onClick = { onCryptoClick(crypto.symbol, "CRYPTO") }
                    )
                    if (cryptos.last() != crypto) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedCryptoCard(
    crypto: CryptoData,
    isEditMode: Boolean,
    onRemove: (String) -> Unit,
    onClick: () -> Unit
) {
    val isPositive = crypto.changePercent >= 0
    val estHigh = crypto.price * (1.0 + max(0.015, abs(crypto.changePercent) / 100.0 * 0.8))
    val estLow = crypto.price * (1.0 - max(0.015, abs(crypto.changePercent) / 100.0 * 0.8))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { if (!isEditMode) onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            crypto.symbol.uppercase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "MCAP: ${formatMarketCap(crypto.marketCap)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        crypto.name.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            formatMarketPrice(crypto.price),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = (if (isPositive) AppGreen else AppRed).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${if (isPositive) "+" else ""}${"%.2f".format(crypto.changePercent)}%",
                                color = if (isPositive) AppGreen else AppRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isEditMode) {
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { onRemove(crypto.symbol) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            IntradayRangeBar(low = estLow, high = estHigh, current = crypto.price)

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "24h Low: ${formatMarketPrice(estLow)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "24h High: ${formatMarketPrice(estHigh)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun AssetRow(
    symbol: String,
    name: String,
    price: Double,
    changePercent: Double,
    isEditMode: Boolean,
    onRemove: (String) -> Unit,
    onClick: () -> Unit = {}
) {
    val isPositive = changePercent >= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if(!isEditMode) onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(symbol, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1, fontWeight = FontWeight.Bold)
        }
        
        Box(modifier = Modifier.weight(1f).height(16.dp).padding(horizontal = 8.dp)) {
            Sparkline(isPositive = isPositive)
        }

        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
            Text(
                formatMarketPrice(price),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${if (isPositive) "+" else ""}${"%.2f".format(changePercent)}%",
                color = if (isPositive) AppGreen else AppRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        if (isEditMode) {
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = { onRemove(symbol) }, modifier = Modifier.size(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
fun Sparkline(isPositive: Boolean) {
    val color = if (isPositive) AppGreen else AppRed
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val path = Path()

        if (isPositive) {
            path.moveTo(0f, height * 0.85f)
            path.cubicTo(width * 0.3f, height * 0.75f, width * 0.5f, height * 0.45f, width * 0.7f, height * 0.5f)
            path.cubicTo(width * 0.85f, height * 0.55f, width * 0.95f, height * 0.2f, width, height * 0.15f)
        } else {
            path.moveTo(0f, height * 0.15f)
            path.cubicTo(width * 0.3f, height * 0.25f, width * 0.5f, height * 0.55f, width * 0.7f, height * 0.5f)
            path.cubicTo(width * 0.85f, height * 0.45f, width * 0.95f, height * 0.8f, width, height * 0.85f)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.4.dp.toPx())
        )
        drawCircle(
            color = color,
            radius = 1.8.dp.toPx(),
            center = Offset(width, if (isPositive) height * 0.15f else height * 0.85f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailedViewSheet(
    sectionType: DetailedSectionType,
    indices: List<IndexData>,
    watchlist: List<StockData>,
    cryptos: List<CryptoData>,
    selectedMarket: MarketType,
    onDismiss: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchFilter by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, GAINERS, LOSERS
    var sortBy by remember { mutableStateOf("DEFAULT") } // DEFAULT, % CHG

    val title = when (sectionType) {
        DetailedSectionType.INDICES -> "CORE INDICES — DETAILED VIEW"
        DetailedSectionType.WATCHLIST -> "STRATEGIC WATCHLIST — DETAILED VIEW"
        DetailedSectionType.CRYPTO -> "CRYPTO INTELLIGENCE — DETAILED VIEW"
    }

    val subtitle = when (sectionType) {
        DetailedSectionType.INDICES -> "${selectedMarket.name} Markets • ${indices.size} Instruments Tracked"
        DetailedSectionType.WATCHLIST -> "${selectedMarket.name} Equities • ${watchlist.size} Instruments Tracked"
        DetailedSectionType.CRYPTO -> "Global Spot Crypto • ${cryptos.size} Assets Tracked"
    }

    // Breadth counts
    val (totalCount, advanceCount, declineCount, avgChange) = remember(sectionType, indices, watchlist, cryptos) {
        when (sectionType) {
            DetailedSectionType.INDICES -> {
                val adv = indices.count { it.changePercent > 0 }
                val dec = indices.count { it.changePercent < 0 }
                val avg = if (indices.isNotEmpty()) indices.map { it.changePercent }.average() else 0.0
                Tuple4(indices.size, adv, dec, avg)
            }
            DetailedSectionType.WATCHLIST -> {
                val adv = watchlist.count { it.changePercent > 0 }
                val dec = watchlist.count { it.changePercent < 0 }
                val avg = if (watchlist.isNotEmpty()) watchlist.map { it.changePercent }.average() else 0.0
                Tuple4(watchlist.size, adv, dec, avg)
            }
            DetailedSectionType.CRYPTO -> {
                val adv = cryptos.count { it.changePercent > 0 }
                val dec = cryptos.count { it.changePercent < 0 }
                val avg = if (cryptos.isNotEmpty()) cryptos.map { it.changePercent }.average() else 0.0
                Tuple4(cryptos.size, adv, dec, avg)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Breadth Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TOTAL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("$totalCount", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("ADVANCES", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("▲ $advanceCount", color = AppGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column {
                    Text("DECLINES", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("▼ $declineCount", color = AppRed, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("AVG RETURN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${if (avgChange >= 0) "+" else ""}${"%.2f".format(avgChange)}%",
                        color = if (avgChange >= 0) AppGreen else AppRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Search Bar inside Sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchFilter,
                    onValueChange = { searchFilter = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (searchFilter.isEmpty()) {
                            Text(
                                "Filter tracked instruments...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchFilter.isNotEmpty()) {
                    IconButton(onClick = { searchFilter = "" }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Filter Chips (ALL, GAINERS, LOSERS) & Sort
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "GAINERS", "LOSERS").forEach { tab ->
                        val isSelected = selectedFilter == tab
                        Surface(
                            onClick = { selectedFilter = tab },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                tab,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("SORT:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    listOf("DEFAULT", "% CHG").forEach { sortOption ->
                        val isSelected = sortBy == sortOption
                        Surface(
                            onClick = { sortBy = sortOption },
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Text(
                                sortOption,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Instruments List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                when (sectionType) {
                    DetailedSectionType.INDICES -> {
                        val filtered = indices.filter {
                            val matchesSearch = it.symbol.contains(searchFilter, ignoreCase = true) || it.name.contains(searchFilter, ignoreCase = true)
                            val matchesTab = when (selectedFilter) {
                                "GAINERS" -> it.changePercent > 0
                                "LOSERS" -> it.changePercent < 0
                                else -> true
                            }
                            matchesSearch && matchesTab
                        }.let { list ->
                            if (sortBy == "% CHG") list.sortedByDescending { it.changePercent } else list
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("No matching indices found.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(filtered) { indexData ->
                                DetailedIndexCard(
                                    data = indexData,
                                    isEditMode = false,
                                    onRemove = {},
                                    onClick = { onNavigateToAnalysis(indexData.symbol, "INDEX") }
                                )
                            }
                        }
                    }
                    DetailedSectionType.WATCHLIST -> {
                        val filtered = watchlist.filter {
                            val matchesSearch = it.symbol.contains(searchFilter, ignoreCase = true) || it.name.contains(searchFilter, ignoreCase = true)
                            val matchesTab = when (selectedFilter) {
                                "GAINERS" -> it.changePercent > 0
                                "LOSERS" -> it.changePercent < 0
                                else -> true
                            }
                            matchesSearch && matchesTab
                        }.let { list ->
                            if (sortBy == "% CHG") list.sortedByDescending { it.changePercent } else list
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("No matching stocks found.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(filtered) { stock ->
                                DetailedStockCard(
                                    stock = stock,
                                    isEditMode = false,
                                    onRemove = {},
                                    onClick = { onNavigateToAnalysis(stock.symbol, "STOCK") }
                                )
                            }
                        }
                    }
                    DetailedSectionType.CRYPTO -> {
                        val filtered = cryptos.filter {
                            val matchesSearch = it.symbol.contains(searchFilter, ignoreCase = true) || it.name.contains(searchFilter, ignoreCase = true)
                            val matchesTab = when (selectedFilter) {
                                "GAINERS" -> it.changePercent > 0
                                "LOSERS" -> it.changePercent < 0
                                else -> true
                            }
                            matchesSearch && matchesTab
                        }.let { list ->
                            if (sortBy == "% CHG") list.sortedByDescending { it.changePercent } else list
                        }

                        if (filtered.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("No matching cryptos found.", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        } else {
                            items(filtered) { crypto ->
                                DetailedCryptoCard(
                                    crypto = crypto,
                                    isEditMode = false,
                                    onRemove = {},
                                    onClick = { onNavigateToAnalysis(crypto.symbol, "CRYPTO") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
