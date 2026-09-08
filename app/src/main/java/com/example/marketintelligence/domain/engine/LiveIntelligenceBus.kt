package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.model.AiTradeSignal
import com.example.marketintelligence.domain.model.AIAnalysisResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A singleton service that acts as a central event bus for sharing
 * live intelligence between different parts of the application.
 */
@Singleton
class LiveIntelligenceBus @Inject constructor() {

    // --- F&O Signal Channel ---
    private val _fnoSignalFlow = MutableSharedFlow<AiTradeSignal>(replay = 1)
    val fnoSignalFlow = _fnoSignalFlow.asSharedFlow()

    suspend fun postFnoSignal(signal: AiTradeSignal) {
        _fnoSignalFlow.emit(signal)
    }

    // --- Scanner Signal Channel ---
    private val _scannerSignalFlow = MutableSharedFlow<AIAnalysisResult>(replay = 1)
    val scannerSignalFlow = _scannerSignalFlow.asSharedFlow()

    suspend fun postScannerSignal(signal: AIAnalysisResult) {
        _scannerSignalFlow.emit(signal)
    }
}
