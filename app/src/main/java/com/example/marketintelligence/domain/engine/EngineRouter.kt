package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.domain.repository.SettingsRepository
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

    private val defaultScannerEngine = scannerEngines.values.first()
    private val defaultMentorEngine = mentorEngines.values.first()
    private val defaultChartEngine = chartEngines.values.first()

    val activeScannerEngine = settingsRepository.selectedScannerEngine
        .map { selectedName ->
            val engine = scannerEngines[selectedName]
            if (engine != null) {
                logService.logSwitch("Scanner", selectedName)
                engine
            } else {
                logService.logFallback("Scanner", selectedName, defaultScannerEngine.engineName)
                defaultScannerEngine 
            }
        }

    val activeMentorEngine = settingsRepository.selectedMentorEngine
        .map { selectedName ->
            val engine = mentorEngines[selectedName]
            if (engine != null) {
                logService.logSwitch("Mentor", selectedName)
                engine
            } else {
                logService.logFallback("Mentor", selectedName, defaultMentorEngine.engineName)
                defaultMentorEngine 
            }
        }

    val activeChartEngine = settingsRepository.selectedChartEngine
        .map { selectedName ->
            val engine = chartEngines[selectedName]
            if (engine != null) {
                logService.logSwitch("Chart", selectedName)
                engine
            } else {
                logService.logFallback("Chart", selectedName, defaultChartEngine.engineName)
                defaultChartEngine 
            }
        }
}
