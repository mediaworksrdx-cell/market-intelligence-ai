package com.example.marketintelligence.domain.engine

import com.example.marketintelligence.data.engine.GeminiMentorEngineImpl
import com.example.marketintelligence.data.engine.ProprietaryMentorEngineImpl
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
