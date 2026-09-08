package com.marketintelligence.cryptotracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.cryptotracker.engine.SetupDirection
import com.marketintelligence.cryptotracker.engine.SetupGrade
import com.marketintelligence.cryptotracker.engine.StructureState
import com.marketintelligence.cryptotracker.scanner.ScanResult

val BgDark = Color(0xFF0D1117)
val SurfaceDark = Color(0xFF161B22)
val TextPrimary = Color.White
val TextSecondary = Color(0xFF8B949E)
val BullishColor = Color(0xFF26A69A)
val BearishColor = Color(0xFFEF5350)
val GradeAPlus = Color(0xFFFFD700)
val GradeA = Color(0xFF26A69A)
val GradeB = Color(0xFF42A5F5)
val GradeWatch = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoScannerScreen(
    viewModel: CryptoScannerViewModel = hiltViewModel(),
    onCoinClick: (String) -> Unit = {}
) {
    val state by viewModel.scannerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CRYPTO SMC SCANNER", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("5m", "15m", "1H", "4H").forEach { tf ->
                    FilterChip(
                        selected = state.selectedTimeframe.equals(tf, ignoreCase = true),
                        onClick = { viewModel.setTimeframe(tf) },
                        label = { Text(tf) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary,
                            selectedContainerColor = BullishColor.copy(alpha = 0.2f),
                            selectedLabelColor = BullishColor
                        ),
                        border = null
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Divider(modifier = Modifier.width(1.dp).height(32.dp), color = TextSecondary.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(8.dp))

                SignalFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.name.replace("_", " ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary,
                            selectedContainerColor = SurfaceDark,
                            selectedLabelColor = TextPrimary
                        ),
                        border = null
                    )
                }
            }

            // Overview Cards
            state.overview?.let { overview ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OverviewCard("Active FVGs", overview.activeFVGCount.toString(), GradeB)
                    OverviewCard("Recent Sweeps", overview.recentSweepCount.toString(), GradeA)
                    OverviewCard("Structure Events", overview.structureEventCount.toString(), GradeAPlus)
                }
            }

            // Signal List
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading && state.overview == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BullishColor
                    )
                } else {
                    val filteredResults = state.overview?.scanResults?.filter {
                        when (state.selectedFilter) {
                            SignalFilter.ALL -> true
                            SignalFilter.LONG_ONLY -> it.signal == SetupDirection.LONG
                            SignalFilter.SHORT_ONLY -> it.signal == SetupDirection.SHORT
                            SignalFilter.A_PLUS_ONLY -> it.grade == SetupGrade.A_PLUS
                        }
                    } ?: emptyList()

                    if (filteredResults.isEmpty()) {
                        Text(
                            "No signals found for the current filters.",
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredResults) { result ->
                                SignalCard(result, onClick = { onCoinClick(result.symbol) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewCard(title: String, value: String, accentColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.width(120.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun SignalCard(result: ScanResult, onClick: () -> Unit) {
    val setup = result.setup
    val direction = result.signal
    val grade = result.grade
    
    val dirColor = when (direction) {
        SetupDirection.LONG -> BullishColor
        SetupDirection.SHORT -> BearishColor
        null -> GradeWatch
    }
    
    val gradeColor = when (grade) {
        SetupGrade.A_PLUS -> GradeAPlus
        SetupGrade.A -> GradeA
        SetupGrade.B -> GradeB
        SetupGrade.WATCH -> GradeWatch
        SetupGrade.IGNORE -> TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(result.symbol, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(text = direction?.name ?: "WATCH", color = dirColor)
                    Badge(text = "Score: ${result.score}", color = TextPrimary, bgColor = BgDark)
                    Badge(text = grade.name.replace("_", "+"), color = BgDark, bgColor = gradeColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val htfBiasColor = when (result.htfBias) {
                StructureState.BULLISH -> BullishColor
                StructureState.BEARISH -> BearishColor
                StructureState.RANGING -> GradeWatch
                StructureState.TRANSITIONING -> GradeAPlus
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SmallChip("HTF: ${result.htfBias}", htfBiasColor)
                SmallChip("RSI: ${String.format("%.1f", result.rsiValue)}", TextSecondary)
                setup?.let {
                    SmallChip("R:R: 1:${String.format("%.1f", it.riskRewardRatio)}", TextSecondary)
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color, bgColor: Color = color.copy(alpha = 0.15f)) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(BgDark, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp)
    }
}
