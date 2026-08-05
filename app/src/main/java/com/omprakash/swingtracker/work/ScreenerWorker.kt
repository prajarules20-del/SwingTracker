package com.omprakash.swingtracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omprakash.swingtracker.data.AppDatabase
import com.omprakash.swingtracker.data.PortfolioSnapshotEntity
import com.omprakash.swingtracker.domain.ScreenerEngine
import com.omprakash.swingtracker.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Runs periodically (see WorkScheduler). For every stock in the watchlist:
 *   1. Fetches fresh price data and runs it through the screener rules
 *   2. Caches the result in Room so the dashboard can show it instantly
 *   3. Fires a notification the moment a stock starts qualifying
 *      (i.e. only on the transition from not-qualifying -> qualifying,
 *      so you're not re-notified every single run while it stays true)
 *   4. Records today's total paper-portfolio value for the history chart
 */
class ScreenerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.get(applicationContext)
        val stockDao = db.stockDao()
        val stocks = stockDao.getAllOnce()

        if (stocks.isNotEmpty()) {
            val niftyBars = try {
                ScreenerEngine.fetchNiftyBars()
            } catch (e: Exception) {
                // Can't screen anything without the index for comparison - retry later.
                return@withContext Result.retry()
            }

            for (stock in stocks) {
                try {
                    val result = ScreenerEngine.evaluate(stock.symbol, niftyBars)

                    val wasQualifying = stock.lastQualifies
                    val updated = stock.copy(
                        lastPrice = result.price,
                        lastEma50 = result.ema50,
                        lastEma200 = result.ema200,
                        last3MonthReturnPct = result.stock3MonthReturnPct,
                        lastNifty3MonthReturnPct = result.nifty3MonthReturnPct,
                        lastVolume = result.volume,
                        lastAvgVolume20d = result.avgVolume20d,
                        lastQualifies = result.qualifies,
                        lastCheckedEpochMillis = System.currentTimeMillis(),
                        lastError = null
                    )
                    stockDao.update(updated)

                    if (result.qualifies && !wasQualifying) {
                        NotificationHelper.notifyMatch(applicationContext, stock.symbol, result.price)
                    }
                } catch (e: Exception) {
                    stockDao.update(
                        stock.copy(
                            lastCheckedEpochMillis = System.currentTimeMillis(),
                            lastError = e.message ?: "Unknown error"
                        )
                    )
                }
            }
        }

        recordPortfolioSnapshot(db)

        Result.success()
    }

    /** Writes (or updates) today's total portfolio value for the history chart. */
    private suspend fun recordPortfolioSnapshot(db: AppDatabase) {
        val holdings = db.holdingDao().observeAll()
        // observeAll() is a Flow; take a single current snapshot of it here.
        val currentHoldings = holdings.first()
        if (currentHoldings.isEmpty()) return

        val latestPrices = db.stockDao().getAllOnce().associate { it.symbol to it.lastPrice }

        var totalValue = 0.0
        var totalInvested = 0.0
        for (holding in currentHoldings) {
            val price = latestPrices[holding.symbol] ?: holding.avgBuyPrice
            totalValue += price * holding.quantity
            totalInvested += holding.avgBuyPrice * holding.quantity
        }

        val now = System.currentTimeMillis()
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))

        db.portfolioSnapshotDao().upsert(
            PortfolioSnapshotEntity(
                dateKey = dateKey,
                totalValue = totalValue,
                totalInvested = totalInvested,
                epochMillis = now
            )
        )
    }
}
