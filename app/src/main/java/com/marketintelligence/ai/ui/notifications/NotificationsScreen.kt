package com.marketintelligence.ai.ui.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNavigateToAnalysis: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedNotification by remember { mutableStateOf<NotificationItem?>(null) }

    val filteredNotifications = remember(uiState.notifications, selectedCategory) {
        when (selectedCategory) {
            "MARKET NEWS" -> uiState.notifications.filter { it.type == NotificationType.NEWS }
            "ECONOMIC CALENDAR" -> uiState.notifications.filter { it.type == NotificationType.EVENT }
            "IPO WATCH" -> uiState.notifications.filter { it.type == NotificationType.IPO }
            "AI SIGNALS" -> uiState.notifications.filter { it.type == NotificationType.AI_SIGNAL }
            else -> uiState.notifications
        }
    }

    val newsCount = remember(uiState.notifications) { uiState.notifications.count { it.type == NotificationType.NEWS } }
    val eventCount = remember(uiState.notifications) { uiState.notifications.count { it.type == NotificationType.EVENT } }
    val ipoCount = remember(uiState.notifications) { uiState.notifications.count { it.type == NotificationType.IPO } }
    val aiCount = remember(uiState.notifications) { uiState.notifications.count { it.type == NotificationType.AI_SIGNAL } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "COMMAND CENTER",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    "Institutional Alerts & Market Surveillance",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (uiState.notifications.any { !it.read }) {
                    TextButton(
                        onClick = { viewModel.markAllAsRead() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("MARK ALL READ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
                if (uiState.notifications.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearAll() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("CLEAR", color = AppRed, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val categories = listOf(
                "ALL" to uiState.notifications.size,
                "MARKET NEWS" to newsCount,
                "ECONOMIC CALENDAR" to eventCount,
                "IPO WATCH" to ipoCount,
                "AI SIGNALS" to aiCount
            )
            items(categories) { (cat, count) ->
                val isSelected = selectedCategory == cat
                Surface(
                    onClick = { selectedCategory = cat },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cat,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "$count",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredNotifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No alerts in $selectedCategory",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(
                    items = filteredNotifications,
                    key = { it.id }
                ) { item ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        LaunchedEffect(item.id) {
                            viewModel.dismissNotification(item.id)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart -> AppRed.copy(alpha = 0.8f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        enableDismissFromStartToEnd = false
                    ) {
                        NotificationCard(
                            item = item,
                            onClick = {
                                viewModel.markAsRead(item.id)
                                selectedNotification = item
                            },
                            onNavigateToAnalysis = onNavigateToAnalysis
                        )
                    }
                }
            }
        }
    }

    if (selectedNotification != null) {
        NotificationDetailSheet(
            item = selectedNotification!!,
            onDismiss = { selectedNotification = null },
            onNavigateToAnalysis = { symbol ->
                selectedNotification = null
                onNavigateToAnalysis(symbol)
            }
        )
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit
) {
    val (icon, tint, label) = when (item.type) {
        NotificationType.AI_SIGNAL -> Triple(Icons.Default.AutoGraph, Color(0xFFFFD700), "AI STUDY")
        NotificationType.NEWS -> Triple(Icons.Default.Newspaper, Color(0xFF00E5FF), "MARKET NEWS")
        NotificationType.EVENT -> Triple(Icons.Default.Event, Color(0xFFFF4081), "ECONOMIC CALENDAR")
        NotificationType.IPO -> Triple(Icons.Default.RocketLaunch, AppGreen, "IPO WATCH")
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (!item.read) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp,
                if (!item.read) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category Badge + Time + Unread Dot
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = tint.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

                if (!item.read) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }

                Spacer(Modifier.weight(1f))
                Text(item.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            // Headline
            Text(
                item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            // Snippet Preview
            val previewSnippet = when {
                item.summary.isNotBlank() -> item.summary
                item.content.isNotBlank() -> item.content.take(110) + "..."
                else -> ""
            }
            if (previewSnippet.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    previewSnippet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // Category Specific Metric Chips
            when (item.type) {
                NotificationType.NEWS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (item.category.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        item.category,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            item.sentiment?.let { sent ->
                                val (sentColor, sentText) = when (sent) {
                                    Sentiment.POSITIVE -> AppGreen to "POSITIVE"
                                    Sentiment.NEGATIVE -> AppRed to "NEGATIVE"
                                    Sentiment.NEUTRAL -> Color.Yellow to "NEUTRAL"
                                }
                                Surface(
                                    color = sentColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        sentText,
                                        color = sentColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("READ FULL STORY", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                NotificationType.EVENT -> {
                    item.eventData?.let { event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(event.currency, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                                Surface(
                                    color = (if (event.impact == "HIGH") AppRed else Color.Yellow).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("${event.impact} IMPACT", color = if (event.impact == "HIGH") AppRed else Color.Yellow, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                                Text("Act: ${event.actual}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("EVENT DETAILS", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                NotificationType.IPO -> {
                    item.ipoData?.let { ipo ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AppGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("GMP: +${ipo.gmpPercent}%", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(ipo.status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("IPO PROSPECTUS", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
                NotificationType.AI_SIGNAL -> {
                    item.aiResult?.let { result ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ZONE: ${result.entryZone ?: "--"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("VIEW STUDY", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(2.dp))
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailSheet(
    item: NotificationItem,
    onDismiss: () -> Unit,
    onNavigateToAnalysis: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val (icon, tint, label) = when (item.type) {
        NotificationType.AI_SIGNAL -> Triple(Icons.Default.AutoGraph, Color(0xFFFFD700), "AI STUDY / SIGNAL")
        NotificationType.NEWS -> Triple(Icons.Default.Newspaper, Color(0xFF00E5FF), "MARKET NEWS")
        NotificationType.EVENT -> Triple(Icons.Default.Event, Color(0xFFFF4081), "ECONOMIC CALENDAR")
        NotificationType.IPO -> Triple(Icons.Default.RocketLaunch, AppGreen, "IPO WATCH")
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
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Category Pill & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = tint.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, tint.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Headline
            Text(
                item.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 22.sp
            )

            if (item.source.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Source: ${item.source}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(14.dp))

            // Body by Type
            when (item.type) {
                NotificationType.NEWS -> {
                    MarketNewsDetailBody(item = item, onNavigateToAnalysis = onNavigateToAnalysis)
                }
                NotificationType.EVENT -> {
                    EconomicCalendarDetailBody(item = item, onNavigateToAnalysis = onNavigateToAnalysis)
                }
                NotificationType.IPO -> {
                    IpoWatchDetailBody(item = item, onNavigateToAnalysis = onNavigateToAnalysis)
                }
                NotificationType.AI_SIGNAL -> {
                    AiSignalDetailBody(item = item, onNavigateToAnalysis = onNavigateToAnalysis)
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun MarketNewsDetailBody(item: NotificationItem, onNavigateToAnalysis: (String) -> Unit) {
    Column {
        // Sentiment & Category Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CATEGORY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(if (item.category.isNotBlank()) item.category else "Financial News", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            item.sentiment?.let { sent ->
                val (color, text) = when (sent) {
                    Sentiment.POSITIVE -> AppGreen to "BULLISH SENTIMENT"
                    Sentiment.NEGATIVE -> AppRed to "BEARISH SENTIMENT"
                    Sentiment.NEUTRAL -> Color.Yellow to "NEUTRAL"
                }
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Full Readable Content
        Text("FULL STORY & MARKET INTELLIGENCE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (item.content.isNotBlank()) item.content else item.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Normal
        )

        item.actionSymbol?.let { symbol ->
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onNavigateToAnalysis(symbol) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("OPEN REAL-TIME CHART ($symbol)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun EconomicCalendarDetailBody(item: NotificationItem, onNavigateToAnalysis: (String) -> Unit) {
    val event = item.eventData
    Column {
        if (event != null) {
            // Impact & Currency Header Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENCY & EVENT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("${event.currency} • ${event.event}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
                Surface(
                    color = (if (event.impact == "HIGH") AppRed else Color.Yellow).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("${event.impact} IMPACT", color = if (event.impact == "HIGH") AppRed else Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // 3-Column Actual vs Forecast vs Previous Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ACTUAL", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(3.dp))
                    Text(event.actual, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.height(28.dp).width(0.5.dp).background(MaterialTheme.colorScheme.outline))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FORECAST", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(event.forecast, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Box(modifier = Modifier.height(28.dp).width(0.5.dp).background(MaterialTheme.colorScheme.outline))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PREVIOUS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(event.previous, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Economic Narrative
            if (event.description.isNotBlank()) {
                Text("MACROECONOMIC ANALYSIS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(event.description, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))
            }

            if (event.affectedMarkets.isNotBlank()) {
                Text("AFFECTED MARKETS & INSTRUMENTS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                Text(event.affectedMarkets, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            Text(item.content.ifBlank { item.title }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
        }

        item.actionSymbol?.let { symbol ->
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onNavigateToAnalysis(symbol) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ANALYZE MARKET REACTION ($symbol)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun IpoWatchDetailBody(item: NotificationItem, onNavigateToAnalysis: (String) -> Unit) {
    val ipo = item.ipoData
    Column {
        if (ipo != null) {
            // GMP Hero Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppGreen.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, AppGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GREY MARKET PREMIUM (GMP)", color = AppGreen, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹${ipo.gmp}", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(6.dp))
                        Text("(+${ipo.gmpPercent}%)", color = AppGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        ipo.status,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Issue Specifications Grid
            Text("ISSUE SPECIFICATIONS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Price Band", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(ipo.priceBand, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (ipo.lotSize.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lot Size", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(ipo.lotSize, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    if (ipo.issueSize.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Issue Size", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(ipo.issueSize, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Issue Period", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text("${ipo.openDate} – ${ipo.closeDate}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    if (ipo.listingDate.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Expected Listing", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(ipo.listingDate, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (ipo.subscriptionStatus.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("SUBSCRIPTION STATUS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        ipo.subscriptionStatus,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (ipo.description.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text("COMPANY PROFILE & VERDICT", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(ipo.description, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
            }
        } else {
            Text(item.content.ifBlank { item.title }, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)
        }

        item.actionSymbol?.let { symbol ->
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onNavigateToAnalysis(symbol) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("ANALYZE ASSET PROFILE ($symbol)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AiSignalDetailBody(item: NotificationItem, onNavigateToAnalysis: (String) -> Unit) {
    val result = item.aiResult
    Column {
        if (result != null) {
            // Signal Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFD700).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0xFFFFD700).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(result.symbol, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text(result.pattern ?: "Institutional Order Block", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Surface(
                    color = (if (result.signal == SignalType.BULLISH) AppGreen else AppRed).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        result.signal.name,
                        color = if (result.signal == SignalType.BULLISH) AppGreen else AppRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Key Quant Levels
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Entry Zone", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(result.entryZone ?: "--", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stop Loss", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(result.stopLoss?.toString() ?: "--", color = AppRed, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Levels", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(result.target?.joinToString(", ") ?: "--", color = AppGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Risk / Reward", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(result.rrRatio, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("QUANT ENGINE RATIONALE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            Text(result.rationale, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 18.sp)

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onNavigateToAnalysis(result.symbol) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("OPEN CANDLESTICK CHART & AI SCANNER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
