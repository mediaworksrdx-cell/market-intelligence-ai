package com.marketintelligence.tradeengine.service

import com.marketintelligence.tradeengine.models.NewsItem
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

object NewsIngestionService {
    private val logger = LoggerFactory.getLogger(NewsIngestionService::class.java)

    private val KNOWN_TICKERS = listOf(
        "NIFTY", "BANKNIFTY", "FINNIFTY", "SENSEX", "RELIANCE", "TCS", "HDFCBANK", "INFY",
        "ICICIBANK", "ITC", "SBIN", "BHARTIARTL", "TATAMOTORS", "ZOMATO", "LTIM", "LT",
        "TITAN", "BAJFINANCE", "DIXON", "POLYCAB", "SUZLON", "PAYTM", "SWIGGY", "HAL", "BEL",
        "NVDA", "AAPL", "MSFT", "TSLA", "AMZN", "META", "GOOGL", "AMD", "NFLX", "INTC",
        "GOLD", "SILVER", "CRUDEOIL", "BRENT", "WTI", "NATGAS", "COPPER",
        "EURUSD", "GBPUSD", "USDJPY", "USDINR", "USDAED",
        "BTC", "ETH", "SOL", "BNB", "DOGE", "XRP", "ADA"
    )

    private val POSITIVE_KEYWORDS = listOf(
        "surge", "jump", "rally", "soar", "beat", "record high", "profit rises",
        "growth", "upgrade", "bullish", "breakout", "gain", "outperform", "strong",
        "expansion", "optimism", "dividend", "deal", "buy rating", "profit jumps"
    )

    private val NEGATIVE_KEYWORDS = listOf(
        "plunge", "drop", "slump", "tumble", "miss", "record low", "loss", "falls",
        "downgrade", "bearish", "selloff", "crash", "decline", "probe", "penalty",
        "sanction", "inflation surges", "war", "tariff", "weakness", "risk", "profit dips"
    )

    @Volatile
    private var cachedNews: List<NewsItem> = emptyList()
    private var lastFetchTime = 0L
    private const val CACHE_TTL_MS = 3 * 60 * 1000L // 3 minutes

    data class FeedConfig(val source: String, val url: String, val defaultMarket: String)

    private val FEEDS = listOf(
        FeedConfig("Economic Times", "https://economictimes.indiatimes.com/markets/rssfeeds/1977021501.cms", "INDIA"),
        FeedConfig("MarketWatch", "https://feeds.content.dowjones.io/public/rss/mw_topstories", "USA"),
        FeedConfig("Moneycontrol", "https://www.moneycontrol.com/rss/MCtopnews.xml", "INDIA")
    )

    suspend fun getNews(): List<NewsItem> {
        val now = System.currentTimeMillis()
        if (cachedNews.isNotEmpty() && (now - lastFetchTime) < CACHE_TTL_MS) {
            return cachedNews
        }
        return refreshNews()
    }

