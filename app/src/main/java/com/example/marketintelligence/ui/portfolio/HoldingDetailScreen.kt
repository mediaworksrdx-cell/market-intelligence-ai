package com.example.marketintelligence.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.data.local.MockData

@Composable
fun HoldingDetailScreen() {
    // Mocking the first holding for detail view
    val holding = MockData.PORTFOLIO_INITIAL.first()

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Text(holding.symbol, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(holding.name, color = Color.Gray, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Performance", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Qty", color = Color.Gray, fontSize = 12.sp)
                        Text("${holding.qty}", color = Color.White)
                    }
                    Column {
                        Text("Avg Price", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${holding.avgPrice}", color = Color.White)
                    }
                     Column {
                        Text("LTP", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${holding.currentPrice}", color = Color.White)
                    }
                }
            }
        }
        
         Spacer(modifier = Modifier.height(16.dp))
         Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth()) {
             Column(modifier = Modifier.padding(16.dp)) {
                 Text("AI Insights", color = Color.White, fontWeight = FontWeight.Bold)
                 Spacer(modifier = Modifier.height(8.dp))
                 Text("AI Score: ${holding.aiScore}/100", color = AppGreen)
                 Text("Sentiment: ${holding.aiSentiment}", color = Color.LightGray)
             }
         }
    }
}

val AppGreen = Color(0xFF00E676)
