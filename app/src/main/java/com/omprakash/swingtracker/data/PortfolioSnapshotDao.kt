package com.omprakash.swingtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioSnapshotDao {

    @Query("SELECT * FROM portfolio_snapshots ORDER BY epochMillis ASC")
    fun observeAll(): Flow<List<PortfolioSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: PortfolioSnapshotEntity)
}
