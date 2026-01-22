package com.example.redxaiscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.redxaiscanner.presentation.ui.components.SignalCard
import com.example.redxaiscanner.ui.theme.REDXAIScannerTheme
import com.example.redxaiscanner.presentation.viewmodel.ScannerUiState
import com.example.redxaiscanner.presentation.viewmodel.ScannerViewModel
import com.example.redxaiscanner.util.Injector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            REDXAIScannerTheme {
                val viewModel = Injector.provideScannerViewModel(LocalContext.current)
                val appConfig = Injector.provideAppConfig()
                
                val uiState by viewModel.uiState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.startScan(appConfig.supportedSymbols)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScannerScreen(
                        uiState = uiState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ScannerScreen(uiState: ScannerUiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        // Fix: Use uiState.tradeSetups which is a Map<String, List<TradeSetup>>
        uiState.tradeSetups.forEach { (symbol, setups) ->
            items(setups) { setup ->
                SignalCard(setup = setup)
            }
        }
    }
}
