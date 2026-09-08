package com.example.tradeengine.live

import com.example.tradeengine.models.Tick
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketClient(
    private val client: HttpClient,
    private val coroutineScope: CoroutineScope
) {

    private val _tickChannel = Channel<Tick>(Channel.BUFFERED)
    val tickFlow = _tickChannel.receiveAsFlow()

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus = _connectionStatus.asStateFlow()

    private val isRunning = AtomicBoolean(false)
    private var session: DefaultClientWebSocketSession? = null

    fun start(url: String) {
        if (isRunning.compareAndSet(false, true)) {
            coroutineScope.launch(Dispatchers.IO) {
                while (isRunning.get()) {
                    try {
                        client.webSocket(
                            method = HttpMethod.Get,
                            host = url,
                            port = 0, // or your specific port
                            path = "/ws" // or your specific path
                        ) {
                            _connectionStatus.value = true
                            session = this
                            // Send subscription message if needed
                            // e.g., session.send("...")

                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    try {
                                        val tick = Json.decodeFromString<Tick>(text)
                                        _tickChannel.send(tick)
                                    } catch (e: Exception) {
                                        // Handle deserialization error
                                        println("Error deserializing tick: ${e.message}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("WebSocket error: ${e.message}")
                    } finally {
                        _connectionStatus.value = false
                        session = null
                        if (isRunning.get()) {
                            kotlinx.coroutines.delay(5000) // Reconnect delay
                            println("Reconnecting to WebSocket...")
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            coroutineScope.launch {
                session?.close()
                _tickChannel.close()
            }
        }
    }
}
