package com.example.marketintelligence.ui.market

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    marketViewModel: MarketViewModel = hiltViewModel(),
    onStockClick: (String) -> Unit = {}
) {
    val uiState by marketViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp)
    ) {
        MacroTicker()

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Market Overview",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
            
            TextButton(onClick = { marketViewModel.toggleEditMode() }) {
                Text(
                    text = if (uiState.isEditMode) "Done" else "Edit",
                    color = AppGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        MarketBreadthIndicator()

        Spacer(modifier = Modifier.height(16.dp))
        
        MarketToggle(
            selectedMarket = uiState.selectedMarket,
            onMarketSelected = { marketViewModel.onMarketSelected(it) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { marketViewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = { Text("Search and Add symbol...", color = Color.Gray, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppGreen,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { marketViewModel.addAsset() },
                modifier = Modifier
                    .size(52.dp)
                    .background(AppGreen, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SectionHeader("Indices")
                IndicesGrid(
                    indices = uiState.indices,
                    isEditMode = uiState.isEditMode,
                    onRemove = { marketViewModel.removeIndex(it) },
                    onIndexClick = onStockClick
                )
            }
            
            item {
                SectionHeader("Strategic Watchlist")
                WatchlistContent(
                    stocks = uiState.watchlist,
                    isEditMode = uiState.isEditMode,
                    onRemove = { marketViewModel.removeStock(it) },
                    onStockClick = onStockClick
                )
            }
            
            item {
                SectionHeader("Crypto Intelligence")
                CryptoContent(
                    cryptos = uiState.cryptos,
                    isEditMode = uiState.isEditMode,
                    onRemove = { marketViewModel.removeCrypto(it) },
                    onCryptoClick = onStockClick
                )
            }
        }
    }
}

@Composable
fun MacroTicker() {
    val items = listOf("DXY: 104.2 (0.1%)", "US10Y: 4.25% (-0.02)", "BRENT: $82.5 (0.5%)", "VIX: 13.4 (-2.1%)")
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            val parts = item.split(": ")
            Row {
                Text(parts[0], color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(parts[1], color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun MarketBreadthIndicator() {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ADV 34", color = AppGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("DEC 16", color = AppRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth().height(4.dp).background(AppRed.copy(alpha = 0.2f), RoundedCornerShape(2.dp))) {
            Box(modifier = Modifier.fillMaxHeight().weight(0.68f).background(AppGreen, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
fun MarketToggle(
    selectedMarket: MarketType,
    onMarketSelected: (MarketType) -> Unit
) {
    val markets = listOf(MarketType.IN to "INDIA", MarketType.US to "USA", MarketType.UAE to "UAE")
    
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            markets.forEach { (market, label) ->
                val isSelected = selectedMarket == market
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .background(
                            if (isSelected) Color(0xFF333333) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onMarketSelected(market) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("View All", color = AppGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun IndicesGrid(indices: List<IndexData>, isEditMode: Boolean, onRemove: (String) -> Unit, onIndexClick: (String) -> Unit) {
    Column {
        val rows = indices.chunked(2).take(2)
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEachIndexed { index, data ->
                    IndexTile(
                        data = data,
                        isEditMode = isEditMode,
                        modifier = Modifier.weight(1f).clickable { if(!isEditMode) onIndexClick(data.symbol) },
                        onRemove = onRemove
                    )
                    if (index == 0) Spacer(modifier = Modifier.width(12.dp))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun IndexTile(data: IndexData, isEditMode: Boolean, modifier: Modifier, onRemove: (String) -> Unit) {
    val isPositive = data.change >= 0
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(100.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(data.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(data.name, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "%.2f".format(data.price),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    "${if (isPositive) "+" else ""}${data.changePercent}%",
                    color = if (isPositive) AppGreen else AppRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (isEditMode) {
                IconButton(
                    onClick = { onRemove(data.symbol) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).padding(4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun WatchlistContent(stocks: List<StockData>, isEditMode: Boolean, onRemove: (String) -> Unit, onStockClick: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            stocks.forEach { stock ->
                AssetRow(
                    symbol = stock.symbol,
                    name = stock.name,
                    price = stock.price,
                    changePercent = stock.changePercent,
                    isEditMode = isEditMode,
                    onRemove = onRemove,
                    onClick = { onStockClick(stock.symbol) }
                )
                Divider(color = Color(0xFF1A1A1A), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun CryptoContent(cryptos: List<CryptoData>, isEditMode: Boolean, onRemove: (String) -> Unit, onCryptoClick: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            cryptos.forEach { crypto ->
                AssetRow(
                    symbol = crypto.symbol,
                    name = crypto.name,
                    price = crypto.price,
                    changePercent = crypto.changePercent,
                    isEditMode = isEditMode,
                    onRemove = onRemove,
                    onClick = { onCryptoClick(crypto.symbol) }
                )
                Divider(color = Color(0xFF1A1A1A), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(name, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
        }
        
        Box(modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 8.dp)) {
            Sparkline(isPositive = isPositive)
        }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                "%.2f".format(price),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Surface(
                color = (if (isPositive) AppGreen else AppRed).copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "${if (isPositive) "+" else ""}${changePercent}%",
                    color = if (isPositive) AppGreen else AppRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        
        if (isEditMode) {
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = { onRemove(symbol) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = AppRed, modifier = Modifier.size(14.dp))
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
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
