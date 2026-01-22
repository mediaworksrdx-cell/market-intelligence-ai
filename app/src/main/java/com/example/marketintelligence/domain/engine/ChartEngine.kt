package com.example.marketintelligence.domain.engine

import androidx.compose.runtime.Composable

/**
 * A common interface for any Charting Engine.
 * Because the chart is pure UI, the engine's primary job is to render a Composable.
 */
interface ChartEngine : Engine {
    /**
     * The unique name of this engine, used for selection in settings.
     */
    override val engineName: String

    /**
     * The Composable function that renders the actual chart UI.
     *
     * @param symbol The stock symbol to display the chart for.
     */
    @Composable
    fun Render(symbol: String)
}
