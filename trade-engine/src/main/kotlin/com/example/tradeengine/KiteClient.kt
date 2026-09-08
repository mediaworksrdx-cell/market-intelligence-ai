package com.example.tradeengine

import com.example.tradeengine.config.AppConfig
import com.zerodhatech.kiteconnect.KiteConnect
import com.zerodhatech.models.HistoricalData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CompletableFuture

object KiteClient {
    private val kiteConnect = KiteConnect(AppConfig.apiKey).apply {
        setAccessToken(AppConfig.accessToken)
    }

    fun getHistoricalData(instrumentToken: Long, interval: String, from: Date, to: Date): CompletableFuture<HistoricalData> {
        return CompletableFuture.supplyAsync {
            kiteConnect.getHistoricalData(from, to, instrumentToken.toString(), interval, false, true)
        }
    }
}