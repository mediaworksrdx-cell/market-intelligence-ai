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
        createNotificationChannel()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "AI Scanner Signals"
            val descriptionText = "Notifications for high-confidence AI trading signals"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("AI_SIGNAL_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
