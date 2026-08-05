package com.omprakash.swingtracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omprakash.swingtracker.data.InvalidSymbolEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    invalidSymbols: List<InvalidSymbolEntity>,
    onBack: () -> Unit,
    onUpdateStockDatabase: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Update Stock Database") },
                supportingContent = {
                    Text(
                        "Checks every bundled stock symbol against Yahoo Finance and " +
                            "hides any that no longer work (renamed, delisted, or merged)."
                    )
                },
                leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                modifier = Modifier.clickableListItem(onUpdateStockDatabase)
            )

            HorizontalDivider()

            if (invalidSymbols.isNotEmpty()) {
                Text(
                    "Currently hidden (${invalidSymbols.size})",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(invalidSymbols, key = { it.symbol }) { entry ->
                        ListItem(
                            headlineContent = { Text(entry.symbol, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(entry.name) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No known issues yet. Run a check to verify the current list.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/** Small helper to make a ListItem's whole row tappable. */
private fun Modifier.clickableListItem(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
fun DatabaseCheckProgressDialog(checked: Int, total: Int) {
    AlertDialog(
        onDismissRequest = { /* Not dismissible while running */ },
        title = { Text("Checking stock database") },
        text = {
            Column {
                val progress = if (total > 0) checked / total.toFloat() else 0f
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("$checked of $total checked")
            }
        },
        confirmButton = {}
    )
}

@Composable
fun DatabaseCheckResultDialog(newlyBrokenCount: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check complete") },
        text = {
            Text(
                if (newlyBrokenCount == 0) {
                    "All bundled stocks checked out fine - nothing broken right now."
                } else {
                    "Found $newlyBrokenCount stock(s) that Yahoo Finance no longer recognizes. " +
                        "They're hidden from search now - view them in Settings, and let me know " +
                        "the symbols so I can look up their correct replacement tickers."
                }
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}
