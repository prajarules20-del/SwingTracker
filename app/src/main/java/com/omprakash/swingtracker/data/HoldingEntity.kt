package com.omprakash.swingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per stock you're holding in the paper-trading portfolio.
 * quantity and avgBuyPrice update automatically if you buy the same
 * stock more than once (weighted-average cost, same as a real broker).
 */
@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey val symbol: String,
    val quantity: Double,
    val avgBuyPrice: Double
)
