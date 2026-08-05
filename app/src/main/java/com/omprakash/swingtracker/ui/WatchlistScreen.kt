package com.omprakash.swingtracker.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omprakash.swingtracker.R
import com.omprakash.swingtracker.data.StockEntity
import com.omprakash.swingtracker.domain.StockSymbolInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppTab { HOME, WATCHLIST, PORTFOLIO, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel = viewModel()) {
    val stocks by viewModel.stocks.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val portfolio by viewModel.portfolio.collectAsState()
    val portfolioHistory by viewModel.portfolioHistory.collectAsState()
    val invalidSymbols by viewModel.invalidSymbols.collectAsState()
    val databaseCheckState by viewModel.databaseCheckState.collectAsState()
    val closedTrades by viewModel.closedTrades.collectAsState()
    val tradeStats by viewModel.tradeStats.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCoinsDialog by remember { mutableStateOf(false) }
    var buyDialogStock by remember { mutableStateOf<StockEntity?>(null) }
    var sellDialogItem by remember { mutableStateOf<PortfolioItem?>(null) }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    var addStockQuery by remember { mutableStateOf("") }
    var stockDetailTarget by remember { mutableStateOf<StockSymbolInfo?>(null) }

    if (stockDetailTarget != null) {
        val target = stockDetailTarget!!
        StockDetailScreen(
            symbolInfo = target,
            isInWatchlist = stocks.any { it.symbol == target.symbol },
            onBack = {
                stockDetailTarget = null
                showAddDialog = true
            },
            onToggleWatchlist = { info ->
                val existing = stocks.find { it.symbol == info.symbol }
                if (existing != null) viewModel.removeStock(existing) else viewModel.addStock(info.symbol)
            }
        )
        return
    }

    if (showSettings) {
        SettingsScreen(
            invalidSymbols = invalidSymbols,
            onBack = { showSettings = false },
            onUpdateStockDatabase = { viewModel.runDatabaseCheck() }
        )

        when (val state = databaseCheckState) {
            is DatabaseCheckState.Checking ->
                DatabaseCheckProgressDialog(checked = state.checked, total = state.total)
            is DatabaseCheckState.Done ->
                DatabaseCheckResultDialog(
                    newlyBrokenCount = state.newlyBrokenCount,
                    onDismiss = { viewModel.clearDatabaseCheckState() }
                )
            null -> {}
        }

        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Swing Tracker")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search and add a stock")
                    }
                    IconButton(onClick = { viewModel.refreshNow() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh now")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            WalletCard(
                balance = walletBalance,
                onAddCoins = { showAddCoinsDialog = true }
            )

            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = tab == AppTab.HOME,
                    onClick = { tab = AppTab.HOME },
                    text = { Text("Home") }
                )
                Tab(
                    selected = tab == AppTab.WATCHLIST,
                    onClick = { tab = AppTab.WATCHLIST },
                    text = { Text("Watchlist") }
                )
                Tab(
                    selected = tab == AppTab.PORTFOLIO,
                    onClick = { tab = AppTab.PORTFOLIO },
                    text = { Text("Portfolio") }
                )
                Tab(
                    selected = tab == AppTab.HISTORY,
                    onClick = { tab = AppTab.HISTORY },
                    text = { Text("History") }
                )
            }

            when (tab) {
                AppTab.HOME -> HomeTab()
                AppTab.WATCHLIST -> WatchlistTab(
                    stocks = stocks,
                    onRemove = { viewModel.removeStock(it) },
                    onBuy = { buyDialogStock = it }
                )
                AppTab.PORTFOLIO -> PortfolioTab(
                    portfolio = portfolio,
                    history = portfolioHistory,
                    onSell = { sellDialogItem = it }
                )
                AppTab.HISTORY -> HistoryTab(trades = closedTrades, stats = tradeStats)
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            viewModel = viewModel,
            query = addStockQuery,
            onQueryChange = { addStockQuery = it },
            onDismiss = { showAddDialog = false },
            onAdd = { symbol ->
                viewModel.addStock(symbol)
                showAddDialog = false
            },
            onSuggestionTap = { suggestion ->
                showAddDialog = false
                stockDetailTarget = suggestion
            }
        )
    }

    if (showAddCoinsDialog) {
        AddCoinsDialog(
            onDismiss = { showAddCoinsDialog = false },
            onAdd = { amount ->
                viewModel.addCoins(amount)
                showAddCoinsDialog = false
            }
        )
    }

    buyDialogStock?.let { stock ->
        BuyStockDialog(
            stock = stock,
            walletBalance = walletBalance,
            onDismiss = { buyDialogStock = null },
            onBuy = { quantity, price, onResult ->
                viewModel.buyStock(stock.symbol, quantity, price, onResult)
            }
        )
    }

    sellDialogItem?.let { item ->
        SellStockDialog(
            item = item,
            onDismiss = { sellDialogItem = null },
            onSell = { quantity, price, onResult ->
                viewModel.sellStock(item.symbol, quantity, price, onResult)
            }
        )
    }
}

