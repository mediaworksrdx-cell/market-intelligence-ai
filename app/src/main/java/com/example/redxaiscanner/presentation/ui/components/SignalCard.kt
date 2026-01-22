package com.example.redxaiscanner.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.redxaiscanner.engine.MarketBias
import com.example.redxaiscanner.engine.TradeSetup

@Composable
fun SignalCard(setup: TradeSetup, modifier: Modifier = Modifier) {
    val biasColor = if (setup.underlyingSignal.underlyingSignal.higherTimeframeBias == MarketBias.BULLISH) Color(0xFF00C853) else Color(0xFFD50000)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = setup.underlyingSignal.underlyingSignal.symbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = setup.underlyingSignal.underlyingSignal.higherTimeframeBias.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(biasColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Study Confidence: ${setup.underlyingSignal.confidenceScore}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(Modifier.height(16.dp))
            
            StudyLevelRow("Study Price:", setup.entryPrice.toPlainString())
            StudyLevelRow("Invalidation:", setup.stopLossPrice.toPlainString(), color = Color(0xFFD50000))
            StudyLevelRow("Projection 1:", setup.takeProfit1.toPlainString(), color = Color(0xFF00C853))
            StudyLevelRow("Projection 2:", setup.takeProfit2.toPlainString(), color = Color(0xFF00C853))
        }
    }
}

@Composable
private fun StudyLevelRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(text = value, color = color, fontWeight = FontWeight.Medium)
    }
}
