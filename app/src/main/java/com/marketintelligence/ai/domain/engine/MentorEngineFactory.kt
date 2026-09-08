package com.marketintelligence.ai.domain.engine

import com.marketintelligence.ai.data.engine.GeminiMentorEngineImpl
import com.marketintelligence.ai.data.engine.ProprietaryMentorEngineImpl
import javax.inject.Inject

class MentorEngineFactory @Inject constructor(
    private val geminiEngine: GeminiMentorEngineImpl,
    private val proprietaryEngine: ProprietaryMentorEngineImpl
) {
    fun getEngine(name: String): MentorEngine {
         return if (name == proprietaryEngine.engineName) {
            proprietaryEngine
        } else {
            geminiEngine
        }
    }
}
