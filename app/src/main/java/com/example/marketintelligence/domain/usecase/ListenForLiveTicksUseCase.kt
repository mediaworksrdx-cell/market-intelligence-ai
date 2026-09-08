package com.example.marketintelligence.domain.usecase

import com.example.tradeengine.engine.CurrentCandleManager
import com.example.tradeengine.live.WebSocketClient
import com.example.tradeengine.models.Tick
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ListenForLiveTicksUseCase @Inject constructor(
    private val webSocketClient: WebSocketClient,
    private val currentCandleManager: CurrentCandleManager
) {
    fun execute(url: String): Flow<Tick> {
        webSocketClient.start(url)
        return webSocketClient.tickFlow
    }

    fun processTick(tick: Tick) {
        currentCandleManager.processTick(tick)
    }
}
