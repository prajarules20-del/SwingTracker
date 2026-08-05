package com.omprakash.swingtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omprakash.swingtracker.domain.ChartPeriod
import com.omprakash.swingtracker.domain.StockSymbolInfo
import com.omprakash.swingtracker.network.StockChartData
import com.omprakash.swingtracker.network.SymbolNotFoundException
import com.omprakash.swingtracker.network.YahooFinanceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Loaded(val data: StockChartData) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StockDetailScreen(
    symbolInfo: StockSymbolInfo,
    isInWatchlist: Boolean,
    onBack: () -> Unit,
    onToggleWatchlist: (StockSymbolInfo) -> Unit
) {
    var period by remember { mutableStateOf(ChartPeriod.ONE_DAY) }
    var uiState by remember { mutableStateOf<DetailUiState>(DetailUiState.Loading) }

    LaunchedEffect(symbolInfo.symbol, period) {
        uiState = DetailUiState.Loading
        while (true) {
            uiState = try {
                val data = withContext(Dispatchers.IO) {
                    YahooFinanceClient.fetchChartData(
                        "${symbolInfo.symbol}.NS",
                        range = period.range,
                        interval = period.interval
                    )
                }
                DetailUiState.Loaded(data)
            } catch (e: SymbolNotFoundException) {
                DetailUiState.Error("No data available for ${symbolInfo.symbol}")
            } catch (e: Exception) {
                DetailUiState.Error(e.message ?: "Couldn't load chart data")
            }
            // Auto-refresh every 30 seconds while this screen stays open -
            // restarts automatically (via the LaunchedEffect keys above)
            // whenever the symbol or selected period changes.
            delay(30_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to search")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleWatchlist(symbolInfo) }) {
                        Icon(
                            if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isInWatchlist) "Remove from watchlist" else "Add to watchlist"
                        )
                    }
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Search, contentDescription = "Search another stock")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "${symbolInfo.symbol} · NSE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                symbolInfo.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is DetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                is DetailUiState.Loaded -> {
                    val data = state.data
                    val isUp = data.change >= 0
                    val changeColor = if (isUp) Color(0xFF1B8A3A) else Color(0xFFC62828)
                    val sign = if (isUp) "+" else ""

                    Text(
                        "₹${"%.2f".format(data.currentPrice)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$sign${"%.2f".format(data.change)} (${"%.2f".format(data.changePercent)}%)",
                            color = changeColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            period.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChartPeriod.entries.forEach { option ->
                            FilterChip(
                                selected = period == option,
                                onClick = { period = option },
                                label = {
                                    Text(
                                        option.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PriceLineChart(
                        values = data.points.map { it.close },
                        lineColor = changeColor
                    )
                }
            }
        }
    }
}
