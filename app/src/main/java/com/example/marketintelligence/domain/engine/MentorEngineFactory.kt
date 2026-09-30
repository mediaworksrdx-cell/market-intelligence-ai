package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.engine.AarkaAiMentorEngineImpl
import com.example.marketintelligence.data.engine.GeminiMentorEngineImpl
import com.example.marketintelligence.data.engine.ProprietaryMentorEngineImpl
import javax.inject.Inject

class MentorEngineFactory @Inject constructor(
    private val geminiEngine: GeminiMentorEngineImpl,
    private val proprietaryEngine: ProprietaryMentorEngineImpl,
    private val aarkaAiEngine: AarkaAiMentorEngineImpl
) {
    fun getEngine(name: String): MentorEngine {
         return when (name) {
             geminiEngine.engineName -> geminiEngine
             proprietaryEngine.engineName -> proprietaryEngine
             else -> aarkaAiEngine
         }
    }
}
