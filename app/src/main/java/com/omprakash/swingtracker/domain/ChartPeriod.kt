package com.omprakash.swingtracker.domain

/**
 * The period tabs shown on the stock detail screen, each mapped to the
 * Yahoo Finance range/interval combination that makes sense for it.
 * Shorter periods use finer intervals (intraday minutes for 1D); longer
 * periods use coarser ones so the response stays a reasonable size.
 */
enum class ChartPeriod(val label: String, val range: String, val interval: String) {
    ONE_DAY("1D", "1d", "5m"),
    ONE_WEEK("1W", "5d", "15m"),
    ONE_MONTH("1M", "1mo", "1d"),
    THREE_MONTH("3M", "3mo", "1d"),
    SIX_MONTH("6M", "6mo", "1d"),
    ONE_YEAR("1Y", "1y", "1d"),
    FIVE_YEAR("5Y", "5y", "1wk"),
    ALL("All", "max", "1mo")
}
