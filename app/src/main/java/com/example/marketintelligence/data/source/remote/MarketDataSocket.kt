package com.example.marketintelligence.data.source.remote

import com.example.tradeengine.LivePrice
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class MarketDataSocket @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {
    suspend fun connect(symbol: String): Flow<LivePrice> {
        val session = client.webSocketSession {
            url("wss://your-websocket-url.com/live-prices") // Replace with your actual WebSocket URL
        }

        // Subscribe to the symbol if required by the WebSocket API
        // session.send(Frame.Text("{\"type\":\"subscribe\", \"symbol\":\"$symbol\"}"))

        return session.incoming.consumeAsFlow()
            .filterIsInstance<Frame.Text>()
            .map { frame ->
                json.decodeFromString<LivePrice>(frame.readText())
            }
    }
}
