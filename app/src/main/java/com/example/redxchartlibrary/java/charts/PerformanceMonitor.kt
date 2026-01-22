package com.example.redxchartlibrary.charts

import android.view.Choreographer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun PerformanceMonitor() {
    var frameCount by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(0) }
    var lastCheck by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCheck >= 1000) {
                fps = frameCount
                frameCount = 0
                lastCheck = currentTime
            }
            delay(16) // Check roughly every frame
        }
    }

    // This recomposes on every frame, allowing us to count frames.
    // Note: In a real production app, you might use a more sophisticated method,
    // but this is a simple and effective demonstration.
    DisposableEffect(Unit) {
        val frameCounter = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(frameCounter)
        onDispose {
            Choreographer.getInstance().removeFrameCallback(frameCounter)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.TopEnd) {
        Text(text = "$fps FPS", color = Color.Gray)
    }
}