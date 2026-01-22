package com.example.redxaiscanner.presentation.viewmodel

import com.example.redxaiscanner.engine.TradeSetup

/**
 * Represents the complete state of the scanner UI at any given moment.
 * The UI will observe this state and react to its changes.
 *
 * @param isLoading True if a scan is currently in progress.
 * @param tradeSetups A map where the key is the symbol and the value is a list of
 *                    fully processed, actionable trade setups for that symbol.
 * @param error An optional error message to display to the user.
 */
data class ScannerUiState(
    val isLoading: Boolean = false,
    val tradeSetups: Map<String, List<TradeSetup>> = emptyMap(),
    val error: String? = null
)