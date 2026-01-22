package com.example.marketintelligence.ui.portfolio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.Holding
import com.example.marketintelligence.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel = hiltViewModel(),
    onHoldingClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isAddDialogVisible) {
        AddAssetDialog(
            symbol = uiState.searchQuery,
            onDismiss = { viewModel.dismissAddDialog() },
            onConfirm = { qty, price -> viewModel.confirmAddAsset(qty, price) }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Text("Portfolio Intelligence", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.weight(1f).height(50.dp),
                placeholder = { Text("Search Stock/Crypto...", color = Color.Gray, fontSize = 14.sp) },
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
                onClick = { viewModel.showAddDialog() },
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Total Portfolio Value", color = Color.Gray, fontSize = 12.sp)
                Text(
                    "₹${"%.2f".format(uiState.totalCurrentValue)}",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invested", color = Color.Gray, fontSize = 12.sp)
                        Text("₹${"%.2f".format(uiState.totalInvestedValue)}", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total P&L", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            "₹${"%.2f".format(uiState.totalPnl)}",
                            color = if (uiState.totalPnl >= 0) AppGreen else AppRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Holdings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (uiState.holdings.isNotEmpty()) {
                TextButton(onClick = { viewModel.liquidateAll() }) {
                    Text("LIQUIDATE ALL", color = AppRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.holdings) { item ->
                HoldingItemView(
                    item = item,
                    onClick = onHoldingClick,
                    onLiquidate = { viewModel.liquidateAsset(item.symbol) }
                )
            }
        }
    }
}

@Composable
fun HoldingItemView(item: Holding, onClick: (String) -> Unit, onLiquidate: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick(item.symbol) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.symbol, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${item.quantity} units @ ₹${item.avgPrice}", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₹${"%.2f".format(item.currentValue)}", color = Color.White, fontWeight = FontWeight.Bold)
                val pnlColor = if (item.totalPnl >= 0) AppGreen else AppRed
                val prefix = if (item.totalPnl >= 0) "+" else ""
                Text(
                    "$prefix₹${"%.2f".format(item.totalPnl)}",
                    color = pnlColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onLiquidate,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Liquidate Asset",
                    tint = AppRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AddAssetDialog(symbol: String, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Asset", color = Color.White) },
        text = {
            Column {
                Text("Adding $symbol to portfolio.", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AppGreen,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Avg. Buy Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AppGreen,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        },
        containerColor = Color(0xFF1E1E1E),
        confirmButton = {
            Button(
                onClick = { onConfirm(quantity, price) },
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
            ) {
                Text("Confirm", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
