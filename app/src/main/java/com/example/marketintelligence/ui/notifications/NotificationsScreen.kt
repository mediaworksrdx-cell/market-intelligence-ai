package com.example.marketintelligence.ui.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.*
import com.example.marketintelligence.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNavigateToAnalysis: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Command Center", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text("Institutional Alerts & Signals", color = Color.Gray, fontSize = 12.sp)
            }
            if (uiState.notifications.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("CLEAR ALL", color = AppRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active alerts", color = Color.DarkGray, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    items = uiState.notifications,
                    key = { it.id }
                ) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.dismissNotification(item.id)
                                true
                            } else false
                        }
                    )

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
                                    .background(color, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                            }
                        },
                        content = {
                            NotificationCard(item, onNavigateToAnalysis)
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, onNavigateToAnalysis: (String) -> Unit) {
    val (icon, tint, label) = when (item.type) {
        NotificationType.AI_SIGNAL -> Triple(Icons.Default.AutoGraph, Color(0xFFFFD700), "AI STUDY")
        NotificationType.NEWS -> Triple(Icons.Default.Newspaper, Color.Cyan, "MARKET NEWS")
        NotificationType.EVENT -> Triple(Icons.Default.Event, Color.Magenta, "ECONOMIC CALENDAR")
        NotificationType.IPO -> Triple(Icons.Default.RocketLaunch, AppGreen, "IPO WATCH")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = tint.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text(item.time, color = Color.DarkGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

            // Dynamic Content based on type
            when (item.type) {
                NotificationType.AI_SIGNAL -> {
                    item.aiResult?.let { result ->
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("STUDY PRICE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(result.entryZone ?: "-", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Button(
                                onClick = { onNavigateToAnalysis(result.symbol) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("VIEW STUDY", color = AppGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                NotificationType.EVENT -> {
                    item.eventData?.let { event ->
                        Spacer(Modifier.height(8.dp))
                        Surface(color = Color(0xFF1A1A1A), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(event.currency, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Divider(modifier = Modifier.height(10.dp).width(1.dp), color = Color.DarkGray)
                                Spacer(Modifier.width(8.dp))
                                Text("Impact: ${event.impact}", color = if(event.impact == "HIGH") AppRed else Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                NotificationType.IPO -> {
                    item.ipoData?.let { ipo ->
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("GMP", color = Color.Gray, fontSize = 9.sp)
                                Text("${ipo.gmpPercent}%", color = AppGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("STATUS", color = Color.Gray, fontSize = 9.sp)
                                Text(ipo.status, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
