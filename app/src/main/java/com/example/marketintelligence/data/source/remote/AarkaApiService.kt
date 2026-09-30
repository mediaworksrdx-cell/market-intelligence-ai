package com.example.marketintelligence.data.source.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@Serializable
data class AarkaPromptRequest(
    @SerialName("query") val query: String,
    @SerialName("user_id") val userId: String = "market_intelligence_user",
    @SerialName("session_id") val sessionId: String = "session_1"
)

@Serializable
data class AarkaPromptResponse(
    @SerialName("response") val response: String = "",
    @SerialName("confidence") val confidence: Float = 0.85f,
    @SerialName("intent") val intent: String? = null,
    @SerialName("sources") val sources: List<String> = emptyList(),
    @SerialName("processing_time") val processingTime: Double? = null
)

@Serializable
data class AarkaHealthResponse(
    @SerialName("status") val status: String = "unknown",
    @SerialName("version") val version: String? = null
)

interface AarkaApiService {
    @POST("prompt")
    suspend fun promptEngine(@Body request: AarkaPromptRequest): AarkaPromptResponse

    @GET("health")
    suspend fun getHealth(): AarkaHealthResponse
}
