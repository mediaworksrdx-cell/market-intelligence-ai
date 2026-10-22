package com.example.marketintelligence.data.util

import android.content.Context
import android.util.Log
import com.example.marketintelligence.domain.model.BuildupType
import com.example.marketintelligence.domain.model.FnoBuildupStock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

@Serializable
data class ZerodhaStockItem(
    val token: Long = 0L,
    val symbol: String,
    val name: String,
    val sector: String = "Indian Equity",
    val exchange: String = "NSE",
    val lastPrice: Double? = null
)

object ZerodhaStockCatalog {
    private const val TAG = "ZerodhaCatalog"
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var catalogList: List<ZerodhaStockItem>? = null
    private val symbolMap = HashMap<String, ZerodhaStockItem>()

    val KNOWN_ZERODHA_PRICES: Map<String, Double> = mapOf(
        "NIFTY 50" to 23146.15,
        "NIFTY" to 23146.15,
        "BANKNIFTY" to 55844.00,
        "FINNIFTY" to 25480.00,
        "MIDCPNIFTY" to 12850.40,
        "SENSEX" to 76450.00,
        "BANKEX" to 62150.00,
        "RELIANCE" to 1211.20,
        "HDFCBANK" to 1782.30,
        "ICICIBANK" to 1286.40,
        "INFY" to 1885.60,
        "TCS" to 3892.50,
        "TATAMOTORS" to 283.70,
        "BHARTIARTL" to 1690.50,
        "ITC" to 485.50,
        "KOTAKBANK" to 1780.20,
        "LT" to 3620.00,
        "BAJFINANCE" to 7120.40,
        "SBIN" to 865.40,
        "AXISBANK" to 1184.50,
        "MARUTI" to 12450.00,
        "TITAN" to 3480.00,
        "ZOMATO" to 268.40,
        "TATASTEEL" to 154.20,
        "ADANIENT" to 2980.50,
        "BEL" to 295.40,
        "HAL" to 4420.00,
        "SUZLON" to 65.80,
        "PAYTM" to 745.20,
        "VEDL" to 495.60,
        "TATACHEM" to 1045.00,
        "DLF" to 865.00,
        "SUNPHARMA" to 1720.00,
        "WIPRO" to 545.00,
        "HINDUNILVR" to 2480.00,
        "ASIANPAINT" to 2890.00,
        "COALINDIA" to 492.00,
        "NTPC" to 418.00,
        "POWERGRID" to 325.00,
        "ONGC" to 288.00,
        "IOC" to 168.00,
        "BPCL" to 345.00,
        "GAIL" to 228.00,
        "SAIL" to 135.00,
        "JSWSTEEL" to 980.00,
        "HINDALCO" to 675.00,
        "DRREDDY" to 6550.00,
        "CIPLA" to 1520.00,
        "DIVISLAB" to 5200.00,
        "APOLLOHOSP" to 6950.00,
        "EICHERMOT" to 4850.00,
        "HEROMOTOCO" to 4950.00,
        "BAJAJ-AUTO" to 9800.00,
        "M&M" to 2890.00,
        "CHOLAFIN" to 1450.00,
        "MUTHOOTFIN" to 1850.00,
        "POLYCAB" to 6750.00,
        "DIXON" to 13950.00,
        "TRENT" to 7650.00,
        "IRFC" to 148.00,
        "RVNL" to 465.00,
        "MAZDOCK" to 4350.00,
        "COFORGE" to 7850.00,
        "PERSISTENT" to 5450.00,
        "BSOFT" to 640.00,
        "LTTS" to 5200.00,
        "IDEA" to 11.20,
        "IRCTC" to 885.00,
        "BHEL" to 245.00,
        "NHPC" to 88.00,
        "TATACOMM" to 1920.00,
        "INDUSTOWER" to 365.00,
        "CANBK" to 108.00,
        "PNB" to 102.00,
        "BANKBARODA" to 248.00,
        "FEDERALBNK" to 198.00,
        "IDFCFIRSTB" to 72.00,
        "AUROPHARMA" to 1450.00,
        "LUPIN" to 2180.00,
        "BIOCON" to 345.00,
        "IREDA" to 225.00,
        "CDSL" to 1480.00,
        "YESBANK" to 22.40,
        "ZEEL" to 128.00,
        "ZYDUSLIFE" to 1060.00
    )

