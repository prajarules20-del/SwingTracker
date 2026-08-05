package com.omprakash.swingtracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * A minimal line chart for a plain list of prices - no library needed, just
 * Compose's Canvas. Used for the stock detail screen's price chart across
 * all time periods (1D through All).
 */
@Composable
fun PriceLineChart(values: List<Double>, lineColor: Color, modifier: Modifier = Modifier) {
    if (values.size < 2) return

    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp)) {
        val stepX = size.width / (values.size - 1)

        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - minValue) / range
            val y = size.height - (normalized * size.height).toFloat()
            Offset(x, y)
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
