package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineRouter @Inject constructor(
    engines: Set<@JvmSuppressWildcards Engine>,
    settingsRepository: SettingsRepository,
    private val logService: EngineLogService
) {
    private val scannerEngines = engines.filterIsInstance<ScannerEngine>().associateBy { it.engineName }
    private val mentorEngines = engines.filterIsInstance<MentorEngine>().associateBy { it.engineName }
    private val chartEngines = engines.filterIsInstance<ChartEngine>().associateBy { it.engineName }

    private val defaultScannerEngine = scannerEngines.values.firstOrNull()
    private val defaultMentorEngine = mentorEngines["Aarka AI"] ?: mentorEngines.values.firstOrNull()
    private val defaultChartEngine = chartEngines.values.firstOrNull()

    val activeScannerEngine = settingsRepository.selectedScannerEngine
        .map { selectedName ->
            val engine = scannerEngines[selectedName]
            if (engine != null) {
                logService.logSwitch("Scanner", selectedName)
                engine
            } else {
                val fallback = defaultScannerEngine
                if (fallback != null) {
                    logService.logFallback("Scanner", selectedName, fallback.engineName)
                } else {
                    logService.logFallback("Scanner", selectedName, "None")
                }
                fallback
            }
        }

    val activeMentorEngine = settingsRepository.selectedMentorEngine
        .map { selectedName ->
            val engine = mentorEngines[selectedName]
            if (engine != null) {
                logService.logSwitch("Mentor", selectedName)
                engine
            } else {
                val fallback = defaultMentorEngine
                if (fallback != null) {
                    logService.logFallback("Mentor", selectedName, fallback.engineName)
                } else {
                    logService.logFallback("Mentor", selectedName, "None")
                }
                fallback
            }
        }

    val activeChartEngine = settingsRepository.selectedChartEngine
        .map { selectedName ->
            val engine = chartEngines[selectedName]
            if (engine != null) {
                logService.logSwitch("Chart", selectedName)
                engine
            } else {
                val fallback = defaultChartEngine
                if (fallback != null) {
                    logService.logFallback("Chart", selectedName, fallback.engineName)
                } else {
                    logService.logFallback("Chart", selectedName, "None")
                }
                fallback
            }
        }
}
