package com.example.marketintelligence

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
open class MarketIntelligenceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .apply {
                if (::workerFactory.isInitialized) {
                    setWorkerFactory(workerFactory)
                }
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
        createNotificationChannels()
        scheduleBackgroundNotifications()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("FATAL_APP_CRASH", "Uncaught exception in thread ${thread.name}", throwable)
            try {
                val crashFile = java.io.File(filesDir, "crash_log.txt")
                crashFile.writeText(
                    "Timestamp: ${java.util.Date()}\nThread: ${thread.name}\nException: ${throwable.message}\n${android.util.Log.getStackTraceString(throwable)}"
                )
            } catch (e: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        com.example.marketintelligence.background.MarketNotificationWorker.ensureChannels(this)
    }

    private fun scheduleBackgroundNotifications() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val periodicWork = androidx.work.PeriodicWorkRequestBuilder<com.example.marketintelligence.background.MarketNotificationWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES,
                5, java.util.concurrent.TimeUnit.MINUTES
            )
            .setConstraints(constraints)
            .build()

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.example.marketintelligence.background.MarketNotificationWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        } catch (e: Exception) {
            android.util.Log.e("WorkManager", "Failed to schedule background notifications: ${e.message}")
        }
    }
}
