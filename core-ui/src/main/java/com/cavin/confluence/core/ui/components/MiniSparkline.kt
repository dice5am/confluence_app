package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens

@Composable
fun MiniSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val up = values.isNotEmpty() && values.last() >= values.first()
    val stroke = if (up) ConfluenceColors.Mint else ConfluenceColors.Rose
    val fillTop = stroke.copy(alpha = 0.35f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ConfluenceDimens.sparklineHeight),
    ) {
        if (values.size < 2) return@Canvas
        val minV = values.minOrNull() ?: return@Canvas
        val maxV = values.maxOrNull() ?: return@Canvas
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val line = Path()
        val area = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minV) / range) * size.height
            if (i == 0) {
                line.moveTo(x, y)
                area.moveTo(x, size.height)
                area.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(size.width, size.height)
        area.close()
        drawPath(
            path = area,
            brush = Brush.verticalGradient(listOf(fillTop, stroke.copy(alpha = 0f))),
        )
        drawPath(line, color = stroke, style = Stroke(width = 2f, cap = StrokeCap.Round))
        val lastY = size.height - ((values.last() - minV) / range) * size.height
        drawLine(
            color = ConfluenceColors.CyberCyan.copy(alpha = 0.2f),
            start = Offset(0f, lastY),
            end = Offset(size.width, lastY),
            strokeWidth = 1f,
        )
    }
}
