package com.marketintelligence.ai.domain.repository

import com.marketintelligence.ai.domain.model.BuildupType
import com.marketintelligence.ai.domain.model.FnoBuildupStock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FnoBuildupRepository @Inject constructor() {

    fun classifyBuildup(priceChange: Double, oiChange: Long): BuildupType {
        return when {
            priceChange > 0 && oiChange > 0 -> BuildupType.LONG_BUILDUP
            priceChange < 0 && oiChange > 0 -> BuildupType.SHORT_BUILDUP
            priceChange > 0 && oiChange < 0 -> BuildupType.SHORT_COVERING
            priceChange < 0 && oiChange < 0 -> BuildupType.LONG_UNWINDING
            else -> BuildupType.NEUTRAL
        }
    }

    fun getInitialBuildupList(): List<FnoBuildupStock> {
        val rawItems = listOf(
            // --- Indices ---
            RawBuildup("NIFTY", "Nifty 50 Index", true, "Indices", 23620.0, 142.5, 0.61, 14850000L, 895000L, 6.42, 3850000L, 1.28, 45.0),
            RawBuildup("BANKNIFTY", "Nifty Bank Index", true, "Indices", 50480.0, 310.0, 0.62, 3120000L, -185000L, -5.60, 1920000L, 1.15, 95.0),
            RawBuildup("FINNIFTY", "Nifty Financial Services", true, "Indices", 21460.0, -85.0, -0.39, 1840000L, -120000L, -6.12, 850000L, 0.92, -15.0),
            RawBuildup("MIDCPNIFTY", "Nifty Midcap Select", true, "Indices", 12280.0, -64.0, -0.52, 2450000L, 190000L, 8.41, 1100000L, 0.81, -22.0),

            // --- Banking & Financials ---
            RawBuildup("HDFCBANK", "HDFC Bank Ltd", false, "Banking", 1724.0, -18.5, -1.06, 114500000L, 7800000L, 7.31, 18400000L, 0.74, -3.5),
            RawBuildup("ICICIBANK", "ICICI Bank Ltd", false, "Banking", 1135.0, 14.2, 1.27, 78500000L, 5200000L, 7.09, 14200000L, 1.34, 4.2),
            RawBuildup("SBIN", "State Bank of India", false, "Banking", 832.0, 11.5, 1.40, 62400000L, -2800000L, -4.29, 9800000L, 1.22, 2.0),
            RawBuildup("AXISBANK", "Axis Bank Ltd", false, "Banking", 1175.0, -16.0, -1.34, 41200000L, -1900000L, -4.41, 6500000L, 0.88, -2.8),
            RawBuildup("KOTAKBANK", "Kotak Mahindra Bank", false, "Banking", 1775.0, -12.0, -0.67, 33500000L, 2100000L, 6.69, 4800000L, 0.82, -4.0),
            RawBuildup("BAJFINANCE", "Bajaj Finance Ltd", false, "Banking", 6940.0, 110.0, 1.61, 8900000L, 720000L, 8.80, 2100000L, 1.45, 18.0),

            // --- IT & Tech ---
            RawBuildup("TCS", "Tata Consultancy Services", false, "IT", 4085.0, -42.0, -1.02, 14200000L, 980000L, 7.41, 2800000L, 0.71, -12.0),
            RawBuildup("INFY", "Infosys Ltd", false, "IT", 1865.0, 22.0, 1.19, 29500000L, -1400000L, -4.53, 5400000L, 1.18, 5.5),
            RawBuildup("WIPRO", "Wipro Ltd", false, "IT", 528.0, -7.5, -1.40, 38400000L, 2400000L, 6.67, 6200000L, 0.76, -1.8),
            RawBuildup("HCLTECH", "HCL Technologies", false, "IT", 1680.0, 24.0, 1.45, 16200000L, 1100000L, 7.28, 3100000L, 1.32, 6.0),
            RawBuildup("TECHM", "Tech Mahindra", false, "IT", 1490.0, -18.0, -1.19, 12400000L, -650000L, -4.98, 2400000L, 0.89, -3.2),

            // --- Automobile ---
            RawBuildup("TATAMOTORS", "Tata Motors Ltd", false, "Auto", 985.0, 18.0, 1.86, 48500000L, 4200000L, 9.48, 11200000L, 1.48, 3.8),
            RawBuildup("MARUTI", "Maruti Suzuki India", false, "Auto", 12380.0, -140.0, -1.12, 3100000L, 240000L, 8.39, 650000L, 0.75, -28.0),
            RawBuildup("M&M", "Mahindra & Mahindra", false, "Auto", 2780.0, 52.0, 1.91, 15400000L, -820000L, -5.06, 3200000L, 1.36, 12.0),
            RawBuildup("BAJAJ-AUTO", "Bajaj Auto Ltd", false, "Auto", 9650.0, -95.0, -0.97, 2800000L, -150000L, -5.08, 480000L, 0.84, -18.0),

            // --- Energy & Power ---
            RawBuildup("RELIANCE", "Reliance Industries", false, "Energy", 1312.0, 16.5, 1.27, 84500000L, 6800000L, 8.75, 14800000L, 1.42, 4.5),
            RawBuildup("ONGC", "Oil & Natural Gas Corp", false, "Energy", 295.0, -4.5, -1.50, 49200000L, 3100000L, 6.72, 8500000L, 0.78, -1.2),
            RawBuildup("BPCL", "Bharat Petroleum Corp", false, "Energy", 345.0, 6.0, 1.77, 31200000L, -1700000L, -5.17, 5200000L, 1.25, 1.8),
            RawBuildup("NTPC", "NTPC Ltd", false, "Energy", 398.0, -5.0, -1.24, 42500000L, -2200000L, -4.92, 7100000L, 0.86, -1.5),

            // --- Metals & Mining ---
            RawBuildup("TATASTEEL", "Tata Steel Ltd", false, "Metals", 154.0, 3.2, 2.12, 142000000L, 11500000L, 8.81, 28500000L, 1.55, 0.8),
            RawBuildup("JSWSTEEL", "JSW Steel Ltd", false, "Metals", 945.0, -14.0, -1.46, 24800000L, 1650000L, 7.13, 4100000L, 0.72, -3.2),
            RawBuildup("HINDALCO", "Hindalco Industries", false, "Metals", 675.0, 12.0, 1.81, 28400000L, -1450000L, -4.86, 5600000L, 1.30, 2.4),

            // --- Pharma & FMCG ---
            RawBuildup("SUNPHARMA", "Sun Pharmaceutical", false, "Pharma", 1695.0, 22.0, 1.31, 18900000L, 1400000L, 7.99, 3400000L, 1.29, 5.0),
            RawBuildup("DRREDDY", "Dr. Reddy's Labs", false, "Pharma", 6450.0, -75.0, -1.15, 4800000L, -280000L, -5.51, 820000L, 0.85, -15.0),
            RawBuildup("ITC", "ITC Ltd", false, "FMCG", 438.0, -3.5, -0.79, 68500000L, 4200000L, 6.53, 11200000L, 0.79, -1.2),
            RawBuildup("HINDUNILVR", "Hindustan Unilever", false, "FMCG", 2465.0, 28.0, 1.15, 19400000L, -950000L, -4.67, 3100000L, 1.16, 8.0),
            RawBuildup("TITAN", "Titan Company Ltd", false, "FMCG", 3485.0, 48.0, 1.40, 9200000L, 780000L, 9.26, 1850000L, 1.41, 14.0),

            // --- Industrials & Telecom ---
            RawBuildup("LT", "Larsen & Toubro Ltd", false, "Industrials", 3560.0, 42.0, 1.19, 13800000L, 1050000L, 8.24, 2600000L, 1.38, 11.0),
            RawBuildup("BHARTIARTL", "Bharti Airtel Ltd", false, "Telecom", 1425.0, 18.0, 1.28, 38500000L, 2900000L, 8.15, 6800000L, 1.35, 4.8)
        )

        return rawItems.map { raw ->
            val type = classifyBuildup(raw.priceChange, raw.oiChange)
            FnoBuildupStock(
                symbol = raw.symbol,
                name = raw.name,
                isIndex = raw.isIndex,
                sector = raw.sector,
                ltp = raw.ltp,
                priceChange = raw.priceChange,
                priceChangePct = raw.priceChangePct,
                openInterest = raw.openInterest,
                oiChange = raw.oiChange,
                oiChangePct = raw.oiChangePct,
                buildupType = type,
                volume = raw.volume,
                pcr = raw.pcr,
                basis = raw.basis
            )
        }
    }

    private data class RawBuildup(
        val symbol: String,
        val name: String,
        val isIndex: Boolean,
        val sector: String,
        val ltp: Double,
        val priceChange: Double,
        val priceChangePct: Double,
        val openInterest: Long,
        val oiChange: Long,
        val oiChangePct: Double,
        val volume: Long,
        val pcr: Double,
        val basis: Double
    )
}
