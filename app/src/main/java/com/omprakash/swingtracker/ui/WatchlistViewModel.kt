package com.omprakash.swingtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omprakash.swingtracker.data.AppDatabase
import com.omprakash.swingtracker.data.ClosedTradeEntity
import com.omprakash.swingtracker.data.HoldingEntity
import com.omprakash.swingtracker.data.InvalidSymbolEntity
import com.omprakash.swingtracker.data.PortfolioSnapshotEntity
import com.omprakash.swingtracker.data.StockEntity
import com.omprakash.swingtracker.data.WalletEntity
import com.omprakash.swingtracker.domain.StockDatabaseValidator
import com.omprakash.swingtracker.domain.StockSymbolInfo
import com.omprakash.swingtracker.domain.StockSymbols
import com.omprakash.swingtracker.network.YahooFinanceClient
import com.omprakash.swingtracker.work.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A holding combined with the stock's latest cached price, for display. */
data class PortfolioItem(
    val symbol: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double?
) {
    val investedValue: Double get() = quantity * avgBuyPrice
    val currentValue: Double? get() = currentPrice?.let { it * quantity }
    val profitLoss: Double? get() = currentValue?.let { it - investedValue }
    val profitLossPct: Double? get() = currentPrice?.let {
        if (avgBuyPrice == 0.0) null else ((it - avgBuyPrice) / avgBuyPrice) * 100.0
    }
}

/** Result of attempting to buy a stock, shown as a message in the Buy dialog. */
sealed class BuyResult {
    object Success : BuyResult()
    data class Error(val message: String) : BuyResult()
}

/** Result of attempting to sell a stock, shown as a message in the Sell dialog. */
sealed class SellResult {
    data class Success(val proceeds: Double, val realizedPnl: Double) : SellResult()
    data class Error(val message: String) : SellResult()
}

/** Progress/result state for the "Update Stock Database" check in Settings. */
sealed class DatabaseCheckState {
    data class Checking(val checked: Int, val total: Int) : DatabaseCheckState()
    data class Done(val newlyBrokenCount: Int) : DatabaseCheckState()
}

/** Summary stats derived from your closed trade history. */
data class TradeStats(
    val totalTrades: Int,
    val winCount: Int,
    val totalRealizedPnl: Double
) {
    val winRatePercent: Double get() = if (totalTrades == 0) 0.0 else (winCount.toDouble() / totalTrades) * 100.0
}

class WatchlistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.get(application)
    private val stockDao = db.stockDao()
    private val walletDao = db.walletDao()
    private val holdingDao = db.holdingDao()
    private val snapshotDao = db.portfolioSnapshotDao()
    private val invalidSymbolDao = db.invalidSymbolDao()
    private val closedTradeDao = db.closedTradeDao()

    init {
        // Make sure the wallet row exists from first launch, starting at zero -
        // the user adds virtual coins themselves via the wallet card.
        viewModelScope.launch {
            if (walletDao.getOnce() == null) {
                walletDao.insert(WalletEntity(id = 1, balance = 0.0))
            }
        }

        // Lightweight price-only refresh so the watchlist and portfolio show
        // a fresh price every ~30 seconds while the app is open, separate
        // from the heavier 30-minute background screener (which also
        // recalculates EMAs/volume and can run with the app closed).
        viewModelScope.launch {
            while (isActive) {
                refreshLivePricesOnce()
                delay(30_000)
            }
        }
    }

    /** Fetches just the current price for every watchlist stock and updates the cache. */
    private suspend fun refreshLivePricesOnce() {
        val currentStocks = stockDao.getAllOnce()
        for (stock in currentStocks) {
            try {
                val data = withContext(Dispatchers.IO) {
                    YahooFinanceClient.fetchChartData("${stock.symbol}.NS", range = "1d", interval = "5m")
                }
                stockDao.update(
                    stock.copy(lastPrice = data.currentPrice, lastCheckedEpochMillis = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                // Keep the previously cached price if this particular fetch fails -
                // the next 30-second cycle will just try again.
            }
            // Small stagger between requests so a big watchlist doesn't fire
            // everything at once.
            delay(300)
        }
    }

    val stocks: StateFlow<List<StockEntity>> = stockDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletBalance: StateFlow<Double> = walletDao.observe()
        .map { it?.balance ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val portfolio: StateFlow<List<PortfolioItem>> = combine(
        holdingDao.observeAll(),
        stockDao.observeAll()
    ) { holdings, stocks ->
        val priceBySymbol = stocks.associate { it.symbol to it.lastPrice }
        holdings.map { h ->
            PortfolioItem(
                symbol = h.symbol,
                quantity = h.quantity,
                avgBuyPrice = h.avgBuyPrice,
                currentPrice = priceBySymbol[h.symbol]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Historical total portfolio value, one point per day, for the history chart. */
    val portfolioHistory: StateFlow<List<PortfolioSnapshotEntity>> = snapshotDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Bundled symbols the last "Update Stock Database" run found broken. */
    val invalidSymbols: StateFlow<List<InvalidSymbolEntity>> = invalidSymbolDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Full history of closed (sold) trades, most recent first. */
    val closedTrades: StateFlow<List<ClosedTradeEntity>> = closedTradeDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Win rate / total P&L summary derived from closedTrades. */
    val tradeStats: StateFlow<TradeStats> = closedTradeDao.observeAll()
        .map { trades ->
            TradeStats(
                totalTrades = trades.size,
                winCount = trades.count { it.realizedPnl > 0 },
                totalRealizedPnl = trades.sumOf { it.realizedPnl }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TradeStats(0, 0, 0.0))

    private val _databaseCheckState = MutableStateFlow<DatabaseCheckState?>(null)
    val databaseCheckState: StateFlow<DatabaseCheckState?> = _databaseCheckState

    /**
     * Runs the "Update Stock Database" check: pings every bundled symbol
     * against Yahoo Finance and records any that fail so they're hidden
     * from future search suggestions. Safe to call again later - each run
     * replaces the previous results.
     */
    fun runDatabaseCheck() {
        if (_databaseCheckState.value is DatabaseCheckState.Checking) return
        viewModelScope.launch {
            _databaseCheckState.value = DatabaseCheckState.Checking(checked = 0, total = 1)

            val failures = StockDatabaseValidator.validateAll(getApplication()) { checked, total ->
                _databaseCheckState.value = DatabaseCheckState.Checking(checked, total)
            }

            invalidSymbolDao.clearAll()
            val now = System.currentTimeMillis()
            for (failure in failures) {
                invalidSymbolDao.upsert(
                    InvalidSymbolEntity(symbol = failure.symbol, name = failure.name, checkedAtEpochMillis = now)
                )
            }

            _databaseCheckState.value = DatabaseCheckState.Done(newlyBrokenCount = failures.size)
        }
    }

    /** Dismisses the database check result banner/dialog after the user has seen it. */
    fun clearDatabaseCheckState() {
        _databaseCheckState.value = null
    }

    /** Offline autocomplete search against the bundled NSE symbol list, excluding known-broken ones. */
    fun searchSymbols(query: String): List<StockSymbolInfo> =
        StockSymbols.search(
            getApplication(),
            query,
            limit = 5,
            excluded = invalidSymbols.value.map { it.symbol }.toSet()
        )

    /** Adds a stock symbol (uppercased, trimmed) to the watchlist if not already present. */
    fun addStock(rawSymbol: String) {
        val symbol = rawSymbol.trim().uppercase()
        if (symbol.isEmpty()) return
        viewModelScope.launch {
            if (!stockDao.exists(symbol)) {
                stockDao.insert(StockEntity(symbol = symbol))
                WorkScheduler.runOnce(getApplication())
            }
        }
    }

    fun removeStock(stock: StockEntity) {
        viewModelScope.launch { stockDao.delete(stock) }
    }

    fun refreshNow() {
        WorkScheduler.runOnce(getApplication())
    }

    /** Adds virtual coins to the paper-trading wallet. This is play money only. */
    fun addCoins(amount: Double) {
        if (amount <= 0) return
        viewModelScope.launch {
            val wallet = walletDao.getOnce() ?: WalletEntity(id = 1, balance = 0.0)
            walletDao.upsert(wallet.copy(balance = wallet.balance + amount))
        }
    }

    /**
     * "Buys" a stock at its last cached price using wallet coins, for paper trading.
     * Calls [onResult] with the outcome so the UI can show a message.
     */
    fun buyStock(symbol: String, quantity: Double, price: Double, onResult: (BuyResult) -> Unit) {
        if (quantity <= 0) {
            onResult(BuyResult.Error("Enter a quantity greater than zero"))
            return
        }
        viewModelScope.launch {
            val wallet = walletDao.getOnce() ?: WalletEntity(id = 1, balance = 0.0)
            val cost = quantity * price

            if (cost > wallet.balance) {
                onResult(BuyResult.Error("Not enough coins - need ₹%.2f, wallet has ₹%.2f".format(cost, wallet.balance)))
                return@launch
            }

            walletDao.upsert(wallet.copy(balance = wallet.balance - cost))

            val existing = holdingDao.get(symbol)
            if (existing == null) {
                holdingDao.upsert(HoldingEntity(symbol = symbol, quantity = quantity, avgBuyPrice = price))
            } else {
                val totalQty = existing.quantity + quantity
                val totalCost = (existing.quantity * existing.avgBuyPrice) + (quantity * price)
                holdingDao.upsert(
                    existing.copy(quantity = totalQty, avgBuyPrice = totalCost / totalQty)
                )
            }

            onResult(BuyResult.Success)
        }
    }

    /**
     * "Sells" some or all of a holding at its last cached price, crediting the
     * proceeds back to the wallet. Selling the full quantity removes the holding.
     */
    fun sellStock(symbol: String, quantity: Double, price: Double, onResult: (SellResult) -> Unit) {
        if (quantity <= 0) {
            onResult(SellResult.Error("Enter a quantity greater than zero"))
            return
        }
        viewModelScope.launch {
            val holding = holdingDao.get(symbol)
            if (holding == null || quantity > holding.quantity) {
                onResult(SellResult.Error("You only hold ${holding?.quantity ?: 0.0} of $symbol"))
                return@launch
            }

            val proceeds = quantity * price
            val realizedPnl = (price - holding.avgBuyPrice) * quantity

            val wallet = walletDao.getOnce() ?: WalletEntity(id = 1, balance = 0.0)
            walletDao.upsert(wallet.copy(balance = wallet.balance + proceeds))

            val remainingQty = holding.quantity - quantity
            if (remainingQty <= 0.0001) {
                holdingDao.delete(holding)
            } else {
                // avgBuyPrice is unchanged when partially selling - only quantity shrinks.
                holdingDao.update(holding.copy(quantity = remainingQty))
            }

            closedTradeDao.insert(
                ClosedTradeEntity(
                    symbol = symbol,
                    quantity = quantity,
                    buyPrice = holding.avgBuyPrice,
                    sellPrice = price,
                    realizedPnl = realizedPnl,
                    closedAtEpochMillis = System.currentTimeMillis()
                )
            )

            onResult(SellResult.Success(proceeds = proceeds, realizedPnl = realizedPnl))
        }
    }
}
