package com.omprakash.swingtracker.domain

import com.omprakash.swingtracker.network.DailyBar
import com.omprakash.swingtracker.network.YahooFinanceClient

/** Result of running the screener rule set against one stock. */
data class ScreenerResult(
    val price: Double,
    val ema50: Double?,
    val ema200: Double?,
    val stock3MonthReturnPct: Double?,
    val nifty3MonthReturnPct: Double?,
    val volume: Long,
    val avgVolume20d: Long?,
    val qualifies: Boolean
)

/**
 * The rule set we settled on for a 3-5 month swing/position trade:
 *   1. Price > 50 EMA, and 50 EMA > 200 EMA          (established uptrend)
 *   2. Stock's 3-month return > Nifty 50's 3-month return   (relative strength)
 *   3. Today's volume > 20-day average volume        (demand confirmation)
 *
 * All three must hold for `qualifies` to be true.
 * This is a trend + relative-strength + volume framework commonly used for
 * multi-month position trades - NOT a guarantee of performance. Markets carry
 * real risk of loss; treat this as a screening tool, not financial advice.
 */
object ScreenerEngine {

    private const val SYMBOL_SUFFIX = ".NS"
    private const val NIFTY_SYMBOL = "^NSEI"

    /** Cache the Nifty bars per run so we don't refetch it for every single stock. */
    fun fetchNiftyBars(): List<DailyBar> =
        YahooFinanceClient.fetchDailyBars(NIFTY_SYMBOL, range = "1y")

    fun evaluate(symbol: String, niftyBars: List<DailyBar>): ScreenerResult {
        val bars = YahooFinanceClient.fetchDailyBars("$symbol$SYMBOL_SUFFIX", range = "1y")
        val closes = bars.map { it.close }
        val volumes = bars.map { it.volume }

        val price = closes.last()
        val ema50 = exponentialMovingAverage(closes, 50)
        val ema200 = exponentialMovingAverage(closes, 200)
        val stockReturn3m = percentReturnOverMonths(closes, 3)
        val niftyReturn3m = percentReturnOverMonths(niftyBars.map { it.close }, 3)
        val avgVol20 = averageVolume(volumes, 20)
        val todayVolume = volumes.last()

        val trendOk = ema50 != null && ema200 != null && price > ema50 && ema50 > ema200
        val relativeStrengthOk = stockReturn3m != null && niftyReturn3m != null &&
            stockReturn3m > niftyReturn3m
        val volumeOk = avgVol20 != null && todayVolume > avgVol20

        val qualifies = trendOk && relativeStrengthOk && volumeOk

        return ScreenerResult(
            price = price,
            ema50 = ema50,
            ema200 = ema200,
            stock3MonthReturnPct = stockReturn3m,
            nifty3MonthReturnPct = niftyReturn3m,
            volume = todayVolume,
            avgVolume20d = avgVol20,
            qualifies = qualifies
        )
    }
}
