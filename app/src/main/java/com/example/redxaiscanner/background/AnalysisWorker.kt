package com.example.redxaiscanner.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.redxaiscanner.domain.usecase.GenerateTradeSetupUseCase
import com.example.redxaiscanner.engine.AlertEvent
import com.example.redxaiscanner.engine.MarketBias
import com.example.redxaiscanner.engine.OrderBlock
import com.example.redxaiscanner.engine.TradeSetup
import kotlinx.coroutines.flow.first

class AnalysisWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val generateTradeSetupUseCase = com.example.redxaiscanner.util.Injector.provideGenerateTradeSetupUseCase(appContext)

    companion object {
        const val KEY_SYMBOLS = "KEY_SYMBOLS"
        const val UNIQUE_PERIODIC_SCAN = "UNIQUE_PERIODIC_SCAN"
    }

    override suspend fun doWork(): Result {
        val symbols = inputData.getStringArray(KEY_SYMBOLS)?.toList()
        if (symbols.isNullOrEmpty()) {
            return Result.failure()
        }

        return try {
            // Execute the entire analysis pipeline and get the final setups
            val tradeSetups = generateTradeSetupUseCase.getTradeSetups(symbols).first()

            // Convert the results into alert events
            val alerts = tradeSetups.values.flatten().map { it.toAlertEvent() }

            if (alerts.isNotEmpty()) {
                // TODO: Trigger notifications or broadcast the alerts
                // For example: NotificationHandler.showSummaryNotification(applicationContext, alerts)
                println("Generated ${alerts.size} alerts.")
            }

            Result.success()
        } catch (e: Exception) {
            // It's important to handle exceptions for background work
            e.printStackTrace()
            Result.failure()
        }
    }
    
    private fun TradeSetup.toAlertEvent(): AlertEvent {
        val smcSignal = this.underlyingSignal.underlyingSignal.smcSignal
        val description = if(smcSignal is OrderBlock) {
            "${smcSignal.direction} Order Block @ ${this.underlyingSignal.underlyingSignal.timeframe}"
        } else {
            "High-Confidence Setup"
        }
        
        return AlertEvent(
            symbol = this.underlyingSignal.underlyingSignal.symbol,
            direction = this.underlyingSignal.underlyingSignal.higherTimeframeBias,
            description = description,
            entryPrice = this.entryPrice,
            confidenceScore = this.underlyingSignal.confidenceScore
        )
    }
}