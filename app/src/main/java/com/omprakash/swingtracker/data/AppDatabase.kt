package com.omprakash.swingtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StockEntity::class,
        WalletEntity::class,
        HoldingEntity::class,
        PortfolioSnapshotEntity::class,
        InvalidSymbolEntity::class,
        ClosedTradeEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun walletDao(): WalletDao
    abstract fun holdingDao(): HoldingDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao
    abstract fun invalidSymbolDao(): InvalidSymbolDao
    abstract fun closedTradeDao(): ClosedTradeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swingtracker.db"
                )
                    // No real financial data to preserve across schema bumps in this
                    // paper-trading app, so we rebuild rather than write migrations.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
