package com.example.marketintelligence.ui.academy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.TrainingModule
import com.example.marketintelligence.ui.theme.AppGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(viewModel: AcademyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Knowledge Framework", "Ask Mentor", "Live Training")

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("REDX AI MENTOR", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            Text("Institutional Learning & Simulation", color = AppGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AppGreen,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AppGreen
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when(selectedTab) {
                0 -> CurriculumTab(uiState.recommendedModules)
                1 -> AskMentorTab(uiState, viewModel)
                2 -> LiveTrainingTab(uiState)
            }
        }
    }
}

@Composable
fun CurriculumTab(modules: List<String>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Text("UNIFIED KNOWLEDGE FRAMEWORK", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
        }
        items(modules) { moduleTitle ->
            TrainingModuleCard(TrainingModule("id", moduleTitle, "2h", false, listOf("Theory", "Simulation")))
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text("CORE DISCIPLINE MODULES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        items(listOf("Scenario Stress Testing", "Outcome Attribution Engine", "Governance & Audit Layer")) { title ->
            TrainingModuleCard(TrainingModule("id", title, "1h 30m", true, listOf("Institutional")))
        }
    }
}

@Composable
fun AskMentorTab(uiState: AcademyUiState, viewModel: AcademyViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.chatHistory) { chat ->
                ChatBubble(chat)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = uiState.currentInput,
                onValueChange = { viewModel.onInputChanged(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about market theory...", color = Color.Gray, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF121212),
                    unfocusedContainerColor = Color(0xFF121212),
                    focusedIndicatorColor = AppGreen,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessage() },
                modifier = Modifier.background(AppGreen, CircleShape)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                else Icon(Icons.Default.Send, null, tint = Color.Black)
            }
        }
    }
}

@Composable
fun LiveTrainingTab(uiState: AcademyUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 16.dp)) {
        Text("LIVE SIGNAL ANALYSIS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(16.dp))
        
        if (uiState.lastFnoSignal != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = AppGreen.copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(uiState.lastFnoSignal!!.underlyingSymbol, color = AppGreen, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(uiState.lastFnoSignal!!.strategyName, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("MENTOR RATIONALE", color = AppGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.mentorExplanation ?: "Analyzing market structure...", color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF1A1A1A))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("LEARNING OBJECTIVE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Study how the ${uiState.lastFnoSignal!!.marketRegime} regime influenced this setup.", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.School, null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No live signals detected for study.", color = Color.DarkGray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(chat: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (chat.isMentor) Alignment.Start else Alignment.End
    ) {
        Surface(
            color = if (chat.isMentor) Color(0xFF1A1A1A) else AppGreen,
            shape = RoundedCornerShape(
                topStart = 12.dp, topEnd = 12.dp,
                bottomStart = if (chat.isMentor) 0.dp else 12.dp,
                bottomEnd = if (chat.isMentor) 12.dp else 0.dp
            )
        ) {
            Text(
                text = chat.message,
                color = if (chat.isMentor) Color.White else Color.Black,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TrainingModuleCard(module: TrainingModule) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(if(module.locked) Color.DarkGray else AppGreen.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (module.locked) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (module.locked) Color.Gray else AppGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(module.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${module.duration} • ${module.topics.joinToString(", ")}", color = Color.Gray, fontSize = 11.sp)
            }
            if (!module.locked) {
                Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray)
            }
        }
    }
}
