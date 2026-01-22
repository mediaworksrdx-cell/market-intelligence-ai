package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.engine.GeminiScannerEngineImpl
import com.example.marketintelligence.data.engine.ProprietaryScannerEngineImpl
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
