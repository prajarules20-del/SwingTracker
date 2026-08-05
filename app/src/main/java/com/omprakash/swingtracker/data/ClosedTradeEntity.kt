package com.omprakash.swingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per sell action - a completed (fully or partially closed) paper trade.
 * Created automatically whenever you sell a holding, so the history/stats
 * screen has something to show even for partial sells.
 */
@Entity(tableName = "closed_trades")
data class ClosedTradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val quantity: Double,
    val buyPrice: Double,
    val sellPrice: Double,
    val realizedPnl: Double,
    val closedAtEpochMillis: Long
)
