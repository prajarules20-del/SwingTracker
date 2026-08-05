package com.omprakash.swingtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClosedTradeDao {

    @Query("SELECT * FROM closed_trades ORDER BY closedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ClosedTradeEntity>>

    @Insert
    suspend fun insert(trade: ClosedTradeEntity)
}
