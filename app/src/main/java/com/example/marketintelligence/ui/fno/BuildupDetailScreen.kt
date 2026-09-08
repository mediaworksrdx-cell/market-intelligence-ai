package com.example.marketintelligence.ui.fno

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed

@Composable
fun BuildupDetailScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            "INSTITUTIONAL BUILDUP INTELLIGENCE",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            "Advanced derivatives positioning analysis",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("SMART MONEY FLOW (PARTICIPANT OI)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Surface(
                                color = AppGreen.copy(0.15f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.border(0.5.dp, AppGreen, RoundedCornerShape(4.dp))
                            ) {
                                Text("FII + PRO BIAS: BULLISH", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text("Exchange participant-wise derivatives positioning matrix", color = Color.Gray, fontSize = 9.sp)
                        
                        Spacer(Modifier.height(12.dp))
                        
                        listOf(
                            Triple("FII (Foreign Inst)", "+42.5K Fut | +90K Net Call", AppGreen),
                            Triple("PRO (Prop Desks)", "+12.2K Fut | Gamma Pin Bias", Color.Cyan),
                            Triple("DII (Domestic Inst)", "-15.4K Fut | Hedged Longs", Color(0xFFFFA726)),
                            Triple("CLIENT (Retail)", "-39.3K Fut | Heavy Net Short", AppRed)
                        ).forEach { (participant, positioning, color) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(participant, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(positioning, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            }
                            Divider(color = Color(0xFF1E1E1E), thickness = 0.5.dp)
                        }
                    }
                }
            }
            item {
                BuildupCategoryCard(
                    title = "LONG BUILDUP",
                    subtitle = "Aggressive Position Building",
                    description = "Institutional participants are creating fresh long positions. High conviction with rising Open Interest and Price.",
                    color = AppGreen,
                    icon = Icons.Default.TrendingUp,
                    metrics = mapOf("OI Change" to "+12.5%", "Avg Volume" to "High", "Sentiment" to "Strong Bullish")
                )
            }
            item {
                BuildupCategoryCard(
                    title = "SHORT BUILDUP",
                    subtitle = "Bearish Exposure Expansion",
                    description = "Fresh short contracts are being written. Institutional bias is shifting towards lower pricing with heavy OI accumulation.",
                    color = AppRed,
                    icon = Icons.Default.TrendingDown,
                    metrics = mapOf("OI Change" to "+8.2%", "Risk Level" to "High", "Sentiment" to "Strong Bearish")
                )
            }
            item {
                BuildupCategoryCard(
                    title = "SHORT COVERING",
                    subtitle = "Defensive De-risking",
                    description = "Short sellers are exiting positions as price rises. Indicates potential short-term momentum shift or technical recovery.",
                    color = Color.Cyan,
                    icon = Icons.Default.Shield,
                    metrics = mapOf("OI Drop" to "-5.4%", "Price Impact" to "Positive", "Flow" to "Institutional Exit")
                )
            }
            item {
                BuildupCategoryCard(
                    title = "LONG UNWINDING",
                    subtitle = "Profit Extraction / Capitulation",
                    description = "Existing long positions are being closed. Open interest is dropping alongside price, signaling exhaustion of the bull move.",
                    color = Color.Yellow,
                    icon = Icons.Default.Bolt,
                    metrics = mapOf("OI Drop" to "-6.1%", "Phase" to "Distribution", "Confidence" to "Weakening")
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BuildupCategoryCard(
    title: String,
    subtitle: String,
    description: String,
    color: Color,
    icon: ImageVector,
    metrics: Map<String, String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Text(description, color = Color.LightGray, fontSize = 11.sp, lineHeight = 16.sp)
            
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                metrics.forEach { (label, value) ->
                    Column {
                        Text(label.uppercase(), color = Color.Gray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
