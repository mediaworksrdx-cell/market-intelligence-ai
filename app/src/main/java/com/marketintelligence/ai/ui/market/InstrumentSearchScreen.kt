package com.marketintelligence.ai.ui.market

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.domain.model.SearchResult
import com.marketintelligence.ai.domain.repository.SyncStatus
import com.marketintelligence.ai.ui.theme.AppGreen
import kotlinx.coroutines.launch

@Composable
fun InstrumentSearchScreen(
    viewModel: InstrumentSearchViewModel = hiltViewModel(),
    onInstrumentSelected: (String, String) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val addedSymbols by viewModel.addedSymbols.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState(initial = SyncStatus.Idle)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dynamic Sync Progress (Non-intrusive)
            Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                if (syncStatus is SyncStatus.Syncing) {
                    Column {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            "Synchronizing instruments from Kite...",
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search input field (Vertically centered, modern institutional design)
            BasicTextField(
                value = searchQuery,
                onValueChange = viewModel::onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(
                        0.8.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
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
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Search symbols (NIFTY, RELIANCE, BTC...)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    fontSize = 12.5.sp
                                )
                            }
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.onQueryChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            )

            // Category filter chips
            val categories = listOf("ALL", "INDICES", "STOCKS", "CRYPTO")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            0.5.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.clickable { viewModel.onCategorySelected(cat) }
                    ) {
                        Text(
                            text = when(cat) {
                                "ALL" -> "ALL"
                                "INDICES" -> "CORE INDICES"
                                "STOCKS" -> "WATCHLIST"
                                "CRYPTO" -> "CRYPTO INTEL"
                                else -> cat
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Results count / state header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "POPULAR INSTRUMENTS (TAP + TO ADD TO HUB)" else "SEARCH RESULTS (${searchResults.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            // Results List
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No instruments found matching \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults, key = { "${it.type}_${it.symbol}" }) { instrument ->
                        val isAdded = addedSymbols.contains(instrument.symbol.uppercase())
                        val targetSection = when (instrument.type.uppercase()) {
                            "INDEX" -> "Core Indices"
                            "CRYPTO" -> "Crypto Intelligence"
                            else -> "Strategic Watchlist"
                        }

                        InstrumentItem(
                            instrument = instrument,
                            isAdded = isAdded,
                            onAddClick = {
                                viewModel.addInstrument(instrument) {
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Added ${instrument.symbol} to $targetSection")
                                    }
                                }
                            },
                            onItemClick = {
                                // Add if not added, and launch analysis
                                if (!isAdded) {
                                    viewModel.addInstrument(instrument)
                                }
                                onInstrumentSelected(instrument.symbol, instrument.type)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InstrumentItem(
    instrument: SearchResult,
    isAdded: Boolean,
    onAddClick: () -> Unit,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = instrument.symbol,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Destination Section Badge
                    val (badgeText, badgeBg, badgeTextColor) = when (instrument.type.uppercase()) {
                        "INDEX" -> Triple("CORE INDICES", Color(0xFF673AB7).copy(alpha = 0.2f), Color(0xFFB39DDB))
                        "STOCK" -> Triple("WATCHLIST", Color(0xFF4CAF50).copy(alpha = 0.2f), Color(0xFF81C784))
                        "CRYPTO" -> Triple("CRYPTO INTEL", Color(0xFFFF9800).copy(alpha = 0.2f), Color(0xFFFFB74D))
                        else -> Triple(instrument.type.uppercase(), Color(0xFF757575).copy(alpha = 0.2f), Color.LightGray)
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeTextColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = instrument.name,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: ADD or ADDED button
            if (isAdded) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AppGreen.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, AppGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Added",
                            tint = AppGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "ADDED",
                            color = AppGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "ADD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}