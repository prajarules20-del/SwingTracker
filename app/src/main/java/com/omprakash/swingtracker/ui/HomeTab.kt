package com.omprakash.swingtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omprakash.swingtracker.domain.MarketIndexInfo
import com.omprakash.swingtracker.domain.MarketIndices
import com.omprakash.swingtracker.network.StockChartData
import com.omprakash.swingtracker.network.YahooFinanceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun HomeTab() {
    // symbol -> latest fetched data, or null while still loading / on error
    var indexData by remember { mutableStateOf<Map<String, StockChartData?>>(emptyMap()) }

    LaunchedEffect(Unit) {
        while (true) {
            for (index in MarketIndices.all) {
                try {
                    val data = withContext(Dispatchers.IO) {
                        YahooFinanceClient.fetchChartData(index.symbol, range = "1d", interval = "5m")
                    }
                    indexData = indexData + (index.symbol to data)
                } catch (e: Exception) {
                    // Keep whatever we last had for this index rather than clearing it.
                }
                delay(300)
            }
            delay(30_000)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MarketIndices.all, key = { it.symbol }) { index ->
            IndexRow(index = index, data = indexData[index.symbol])
        }
    }
}

@Composable
private fun IndexRow(index: MarketIndexInfo, data: StockChartData?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(index.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (data == null) {
                Text("Loading...", style = MaterialTheme.typography.bodySmall)
            } else {
                val isUp = data.change >= 0
                val changeColor = if (isUp) Color(0xFF1B8A3A) else Color(0xFFC62828)
                val sign = if (isUp) "+" else ""

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%.2f".format(data.currentPrice),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "$sign${"%.2f".format(data.change)} (${"%.2f".format(data.changePercent)}%)",
                        color = changeColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
