package com.omprakash.swingtracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omprakash.swingtracker.data.ClosedTradeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryTab(trades: List<ClosedTradeEntity>, stats: TradeStats) {
    if (trades.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No closed trades yet. Sell a holding from the Portfolio tab to see it here.",
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
        item { StatsCard(stats) }

        items(trades, key = { it.id }) { trade ->
            TradeRow(trade)
        }
    }
}

@Composable
private fun StatsCard(stats: TradeStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(label = "Trades", value = stats.totalTrades.toString())
            StatColumn(label = "Win rate", value = "%.0f%%".format(stats.winRatePercent))
            StatColumn(
                label = "Total P&L",
                value = "₹%.2f".format(stats.totalRealizedPnl),
                color = if (stats.totalRealizedPnl >= 0) Color(0xFF1B8A3A) else Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun TradeRow(trade: ClosedTradeEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(trade.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val isWin = trade.realizedPnl >= 0
                val color = if (isWin) Color(0xFF1B8A3A) else Color(0xFFC62828)
                val sign = if (isWin) "+" else ""
                Text(
                    "$sign₹${"%.2f".format(trade.realizedPnl)}",
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Qty ${"%.2f".format(trade.quantity)}  ·  Bought ₹${"%.2f".format(trade.buyPrice)}  ·  Sold ₹${"%.2f".format(trade.sellPrice)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(trade.closedAtEpochMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
