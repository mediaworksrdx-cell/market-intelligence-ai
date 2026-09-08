package com.example.redxchartlibrary.charts.renderer

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import com.example.redxchartlibrary.state.ChartViewportState

interface ChartLayer {
    fun draw(scope: DrawScope, viewportState: ChartViewportState, textMeasurer: TextMeasurer)
}
