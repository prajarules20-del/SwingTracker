package com.omprakash.swingtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.omprakash.swingtracker.data.PortfolioSnapshotEntity

/**
 * A minimal line chart of total portfolio value over time - no external
 * charting library needed, just Compose's Canvas. Shows one point per
 * recorded day (see ScreenerWorker.recordPortfolioSnapshot).
 */
@Composable
fun PortfolioHistoryChart(history: List<PortfolioSnapshotEntity>, modifier: Modifier = Modifier) {
    if (history.size < 2) return // Nothing meaningful to plot yet.

    val lineColor = MaterialTheme.colorScheme.primary
    val values = history.map { it.totalValue }
    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val stepX = size.width / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - minValue) / range
            // Invert Y since Canvas origin is top-left but higher value should be higher up.
            val y = size.height - (normalized * size.height).toFloat()
            Offset(x, y.toFloat())
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = lineColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
    }
}
