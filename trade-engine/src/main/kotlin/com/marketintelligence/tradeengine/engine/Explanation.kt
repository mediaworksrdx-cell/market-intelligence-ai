package com.marketintelligence.tradeengine.engine

import kotlinx.serialization.Serializable

@Serializable
data class SignalExplanation(
    val title: String,
    val components: List<ExplanationComponent>
)

@Serializable
data class ExplanationComponent(
    val componentName: String,
    val reasoning: String,
    val details: Map<String, String>
)