    fun ensureLoaded(context: Context): List<ZerodhaStockItem> {
        catalogList?.let { return it }
        synchronized(this) {
            catalogList?.let { return it }
            val loaded = try {
                context.assets.open("zerodhaNseCatalog.json").use { stream ->
                    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
                        json.decodeFromString<List<ZerodhaStockItem>>(reader.readText())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load zerodhaNseCatalog.json: ${e.message}")
                emptyList()
            }
            symbolMap.clear()
            for (item in loaded) {
                symbolMap[item.symbol.uppercase()] = item
            }
            catalogList = loaded
            Log.i(TAG, "Successfully loaded ${loaded.size} Zerodha Kite instruments into memory")
            return loaded
        }
    }

    fun getFallbackSpotPrice(item: ZerodhaStockItem): Double {
        if (item.lastPrice != null && item.lastPrice > 0.0) return item.lastPrice
        val sym = item.symbol.uppercase()
        KNOWN_ZERODHA_PRICES[sym]?.let { return it }

        var hash = 0
        val str = "${item.symbol}-${item.token}"
        for (c in str) {
            hash = (hash shl 5) - hash + c.code
        }
        val absVal = abs(hash)
        val tiers = listOf(
            45.0 to 280.0,
            280.0 to 950.0,
            950.0 to 2400.0,
            2400.0 to 4800.0
        )
        val (minP, maxP) = tiers[absVal % tiers.size]
        val price = minP + (absVal % (maxP - minP).toInt()) + ((absVal % 90) + 10) / 100.0
        return round(price * 100.0) / 100.0
    }

    fun calculateRealisticStrikeStep(spot: Double): Double {
        return when {
            spot >= 25000.0 -> 500.0
            spot >= 10000.0 -> 200.0
            spot >= 5000.0 -> 100.0
            spot >= 2500.0 -> 50.0
            spot >= 1000.0 -> 20.0
            spot >= 500.0 -> 10.0
            spot >= 200.0 -> 5.0
            spot >= 100.0 -> 2.5
            spot >= 50.0 -> 1.0
            else -> 0.5
        }
    }

    fun calculateRealisticLotSize(spot: Double): Int {
        if (spot <= 0.0) return 100
        val targetContractValue = 750000.0 // ₹7.5 Lakhs
        val rawLot = targetContractValue / spot
        return when {
            rawLot >= 5000 -> min(10000, (round(rawLot / 500.0) * 500.0).toInt())
            rawLot >= 2000 -> (round(rawLot / 250.0) * 250.0).toInt()
            rawLot >= 1000 -> (round(rawLot / 100.0) * 100.0).toInt()
            rawLot >= 500 -> (round(rawLot / 50.0) * 50.0).toInt()
            rawLot >= 200 -> (round(rawLot / 25.0) * 25.0).toInt()
            rawLot >= 100 -> (round(rawLot / 25.0) * 25.0).toInt()
            rawLot >= 50 -> (round(rawLot / 10.0) * 10.0).toInt()
            else -> max(5, (round(rawLot / 5.0) * 5.0).toInt())
        }
    }

    fun findStockBySymbol(context: Context, symbol: String): ZerodhaStockItem? {
        ensureLoaded(context)
        val clean = symbol.trim().uppercase().removeSuffix(".NS").removeSuffix(".BO")
        return symbolMap[clean]
    }

    fun search(context: Context, query: String, limit: Int = 30): List<ZerodhaStockItem> {
        val list = ensureLoaded(context)
        val q = query.trim().uppercase()
        if (q.isBlank()) return list.take(limit)

        val exact = mutableListOf<ZerodhaStockItem>()
        val prefix = mutableListOf<ZerodhaStockItem>()
        val contains = mutableListOf<ZerodhaStockItem>()

        for (item in list) {
            val sym = item.symbol.uppercase()
            val nm = item.name.uppercase()
            when {
                sym == q -> exact.add(item)
                sym.startsWith(q) -> prefix.add(item)
                sym.contains(q) || nm.contains(q) -> contains.add(item)
            }
            if (exact.size + prefix.size + contains.size >= limit * 2) break
        }

        return (exact + prefix + contains).distinctBy { it.symbol }.take(limit)
    }

    fun toBuildupStock(item: ZerodhaStockItem): FnoBuildupStock {
        val spot = getFallbackSpotPrice(item)
        var hash = 0
        val str = "buildup-${item.symbol}-${item.token}"
        for (c in str) {
            hash = (hash shl 5) - hash + c.code
        }
        val absVal = abs(hash)
        val pct = round((((absVal % 700) - 330) / 100.0) * 100.0) / 100.0
        val change = round(spot * (pct / 100.0) * 100.0) / 100.0
        val oi = 1000000L + (absVal.toLong() % 10000000L)
        val oiChg = ((absVal.toLong() % 1000000L) - 450000L)
        val oiChgPct = round((oiChg.toDouble() / oi.toDouble() * 100.0) * 100.0) / 100.0
        val buildupType = when {
            change > 0 && oiChg > 0 -> BuildupType.LONG_BUILDUP
            change < 0 && oiChg > 0 -> BuildupType.SHORT_BUILDUP
            change > 0 && oiChg < 0 -> BuildupType.SHORT_COVERING
            else -> BuildupType.LONG_UNWINDING
        }

        return FnoBuildupStock(
            symbol = item.symbol.uppercase(),
            name = item.name,
            isIndex = item.symbol.contains("NIFTY") || item.symbol.contains("SENSEX") || item.symbol.contains("BANKEX"),
            sector = item.sector,
            ltp = spot,
            priceChange = change,
            priceChangePct = pct,
            openInterest = oi,
            oiChange = oiChg,
            oiChangePct = oiChgPct,
            buildupType = buildupType,
            volume = (oi * 0.4).toLong(),
            pcr = 1.0,
            basis = 0.0,
            isZerodha = true
        )
    }
}
