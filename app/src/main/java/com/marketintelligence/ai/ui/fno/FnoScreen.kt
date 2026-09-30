package com.marketintelligence.ai.ui.fno

import kotlin.math.abs
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.ui.theme.AppGreen
import com.marketintelligence.ai.ui.theme.AppRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FnoScreen(
    viewModel: FnoViewModel = hiltViewModel(),
    onOpenBuildupScanner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("MARKET", "ANALYTICS", "BUILDER")

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp)) {
        Spacer(modifier = Modifier.height(6.dp))
        
        Text("F&O INTELLIGENCE HUB", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Search & Add Header (Hidden when on Strategy Builder tab)
        if (selectedTab != 2) {
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
                                    Text(
                                        "Search F&O Asset...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                innerTextField()
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
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Asset Mini-Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(uiState.selectedAsset.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "SPOT: ₹${"%.2f".format(uiState.spotPrice)}",
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
                0 -> SummaryTab(uiState)
                1 -> OptionsStudyTab(uiState, viewModel, onOpenBuildupScanner)
                2 -> StrategyBuilderTab(uiState, viewModel)
            }
        }
    }
}

@Composable
fun SummaryTab(uiState: FnoUiState) {
    val scrollState = rememberScrollState()
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
                        Text("₹${"%.2f".format(fp.ltp)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "${if (fp.changePercent >= 0) "+" else ""}${"%.2f".format(fp.changePercent)}%",
                            color = if (fp.changePercent >= 0) AppGreen else AppRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.summary.contracts.lastOrNull() != fp) Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStatCard("IMPLIED VOL (IV)", "${"%.1f".format(uiState.summary.iv)}%", Modifier.weight(1f))
            SummaryStatCard("PUT-CALL RATIO", "%.2f".format(uiState.summary.pcr), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStatCard("MAX PAIN", "₹${uiState.summary.maxPain.toInt()}", Modifier.weight(1f))
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
                    Column {
                        Text("RESISTANCE (CALL OI)", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text("₹${uiState.summary.highestCallOIStrike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SUPPORT (PUT OI)", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text("₹${uiState.summary.highestPutOIStrike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
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
    var viewMode by remember { mutableStateOf("OI") } // "OI", "GREEKS", "GEX", "PAIN", "IV"

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

        // Toggle View
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("DERIVATIVES ANALYTICS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(2.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                listOf("OI", "GREEKS", "GEX", "PAIN", "IV").forEach { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent, RoundedCornerShape(3.dp))
                            .clickable { viewMode = mode }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(mode, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (viewMode == "GEX") {
            GexAnalyticsView(
                profile = uiState.gexProfile,
                spotPrice = uiState.spotPrice
            )
        } else if (viewMode == "PAIN") {
            MaxPainAnalyticsView(
                history = uiState.maxPainHistory,
                currentMaxPain = uiState.summary.maxPain,
                spotPrice = uiState.spotPrice
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
                if (currentStrike != null && chain != null) {
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
                        } else {
                            Modifier.fillMaxWidth()
                        }

                        Box(modifier = rowModifier) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewMode == "OI") {
                                    // CALL OI
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isITMCall && !isCurrentStrike) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                else Modifier
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((strike.callOI / maxOI).toFloat().coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .background(AppRed.copy(if (isCurrentStrike) 0.30f else 0.15f), RoundedCornerShape(2.dp))
                                        )
                                        Text(
                                            "${(strike.callOI / 1000).toInt()}k",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Bold,
                                            modifier = Modifier.padding(start = 2.dp),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    // CALL LTP
                                    val callLtpStr = if (strike.callLTP >= 10.0) "${strike.callLTP.toInt()}" else "%.1f".format(strike.callLTP)
                                    Text(
                                        callLtpStr,
                                        color = if (isCurrentStrike) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // STRIKE (CURRENT / ATM HIGHLIGHTED - SIMPLE OVERLAY)
                                    Column(
                                        modifier = Modifier.weight(1f),
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
                                            Text(
                                                "ATM",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                    // PUT LTP
                                    val putLtpStr = if (strike.putLTP >= 10.0) "${strike.putLTP.toInt()}" else "%.1f".format(strike.putLTP)
                                    Text(
                                        putLtpStr,
                                        color = if (isCurrentStrike) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(0.6f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // PUT OI
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .then(
                                                if (isITMPut && !isCurrentStrike) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((strike.putOI / maxOI).toFloat().coerceIn(0f, 1f))
                                                .height(14.dp)
                                                .background(AppGreen.copy(if (isCurrentStrike) 0.30f else 0.15f), RoundedCornerShape(2.dp))
                                        )
                                        Text(
                                            "${(strike.putOI / 1000).toInt()}k",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Bold,
                                            modifier = Modifier.padding(end = 2.dp),
                                            fontFamily = FontFamily.Monospace
                                        )
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
                                            ),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // CALL THETA
                                    Text(
                                        "%.1f".format(strike.callGreeks.theta),
                                        color = Color.Magenta,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // STRIKE (CURRENT / ATM HIGHLIGHTED - SIMPLE OVERLAY)
                                    Column(
                                        modifier = Modifier.weight(1f),
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
                                            Text(
                                                "ATM",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                    // PUT THETA
                                    Text(
                                        "%.1f".format(strike.putGreeks.theta),
                                        color = Color.Magenta,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrentStrike) FontWeight.ExtraBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f),
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
                                            ),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        if (!isCurrentStrike) {
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
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

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text("INSTITUTIONAL STRATEGY BUILDER", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // --- LIVE STRATEGY P&L & GREEKS BANNER ---
        if (uiState.activeLegs.isNotEmpty()) {
            val pnlColor = if (uiState.totalStrategyPnl >= 0.0) AppGreen else AppRed
            val pnlSign = if (uiState.totalStrategyPnl >= 0.0) "+" else ""
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, pnlColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AppGreen)
                            )
                            Text(
                                "LIVE STRATEGY VALUE",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Surface(
                            color = pnlColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.border(0.5.dp, pnlColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        ) {
                            Text(
                                "$pnlSign₹${"%.1f".format(uiState.totalStrategyPnl)} ($pnlSign${"%.2f".format(uiState.totalStrategyPnlPct)}%)",
                                color = pnlColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NET PREMIUM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            val isDebit = uiState.netPremium >= 0.0
                            Text(
                                "${if (isDebit) "Debit: ₹" else "Credit: ₹"}${"%.1f".format(abs(uiState.netPremium))}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("CURRENT VALUE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "₹${"%.1f".format(uiState.currentStrategyValue)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column {
                            Text("UNDERLYING", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "₹${"%.2f".format(uiState.spotPrice)}",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Greeks strip
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Δ: %.2f".format(uiState.strategyGreeks.delta), color = Color.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Γ: %.4f".format(uiState.strategyGreeks.gamma), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Θ: %.1f/d".format(uiState.strategyGreeks.theta), color = Color.Magenta, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("V: %.1f".format(uiState.strategyGreeks.vega), color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 0. Active Editing Indicator Banner (if editing an existing saved strategy)
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
                            text = "EDITING SAVED STRATEGY",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clickable { viewModel.deleteOrClearStrategy() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                Text("Delete", color = MaterialTheme.colorScheme.error, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .clickable { viewModel.cancelEditMode() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Cancel / New", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }


        
        // 2. Category & Strategy Architecture Selection Dropdowns
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Category Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(
                    onClick = { isCategoryDropdownExpanded = true },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.selectedCategory.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.selectedStrategyName.ifBlank { "SELECT ARCHITECTURE" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        Spacer(Modifier.height(16.dp))
        
        // 3. Strategy Legs Management (Add more legs, modify strike, remove leg)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("STRATEGY LEGS (${uiState.activeLegs.size})", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            if (uiState.activeLegs.isNotEmpty()) {
                Text("EDITABLE", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (uiState.activeLegs.isNotEmpty()) {
            uiState.activeLegs.forEach { leg ->
                StrategyLegRow(
                    leg = leg,
                    lotSize = uiState.summary.lotSize,
                    onShiftStrike = { delta -> viewModel.shiftLegStrike(leg.id, delta) },
                    onToggleType = { viewModel.toggleLegType(leg.id) },
                    onToggleInstrument = { viewModel.toggleLegInstrument(leg.id) },
                    onRemove = { viewModel.removeLeg(leg.id) }
                )
                Spacer(Modifier.height(4.dp))
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "No legs in strategy",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Select an architecture above or add custom legs below",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 9.5.sp
                    )
                }
            }
        }
        
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.addLeg("CALL", "BUY", uiState.spotPrice) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().height(34.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("ADD CUSTOM LEG", color = MaterialTheme.colorScheme.primary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
        }

        // 4. Action Buttons (ANALYZE, SAVE / UPDATE, DELETE)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { viewModel.analyzeStrategy() },
                modifier = Modifier.weight(1f).height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                enabled = uiState.activeLegs.isNotEmpty()
            ) {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ANALYZE", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.saveStrategy() },
                modifier = Modifier.weight(1.1f).height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 4.dp),
                enabled = uiState.activeLegs.isNotEmpty()
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (uiState.editingStrategyId != null) "UPDATE" else "SAVE STRATEGY",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                )
            }
            Button(
                onClick = { viewModel.deleteOrClearStrategy() },
                modifier = Modifier.weight(0.9f).height(42.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 4.dp),
                enabled = uiState.activeLegs.isNotEmpty() || uiState.editingStrategyId != null || uiState.selectedStrategy != null
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        // 5. Payoff Metrics & Visualization (Rendered ONLY after clicking ANALYZE)
        Spacer(Modifier.height(16.dp))
        if (uiState.isStrategyAnalyzed && uiState.selectedStrategy != null) {
            val strategy = uiState.selectedStrategy!!
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            strategy.name.uppercase(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AppGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.border(0.5.dp, AppGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            ) {
                                Text(
                                    "ANALYZED",
                                    color = AppGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteOrClearStrategy() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Strategy",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StrategyMetric("MAX PROFIT", if(strategy.maxProfit > 1000000) "UNLIMITED" else "₹${strategy.maxProfit.toInt()}", AppGreen)
                        StrategyMetric("MAX LOSS", if(strategy.maxLoss < -1000000) "UNLIMITED" else "₹${strategy.maxLoss.toInt()}", AppRed)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StrategyMetric("BREAKEVEN", if (strategy.breakeven.isEmpty()) "NONE" else strategy.breakeven.joinToString(", ") { it.toInt().toString() }, MaterialTheme.colorScheme.onSurface)
                        StrategyMetric("WIN PROB", "${strategy.probability}%", Color.Cyan)
                    }

                    // Greeks Strip
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Δ: %.2f".format(uiState.strategyGreeks.delta), color = Color.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Γ: %.4f".format(uiState.strategyGreeks.gamma), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Θ: %.1f/d".format(uiState.strategyGreeks.theta), color = Color.Magenta, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("V: %.1f".format(uiState.strategyGreeks.vega), color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        AdvancedPayoffChart(strategy.payoffPoints, uiState.spotPrice)
                    }
                }
            }
        } else if (uiState.activeLegs.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Strategy Ready for Analysis",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap 'ANALYZE' above to generate payoff chart, P&L profile, Greeks, and risk metrics.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.5.sp
                        )
                    }
                }
            }
        }
        
        // 6. Tracked Strategies Section (Saved Strategies with EDIT and DELETE)
        if (uiState.trackedStrategies.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(
                "MY TRACKED STRATEGIES (${uiState.trackedStrategies.size})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            uiState.trackedStrategies.forEach { saved ->
                val isCurrent = uiState.editingStrategyId == saved.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(
                            if (isCurrent) 1.dp else 0.5.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(saved.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (isCurrent) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            "ACTIVE",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = saved.legs.joinToString(" • "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.5.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("POP: ${saved.probability}%", color = Color.Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Max P/L: ₹${if (saved.maxProfit > 1000000) "∞" else saved.maxProfit.toInt().toString()} / ₹${if (saved.maxLoss < -1000000) "∞" else saved.maxLoss.toInt().toString()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                                if (saved.rawLegs.isNotEmpty()) {
                                    val savedPnl = saved.rawLegs.sumOf { it.pnl }
                                    val savedPnlColor = if (savedPnl >= 0.0) AppGreen else AppRed
                                    val savedPnlSign = if (savedPnl >= 0.0) "+" else ""
                                    Text(
                                        "Live: $savedPnlSign₹${"%.1f".format(savedPnl)}",
                                        color = savedPnlColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // EDIT BUTTON: Loads into builder to add more legs, re-analyze, re-save
                            IconButton(
                                onClick = { viewModel.editTrackedStrategy(saved) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Strategy",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            // DELETE BUTTON
                            IconButton(
                                onClick = { viewModel.deleteTrackedStrategy(saved.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Strategy",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StrategyLegRow(
    leg: StrategyLeg,
    lotSize: Int = 50,
    onShiftStrike: (Int) -> Unit,
    onToggleType: () -> Unit,
    onToggleInstrument: () -> Unit,
    onRemove: () -> Unit
) {
    val legPnlColor = if (leg.pnl >= 0.0) AppGreen else AppRed
    val legPnlSign = if (leg.pnl >= 0.0) "+" else ""
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Top Row: Type & Instrument chips + Strike Stepper + Remove
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

                    val baseLot = if (lotSize > 0) lotSize else 50
                    if (leg.qty > baseLot) {
                        Text(
                            "${leg.qty / baseLot}x",
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

            Spacer(Modifier.height(6.dp))

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
fun MaxPainAnalyticsView(history: List<Double>, currentMaxPain: Double, spotPrice: Double) {
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
                        Text("₹${currentMaxPain.toInt()}", color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
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
fun GexAnalyticsView(profile: com.example.marketintelligence.domain.engine.GexProfile?, spotPrice: Double) {
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
                        Text("₹${profile.gammaFlipStrike.toInt()}", color = Color.Cyan, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CALL / PUT WALL", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("₹${profile.callWallStrike.toInt()} / ₹${profile.putWallStrike.toInt()}", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
                    Text("Spot ● Flip ₹${profile.gammaFlipStrike.toInt()}", color = onSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("Call GEX (Pinning Magnet) →", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}
