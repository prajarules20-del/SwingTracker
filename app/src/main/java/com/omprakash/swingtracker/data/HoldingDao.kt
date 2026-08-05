package com.omprakash.swingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {

    @Query("SELECT * FROM holdings ORDER BY symbol ASC")
    fun observeAll(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE symbol = :symbol")
    suspend fun get(symbol: String): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(holding: HoldingEntity)

    @Update
    suspend fun update(holding: HoldingEntity)

    @Delete
    suspend fun delete(holding: HoldingEntity)
}
