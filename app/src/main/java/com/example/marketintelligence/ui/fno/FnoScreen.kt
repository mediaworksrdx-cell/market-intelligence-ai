package com.example.marketintelligence.ui.fno

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FnoScreen(
    viewModel: FnoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Options Study", "Strategies")

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        // Search & Add Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.weight(1f).height(50.dp),
                placeholder = { Text("Search F&O Asset...", color = Color.Gray, fontSize = 14.sp) },
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
            Button(
                onClick = { viewModel.addAsset() },
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Asset Mini-Header
        Column {
            Text(uiState.selectedAsset, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Spot: ₹${"%.2f".format(uiState.spotPrice)}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AppGreen,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AppGreen
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SummaryTab(uiState)
                1 -> OptionsStudyTab(uiState)
                2 -> StrategiesTab(uiState, viewModel)
            }
        }
    }
}

@Composable
fun SummaryTab(uiState: FnoUiState) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Futures Price (3 Month)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                uiState.summary.futuresPrices.forEach { fp ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fp.expiry, color = Color.Gray, fontSize = 13.sp)
                        Text("₹${"%.2f".format(fp.price)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard("IV", "${uiState.summary.iv}%", Modifier.weight(1f))
            SummaryStatCard("PCR", "${uiState.summary.pcr}", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryStatCard("Max Pain", "₹${uiState.summary.maxPain}", Modifier.weight(1f))
            SummaryStatCard("Near Strike", "₹${uiState.summary.nearMonthStrike}", Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Highest Open Interest", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Calls", color = AppRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("₹${uiState.summary.highestCallOIStrike}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Puts", color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("₹${uiState.summary.highestPutOIStrike}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(label: String, value: String, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color.Gray, fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OptionsStudyTab(uiState: FnoUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(200.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Option Chain Visual Analysis", color = Color.DarkGray, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("CALL OI", color = AppRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("STRIKE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("PUT OI", color = AppGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            uiState.optionChain?.let { chain ->
                items(chain.strikes) { strike ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${(strike.callOI / 1000).toInt()}k", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("₹${strike.strike.toInt()}", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Text("${(strike.putOI / 1000).toInt()}k", color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0xFF121212), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun StrategiesTab(uiState: FnoUiState, viewModel: FnoViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Select Strategy", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.availableStrategies) { strategy ->
                val isSelected = uiState.selectedStrategy?.id == strategy.id
                Surface(
                    color = if (isSelected) AppGreen else Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { viewModel.selectStrategy(strategy) }
                ) {
                    Text(
                        text = strategy.name,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        uiState.selectedStrategy?.let { strategy ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theoretical Payoff", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        FnoPayoffChart(strategy.payoffPoints)
                    }
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StrategyDetailItem("Max Profit", "₹${strategy.maxProfit}", AppGreen)
                        StrategyDetailItem("Max Loss", "₹${strategy.maxLoss}", AppRed)
                        StrategyDetailItem("Breakeven", "₹${strategy.breakeven}", Color.White)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF1A1A1A))
                    Spacer(Modifier.height(16.dp))
                    
                    Text("STRATEGY LEGS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    strategy.legs.forEach { leg ->
                        Text("• $leg", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StrategyDetailItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun FnoPayoffChart(points: List<PayoffPoint>) {
    if (points.isEmpty()) return
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val minPrice = points.minOf { it.price }
        val maxPrice = points.maxOf { it.price }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0).toFloat()
        
        val minPnl = points.minOf { it.pnl }
        val maxPnl = points.maxOf { it.pnl }
        val pnlRange = (maxPnl - minPnl).coerceAtLeast(1.0).toFloat()
        
        val zeroY = height * (1 - (0 - minPnl) / pnlRange).toFloat()
        drawLine(
            color = Color.DarkGray,
            start = Offset(0f, zeroY),
            end = Offset(width, zeroY),
            strokeWidth = 1.dp.toPx()
        )

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = width * ((point.price - minPrice) / priceRange).toFloat()
            val y = height * (1 - (point.pnl - minPnl) / pnlRange).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = AppGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
