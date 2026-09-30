package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.engine.AarkaAiMentorEngineImpl
import com.marketintelligence.ai.data.engine.GeminiMentorEngineImpl
import com.marketintelligence.ai.data.engine.ProprietaryMentorEngineImpl
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
