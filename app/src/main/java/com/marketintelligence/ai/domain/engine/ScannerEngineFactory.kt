package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.engine.GeminiScannerEngineImpl
import com.marketintelligence.ai.data.engine.ProprietaryScannerEngineImpl
import javax.inject.Inject

class ScannerEngineFactory @Inject constructor(
    private val geminiEngine: GeminiScannerEngineImpl,
    private val proprietaryEngine: ProprietaryScannerEngineImpl
) {
    fun getEngine(name: String): ScannerEngine {
         return if (name == proprietaryEngine.engineName) {
            proprietaryEngine
        } else {
            geminiEngine
        }
    }
}