@Composable
private fun WalletCard(balance: Double, onAddCoins: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Paper wallet", style = MaterialTheme.typography.labelMedium)
                Text(
                    "₹%.2f".format(balance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = onAddCoins) { Text("Add coins") }
        }
    }
}

@Composable
private fun WatchlistTab(
    stocks: List<StockEntity>,
    onRemove: (StockEntity) -> Unit,
    onBuy: (StockEntity) -> Unit
) {
    if (stocks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No stocks yet. Tap the search icon above to add one, e.g. RELIANCE, TCS, INFY.",
                modifier = Modifier.padding(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stocks, key = { it.symbol }) { stock ->
                StockRow(stock = stock, onRemove = { onRemove(stock) }, onBuy = { onBuy(stock) })
            }
        }
    }
}

@Composable
private fun PortfolioTab(
    portfolio: List<PortfolioItem>,
    history: List<com.omprakash.swingtracker.data.PortfolioSnapshotEntity>,
    onSell: (PortfolioItem) -> Unit
) {
    if (portfolio.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No holdings yet. Buy a stock from the Watchlist tab using your paper wallet.",
                modifier = Modifier.padding(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (history.size >= 2) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Portfolio value over time",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PortfolioHistoryChart(history = history)
                    }
                }
            }
        }

        items(portfolio, key = { it.symbol }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onSell(item) }, enabled = item.currentPrice != null) {
                            Text("Sell")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Qty ${"%.2f".format(item.quantity)}  ·  Avg buy ₹${"%.2f".format(item.avgBuyPrice)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (item.currentPrice != null) {
                        Text(
                            "Current ₹${"%.2f".format(item.currentPrice)}  ·  Invested ₹${"%.2f".format(item.investedValue)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        val pl = item.profitLoss ?: 0.0
                        val plPct = item.profitLossPct ?: 0.0
                        val color = if (pl >= 0) Color3(0xFF1B8A3A) else Color3(0xFFC62828)
                        val sign = if (pl >= 0) "+" else ""
                        Text(
                            "$sign₹${"%.2f".format(pl)} (${"%.1f".format(plPct)}%)",
                            color = color,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text("Waiting for price data...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Small helper so we don't need an extra import alias juggling act above. */
private fun Color3(argb: Long) = androidx.compose.ui.graphics.Color(argb)

@Composable
private fun StockRow(stock: StockEntity, onRemove: () -> Unit, onBuy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(stock)
                    IconButton(onClick = onBuy, enabled = stock.lastPrice != null) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Buy ${stock.symbol}")
                    }
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove ${stock.symbol}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (stock.lastError != null) {
                Text(
                    "Error: ${stock.lastError}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (stock.lastCheckedEpochMillis == null) {
                Text("Waiting for first check...", style = MaterialTheme.typography.bodySmall)
            } else {
                Text(
                    "Price ₹${"%.2f".format(stock.lastPrice ?: 0.0)}  ·  " +
                        "50EMA ₹${stock.lastEma50?.let { "%.2f".format(it) } ?: "-"}  ·  " +
                        "200EMA ₹${stock.lastEma200?.let { "%.2f".format(it) } ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "3mo return: ${stock.last3MonthReturnPct?.let { "%.1f%%".format(it) } ?: "-"}  " +
                        "vs Nifty: ${stock.lastNifty3MonthReturnPct?.let { "%.1f%%".format(it) } ?: "-"}",
                    style = MaterialTheme.typography.bodySmall
                )
                val lastChecked = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
                    .format(Date(stock.lastCheckedEpochMillis))
                Text(
                    "Last checked: $lastChecked",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(stock: StockEntity) {
    if (stock.lastCheckedEpochMillis == null || stock.lastError != null) return

    val (label, containerColor) = if (stock.lastQualifies) {
        "MATCH" to MaterialTheme.colorScheme.primaryContainer
    } else {
        "watching" to MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStockDialog(
    viewModel: WatchlistViewModel,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onSuggestionTap: (StockSymbolInfo) -> Unit
) {
    var suggestions by remember { mutableStateOf<List<StockSymbolInfo>>(emptyList()) }

    // Recompute suggestions if the dialog reopens with text already in the
    // field (e.g. coming back from the stock detail screen via Back).
    LaunchedEffect(Unit) {
        suggestions = if (query.isNotBlank()) viewModel.searchSymbols(query) else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add stock to watchlist") },
        text = {
            Column {
                Text(
                    "Type an NSE symbol or company name.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        suggestions = if (it.isNotBlank()) viewModel.searchSymbols(it) else emptyList()
                    },
                    label = { Text("e.g. RELIANCE or Reliance") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        suggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionTap(suggestion) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(suggestion.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Stock · ${suggestion.symbol}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(query) }, enabled = query.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddCoinsDialog(onDismiss: () -> Unit, onAdd: (Double) -> Unit) {
    var text by remember { mutableStateOf("") }
    val amount = text.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add coins to wallet") },
        text = {
            Column {
                Text(
                    "Virtual coins for paper trading only - not real money.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { amount?.let { onAdd(it) } }, enabled = amount != null && amount > 0) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BuyStockDialog(
    stock: StockEntity,
    walletBalance: Double,
    onDismiss: () -> Unit,
    onBuy: (quantity: Double, price: Double, onResult: (BuyResult) -> Unit) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val price = stock.lastPrice ?: 0.0
    val quantity = text.toDoubleOrNull()
    val cost = quantity?.let { it * price }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buy ${stock.symbol} (paper trade)") },
        text = {
            Column {
                Text("Price: ₹%.2f  ·  Wallet: ₹%.2f".format(price, walletBalance), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; errorMessage = null },
                    label = { Text("Quantity") },
                    singleLine = true
                )
                if (cost != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total cost: ₹%.2f".format(cost), style = MaterialTheme.typography.bodySmall)
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    quantity?.let { qty ->
                        onBuy(qty, price) { result ->
                            when (result) {
                                is BuyResult.Success -> onDismiss()
                                is BuyResult.Error -> errorMessage = result.message
                            }
                        }
                    }
                },
                enabled = quantity != null && quantity > 0
            ) {
                Text("Buy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SellStockDialog(
    item: PortfolioItem,
    onDismiss: () -> Unit,
    onSell: (quantity: Double, price: Double, onResult: (SellResult) -> Unit) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val price = item.currentPrice ?: 0.0
    val quantity = text.toDoubleOrNull()
    val proceeds = quantity?.let { it * price }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sell ${item.symbol} (paper trade)") },
        text = {
            Column {
                Text(
                    "Price: ₹%.2f  ·  You hold: %.2f".format(price, item.quantity),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; errorMessage = null },
                    label = { Text("Quantity to sell") },
                    singleLine = true
                )
                if (proceeds != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Proceeds: ₹%.2f".format(proceeds), style = MaterialTheme.typography.bodySmall)
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                successMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color3(0xFF1B8A3A), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    quantity?.let { qty ->
                        onSell(qty, price) { result ->
                            when (result) {
                                is SellResult.Success -> {
                                    successMessage = "Sold - realized P&L ₹%.2f".format(result.realizedPnl)
                                }
                                is SellResult.Error -> errorMessage = result.message
                            }
                        }
                    }
                },
                enabled = quantity != null && quantity > 0 && successMessage == null
            ) {
                Text("Sell")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (successMessage != null) "Done" else "Cancel") }
        }
    )
}
