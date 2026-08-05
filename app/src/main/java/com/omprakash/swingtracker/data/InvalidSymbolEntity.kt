package com.omprakash.swingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Recorded when "Update Stock Database" in Settings finds a bundled symbol
 * that Yahoo Finance no longer recognizes (renamed, delisted, merged, etc).
 * These are hidden from search suggestions until the bundled symbol list
 * itself is corrected in a future app update.
 */
@Entity(tableName = "invalid_symbols")
data class InvalidSymbolEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val checkedAtEpochMillis: Long
)
