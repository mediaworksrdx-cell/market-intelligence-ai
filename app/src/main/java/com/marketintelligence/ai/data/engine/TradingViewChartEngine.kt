package com.marketintelligence.ai.data.engine

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.marketintelligence.ai.domain.engine.ChartEngine
import com.marketintelligence.ai.ui.theme.SurfaceDark
import com.marketintelligence.tradeengine.models.Candle
import javax.inject.Inject

class TradingViewChartEngine @Inject constructor() : ChartEngine {
    override val engineName: String = "Standard (TradingView)"

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    override fun Render(symbol: String, timeframe: String, candles: List<Candle>) {
        // Map app timeframes to TradingView intervals
        val interval = when (timeframe) {
            "1m" -> "1"
            "5m" -> "5"
            "15m" -> "15"
            "30m" -> "30"
            "1H" -> "60"
            "4H" -> "240"
            "1D" -> "D"
            "1W" -> "W"
            "1M" -> "M"
            else -> "60"
        }

        // Sanitize symbol for TradingView
        val formattedSymbol = when {
            symbol.contains("NIFTY") -> "NSE:NIFTY"
            symbol.contains("BANKNIFTY") -> "NSE:BANKNIFTY"
            symbol.endsWith(".NS") -> "NSE:${symbol.removeSuffix(".NS")}"
            symbol.contains("-USD") -> "BINANCE:${symbol.replace("-", "")}T"
            else -> symbol
        }

        val encodedSymbol = java.net.URLEncoder.encode(formattedSymbol, "UTF-8")
        val url = "https://www.tradingview.com/widgetembed/?symbol=$encodedSymbol&interval=$interval&hidesidetoolbar=0&symboledit=1&saveimage=1&toolbarbg=f1f3f6&studies=%5B%5D&theme=dark&style=1&timezone=Etc%2FUTC&studies_overrides=%7B%7D&overrides=%7B%7D&enabled_features=%5B%5D&disabled_features=%5B%5D&locale=en"
        
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = false
                        allowContentAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            update = { webView ->
                // Check if the URL has changed before reloading to prevent flickering
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
        )
    }
}
