package com.omprakash.swingtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Query("SELECT * FROM stocks ORDER BY addedAtEpochMillis DESC")
    fun observeAll(): Flow<List<StockEntity>>

    @Query("SELECT * FROM stocks")
    suspend fun getAllOnce(): List<StockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(stock: StockEntity)

    @Update
    suspend fun update(stock: StockEntity)

    @Delete
    suspend fun delete(stock: StockEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM stocks WHERE symbol = :symbol)")
    suspend fun exists(symbol: String): Boolean
}
