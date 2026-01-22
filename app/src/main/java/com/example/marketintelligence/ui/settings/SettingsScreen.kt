package com.example.marketintelligence.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
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
import com.example.marketintelligence.domain.model.MarketType
import com.example.marketintelligence.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("System Configuration", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Institutional Control Panel", color = Color.Gray, fontSize = 12.sp)
        
        Spacer(modifier = Modifier.height(24.dp))

        // 1. General Preferences
        SettingsSection(title = "GENERAL PREFERENCES") {
            SettingsToggleItem(
                title = "Institutional Dark Mode",
                icon = Icons.Filled.DarkMode,
                checked = uiState.isDarkMode,
                onCheckedChange = { viewModel.setDarkMode(it) }
            )
            SettingsToggleItem(
                title = "Global Notifications",
                icon = Icons.Filled.Notifications,
                checked = uiState.isNotificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Intelligence Engines
        SettingsSection(title = "INTELLIGENCE ENGINES") {
            SettingsSelectItem(
                title = "Market Scanner Engine",
                icon = Icons.Filled.Analytics,
                selected = uiState.selectedScannerEngine,
                options = listOf("Standard (Gemini)", "Proprietary Engine"),
                onSelected = { viewModel.setScannerEngine(it) }
            )
            SettingsSelectItem(
                title = "AI Mentor Intelligence",
                icon = Icons.Filled.School,
                selected = uiState.selectedMentorEngine,
                options = listOf("Standard (Gemini)", "Proprietary Engine"),
                onSelected = { viewModel.setMentorEngine(it) }
            )
            SettingsSelectItem(
                title = "Charting Visualization",
                icon = Icons.AutoMirrored.Filled.ShowChart,
                selected = uiState.selectedChartEngine,
                options = listOf("Standard (TradingView)", "Proprietary Engine"),
                onSelected = { viewModel.setChartEngine(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Risk Framework
        SettingsSection(title = "RISK FRAMEWORK") {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Study Risk Profile", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CONSERVATIVE", "AGGRESSIVE").forEach { profile ->
                        val isSelected = uiState.riskProfile == profile
                        Surface(
                            color = if (isSelected) AppGreen else Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { viewModel.setRiskProfile(profile) }
                        ) {
                            Text(
                                text = profile,
                                color = if (isSelected) Color.Black else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Data Management
        Text("DATA MANAGEMENT", color = AppRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { /* Reset Logic */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reset Institutional Data", color = AppRed, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, color = Color.White, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AppGreen,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1A1A1A)
            )
        )
    }
}

@Composable
fun SettingsSelectItem(title: String, icon: ImageVector, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 14.sp)
                    Text(selected, color = AppGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(Icons.Filled.ArrowDropDown, null, tint = Color.Gray)
        }
        
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            options.forEach { option ->
                Text(
                    text = option,
                    color = if(option == selected) Color.White else Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            onSelected(option)
                            expanded = false
                        }
                        .padding(vertical = 8.dp, horizontal = 32.dp),
                    fontSize = 13.sp
                )
            }
        }
    }
}
