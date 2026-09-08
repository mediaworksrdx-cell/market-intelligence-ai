package com.marketintelligence.ai.data.source.remote

import com.marketintelligence.ai.BuildConfig
import com.marketintelligence.tradeengine.LivePrice
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject

class MarketDataSocket @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    suspend fun connect(symbol: String): Flow<LivePrice> {
        val baseUrl = BuildConfig.WS_BASE_URL.ifBlank { "ws://35.225.45.190:8080/ws" }
        val session = client.webSocketSession {
            url(baseUrl)
        }

        return session.incoming.receiveAsFlow()
            .filterIsInstance<Frame.Text>()
            .mapNotNull { frame ->
                runCatching {
                    json.decodeFromString<LivePrice>(frame.readText())
                }.getOrNull()
            }
    }
}
