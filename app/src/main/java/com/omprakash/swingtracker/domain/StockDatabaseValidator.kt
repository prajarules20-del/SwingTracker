package com.omprakash.swingtracker.domain

import android.content.Context
import com.omprakash.swingtracker.network.SymbolNotFoundException
import com.omprakash.swingtracker.network.YahooFinanceClient
import kotlinx.coroutines.delay

/** One result from checking a single bundled symbol. */
data class SymbolCheckResult(
    val symbol: String,
    val name: String,
    val isValid: Boolean
)

/**
 * Checks every symbol in the bundled NSE list against Yahoo Finance, one at a
 * time with a short delay between requests so we don't hammer the endpoint.
 * This can only detect that a symbol is broken (404/no data) - it can't
 * figure out what a renamed company's new ticker is, since that needs
 * outside research. Broken symbols get hidden from search until the
 * bundled list itself is corrected in a future update.
 */
object StockDatabaseValidator {

    /**
     * Runs the check over all bundled symbols. [onProgress] is called after
     * each symbol with (checked count, total count). Returns only the
     * symbols that failed.
     */
    suspend fun validateAll(
        context: Context,
        onProgress: (checked: Int, total: Int) -> Unit
    ): List<SymbolCheckResult> {
        val all = StockSymbols.loadAll(context)
        val failures = mutableListOf<SymbolCheckResult>()

        for ((index, info) in all.withIndex()) {
            val isValid = try {
                YahooFinanceClient.fetchDailyBars("${info.symbol}.NS", range = "5d")
                true
            } catch (e: SymbolNotFoundException) {
                false
            } catch (e: Exception) {
                // Network hiccup or timeout - don't flag as broken on an
                // inconclusive result, just skip it this run.
                true
            }

            if (!isValid) {
                failures.add(SymbolCheckResult(info.symbol, info.name, isValid = false))
            }

            onProgress(index + 1, all.size)

            // Be polite to Yahoo's public endpoint - a small gap between
            // ~400+ sequential requests avoids tripping rate limits.
            delay(200)
        }

        return failures
    }
}
