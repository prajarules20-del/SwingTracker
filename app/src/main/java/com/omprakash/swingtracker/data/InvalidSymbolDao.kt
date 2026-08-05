package com.omprakash.swingtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvalidSymbolDao {

    @Query("SELECT * FROM invalid_symbols ORDER BY symbol ASC")
    fun observeAll(): Flow<List<InvalidSymbolEntity>>

    @Query("SELECT * FROM invalid_symbols ORDER BY symbol ASC")
    suspend fun getAllOnce(): List<InvalidSymbolEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InvalidSymbolEntity)

    @Query("DELETE FROM invalid_symbols")
    suspend fun clearAll()
}
