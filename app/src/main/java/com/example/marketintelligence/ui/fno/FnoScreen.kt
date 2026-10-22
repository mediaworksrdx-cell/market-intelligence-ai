package com.example.marketintelligence.ui.fno

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.ui.fno.builder.*
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed
import com.example.tradeengine.models.Candle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FnoScreen(
    viewModel: FnoViewModel = hiltViewModel(),
    onOpenBuildupScanner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("MARKET", "ANALYTICS", "BUILDER")
    val curr = when (uiState.selectedMarket) {
        MarketType.US -> "$"
        MarketType.UAE -> "AED "
        MarketType.IN -> "₹"
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp)) {
        Spacer(modifier = Modifier.height(6.dp))
        
        Text("F&O INTELLIGENCE HUB", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Search & Add Header (Hidden when on Strategy Builder tab)
        if (selectedTab != 2) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.searchQuery.isEmpty()) {
                                        val placeholder = when (uiState.selectedMarket) {
                                            MarketType.IN -> "Search 3,800+ Zerodha Kite & NSE F&O assets..."
                                            MarketType.US -> "Search US Derivatives & Options..."
                                            MarketType.UAE -> "Search UAE Derivatives..."
                                        }
                                        Text(
                                            placeholder,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                    innerTextField()
                                }
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onSearchQueryChanged("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.addAsset() },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                // Autocomplete suggestion dropdown overlay
                val suggestions = if (uiState.searchQuery.isNotBlank()) viewModel.getSearchSuggestions(uiState.searchQuery) else emptyList()
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            suggestions.forEach { stock ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadAssetData(stock.symbol)
                                            viewModel.onSearchQueryChanged("")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stock.symbol, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                            if (stock.isZerodha) {
                                                Spacer(Modifier.width(4.dp))
                                                Surface(
                                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(3.dp),
                                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        "KITE",
                                                        color = Color(0xFFF59E0B),
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(stock.name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("$curr${"%.2f".format(stock.ltp)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        Text(
                                            "${if (stock.priceChangePct >= 0) "+" else ""}${"%.2f".format(stock.priceChangePct)}%",
                                            color = if (stock.priceChangePct >= 0) AppGreen else AppRed,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (stock != suggestions.last()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Asset Mini-Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(uiState.selectedAsset.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "SPOT: $curr${"%.2f".format(uiState.spotPrice)}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Surface(
                color = (if (uiState.summary.changePercent >= 0) AppGreen else AppRed).copy(0.1f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.border(0.5.dp, if(uiState.summary.changePercent >= 0) AppGreen else AppRed, RoundedCornerShape(4.dp))
            ) {
                Text(
                    "${if (uiState.summary.changePercent >= 0) "+" else ""}${"%.2f".format(uiState.summary.changePercent)}%",
                    color = if (uiState.summary.changePercent >= 0) AppGreen else AppRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 2.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SummaryTab(uiState, onSelectStrike = { strike, side -> viewModel.selectStrikeForDetail(strike, side) })
                1 -> OptionsStudyTab(uiState, viewModel, onOpenBuildupScanner)
                2 -> StrategyBuilderTab(uiState, viewModel)
            }
        }

        // Dedicated Individual Strike Price Detail Bottom Sheet
        if (uiState.selectedStrikeForDetail != null) {
            StrikePriceDetailSheet(
                strike = uiState.selectedStrikeForDetail!!,
                spotPrice = uiState.spotPrice,
                assetSymbol = uiState.selectedAsset,
                selectedSide = uiState.selectedOptionSide,
                lotSize = if (uiState.lotSize > 0) uiState.lotSize else uiState.summary.lotSize,
                strikeChartState = uiState.strikeChartState,
                onTimeframeChange = { viewModel.onStrikeTimeframeChanged(it) },
                onOptionTypeChange = { viewModel.onStrikeOptionTypeChanged(it) },
                onRetryChart = { viewModel.retryLoadStrikeChart() },
                onDismiss = { viewModel.dismissStrikeDetail() },
                onAddLeg = { strikePrice, instrument, action ->
                    viewModel.addLegFromStrike(strikePrice, instrument, action)
                    viewModel.dismissStrikeDetail()
                    selectedTab = 2
                },
                onBuildStraddle = { strikePrice ->
                    viewModel.addStraddleFromStrike(strikePrice)
                    viewModel.dismissStrikeDetail()
                    selectedTab = 2
                },
                onNavigateToBuilder = {
                    viewModel.dismissStrikeDetail()
                    selectedTab = 2
                }
            )
        }
    }
}

@Composable
fun SummaryTab(
    uiState: FnoUiState,
    onSelectStrike: (strike: OptionChainData, side: String) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    val curr = when (uiState.selectedMarket) {
        MarketType.US -> "$"
        MarketType.UAE -> "AED "
        MarketType.IN -> "₹"
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("FUTURES TERM STRUCTURE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Spacer(Modifier.height(8.dp))
                for (fp in uiState.summary.contracts) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fp.expiry, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("$curr${"%.2f".format(fp.ltp)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "${if (fp.changePercent >= 0) "+" else ""}${"%.2f".format(fp.changePercent)}%",
                            color = if (fp.changePercent >= 0) AppGreen else AppRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.summary.contracts.lastOrNull() != fp) HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStatCard("IMPLIED VOL (IV)", "${"%.1f".format(uiState.summary.iv)}%", Modifier.weight(1f))
            SummaryStatCard("PUT-CALL RATIO", "%.2f".format(uiState.summary.pcr), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStatCard("MAX PAIN", "$curr${uiState.summary.maxPain.toInt()}", Modifier.weight(1f))
            SummaryStatCard("LOT SIZE", "${uiState.summary.lotSize}", Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("OPEN INTEREST CONCENTRATION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val callStrikeObj = uiState.optionChain?.strikes?.find { it.strike == uiState.summary.highestCallOIStrike }
                    Column(
                        modifier = if (callStrikeObj != null) Modifier.clickable { onSelectStrike(callStrikeObj, "CALL") } else Modifier
                    ) {
                        Text("RESISTANCE (CALL OI)", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text("$curr${uiState.summary.highestCallOIStrike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        if (callStrikeObj != null) {
                            Text("Tap to analyze →", color = AppRed.copy(alpha = 0.7f), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    val putStrikeObj = uiState.optionChain?.strikes?.find { it.strike == uiState.summary.highestPutOIStrike }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = if (putStrikeObj != null) Modifier.clickable { onSelectStrike(putStrikeObj, "PUT") } else Modifier
                    ) {
                        Text("SUPPORT (PUT OI)", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text("$curr${uiState.summary.highestPutOIStrike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        if (putStrikeObj != null) {
                            Text("Tap to analyze →", color = AppGreen.copy(alpha = 0.7f), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SummaryStatCard(label: String, value: String, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun OptionsStudyTab(uiState: FnoUiState, viewModel: FnoViewModel, onOpenBuildupScanner: () -> Unit) {
    var viewMode by remember { mutableStateOf("PAIN") } // "PAIN", "OI", "GREEKS", "GEX", "IV"
    val curr = when (uiState.selectedMarket) {
        MarketType.US -> "$"
        MarketType.UAE -> "AED "
        MarketType.IN -> "₹"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Buildup Scanner Quick Header Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenBuildupScanner() }
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("FNO BUILDUP SCANNER", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = AppGreen.copy(0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("LIVE", color = AppGreen, fontSize = 7.5.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                    Text("Screening Long Buildup, Short Buildup, Short Covering & Unwinding", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }
                Surface(color = MaterialTheme.colorScheme.primary.copy(0.12f), shape = CircleShape) {
                    Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Toggle View: High Quality Visual Analytics Tabs (Max Pain, PCR/OI, Greeks, DEX/GEX, IV)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("DERIVATIVES ANALYTICS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(2.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf(
                    "PAIN" to "MAX PAIN",
                    "OI" to "PCR / OI",
                    "GREEKS" to "GREEKS",
                    "GEX" to "DEX / GEX",
                    "IV" to "IV"
                ).forEach { (mode, label) ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { viewMode = mode }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (viewMode == "GEX") {
            GexAnalyticsView(
                profile = uiState.gexProfile,
                spotPrice = uiState.spotPrice,
                curr = curr
            )
        } else if (viewMode == "PAIN") {
            MaxPainAnalyticsView(
                history = uiState.maxPainHistory,
                currentMaxPain = uiState.summary.maxPain,
                spotPrice = uiState.spotPrice,
                curr = curr
            )
        } else if (viewMode == "IV") {
            IvAnalyticsView(
                history = uiState.ivHistory,
                currentIv = uiState.summary.iv
            )
        } else {
            // Existing Chain Logic
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewMode == "OI") {
                    Text("CALL OI", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("LTP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("STRIKE", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("LTP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("PUT OI", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                } else {
                    Text("DELTA", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("THETA", color = Color.Magenta, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("STRIKE", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("THETA", color = Color.Magenta, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("DELTA", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }

            val listState = rememberLazyListState()
            val chain = uiState.optionChain
            val currentStrike = chain?.strikes?.minByOrNull { kotlin.math.abs(it.strike - uiState.spotPrice) }?.strike

            LaunchedEffect(currentStrike) {
                if (currentStrike != null) {
                    val atmIndex = chain.strikes.indexOfFirst { it.strike == currentStrike }
                    if (atmIndex >= 0) {
                        val scrollTarget = (atmIndex - 4).coerceAtLeast(0)
                        listState.animateScrollToItem(scrollTarget)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 60.dp)
            ) {
                if (chain != null) {
                    val maxOI = chain.strikes.maxOf { maxOf(it.callOI, it.putOI) }.coerceAtLeast(1.0)
                    items(chain.strikes) { strike ->
                        val isCurrentStrike = strike.strike == currentStrike
                        val isITMCall = strike.strike < uiState.spotPrice
                        val isITMPut = strike.strike > uiState.spotPrice
                        val callMoneyness = if (isCurrentStrike) "ATM" else if (isITMCall) "ITM" else "OTM"
                        val putMoneyness = if (isCurrentStrike) "ATM" else if (isITMPut) "ITM" else "OTM"

                        val rowModifier = if (isCurrentStrike) {
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    width = 0.8.dp,
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.selectStrikeForDetail(strike, null) }
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectStrikeForDetail(strike, null) }
                        }

                        Box(modifier = rowModifier) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewMode == "OI") {
                                    // CALL OI WITH ITM / ATM / OTM BADGE
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isITMCall && !isCurrentStrike) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                else Modifier
                                            )
                                            .clickable { viewModel.selectStrikeForDetail(strike, "CALL") }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((strike.callOI / maxOI).toFloat().coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .background(AppRed.copy(if (isCurrentStrike) 0.30f else 0.15f), RoundedCornerShape(2.dp))
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${(strike.callOI / 1000).toInt()}k",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Surface(
                                                color = when (callMoneyness) {
                                                    "ITM" -> AppGreen.copy(alpha = 0.22f)
                                                    "ATM" -> Color(0xFF00E5FF).copy(alpha = 0.22f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                },
                                                shape = RoundedCornerShape(2.dp)
                                            ) {
                                                Text(
                                                    callMoneyness,
                                                    color = when (callMoneyness) {
                                                        "ITM" -> AppGreen
                                                        "ATM" -> Color(0xFF00E5FF)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        }
                                    }
                                    // CALL LTP
                                    val callLtpStr = if (strike.callLTP >= 10.0) "${strike.callLTP.toInt()}" else "%.1f".format(strike.callLTP)
                                    Text(
                                        callLtpStr,
                                        color = if (isCurrentStrike) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(0.6f)
                                            .clickable { viewModel.selectStrikeForDetail(strike, "CALL") },
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // STRIKE (CURRENT / ATM HIGHLIGHTED + CE/PE MONEYNESS SUBTITLE)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectStrikeForDetail(strike, null) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "₹${strike.strike.toInt()}",
                                            color = if (isCurrentStrike) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrentStrike) FontWeight.Black else FontWeight.ExtraBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (isCurrentStrike) {
                                            Surface(
                                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(3.dp),
                                                modifier = Modifier.border(0.5.dp, Color(0xFF00E5FF), RoundedCornerShape(3.dp))
                                            ) {
                                                Text(
                                                    "ATM",
                                                    color = Color(0xFF00E5FF),
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 0.5.sp,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                Text(
                                                    callMoneyness,
                                                    color = if (callMoneyness == "ITM") AppGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text("•", color = MaterialTheme.colorScheme.outline, fontSize = 6.sp)
                                                Text(
                                                    putMoneyness,
                                                    color = if (putMoneyness == "ITM") AppGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                    // PUT LTP
                                    val putLtpStr = if (strike.putLTP >= 10.0) "${strike.putLTP.toInt()}" else "%.1f".format(strike.putLTP)
                                    Text(
                                        putLtpStr,
                                        color = if (isCurrentStrike) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(0.6f)
                                            .clickable { viewModel.selectStrikeForDetail(strike, "PUT") },
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // PUT OI WITH ITM / ATM / OTM BADGE
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isITMPut && !isCurrentStrike) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                else Modifier
                                            )
                                            .clickable { viewModel.selectStrikeForDetail(strike, "PUT") },
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((strike.putOI / maxOI).toFloat().coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .background(AppGreen.copy(if (isCurrentStrike) 0.30f else 0.15f), RoundedCornerShape(2.dp))
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = when (putMoneyness) {
                                                    "ITM" -> AppGreen.copy(alpha = 0.22f)
                                                    "ATM" -> Color(0xFF00E5FF).copy(alpha = 0.22f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                },
                                                shape = RoundedCornerShape(2.dp)
                                            ) {
                                                Text(
                                                    putMoneyness,
                                                    color = when (putMoneyness) {
                                                        "ITM" -> AppGreen
                                                        "ATM" -> Color(0xFF00E5FF)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                                )
                                            }
                                            Text(
                                                "${(strike.putOI / 1000).toInt()}k",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                } else {
                                    // GREEKS VIEW
                                    // CALL DELTA
                                    Text(
                                        "%.2f".format(strike.callGreeks.delta),
                                        color = Color.Cyan,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isITMCall && !isCurrentStrike) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                else Modifier
                                            )
                                            .clickable { viewModel.selectStrikeForDetail(strike, "CALL") },
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // CALL THETA
                                    Text(
                                        "%.1f".format(strike.callGreeks.theta),
                                        color = Color.Magenta,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectStrikeForDetail(strike, "CALL") },
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // STRIKE (CURRENT / ATM HIGHLIGHTED + CE/PE MONEYNESS SUBTITLE)
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectStrikeForDetail(strike, null) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "₹${strike.strike.toInt()}",
                                            color = if (isCurrentStrike) Color.White else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrentStrike) FontWeight.Black else FontWeight.ExtraBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (isCurrentStrike) {
                                            Surface(
                                                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(3.dp),
                                                modifier = Modifier.border(0.5.dp, Color(0xFF00E5FF), RoundedCornerShape(3.dp))
                                            ) {
                                                Text(
                                                    "ATM",
                                                    color = Color(0xFF00E5FF),
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 0.5.sp,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                Text(
                                                    callMoneyness,
                                                    color = if (callMoneyness == "ITM") AppGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text("•", color = MaterialTheme.colorScheme.outline, fontSize = 6.sp)
                                                Text(
                                                    putMoneyness,
                                                    color = if (putMoneyness == "ITM") AppGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    fontSize = 6.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                    // PUT THETA
                                    Text(
                                        "%.1f".format(strike.putGreeks.theta),
                                        color = Color.Magenta,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { viewModel.selectStrikeForDetail(strike, "PUT") },
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // PUT DELTA
                                    Text(
                                        "%.2f".format(strike.putGreeks.delta),
                                        color = Color.Cyan,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isITMPut && !isCurrentStrike) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                else Modifier
                                            )
                                            .clickable { viewModel.selectStrikeForDetail(strike, "PUT") },
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        if (!isCurrentStrike) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyBuilderTab(uiState: FnoUiState, viewModel: FnoViewModel) {
    val scrollState = rememberScrollState()
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isStrategyDropdownExpanded by remember { mutableStateOf(false) }
    var isExpiryDropdownExpanded by remember { mutableStateOf(false) }
    var expiryTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "WEEKLY", "MONTHLY"
    var isStrikePickerOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // --- 1. HEADER: INSTITUTIONAL BRANDING ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "INSTITUTIONAL OPTIONS STRATEGY WORKSPACE",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "STRATEGY BUILDER PRO",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        // --- 2. UNDERLYING CONTRACT & SPOT CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("UNDERLYING INSTRUMENT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        Text(uiState.selectedAsset, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SPOT PRICE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        Text("₹${"%,.2f".format(uiState.spotPrice)}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ACTIVE EXPIRY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        Text(uiState.selectedExpiry.ifBlank { "CURRENT" }, color = MaterialTheme.colorScheme.primary, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LOT SIZE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        Text("${uiState.lotSize} units", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val currentAtm = uiState.optionChain?.strikes?.minByOrNull { abs(it.strike - uiState.spotPrice) }?.strike ?: (kotlin.math.round(uiState.spotPrice / uiState.strikeStep) * uiState.strikeStep)
                        Text("ATM STRIKE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                        Text("₹${currentAtm.toInt()}", color = Color(0xFF00E5FF), fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // --- 3. EDITING EXISTING STRATEGY BANNER ---
        if (uiState.editingStrategyId != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "EDITING STRATEGY: ${uiState.selectedStrategyName}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { viewModel.cancelEditMode() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("New Strategy", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // --- 4. PRESET ARCHITECTURE DROPDOWNS ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Category Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(
                    onClick = { isCategoryDropdownExpanded = true },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.selectedCategory.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    StrategyCategory.values().forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.setStrategyCategory(category)
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Strategy Architecture Dropdown
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedCard(
                    onClick = { isStrategyDropdownExpanded = true },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.selectedStrategyName.ifBlank { "SELECT ARCHITECTURE" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(
                    expanded = isStrategyDropdownExpanded,
                    onDismissRequest = { isStrategyDropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    viewModel.strategyDefinitions[uiState.selectedCategory]?.forEach { strategyName ->
                        DropdownMenuItem(
                            text = { Text(strategyName, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.selectPredefinedStrategy(strategyName)
                                isStrategyDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- 5. SETTLEMENT EXPIRY DROPDOWN SELECTOR ---
        if (uiState.availableExpiries.isNotEmpty()) {
            val weeklyExpiries = remember(uiState.availableExpiries) { viewModel.getWeeklyExpiries() }
            val monthlyExpiries = remember(uiState.availableExpiries) { viewModel.getMonthlyExpiries() }
            val isSelectedMonthly = monthlyExpiries.contains(uiState.selectedExpiry)
            val currentExpiryTypeLabel = if (isSelectedMonthly) "MONTHLY" else "WEEKLY"

            val filteredExpiries = when (expiryTypeFilter) {
                "WEEKLY" -> weeklyExpiries
                "MONTHLY" -> monthlyExpiries
                else -> uiState.availableExpiries
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SETTLEMENT EXPIRY",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        // Weekly vs Monthly quick toggle pills
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("ALL", "WEEKLY", "MONTHLY").forEach { filter ->
                                val isSelected = expiryTypeFilter == filter
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable { expiryTypeFilter = filter }
                                        .border(
                                            width = 0.5.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                ) {
                                    Text(
                                        text = filter,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 7.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // The Dropdown Button
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { isExpiryDropdownExpanded = true },
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Text(
                                        uiState.selectedExpiry.ifBlank { "SELECT EXPIRY" },
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Surface(
                                        color = if (isSelectedMonthly) Color(0xFFFFD54F).copy(alpha = 0.2f) else Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            currentExpiryTypeLabel,
                                            color = if (isSelectedMonthly) Color(0xFFFFD54F) else Color(0xFF00E5FF),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        DropdownMenu(
                            expanded = isExpiryDropdownExpanded,
                            onDismissRequest = { isExpiryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (expiryTypeFilter != "MONTHLY") {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "WEEKLY EXPIRIES",
                                            color = Color(0xFF00E5FF),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                weeklyExpiries.forEachIndexed { idx, exp ->
                                    val tag = when (idx) {
                                        0 -> "Current Week"
                                        1 -> "Next Week"
                                        else -> "+$idx Weeks"
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(exp, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text(tag, color = Color(0xFF00E5FF), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setStrategyExpiry(exp)
                                            isExpiryDropdownExpanded = false
                                        }
                                    )
                                }
                            }

                            if (expiryTypeFilter != "WEEKLY") {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "MONTHLY EXPIRIES",
                                            color = Color(0xFFFFD54F),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                monthlyExpiries.forEachIndexed { idx, exp ->
                                    val tag = when (idx) {
                                        0 -> "Near Month"
                                        1 -> "Next Month"
                                        else -> "Far Month"
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(exp, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                Text(tag, color = Color(0xFFFFD54F), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        onClick = {
                                            viewModel.setStrategyExpiry(exp)
                                            isExpiryDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Quick-select chips below dropdown for fast 1-tap switching
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        items(filteredExpiries) { exp ->
                            val isSelected = exp == uiState.selectedExpiry
                            val isMon = monthlyExpiries.contains(exp)
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .clickable { viewModel.setStrategyExpiry(exp) }
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        exp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        if (isMon) "M" else "W",
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // --- 6. INSTITUTIONAL STRATEGY WORKSPACE TABS ---
        OpstraWorkspaceTabs(
            selectedTab = uiState.selectedWorkspaceTab,
            onTabSelected = { viewModel.setWorkspaceTab(it) },
            analysisResult = uiState.analysisResult,
            spotPrice = uiState.spotPrice,
            legs = uiState.activeLegs,
            rangePct = uiState.chartPriceRangePct,
            onRangeChange = { viewModel.setChartPriceRangePct(it) },
            spotSimulationPct = uiState.spotSimulationPct
        )

        Spacer(Modifier.height(12.dp))

        // --- 7. ACTION BUTTONS: ANALYZE, SAVE / UPDATE, RESET ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { viewModel.analyzeStrategy() },
                modifier = Modifier.weight(1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                enabled = uiState.activeLegs.isNotEmpty()
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ANALYZE", fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
            }

            Button(
                onClick = { viewModel.saveStrategy() },
                modifier = Modifier.weight(1.1f).height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                enabled = uiState.activeLegs.isNotEmpty()
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (uiState.editingStrategyId != null) "UPDATE" else "SAVE STRATEGY",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Button(
                onClick = { viewModel.requestResetStrategy() },
                modifier = Modifier.weight(0.9f).height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                enabled = uiState.activeLegs.isNotEmpty()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("RESET", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        
        // --- 8. STRATEGY POSITIONS & RISK METRICS PANEL ---
        OpstraPositionsPanel(
            legs = uiState.activeLegs,
            analysisResult = uiState.analysisResult,
            spotPrice = uiState.spotPrice,
            lotSize = uiState.lotSize,
            strikeStep = uiState.strikeStep,
            onToggleLegEnabled = { viewModel.toggleLegEnabled(it) },
            onDuplicateLeg = { viewModel.duplicateLeg(it) },
            onRemoveLeg = { viewModel.removeLeg(it) },
            onToggleLegType = { viewModel.toggleLegType(it) },
            onToggleLegInstrument = { viewModel.toggleLegInstrument(it) },
            onShiftStrike = { id, d -> viewModel.shiftLegStrike(id, d) },
            onUpdateLegQty = { id, q -> viewModel.updateLegQty(id, q) },
            onUpdateLegEntryPrice = { id, p -> viewModel.updateLegEntryPrice(id, p) },
            onSelectAllLegs = { viewModel.selectAllLegs(it) },
            onRequestReset = { viewModel.requestResetStrategy() },
            onAddAtmLeg = {
                val step = uiState.strikeStep.coerceAtLeast(1.0)
                val atm = kotlin.math.round(uiState.spotPrice / step) * step
                viewModel.addLeg("CALL", "BUY", atm)
            },
            onOpenChainPicker = { isStrikePickerOpen = true }
        )

        Spacer(Modifier.height(14.dp))

        // --- 9. MARKET SCENARIO SIMULATOR ---
        OpstraSimulationBar(
            spotPrice = uiState.spotPrice,
            baseIv = uiState.summary.iv.coerceAtLeast(10.0),
            spotShiftPct = uiState.spotSimulationPct,
            ivShift = uiState.ivSimulation,
            timeShiftDays = uiState.timeSimulation,
            onSimulationChange = { s, i, t -> viewModel.onSimulationChanged(s, i, t) },
            onResetSimulation = { viewModel.resetSimulation() }
        )

        Spacer(Modifier.height(20.dp))

        // --- 10. TRACKED PORTFOLIO STRATEGIES ---
        OpstraSavedStrategiesList(
            strategies = uiState.trackedStrategies,
            editingStrategyId = uiState.editingStrategyId,
            onEdit = { viewModel.editTrackedStrategy(it) },
            onRename = { id, name -> viewModel.openRenameDialog(id, name) },
            onDuplicate = { viewModel.duplicateTrackedStrategy(it) },
            onDeleteRequest = { viewModel.requestDeleteTrackedStrategy(it) }
        )

        Spacer(Modifier.height(100.dp))
    }

    // --- DIALOGS ---
    if (isStrikePickerOpen) {
        StrikeChainPickerDialog(
            chain = uiState.optionChain,
            spotPrice = uiState.spotPrice,
            onDismiss = { isStrikePickerOpen = false },
            onAddLeg = { strike, instrument, action ->
                viewModel.addLeg(instrument, action, strike)
            }
        )
    }

    OpstraResetConfirmDialog(
        isOpen = uiState.showResetConfirmDialog,
        onConfirm = { viewModel.confirmResetStrategy() },
        onDismiss = { viewModel.dismissResetDialog() }
    )

    OpstraDeleteConfirmDialog(
        isOpen = uiState.showDeleteConfirmDialog,
        onConfirm = { viewModel.confirmDeleteTrackedStrategy() },
        onDismiss = { viewModel.dismissDeleteDialog() }
    )

    OpstraRenameDialog(
        isOpen = uiState.isRenameDialogOpen,
        currentName = uiState.strategyRenameInput,
        onNameChange = { viewModel.setRenameInput(it) },
        onConfirm = { viewModel.confirmRenameStrategy() },
        onDismiss = { viewModel.dismissRenameDialog() }
    )
}

@Composable
fun StrategyLegRow(
    leg: StrategyLeg,
    lotSize: Int = 65,
    spotPrice: Double = 0.0,
    strikeStep: Double = 50.0,
    underlying: String = "",
    onShiftStrike: (Int) -> Unit,
    onToggleType: () -> Unit,
    onToggleInstrument: () -> Unit,
    onRemove: () -> Unit
) {
    val legPnlColor = if (leg.pnl >= 0.0) AppGreen else AppRed
    val legPnlSign = if (leg.pnl >= 0.0) "+" else ""
    val effLotSize = if (leg.lotSize > 0) leg.lotSize else if (lotSize > 0) lotSize else 65
    val effLots = if (leg.lots > 0) leg.lots else (leg.qty / effLotSize).coerceAtLeast(1)
    val legUnderlying = leg.underlying.ifBlank { underlying }
    val halfStep = (strikeStep / 2.0).coerceAtLeast(0.5)
    val isAtm = spotPrice > 0.0 && abs(leg.strike - spotPrice) <= halfStep
    val isCall = leg.instrument.equals("CALL", ignoreCase = true)
    val isPut = leg.instrument.equals("PUT", ignoreCase = true)
    val computedMoneyness = when {
        leg.moneyness.isNotBlank() -> leg.moneyness
        leg.instrument == "FUT" -> "FUT"
        isAtm -> "ATM"
        isCall && spotPrice > 0.0 && leg.strike < spotPrice -> "ITM"
        isCall && spotPrice > 0.0 && leg.strike > spotPrice -> "OTM"
        isPut && spotPrice > 0.0 && leg.strike > spotPrice -> "ITM"
        isPut && spotPrice > 0.0 && leg.strike < spotPrice -> "OTM"
        else -> "ATM"
    }
    val moneynessColor = when (computedMoneyness) {
        "ATM" -> Color(0xFF00E5FF)
        "ITM" -> Color(0xFFFFB300)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Top Row: Type & Instrument chips + Moneyness Chip + Expiry + Strike Stepper + Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // BUY / SELL Chip (Clickable to toggle)
                    Surface(
                        color = (if (leg.type == "BUY") AppGreen else AppRed).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onToggleType() }
                            .border(0.5.dp, if (leg.type == "BUY") AppGreen else AppRed, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            leg.type,
                            color = if (leg.type == "BUY") AppGreen else AppRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Instrument Chip (CALL / PUT / FUT)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onToggleInstrument() }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            leg.instrument,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Moneyness Chip (ITM / ATM / OTM)
                    Surface(
                        color = moneynessColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(0.5.dp, moneynessColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            computedMoneyness,
                            color = moneynessColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Expiry Chip
                    if (leg.expiry.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                leg.expiry,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (effLots > 1) {
                        Text(
                            "${effLots}x",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Interactive Strike Stepper: [-] STRIKE [+]
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onShiftStrike(-1) }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Text(
                        "${leg.strike.toInt()}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onShiftStrike(1) }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Middle Row: Institutional Contract Details (Underlying, Lot Size, Lots, Leg Outlay)
            val legOutlay = leg.entryPrice * leg.qty
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${if (legUnderlying.isNotBlank()) "${legUnderlying.uppercase()} • " else ""}Lot: $effLotSize • $effLots Lot(s) (${leg.qty} Qty)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Outlay: ₹${"%,d".format(legOutlay.toLong())}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            // Bottom Row: Entry vs LTP & Live Leg PnL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Entry: ₹${"%.1f".format(leg.entryPrice)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "LTP: ₹${"%.1f".format(leg.currentPrice)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    "$legPnlSign₹${"%.1f".format(leg.pnl)} ($legPnlSign${"%.1f".format(leg.pnlPercent)}%)",
                    color = legPnlColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun StrategyMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun AdvancedPayoffChart(points: List<PayoffPoint>, spotPrice: Double) {
    if (points.isEmpty()) return
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val minPrice = points.minOf { it.price }
        val maxPrice = points.maxOf { it.price }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0).toFloat()
        
        val minPnl = points.minOf { it.pnl }
        val maxPnl = points.maxOf { it.pnl }
        val pnlRange = (maxPnl - minPnl).coerceAtLeast(1.0).toFloat()
        
        // Zero Line
        val zeroY = height * (1 - (0 - minPnl) / pnlRange).toFloat()
        drawLine(
            color = outline,
            start = Offset(0f, zeroY),
            end = Offset(width, zeroY),
            strokeWidth = 1.dp.toPx()
        )
        
        // Spot Price Line
        val spotX = width * ((spotPrice - minPrice) / priceRange).toFloat()
        if (spotX in 0f..width) {
            drawLine(
                color = onSurface.copy(0.3f),
                start = Offset(spotX, 0f),
                end = Offset(spotX, height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // Main Payoff Path
        val mainPath = Path()
        points.forEachIndexed { index, point ->
            val x = width * ((point.price - minPrice) / priceRange).toFloat()
            val y = height * (1 - (point.pnl - minPnl) / pnlRange).toFloat()
            if (index == 0) mainPath.moveTo(x, y) else mainPath.lineTo(x, y)
        }
        
        drawPath(
            path = mainPath,
            color = onSurface,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Mark Breakeven points
        points.forEachIndexed { index, point ->
            if (index > 0) {
                val prev = points[index-1]
                if ((prev.pnl < 0 && point.pnl >= 0) || (prev.pnl >= 0 && point.pnl < 0)) {
                    val x = width * ((point.price - minPrice) / priceRange).toFloat()
                    drawCircle(onSurface, radius = 3.dp.toPx(), center = Offset(x, zeroY))
                }
            }
        }
    }
}

@Composable
fun MaxPainAnalyticsView(history: List<Double>, currentMaxPain: Double, spotPrice: Double, curr: String = "₹") {
    val diff = spotPrice - currentMaxPain
    val diffPercent = if (currentMaxPain > 0) (diff / currentMaxPain) * 100 else 0.0
    val dataPoints = if (history.isNotEmpty()) history else listOf(currentMaxPain - 100, currentMaxPain - 50, currentMaxPain + 50, currentMaxPain)
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MAX PAIN PINNING & MIGRATION", color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Text("Theoretical writer optimal settlement strike vs spot trajectory", color = onSurfaceVariant, fontSize = 9.sp)

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("MAX PAIN STRIKE", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("$curr${currentMaxPain.toInt()}", color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPOT GAP", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${if (diff >= 0) "+" else ""}${"%.1f".format(diff)} (${if (diffPercent >= 0) "+" else ""}${"%.2f".format(diffPercent)}%)",
                            color = if (diff >= 0) AppGreen else AppRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PINNING BIAS", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(if (kotlin.math.abs(diffPercent) < 0.8) "HIGH PIN" else "VOL EXPAND", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val minVal = (dataPoints + spotPrice).minOrNull() ?: 0.0
                        val maxVal = (dataPoints + spotPrice).maxOrNull() ?: 1.0
                        val range = (maxVal - minVal).coerceAtLeast(10.0).toFloat()

                        // Grid lines
                        for (step in 0..3) {
                            val y = h * (step / 3f)
                            drawLine(outlineColor.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
                        }

                        // Spot price line
                        val spotY = h * (1f - ((spotPrice - minVal) / range).toFloat()).coerceIn(0f, h)
                        drawLine(
                            color = onSurface.copy(alpha = 0.4f),
                            start = Offset(0f, spotY),
                            end = Offset(w, spotY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )

                        // Max Pain Line
                        val path = Path()
                        val stepX = if (dataPoints.size > 1) w / (dataPoints.size - 1) else w
                        dataPoints.forEachIndexed { index, pt ->
                            val x = index * stepX
                            val y = h * (1f - ((pt - minVal) / range).toFloat()).coerceIn(0f, h)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(primaryColor, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path, color = primaryColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("--- Spot Price Baseline", color = onSurface.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("— Max Pain Migration", color = primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun IvAnalyticsView(history: List<Double>, currentIv: Double) {
    val dataPoints = if (history.isNotEmpty()) history else listOf(currentIv - 1.2, currentIv - 0.5, currentIv + 0.8, currentIv)
    val maxIv = dataPoints.maxOrNull() ?: currentIv
    val minIv = dataPoints.minOrNull() ?: currentIv
    val avgIv = if (dataPoints.isNotEmpty()) dataPoints.average() else currentIv
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val ivColor = Color(0xFFBD7CFF)

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("IMPLIED VOLATILITY (IV) REGIME", color = ivColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Text("Option pricing volatility skew across recent sessions", color = onSurfaceVariant, fontSize = 9.sp)

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CURRENT IV", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(currentIv)}%", color = ivColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("10-DAY HIGH / LOW", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(maxIv)}% / ${"%.1f".format(minIv)}%", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MEAN IV", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(avgIv)}%", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val minVal = (minIv * 0.9).coerceAtLeast(0.0)
                        val maxVal = maxIv * 1.1
                        val range = (maxVal - minVal).coerceAtLeast(1.0).toFloat()

                        // Grid lines
                        for (step in 0..3) {
                            val y = h * (step / 3f)
                            drawLine(outlineColor.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
                        }

                        // Mean line
                        val meanY = h * (1f - ((avgIv - minVal) / range).toFloat()).coerceIn(0f, h)
                        drawLine(
                            color = Color.Cyan.copy(alpha = 0.4f),
                            start = Offset(0f, meanY),
                            end = Offset(w, meanY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // IV Path
                        val path = Path()
                        val stepX = if (dataPoints.size > 1) w / (dataPoints.size - 1) else w
                        dataPoints.forEachIndexed { index, pt ->
                            val x = index * stepX
                            val y = h * (1f - ((pt - minVal) / range).toFloat()).coerceIn(0f, h)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(ivColor, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path, color = ivColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("--- Mean IV Baseline", color = Color.Cyan.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("— Implied Volatility Curve", color = ivColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun GexAnalyticsView(profile: com.example.marketintelligence.domain.engine.GexProfile?, spotPrice: Double, curr: String = "₹") {
    if (profile == null || profile.strikeGexList.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Calculating Dealer Gamma Exposure (GEX)...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val isLongGamma = profile.totalNetGex >= 0

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DEALER GAMMA EXPOSURE (GEX)", color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                        Text("Market Maker hedging flow & structural boundaries", color = onSurfaceVariant, fontSize = 9.sp)
                    }
                    Surface(
                        color = (if (isLongGamma) AppGreen else AppRed).copy(0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(0.5.dp, if (isLongGamma) AppGreen else AppRed, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            if (isLongGamma) "LONG GAMMA" else "SHORT GAMMA",
                            color = if (isLongGamma) AppGreen else AppRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TOTAL NET GEX", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${if (profile.totalNetGex >= 0) "+" else ""}${"%.1f".format(profile.totalNetGex)} Cr", color = if (isLongGamma) AppGreen else AppRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GAMMA FLIP", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("$curr${profile.gammaFlipStrike.toInt()}", color = Color.Cyan, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CALL / PUT WALL", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("$curr${profile.callWallStrike.toInt()} / $curr${profile.putWallStrike.toInt()}", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // GEX Strike distribution Canvas
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val strikes = profile.strikeGexList
                        val maxAbsGex = strikes.maxOfOrNull { maxOf(kotlin.math.abs(it.callGex), kotlin.math.abs(it.putGex)) }?.coerceAtLeast(1.0) ?: 1.0
                        val barHeight = (h / strikes.size).coerceAtMost(16.dp.toPx())
                        val centerX = w * 0.5f

                        // Center Zero Line
                        drawLine(
                            color = outlineColor.copy(alpha = 0.5f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        strikes.forEachIndexed { index, s ->
                            val y = index * (h / strikes.size) + 2f
                            
                            // Put GEX (Left, negative / red)
                            val putWidth = ((kotlin.math.abs(s.putGex) / maxAbsGex) * (w * 0.45f)).toFloat()
                            drawRect(
                                color = AppRed.copy(alpha = 0.8f),
                                topLeft = Offset(centerX - putWidth, y),
                                size = Size(putWidth, barHeight * 0.7f)
                            )

                            // Call GEX (Right, positive / green)
                            val callWidth = ((s.callGex / maxAbsGex) * (w * 0.45f)).toFloat()
                            drawRect(
                                color = AppGreen.copy(alpha = 0.8f),
                                topLeft = Offset(centerX, y),
                                size = Size(callWidth, barHeight * 0.7f)
                            )

                            // Highlight Spot Strike
                            if (kotlin.math.abs(s.strike - spotPrice) < 50.0) {
                                drawCircle(
                                    color = onSurface,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(centerX, y + barHeight * 0.35f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("← Put GEX (Short Vol Risk)", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Spot ● Flip $curr${profile.gammaFlipStrike.toInt()}", color = onSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("Call GEX (Pinning Magnet) →", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun OptionStrikeCandleChart(
    candles: List<Candle>,
    currentLtp: Double,
    isCall: Boolean,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No candle data available", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        return
    }

    var touchOffset by remember { mutableStateOf<Offset?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val rightMarginPx = with(density) { 52.dp.toPx() }
    val bottomMarginPx = with(density) { 20.dp.toPx() }

    val rawMin = candles.minOf { it.low }
    val rawMax = candles.maxOf { it.high }
    val padding = ((rawMax - rawMin) * 0.08).coerceAtLeast(0.5)
    val priceMin = (rawMin - padding).coerceAtLeast(0.01)
    val priceMax = rawMax + padding
    val priceRange = (priceMax - priceMin).coerceAtLeast(0.01)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()
        val chartAreaWidth = (totalWidth - rightMarginPx).coerceAtLeast(1f)
        val chartAreaHeight = (totalHeight - bottomMarginPx).coerceAtLeast(1f)
        val candleCount = candles.size
        val candleSlotWidth = (chartAreaWidth / candleCount).coerceAtLeast(1f)

        val hoveredIndex = touchOffset?.let { pos ->
            val idx = (pos.x / candleSlotWidth).toInt().coerceIn(0, candles.lastIndex)
            idx
        }
        val displayCandle = if (hoveredIndex != null) candles[hoveredIndex] else candles.last()

        Column(modifier = Modifier.fillMaxSize()) {
            // OHLCV Strip Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isBullish = displayCandle.close >= displayCandle.open
                val candleColor = if (isBullish) AppGreen else AppRed
                val timeStr = SimpleDateFormat("dd MMM, HH:mm", Locale.US).format(Date(displayCandle.openTime))

                Text(timeStr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("O: ₹${"%.1f".format(displayCandle.open)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("H: ₹${"%.1f".format(displayCandle.high)}", color = AppGreen, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("L: ₹${"%.1f".format(displayCandle.low)}", color = AppRed, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                    Text("C: ₹${"%.1f".format(displayCandle.close)}", color = candleColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .pointerInput(candles) {
                        detectDragGestures(
                            onDragStart = { offset -> touchOffset = offset },
                            onDrag = { change, _ ->
                                change.consume()
                                touchOffset = change.position
                            },
                            onDragEnd = { touchOffset = null },
                            onDragCancel = { touchOffset = null }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { touchOffset = null }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val chartW = (w - rightMarginPx).coerceAtLeast(1f)
                val chartH = (h - bottomMarginPx).coerceAtLeast(1f)
                val volumeH = chartH * 0.20f
                val priceH = chartH - volumeH

                val maxVol = candles.maxOfOrNull { it.volume }?.coerceAtLeast(1.0) ?: 1.0

                // 1. Grid Lines
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val frac = i.toFloat() / gridSteps
                    val y = priceH * frac
                    val priceVal = priceMax - frac * priceRange

                    drawLine(
                        color = Color.White.copy(alpha = 0.07f),
                        start = Offset(0f, y),
                        end = Offset(chartW, y),
                        strokeWidth = 1f
                    )

                    val priceStr = "₹${"%.1f".format(priceVal)}"
                    val textLayout = textMeasurer.measure(
                        text = priceStr,
                        style = TextStyle(color = Color(0xFF787B86), fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                    )
                    drawText(textLayout, topLeft = Offset(chartW + 4f, y - textLayout.size.height / 2f))
                }

                // 2. Volume Bars
                val slotW = chartW / candles.size
                val barW = (slotW * 0.7f).coerceAtLeast(1f)

                for (idx in candles.indices) {
                    val c = candles[idx]
                    val cx = idx * slotW + slotW / 2f
                    val isBull = c.close >= c.open
                    val col = if (isBull) AppGreen.copy(alpha = 0.35f) else AppRed.copy(alpha = 0.35f)

                    val volFrac = (c.volume / maxVol).toFloat().coerceIn(0f, 1f)
                    val vBarH = volumeH * volFrac
                    val vTop = chartH - vBarH

                    drawRect(
                        color = col,
                        topLeft = Offset(cx - barW / 2f, vTop),
                        size = Size(barW, vBarH)
                    )
                }

                // 3. Candlesticks
                for (idx in candles.indices) {
                    val c = candles[idx]
                    val cx = idx * slotW + slotW / 2f
                    val isBull = c.close >= c.open
                    val col = if (isBull) Color(0xFF00E676) else Color(0xFFFF5252)

                    val openY = (priceH * (1f - ((c.open - priceMin) / priceRange).toFloat())).coerceIn(0f, priceH)
                    val closeY = (priceH * (1f - ((c.close - priceMin) / priceRange).toFloat())).coerceIn(0f, priceH)
                    val highY = (priceH * (1f - ((c.high - priceMin) / priceRange).toFloat())).coerceIn(0f, priceH)
                    val lowY = (priceH * (1f - ((c.low - priceMin) / priceRange).toFloat())).coerceIn(0f, priceH)

                    drawLine(
                        color = col,
                        start = Offset(cx, highY),
                        end = Offset(cx, lowY),
                        strokeWidth = 1.2f
                    )

                    val topY = min(openY, closeY)
                    val bodyH = max(abs(openY - closeY), 1.5f)
                    drawRect(
                        color = col,
                        topLeft = Offset(cx - barW / 2f, topY),
                        size = Size(barW, bodyH)
                    )
                }

                // 4. Live Current Price Guideline
                val liveLtpY = (priceH * (1f - ((currentLtp - priceMin) / priceRange).toFloat())).coerceIn(0f, priceH)
                val lineCol = if (isCall) AppRed else AppGreen

                drawLine(
                    color = lineCol.copy(alpha = 0.75f),
                    start = Offset(0f, liveLtpY),
                    end = Offset(chartW, liveLtpY),
                    strokeWidth = 1.2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )

                val badgeStr = "₹${"%.2f".format(currentLtp)}"
                val badgeLayout = textMeasurer.measure(
                    text = badgeStr,
                    style = TextStyle(color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                )
                val badgePadX = 3f
                val badgePadY = 1.5f
                val badgeRectLeft = chartW + 2f
                val badgeRectTop = liveLtpY - badgeLayout.size.height / 2f - badgePadY

                drawRoundRect(
                    color = lineCol,
                    topLeft = Offset(badgeRectLeft, badgeRectTop),
                    size = Size(badgeLayout.size.width + badgePadX * 2, badgeLayout.size.height + badgePadY * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
                drawText(badgeLayout, topLeft = Offset(badgeRectLeft + badgePadX, badgeRectTop + badgePadY))

                // 5. Crosshair
                val touch = touchOffset
                if (touch != null && touch.x in 0f..chartW && touch.y in 0f..chartH) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(touch.x, 0f),
                        end = Offset(touch.x, chartH),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(0f, touch.y),
                        end = Offset(chartW, touch.y),
                        strokeWidth = 1f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }

                // 6. Time labels
                val labelStep = (candles.size / 4).coerceAtLeast(1)
                for (idx in candles.indices step labelStep) {
                    val c = candles[idx]
                    val cx = idx * slotW + slotW / 2f
                    val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date(c.openTime))
                    val layout = textMeasurer.measure(
                        text = timeStr,
                        style = TextStyle(color = Color(0xFF787B86), fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                    )
                    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, chartH + 3f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrikePriceDetailSheet(
    strike: OptionChainData,
    spotPrice: Double,
    assetSymbol: String,
    selectedSide: String? = null,
    lotSize: Int,
    strikeChartState: StrikeChartState,
    onTimeframeChange: (String) -> Unit,
    onOptionTypeChange: (String) -> Unit,
    onRetryChart: () -> Unit,
    onDismiss: () -> Unit,
    onAddLeg: (strikePrice: Double, instrument: String, action: String) -> Unit,
    onBuildStraddle: (strikePrice: Double) -> Unit,
    onNavigateToBuilder: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeTab by remember { mutableStateOf("CHART") }
    val strikeDiff = strike.strike - spotPrice
    val halfStep = when {
        assetSymbol.contains("BANKNIFTY", ignoreCase = true) -> 50.0
        assetSymbol.contains("TATASTEEL", ignoreCase = true) -> 1.25
        assetSymbol.contains("SPX", ignoreCase = true) -> 12.5
        assetSymbol.contains("BTC", ignoreCase = true) -> 250.0
        else -> 25.0
    }
    val isAtm = abs(strikeDiff) <= halfStep
    val totalOi = (strike.callOI + strike.putOI).coerceAtLeast(1.0)
    val callOiRatio = (strike.callOI / totalOi).coerceIn(0.05, 0.95)
    val pcr = if (strike.callOI > 0) strike.putOI / strike.callOI else 1.0

    val isCurrentCall = strikeChartState.optionType == "CE"
    val activeColor = if (isCurrentCall) AppRed else AppGreen
    val activeLtp = if (isCurrentCall) strike.callLTP else strike.putLTP
    val activeChange = if (isCurrentCall) strike.callChange else strike.putChange
    val activeIv = if (isCurrentCall) strike.callIV else strike.putIV
    val activeOi = if (isCurrentCall) strike.callOI else strike.putOI
    val activeGreeks = if (isCurrentCall) strike.callGreeks else strike.putGreeks

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Back Navigation, Asset & Contract Identification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back to OI",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "${assetSymbol.uppercase()} DERIVATIVES",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "₹${strike.strike.toInt()} ${strikeChartState.optionType}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                color = when {
                                    isAtm -> Color(0xFF00E5FF).copy(alpha = 0.15f)
                                    (isCurrentCall && strikeDiff < 0) || (!isCurrentCall && strikeDiff > 0) -> AppGreen.copy(alpha = 0.15f)
                                    else -> AppRed.copy(alpha = 0.15f)
                                },
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.border(
                                    0.5.dp,
                                    when {
                                        isAtm -> Color(0xFF00E5FF)
                                        (isCurrentCall && strikeDiff < 0) || (!isCurrentCall && strikeDiff > 0) -> AppGreen
                                        else -> AppRed
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                            ) {
                                Text(
                                    text = when {
                                        isAtm -> "ATM (AT-THE-MONEY)"
                                        isCurrentCall && strikeDiff < 0 -> "ITM CALL (${"%.1f".format(abs(strikeDiff))} pts)"
                                        isCurrentCall && strikeDiff >= 0 -> "OTM CALL (+${"%.1f".format(strikeDiff)} pts)"
                                        !isCurrentCall && strikeDiff > 0 -> "ITM PUT (+${"%.1f".format(strikeDiff)} pts)"
                                        else -> "OTM PUT (${"%.1f".format(abs(strikeDiff))} pts)"
                                    },
                                    color = when {
                                        isAtm -> Color(0xFF00E5FF)
                                        (isCurrentCall && strikeDiff < 0) || (!isCurrentCall && strikeDiff > 0) -> AppGreen
                                        else -> AppRed
                                    },
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SPOT: ₹${"%.2f".format(spotPrice)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "EXP: ${strikeChartState.expiry.ifBlank { "CURRENT" }}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "LOT SIZE: $lotSize",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Prominent CE / PE Option Contract Switcher
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // CALL (CE) CARD
                val isCallSelected = strikeChartState.optionType == "CE"
                Surface(
                    color = if (isCallSelected) AppRed.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionTypeChange("CE") }
                        .border(
                            width = if (isCallSelected) 1.5.dp else 0.5.dp,
                            color = if (isCallSelected) AppRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("CALL (CE)", color = AppRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            if (isCallSelected) {
                                Surface(color = AppRed, shape = RoundedCornerShape(3.dp)) {
                                    Text("VIEWING", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("₹${"%.2f".format(strike.callLTP)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            Text(
                                "${if (strike.callChange >= 0) "+" else ""}${"%.1f".format(strike.callChange)}%",
                                color = if (strike.callChange >= 0) AppGreen else AppRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // PUT (PE) CARD
                val isPutSelected = strikeChartState.optionType == "PE"
                Surface(
                    color = if (isPutSelected) AppGreen.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOptionTypeChange("PE") }
                        .border(
                            width = if (isPutSelected) 1.5.dp else 0.5.dp,
                            color = if (isPutSelected) AppGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("PUT (PE)", color = AppGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            if (isPutSelected) {
                                Surface(color = AppGreen, shape = RoundedCornerShape(3.dp)) {
                                    Text("VIEWING", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text("₹${"%.2f".format(strike.putLTP)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            Text(
                                "${if (strike.putChange >= 0) "+" else ""}${"%.1f".format(strike.putChange)}%",
                                color = if (strike.putChange >= 0) AppGreen else AppRed,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Navigation Tabs inside Detail View
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("CHART", "OVERVIEW", "GREEKS").forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeTab = tab }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (activeTab) {
                "CHART" -> {
                    // Timeframe Selector & Chart Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strikeChartState.contractDisplayName} CHART",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            listOf("1m", "5m", "15m", "1H", "1D").forEach { tf ->
                                val isSelected = strikeChartState.selectedTimeframe.equals(tf, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { onTimeframeChange(tf) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tf,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Candlestick Chart Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.8.dp, activeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        if (strikeChartState.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                                    Text("Loading ${strikeChartState.contractDisplayName} Chart...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else if (strikeChartState.error != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AppRed, modifier = Modifier.size(28.dp))
                                    Text(strikeChartState.error, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = onRetryChart,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text("RETRY", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            OptionStrikeCandleChart(
                                candles = strikeChartState.candles,
                                currentLtp = strikeChartState.ltp,
                                isCall = isCurrentCall,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Contract Greeks & Metrics Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CONTRACT GREEKS & VOLATILITY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                Text("OI: ${(activeOi / 1000).toInt()}k contracts", color = activeColor, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("DELTA (Δ)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp)
                                    Text("%.2f".format(activeGreeks.delta), color = Color.Cyan, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("THETA (Θ)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp)
                                    Text("%.1f/d".format(activeGreeks.theta), color = Color.Magenta, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("GAMMA (Γ)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp)
                                    Text("%.4f".format(activeGreeks.gamma), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("VEGA (V)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp)
                                    Text("%.1f".format(activeGreeks.vega), color = AppGreen, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("IV", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp)
                                    Text("${"%.1f".format(activeIv)}%", color = Color(0xFFFFCA28), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Quick Buy / Sell Action Buttons for Active Contract
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onAddLeg(strike.strike, if (isCurrentCall) "CALL" else "PUT", "BUY") },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ BUY ${strikeChartState.optionType}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onAddLeg(strike.strike, if (isCurrentCall) "CALL" else "PUT", "SELL") },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("- SELL ${strikeChartState.optionType}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "OVERVIEW" -> {
                    // Open Interest Tug-of-War Bar & PCR
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("STRIKE OPEN INTEREST FLOW", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("PCR: ${"%.2f".format(pcr)}", color = if (pcr >= 1.0) AppGreen else AppRed, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight((callOiRatio).toFloat().coerceIn(0.05f, 0.95f))
                                        .fillMaxHeight()
                                        .background(AppRed)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight((1.0 - callOiRatio).toFloat().coerceIn(0.05f, 0.95f))
                                        .fillMaxHeight()
                                        .background(AppGreen)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CALL OI: ${(strike.callOI / 1000).toInt()}k (${"%.1f".format(callOiRatio * 100)}%)", color = AppRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("PUT OI: ${(strike.putOI / 1000).toInt()}k (${"%.1f".format((1.0 - callOiRatio) * 100)}%)", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Multi-Leg Strategy Builder Actions
                    Text("STRATEGY BUILDER CONSTRUCTS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onBuildStraddle(strike.strike) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("BUILD STRADDLE", color = MaterialTheme.colorScheme.primary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onNavigateToBuilder,
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("OPEN BUILDER", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "GREEKS" -> {
                    StrikeGreeksComparisonCard(strike)
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun StrikeSideDetailCard(
    title: String,
    ltp: Double,
    changePct: Double,
    oi: Double,
    iv: Double,
    greeks: Greeks,
    isCall: Boolean,
    onAddBuy: () -> Unit,
    onAddSell: () -> Unit
) {
    val themeColor = if (isCall) AppRed else AppGreen
    val changeColor = if (changePct >= 0) AppGreen else AppRed
    val changeSign = if (changePct >= 0) "+" else ""

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = themeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    color = changeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, changeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        text = "$changeSign${"%.2f".format(changePct)}%",
                        color = changeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("LAST TRADED PRICE (LTP)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "₹${"%.2f".format(ltp)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("OPEN INTEREST", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${(oi / 1000).toInt()}k contracts",
                        color = themeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            // Greeks & Volatility Grid
            Text("CONTRACT GREEKS & VOLATILITY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("DELTA (Δ)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("%.2f".format(greeks.delta), color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("THETA (Θ)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("%.1f/d".format(greeks.theta), color = Color.Magenta, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GAMMA (Γ)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("%.4f".format(greeks.gamma), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("VEGA (V)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text("%.1f".format(greeks.vega), color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Add Leg Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddBuy,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("BUY CONTRACT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddSell,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SELL CONTRACT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StrikeGreeksComparisonCard(strike: OptionChainData) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "INSTITUTIONAL GREEKS COMPARISON • ₹${strike.strike.toInt()}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GREEK", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("CALL (CE)", color = AppRed, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("PUT (PE)", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }

            Spacer(Modifier.height(6.dp))

            val greekRows = listOf(
                Triple("DELTA (Δ)", "%.2f".format(strike.callGreeks.delta), "%.2f".format(strike.putGreeks.delta)),
                Triple("GAMMA (Γ)", "%.4f".format(strike.callGreeks.gamma), "%.4f".format(strike.putGreeks.gamma)),
                Triple("THETA (Θ/day)", "%.1f".format(strike.callGreeks.theta), "%.1f".format(strike.putGreeks.theta)),
                Triple("VEGA (V)", "%.1f".format(strike.callGreeks.vega), "%.1f".format(strike.putGreeks.vega)),
                Triple("IV (%)", "${"%.1f".format(strike.callIV)}%", "${"%.1f".format(strike.putIV)}%"),
                Triple("LTP (₹)", "₹${"%.2f".format(strike.callLTP)}", "₹${"%.2f".format(strike.putLTP)}"),
                Triple("OI (Contracts)", "${(strike.callOI / 1000).toInt()}k", "${(strike.putOI / 1000).toInt()}k")
            )

            greekRows.forEachIndexed { idx, (label, callVal, putVal) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Text(callVal, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(putVal, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                if (idx < greekRows.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrikeChainPickerDialog(
    chain: OptionChain?,
    spotPrice: Double,
    onDismiss: () -> Unit,
    onAddLeg: (strike: Double, instrument: String, action: String) -> Unit
) {
    if (chain == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SELECT STRIKE FOR STRATEGY",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Spot: ₹${"%.2f".format(spotPrice)} • Tap to add leg",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Column Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CALL (+ / -)", color = AppRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                Text("STRIKE", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("PUT (+ / -)", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }

            Spacer(Modifier.height(6.dp))

            val currentStrike = chain.strikes.minByOrNull { abs(it.strike - spotPrice) }?.strike
            val listState = rememberLazyListState()
            LaunchedEffect(currentStrike) {
                if (currentStrike != null) {
                    val idx = chain.strikes.indexOfFirst { it.strike == currentStrike }
                    if (idx >= 0) listState.scrollToItem((idx - 3).coerceAtLeast(0))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chain.strikes) { strike ->
                    val isAtm = strike.strike == currentStrike
                    val isITMCall = strike.strike < spotPrice
                    val isITMPut = strike.strike > spotPrice
                    val callMoneyness = if (isAtm) "ATM" else if (isITMCall) "ITM" else "OTM"
                    val putMoneyness = if (isAtm) "ATM" else if (isITMPut) "ITM" else "OTM"
                    val callMoneynessColor = if (isAtm) Color(0xFF00E5FF) else if (isITMCall) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    val putMoneynessColor = if (isAtm) Color(0xFF00E5FF) else if (isITMPut) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    
                    Surface(
                        color = if (isAtm) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isAtm) 1.dp else 0.5.dp,
                                color = if (isAtm) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CALL BUTTONS & MONEYNESS
                            Row(
                                modifier = Modifier.weight(1.25f),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = callMoneynessColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.border(0.5.dp, callMoneynessColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                ) {
                                    Text(
                                        callMoneyness,
                                        color = callMoneynessColor,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    color = AppGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable { onAddLeg(strike.strike, "CALL", "BUY") }
                                        .border(0.5.dp, AppGreen, RoundedCornerShape(4.dp))
                                ) {
                                    Text("+B", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
                                }
                                Surface(
                                    color = AppRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable { onAddLeg(strike.strike, "CALL", "SELL") }
                                        .border(0.5.dp, AppRed, RoundedCornerShape(4.dp))
                                ) {
                                    Text("-S", color = AppRed, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
                                }
                                Text("₹${strike.callLTP.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            // STRIKE & MONEYNESS HINT
                            Column(
                                modifier = Modifier.weight(0.9f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("₹${strike.strike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                if (isAtm) {
                                    Surface(
                                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.border(0.5.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                                    ) {
                                        Text("ATM", color = Color(0xFF00E5FF), fontSize = 7.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                } else {
                                    Text("C:$callMoneyness • P:$putMoneyness", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 6.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // PUT BUTTONS & MONEYNESS
                            Row(
                                modifier = Modifier.weight(1.25f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("₹${strike.putLTP.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.width(3.dp))
                                Surface(
                                    color = AppGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable { onAddLeg(strike.strike, "PUT", "BUY") }
                                        .border(0.5.dp, AppGreen, RoundedCornerShape(4.dp))
                                ) {
                                    Text("+B", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
                                }
                                Spacer(Modifier.width(3.dp))
                                Surface(
                                    color = AppRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .clickable { onAddLeg(strike.strike, "PUT", "SELL") }
                                        .border(0.5.dp, AppRed, RoundedCornerShape(4.dp))
                                ) {
                                    Text("-S", color = AppRed, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
                                }
                                Spacer(Modifier.width(3.dp))
                                Surface(
                                    color = putMoneynessColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(3.dp),
                                    modifier = Modifier.border(0.5.dp, putMoneynessColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                ) {
                                    Text(
                                        putMoneyness,
                                        color = putMoneynessColor,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
