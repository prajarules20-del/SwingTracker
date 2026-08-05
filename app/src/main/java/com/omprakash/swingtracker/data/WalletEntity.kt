package com.omprakash.swingtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table (id is always 1) holding the paper-trading wallet balance.
 * This is virtual/play money only - no real currency is ever involved.
 */
@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val id: Int = 1,
    val balance: Double = 0.0
)
