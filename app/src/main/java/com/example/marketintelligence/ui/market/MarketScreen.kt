package com.example.marketintelligence.ui.market

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    marketViewModel: MarketViewModel = hiltViewModel(),
    onNavigateToAnalysis: (String, String) -> Unit = { _, _ -> },
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState by marketViewModel.uiState.collectAsState()
    val searchResults by marketViewModel.searchResults.collectAsState()

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
            SectionHeader("Core Indices", onDetailClick = {})
            IndicesGrid(
                indices = uiState.indices,
                isEditMode = uiState.isEditMode,
                onRemove = { marketViewModel.removeIndex(it) },
                onIndexClick = onNavigateToAnalysis
            )
        }
        
        item {
            SectionHeader("Strategic Watchlist", onDetailClick = {})
            WatchlistContent(
                stocks = uiState.watchlist,
                isEditMode = uiState.isEditMode,
                onRemove = { marketViewModel.removeStock(it) },
                onStockClick = onNavigateToAnalysis
            )
        }
        
        item {
            SectionHeader("Crypto Intelligence", onDetailClick = {})
            CryptoContent(
                cryptos = uiState.cryptos,
                isEditMode = uiState.isEditMode,
                onRemove = { marketViewModel.removeCrypto(it) },
                onCryptoClick = onNavigateToAnalysis
            )
        }
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
        listOf("DXY: --", "XAU/USD: --", "US10Y: --", "BRENT: --", "VIX: --")
    } else {
        macroData.map { "${it.symbol}: ${it.value}" }
    }
    val tickerItems = items + items
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(Unit) {
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
    onDetailClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            "DETAILED VIEW", 
            color = MaterialTheme.colorScheme.primary, 
            fontSize = 9.sp, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onDetailClick() }
        )
    }
}

@Composable
fun IndicesGrid(
    indices: List<IndexData>,
    isEditMode: Boolean,
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
                Text(data.symbol.uppercase(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(data.name.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.2f".format(data.price),
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
fun WatchlistContent(
    stocks: List<StockData>,
    isEditMode: Boolean,
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

@Composable
fun CryptoContent(
    cryptos: List<CryptoData>,
    isEditMode: Boolean,
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
            val formattedPrice = when {
                price <= 0.0 -> "0.00"
                price < 0.001 -> String.format(java.util.Locale.US, "%.7f", price)
                price < 1.0 -> String.format(java.util.Locale.US, "%.4f", price)
                else -> String.format(java.util.Locale.US, "%,.2f", price)
            }
            Text(
                formattedPrice,
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
        
        path.moveTo(0f, height * 0.7f)
        path.quadraticBezierTo(width * 0.25f, height * 0.4f, width * 0.5f, height * 0.6f)
        path.quadraticBezierTo(width * 0.75f, height * 0.8f, width, if (isPositive) height * 0.2f else height * 0.9f)
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}




