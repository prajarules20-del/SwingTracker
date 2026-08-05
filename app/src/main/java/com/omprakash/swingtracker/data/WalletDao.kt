package com.omprakash.swingtracker.data

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@androidx.room.Dao
interface WalletDao {

    @Query("SELECT * FROM wallet WHERE id = 1")
    fun observe(): Flow<WalletEntity?>

    @Query("SELECT * FROM wallet WHERE id = 1")
    suspend fun getOnce(): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(wallet: WalletEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)

    @Update
    suspend fun update(wallet: WalletEntity)
}
