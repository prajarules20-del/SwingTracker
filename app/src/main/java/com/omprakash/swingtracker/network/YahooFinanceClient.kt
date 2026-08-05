package com.omprakash.swingtracker.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** One day's worth of price/volume data. */
data class DailyBar(
    val timestampSec: Long,
    val close: Double,
    val volume: Long
)

/** One point on a price chart - no volume needed here, just time + price. */
data class ChartPoint(
    val timestampSec: Long,
    val close: Double
)

/** Everything needed to render a stock's detail page for one time period. */
data class StockChartData(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double,
    val previousClose: Double,
    val points: List<ChartPoint>
) {
    val change: Double get() = currentPrice - previousClose
    val changePercent: Double get() = if (previousClose == 0.0) 0.0 else (change / previousClose) * 100.0
}

/** Thrown when Yahoo returns no usable data for a symbol - usually a wrong ticker. */
class SymbolNotFoundException(symbol: String) : Exception("No data found for symbol: $symbol")

/**
 * Talks to Yahoo Finance's public (unofficial, undocumented) chart endpoint.
 * No API key needed. This endpoint can change or rate-limit without notice -
 * if it ever stops working, this is the one file that needs replacing.
 */
object YahooFinanceClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches daily bars for the given range (used by the screener, which
     * always wants one-per-day data for EMA/volume calculations).
     * @param symbol Yahoo-format symbol, e.g. "RELIANCE.NS" or "^NSEI" for Nifty 50.
     * @param range Yahoo range string, e.g. "1y", "9mo".
     */
    @Throws(IOException::class, SymbolNotFoundException::class)
    fun fetchDailyBars(symbol: String, range: String = "1y"): List<DailyBar> {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
            "?range=$range&interval=1d&includeAdjustedClose=true"
        val body = executeRequest(url, symbol)
        return parseChartResponse(body, symbol)
    }

    /**
     * Fetches chart data for the stock detail page, at whatever [range]/[interval]
     * the selected time period needs (e.g. range=1d&interval=5m for an intraday
     * "1D" view, or range=5y&interval=1wk for a "5Y" view).
     */
    @Throws(IOException::class, SymbolNotFoundException::class)
    fun fetchChartData(symbol: String, range: String, interval: String): StockChartData {
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$symbol" +
            "?range=$range&interval=$interval&includeAdjustedClose=true"
        val body = executeRequest(url, symbol)
        return parseChartDataResponse(body, symbol)
    }

    private fun executeRequest(url: String, symbol: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) SwingTracker/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} fetching $symbol")
            }
            return response.body?.string() ?: throw SymbolNotFoundException(symbol)
        }
    }

    private fun parseChartResponse(body: String, symbol: String): List<DailyBar> {
        val result = extractResult(body, symbol)
        val timestamps = result.optJSONArray("timestamp") ?: throw SymbolNotFoundException(symbol)

        val indicators = result.getJSONObject("indicators")
        val quote = indicators.getJSONArray("quote").getJSONObject(0)
        val closeArray = quote.getJSONArray("close")
        val volumeArray = quote.getJSONArray("volume")

        val bars = mutableListOf<DailyBar>()
        for (i in 0 until timestamps.length()) {
            // Yahoo sometimes returns null for a day (holiday gaps) - skip those.
            if (closeArray.isNull(i) || volumeArray.isNull(i)) continue
            bars.add(
                DailyBar(
                    timestampSec = timestamps.getLong(i),
                    close = closeArray.getDouble(i),
                    volume = volumeArray.getLong(i)
                )
            )
        }
        if (bars.isEmpty()) throw SymbolNotFoundException(symbol)
        return bars
    }

    private fun parseChartDataResponse(body: String, symbol: String): StockChartData {
        val result = extractResult(body, symbol)
        val timestamps = result.optJSONArray("timestamp") ?: throw SymbolNotFoundException(symbol)

        val indicators = result.getJSONObject("indicators")
        val quote = indicators.getJSONArray("quote").getJSONObject(0)
        val closeArray = quote.getJSONArray("close")

        val points = mutableListOf<ChartPoint>()
        for (i in 0 until timestamps.length()) {
            if (closeArray.isNull(i)) continue
            points.add(ChartPoint(timestamps.getLong(i), closeArray.getDouble(i)))
        }
        if (points.isEmpty()) throw SymbolNotFoundException(symbol)

        val meta = result.getJSONObject("meta")
        val companyName = meta.optString("longName").ifBlank {
            meta.optString("shortName").ifBlank { symbol }
        }
        val currentPrice = if (meta.has("regularMarketPrice")) {
            meta.getDouble("regularMarketPrice")
        } else {
            points.last().close
        }
        val previousClose = when {
            meta.has("previousClose") -> meta.getDouble("previousClose")
            meta.has("chartPreviousClose") -> meta.getDouble("chartPreviousClose")
            else -> points.first().close
        }

        return StockChartData(
            symbol = symbol,
            companyName = companyName,
            currentPrice = currentPrice,
            previousClose = previousClose,
            points = points
        )
    }

    private fun extractResult(body: String, symbol: String): JSONObject {
        val root = JSONObject(body)
        val chart = root.optJSONObject("chart") ?: throw SymbolNotFoundException(symbol)
        val resultArray = chart.optJSONArray("result")
        if (resultArray == null || resultArray.length() == 0) {
            throw SymbolNotFoundException(symbol)
        }
        return resultArray.getJSONObject(0)
    }
}
