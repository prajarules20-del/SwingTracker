package com.omprakash.swingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar day, recording the total paper-portfolio value at the
 * time of the last screener run that day. dateKey is "yyyyMMdd" so re-running
 * the worker multiple times in a day just updates today's row instead of
 * creating duplicates - you get roughly one data point per day over time.
 */
@Entity(tableName = "portfolio_snapshots")
data class PortfolioSnapshotEntity(
    @PrimaryKey val dateKey: String,
    val totalValue: Double,
    val totalInvested: Double,
    val epochMillis: Long
)
