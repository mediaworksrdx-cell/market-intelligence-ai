package com.example.redxaiscanner.domain.usecase

import com.example.redxaiscanner.config.AppConfig
import com.example.redxaiscanner.domain.model.Timeframe
import com.example.redxaiscanner.domain.repository.MarketDataRepository
import com.example.redxaiscanner.engine.*
import com.example.redxaiscanner.telemetry.PerformanceProfiler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

// This new use case follows the established pattern for engine integration.
class ScanForVolumeSpikesUseCase(
    private val marketDataRepository: MarketDataRepository,
    private val volumeSpikeEngine: VolumeSpikeEngine
) {
    fun scan(symbols: List<String>, timeframe: Timeframe): Flow<Map<String, List<VolumeSpikeResult>>> {
        return marketDataRepository.getCandles(symbols, timeframe)
            .map { it.mapValues { (_, candles) -> volumeSpikeEngine.analyze(candles) } }
    }
}

class FindConfluenceUseCase(
    private val smcUseCase: ScanForSMCUseCase,
    private val patternUseCase: ScanForPatternsUseCase,
    private val fvgUseCase: ScanForFVGUseCase,
    private val volumeSpikeUseCase: ScanForVolumeSpikesUseCase, // New engine integrated
    private val appConfig: AppConfig, // For feature flagging
    private val profiler: PerformanceProfiler // For performance monitoring
) {

    private val entryTimeframe = Timeframe.ONE_HOUR
    private val structureTimeframe = Timeframe.FOUR_HOURS

    fun find(symbols: List<String>): Flow<Map<String, List<ConfluenceSignal>>> {
        val smc1H = smcUseCase.scan(symbols, entryTimeframe)
        val patterns1H = patternUseCase.scan(symbols, entryTimeframe)
        val fvgs1H = fvgUseCase.scan(symbols, entryTimeframe)
        val smc4H = smcUseCase.scan(symbols, structureTimeframe)
        
        // --- Feature-Flagged Engine Integration ---
        val volumeSpikes1H = if (appConfig.isVolumeSpikeEngineEnabled) {
            volumeSpikeUseCase.scan(symbols, entryTimeframe)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyMap()) // Return empty flow if disabled
        }

        return combine(smc1H, patterns1H, fvgs1H, smc4H, volumeSpikes1H) { s1, p1, f1, s4, v1 ->
            val startTime = profiler.start("confluence_analysis_batch")
            val allSignals = mutableMapOf<String, MutableList<ConfluenceSignal>>()
            // ... (rest of the confluence logic)
            profiler.end("confluence_analysis_batch", startTime, true)
            allSignals
        }
    }
     private fun findAlignedSignals(
        htfBias: MarketBias,
        smcData: SMCAnalysisResult,
        patterns: List<PatternResult>,
        fvgs: List<FVGResult>
    ): List<ConfluenceSignal> {
        return emptyList()
     }
}