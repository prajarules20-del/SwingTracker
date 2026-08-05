package com.omprakash.swingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per stock you're tracking.
 * `symbol` should be the NSE ticker WITHOUT the .NS suffix, e.g. "RELIANCE", "TCS".
 * We add ".NS" automatically wherever we call Yahoo Finance.
 *
 * The `last*` fields cache the most recent screener result so the dashboard
 * can show a status instantly without re-hitting the network every time you
 * open the app - they get refreshed by the background worker.
 */
@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey val symbol: String,
    val addedAtEpochMillis: Long = System.currentTimeMillis(),

    // Cached last screener result (nullable until first check runs)
    val lastPrice: Double? = null,
    val lastEma50: Double? = null,
    val lastEma200: Double? = null,
    val last3MonthReturnPct: Double? = null,
    val lastNifty3MonthReturnPct: Double? = null,
    val lastVolume: Long? = null,
    val lastAvgVolume20d: Long? = null,
    val lastQualifies: Boolean = false,
    val lastCheckedEpochMillis: Long? = null,
    val lastError: String? = null
)
