package com.cavin.confluence.feature.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.data.model.Candle
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * MOB-2.1 — Compose Canvas OHLC candle chart.
 *
 * Pan + pinch-zoom on the time axis; long-press drag for crosshair / nearest candle.
 * Draws only the visible window (no per-frame full-series alloc beyond index math).
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    onCrosshairCandle: (Candle?) -> Unit = {},
) {
    if (candles.isEmpty()) return

    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val minCandleWidthPx = with(density) { 4.dp.toPx() }
    val maxCandleWidthPx = with(density) { 28.dp.toPx() }

    // Visible window: start index + how many candles fit
    var startIndex by remember(candles.size) { mutableIntStateOf(max(0, candles.size - 48)) }
    var candleWidthPx by remember { mutableFloatStateOf(with(density) { 10.dp.toPx() }) }
    var crosshairX by remember { mutableStateOf<Float?>(null) }

    fun visibleCount(width: Float, cw: Float): Int {
        if (width <= 0f || cw <= 0f) return 1
        return max(8, floor(width / cw).toInt())
    }

    fun clampWindow(start: Int, count: Int): Int {
        val c = count.coerceAtMost(candles.size)
        return start.coerceIn(0, max(0, candles.size - c))
    }

    fun candleAtX(x: Float, start: Int, count: Int, cw: Float): Candle? {
        if (count <= 0 || cw <= 0f) return null
        val idx = start + (x / cw).toInt().coerceIn(0, count - 1)
        return candles.getOrNull(idx)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasWidthPx = it.width.toFloat() }
            .pointerInput(candles.size) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                    var cw = (candleWidthPx * zoom).coerceIn(minCandleWidthPx, maxCandleWidthPx)
                    val count = visibleCount(width, cw)
                    // pan.x > 0 means drag right → show older (decrease start) in typical charts
                    // we treat drag right as looking at older data
                    val shiftCandles = (-pan.x / cw).roundToInt()
                    val newStart = clampWindow(startIndex + shiftCandles, count)
                    candleWidthPx = cw
                    startIndex = newStart
                    crosshairX = null
                    onCrosshairCandle(null)
                }
            }
            .pointerInput(candles.size, startIndex, candleWidthPx) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        crosshairX = offset.x
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val count = visibleCount(width, candleWidthPx)
                        val start = clampWindow(startIndex, count)
                        onCrosshairCandle(candleAtX(offset.x, start, count, candleWidthPx))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val x = change.position.x
                        crosshairX = x
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val count = visibleCount(width, candleWidthPx)
                        val start = clampWindow(startIndex, count)
                        onCrosshairCandle(candleAtX(x, start, count, candleWidthPx))
                    },
                    onDragEnd = { /* keep last crosshair until next transform */ },
                    onDragCancel = {
                        crosshairX = null
                        onCrosshairCandle(null)
                    },
                )
            },
    ) {
        val width = size.width
        val height = size.height
        val cw = candleWidthPx
        val count = visibleCount(width, cw)
        val start = clampWindow(startIndex, count)
        val end = min(candles.size, start + count)
        if (start >= end) return@Canvas

        val visible = candles.subList(start, end)
        var lo = Float.POSITIVE_INFINITY
        var hi = Float.NEGATIVE_INFINITY
        for (c in visible) {
            lo = min(lo, c.low.toFloat())
            hi = max(hi, c.high.toFloat())
        }
        val pad = ((hi - lo) * 0.08f).coerceAtLeast(1f)
        lo -= pad
        hi += pad
        val span = (hi - lo).coerceAtLeast(1e-3f)

        fun yFor(price: Float): Float = height - ((price - lo) / span) * height

        // light grid
        val gridColor = ConfluenceColors.Grid
        for (i in 0..3) {
            val gy = height * i / 4f
            drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
        }

        val bodyFrac = 0.62f
        for ((i, c) in visible.withIndex()) {
            val cx = i * cw + cw / 2f
            val bull = c.close >= c.open
            val color = if (bull) ConfluenceColors.Bull else ConfluenceColors.Bear
            val yHigh = yFor(c.high.toFloat())
            val yLow = yFor(c.low.toFloat())
            val yOpen = yFor(c.open.toFloat())
            val yClose = yFor(c.close.toFloat())
            drawLine(color, Offset(cx, yHigh), Offset(cx, yLow), strokeWidth = 2f)
            val top = min(yOpen, yClose)
            val bot = max(yOpen, yClose)
            val bodyH = max(bot - top, 2f)
            val bodyW = cw * bodyFrac
            drawRect(
                color = color,
                topLeft = Offset(cx - bodyW / 2f, top),
                size = Size(bodyW, bodyH),
            )
        }

        val xh = crosshairX
        if (xh != null) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(
                ConfluenceColors.Crosshair,
                Offset(xh, 0f),
                Offset(xh, height),
                strokeWidth = 1.5f,
                pathEffect = dash,
            )
            val c = candleAtX(xh, start, count, cw)
            if (c != null) {
                val y = yFor(c.close.toFloat())
                drawCircle(ConfluenceColors.Crosshair, radius = 5f, center = Offset(xh, y))
                drawCircle(Color.Transparent, radius = 5f, center = Offset(xh, y), style = Stroke(2f))
            }
        }
    }
}
