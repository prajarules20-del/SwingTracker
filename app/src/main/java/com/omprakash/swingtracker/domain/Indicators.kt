package com.omprakash.swingtracker.domain

/**
 * Exponential Moving Average over a list of closes (oldest first).
 * Returns null if there isn't enough data for the given period.
 * The EMA is seeded with a simple average of the first `period` values,
 * which is the standard approach.
 */
fun exponentialMovingAverage(closes: List<Double>, period: Int): Double? {
    if (closes.size < period) return null

    val multiplier = 2.0 / (period + 1)
    var ema = closes.take(period).average() // seed with SMA

    for (i in period until closes.size) {
        ema = (closes[i] - ema) * multiplier + ema
    }
    return ema
}

/** Simple average of the last N volumes. */
fun averageVolume(volumes: List<Long>, period: Int): Long? {
    if (volumes.size < period) return null
    return volumes.takeLast(period).average().toLong()
}

/**
 * Percentage return from `monthsAgo` months back (approx. 21 trading days/month)
 * to the latest close. Returns null if there's not enough history.
 */
fun percentReturnOverMonths(closes: List<Double>, monthsAgo: Int): Double? {
    val tradingDays = monthsAgo * 21
    if (closes.size <= tradingDays) return null
    val past = closes[closes.size - 1 - tradingDays]
    val latest = closes.last()
    if (past == 0.0) return null
    return ((latest - past) / past) * 100.0
}
