
package com.marketintelligence.ai.domain.engine

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "EngineSystem"

@Singleton
class EngineLogService @Inject constructor() {

    fun logSwitch(engineType: String, newEngine: String) {
        Log.i(LOG_TAG, "SWITCH: $engineType has been switched to -> $newEngine")
    }

    fun logFallback(engineType: String, failedSelection: String, fallbackEngine: String) {
        Log.w(LOG_TAG, "FALLBACK: Failed to find '$failedSelection' for $engineType. Falling back to -> $fallbackEngine")
    }

    fun logError(engineName: String, error: Throwable) {
        Log.e(LOG_TAG, "ERROR: Engine '$engineName' encountered an error.", error)
    }
}