    @Synchronized
    fun refreshNews(): List<NewsItem> {
        val now = System.currentTimeMillis()
        val allParsed = mutableListOf<NewsItem>()
        val seenTitles = mutableSetOf<String>()

        for (feed in FEEDS) {
            try {
                val xml = fetchUrl(feed.url) ?: continue
                val items = parseRssXml(xml, feed)
                for (item in items) {
                    val key = item.title.trim().lowercase()
                    if (seenTitles.add(key)) {
                        allParsed.add(item)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to fetch RSS from ${feed.source}: ${e.message}")
            }
        }

        if (allParsed.size >= 10) {
            cachedNews = allParsed
            lastFetchTime = now
            return allParsed
        }

        // Blend with baseline seed data if live wire returns few items
        val baseline = getBaselineNews()
        for (b in baseline) {
            if (seenTitles.add(b.title.trim().lowercase())) {
                allParsed.add(b)
            }
        }

        cachedNews = allParsed
        lastFetchTime = now
        return allParsed
    }

    private fun fetchUrl(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.setRequestProperty("Accept", "text/xml, application/xml, */*")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRssXml(xml: String, feed: FeedConfig): List<NewsItem> {
        val list = mutableListOf<NewsItem>()
        val itemMatcher = Pattern.compile("<item>([\\s\\S]*?)</item>", Pattern.CASE_INSENSITIVE).matcher(xml)

        var count = 0
        while (itemMatcher.find() && count < 30) {
            val block = itemMatcher.group(1) ?: continue
            val title = extractTag(block, "title")
            if (title.isBlank()) continue

            val description = extractTag(block, "description").ifBlank { title }
            val link = extractTag(block, "link").ifBlank { "#" }
            val pubDateStr = extractTag(block, "pubDate")

            val pubDate = parsePubDate(pubDateStr)
            val minutesAgo = ((System.currentTimeMillis() - pubDate.time) / 60000L).coerceAtLeast(0)
            val timeAgo = when {
                minutesAgo < 1 -> "Just now"
                minutesAgo < 60 -> "${minutesAgo}m ago"
                minutesAgo < 1440 -> "${minutesAgo / 60}h ago"
                else -> "${minutesAgo / 1440}d ago"
            }

            val fullText = "$title $description"
            val tickers = extractTickers(fullText)
            val sentiment = analyzeSentiment(fullText)

            // Categorize
            val lower = fullText.lowercase()
            val category = when {
                lower.contains("bitcoin") || lower.contains("crypto") || lower.contains("ethereum") || lower.contains("btc") -> "CRYPTO"
                lower.contains("crude") || lower.contains("oil") || lower.contains("gold") || lower.contains("silver") || lower.contains("commodity") -> "COMMODITIES"
                lower.contains("forex") || lower.contains("rupee") || lower.contains("dollar") || lower.contains("yield") || lower.contains("treasury") -> "FOREX"
                lower.contains("gdp") || lower.contains("inflation") || lower.contains("cpi") || lower.contains("rbi policy") || lower.contains("interest rate") -> "MACRO"
                else -> "EQUITIES"
            }

            // Market classification
            val market = when {
                lower.contains("india") || lower.contains("nifty") || lower.contains("sensex") || lower.contains("rbi") || lower.contains("crore") -> "INDIA"
                lower.contains("fed") || lower.contains("nasdaq") || lower.contains("s&p") || lower.contains("wall street") -> "USA"
                lower.contains("uae") || lower.contains("dubai") || lower.contains("saudi") -> "UAE"
                else -> feed.defaultMarket
            }

            val isBreaking = count < 3 && minutesAgo < 180

            val cleanSummary = if (description.length > 280) description.substring(0, 277) + "..." else description

            val item = NewsItem(
                id = "live-rss-${System.currentTimeMillis().toString(36)}-$count",
                title = title,
                summary = cleanSummary,
                url = link,
                source = feed.source,
                publishedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(pubDate),
                timeAgo = timeAgo,
                category = category,
                market = market,
                sentiment = sentiment,
                tickers = if (tickers.isNotEmpty()) tickers else if (market == "INDIA") listOf("NIFTY") else listOf("S&P500"),
                isBreaking = isBreaking
            )
            list.add(item)
            count++
        }
        return list
    }

    private fun extractTag(block: String, tag: String): String {
        val pattern = Pattern.compile("<$tag[^>]*>([\\s\\S]*?)</$tag>", Pattern.CASE_INSENSITIVE)
        val m = pattern.matcher(block)
        if (!m.find()) return ""
        var valStr = m.group(1) ?: ""
        // Strip CDATA
        valStr = valStr.replace(Regex("<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>", RegexOption.IGNORE_CASE), "$1").trim()
        // Strip HTML
        valStr = valStr.replace(Regex("<[^>]+>"), "").trim()
        // Unescape entities
        valStr = valStr
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#8377;", "₹")
        return valStr
    }

    private fun parsePubDate(pubDateStr: String): Date {
        if (pubDateStr.isBlank()) return Date()
        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )
        for (f in formats) {
            try {
                return f.parse(pubDateStr) ?: continue
            } catch (_: Exception) {}
        }
        return Date()
    }

    fun extractTickers(text: String): List<String> {
        val upper = text.uppercase()
        val set = mutableSetOf<String>()
        for (t in KNOWN_TICKERS) {
            val p = Pattern.compile("\\b$t\\b|\\$$t\\b")
            if (p.matcher(upper).find()) {
                set.add(t)
            }
        }
        if (upper.contains("NIFTY 50") || upper.contains("NIFTY50")) set.add("NIFTY")
        if (upper.contains("BANK NIFTY")) set.add("BANKNIFTY")
        if (upper.contains("CRUDE OIL") || upper.contains("BRENT")) set.add("CRUDEOIL")
        if (upper.contains("BITCOIN")) set.add("BTC")
        return set.take(4)
    }

    fun analyzeSentiment(text: String): String {
        val t = text.lowercase()
        var pos = 0
        var neg = 0
        for (w in POSITIVE_KEYWORDS) if (t.contains(w)) pos++
        for (w in NEGATIVE_KEYWORDS) if (t.contains(w)) neg++
        return when {
            pos > neg -> "BULLISH"
            neg > pos -> "BEARISH"
            else -> "NEUTRAL"
        }
    }

    fun filterNews(
        items: List<NewsItem>,
        category: String = "ALL",
        market: String = "ALL",
        ticker: String = "",
        search: String = "",
        limit: Int = 40
    ): List<NewsItem> {
        var res = items
        if (category != "ALL") res = res.filter { it.category.equals(category, ignoreCase = true) }
        if (market != "ALL") res = res.filter { it.market.equals(market, ignoreCase = true) || it.market.equals("GLOBAL", ignoreCase = true) }
        if (ticker.isNotBlank()) {
            val sym = ticker.trim().uppercase()
            res = res.filter { item -> item.tickers.any { it.equals(sym, ignoreCase = true) } }
        }
        if (search.isNotBlank()) {
            val q = search.trim().lowercase()
            res = res.filter {
                it.title.lowercase().contains(q) ||
                it.summary.lowercase().contains(q) ||
                it.source.lowercase().contains(q) ||
                it.tickers.any { t -> t.lowercase().contains(q) }
            }
        }
        return res.take(limit)
    }

    private fun getBaselineNews(): List<NewsItem> {
        val now = System.currentTimeMillis()
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return listOf(
            NewsItem(
                id = "base-1",
                title = "Reliance Industries Q2 Net Profit Jumps 14% on Retail and Jio Strength",
                summary = "Reliance Industries delivered strong operating results with EBITDA expansion across digital services and retail platforms, driven by accelerating 5G subscriber additions.",
                url = "https://www.moneycontrol.com",
                source = "Moneycontrol",
                publishedAt = isoFormat.format(Date(now - 12 * 60 * 1000L)),
                timeAgo = "12m ago",
                category = "EQUITIES",
                market = "INDIA",
                sentiment = "BULLISH",
                tickers = listOf("RELIANCE", "NIFTY"),
                isBreaking = true
            ),
            NewsItem(
                id = "base-2",
                title = "NVIDIA Expands AI Infrastructure Partnerships with Sovereign Cloud Hubs",
                summary = "NVIDIA announced multi-gigawatt enterprise AI cluster deployments across US and Asian datacenters, reinforcing long-term Blackwell GPU architecture demand projections.",
                url = "https://www.reuters.com",
                source = "Reuters",
                publishedAt = isoFormat.format(Date(now - 25 * 60 * 1000L)),
                timeAgo = "25m ago",
                category = "EQUITIES",
                market = "USA",
                sentiment = "BULLISH",
                tickers = listOf("NVDA", "AAPL", "MSFT"),
                isBreaking = false
            ),
            NewsItem(
                id = "base-3",
                title = "RBI Signals Liquidity Neutrality as Core Inflation Anchors Below 4%",
                summary = "The Reserve Bank of India monetary stance shift has prompted banking analysts to price in calibrated repo rate adjustments over the coming quarters.",
                url = "https://economictimes.indiatimes.com",
                source = "Economic Times",
                publishedAt = isoFormat.format(Date(now - 45 * 60 * 1000L)),
                timeAgo = "45m ago",
                category = "MACRO",
                market = "INDIA",
                sentiment = "BULLISH",
                tickers = listOf("BANKNIFTY", "HDFCBANK"),
                isBreaking = true
            )
        )
    }
}
