package com.example.redxaiscanner.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BackgroundScannerEngine(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Enqueues an immediate, one-off scan for the provided list of symbols.
     *
     * @param symbols The list of market symbols to scan.
     */
    fun startManualScan(symbols: List<String>) {
        val inputData = Data.Builder()
            .putStringArray(AnalysisWorker.KEY_SYMBOLS, symbols.toTypedArray())
            .build()
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val scanRequest = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("MANUAL_SCAN")
            .build()

        workManager.enqueue(scanRequest)
    }

    /**
     * Schedules a recurring, periodic scan that will run automatically.
     * Any existing scheduled scan will be replaced with this new one.
     *
     * @param symbols The list of symbols to scan on each run.
     * @param interval The numeric interval for the scan (e.g., 4).
     * @param timeUnit The unit of time for the interval (e.g., TimeUnit.HOURS).
     */
    fun scheduleAutoScan(symbols: List<String>, interval: Long, timeUnit: TimeUnit) {
        val inputData = Data.Builder()
            .putStringArray(AnalysisWorker.KEY_SYMBOLS, symbols.toTypedArray())
            .build()
        
        // --- PRODUCTION OPTIMIZATIONS ---
        // For long-running, periodic tasks, it's critical to respect device resources.
        // These constraints ensure the work only runs when conditions are optimal,
        // preventing battery drain and unnecessary data usage.
         val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // Use Wi-Fi to save mobile data
            .setRequiresCharging(true)                  // Only run when the device is charging
            .setRequiresDeviceIdle(true)                // Only run when the device is not actively in use
            .build()

        val periodicScanRequest = PeriodicWorkRequestBuilder<AnalysisWorker>(interval, timeUnit)
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag("AUTO_SCAN")
            .build()

        workManager.enqueueUniquePeriodicWork(
            AnalysisWorker.UNIQUE_PERIODIC_SCAN,
            ExistingPeriodicWorkPolicy.REPLACE, // Replace the old scan with the new one
            periodicScanRequest
        )
    }

    /**
     * Cancels all scheduled auto-scans.
     */
    fun cancelAutoScans() {
        workManager.cancelUniqueWork(AnalysisWorker.UNIQUE_PERIODIC_SCAN)
    }
}