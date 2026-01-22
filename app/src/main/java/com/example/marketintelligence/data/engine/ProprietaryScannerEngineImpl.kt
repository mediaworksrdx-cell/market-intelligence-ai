package com.example.marketintelligence.data.engine

import com.example.marketintelligence.domain.engine.ScannerEngine
import com.example.marketintelligence.data.model.AIAnalysisResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ProprietaryScannerEngineImpl @Inject constructor() : ScannerEngine {
    override val engineName: String = "Proprietary Engine"

    override fun scan(symbol: String, timeframe: String):  Flow<Result<AIAnalysisResult>> = flow {
        emit(Result.failure(Exception("Not implemented")))
    }
}
