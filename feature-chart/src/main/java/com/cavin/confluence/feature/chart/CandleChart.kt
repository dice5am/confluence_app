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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.data.model.Candle
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * MOB-2.1 / 2.7 — Compose Canvas OHLC + optional volume strip.
 *
 * - Horizontal pan + pinch-zoom on the time axis
 * - Long-press drag → crosshair + nearest candle callback
 * - Draws the visible index window only (no full-series copy per frame)
 * - MOB-2.7: collapsible volume histogram (~22% bottom)
 *
 * TODO(MOB-2.5): live append without full rebuild
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    showVolume: Boolean = true,
    seriesKey: String = "",
    onCrosshairCandle: (Candle?) -> Unit = {},
) {
    if (candles.isEmpty()) return

    val chartColors = ConfluenceThemeAccess.chartColors
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val minCandleWidthPx = with(density) { 4.dp.toPx() }
    val maxCandleWidthPx = with(density) { 28.dp.toPx() }

    var startIndex by remember(seriesKey, candles.size) {
        mutableIntStateOf(max(0, candles.size - 48))
    }
    var candleWidthPx by remember(seriesKey) {
        mutableFloatStateOf(with(density) { 10.dp.toPx() })
    }
    var crosshairX by remember { mutableStateOf<Float?>(null) }
    var drawEpoch by remember { mutableIntStateOf(0) }

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
            .pointerInput(seriesKey, candles.size) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                    val cw = (candleWidthPx * zoom).coerceIn(minCandleWidthPx, maxCandleWidthPx)
                    val count = visibleCount(width, cw)
                    val shiftCandles = (-pan.x / cw).roundToInt()
                    candleWidthPx = cw
                    startIndex = clampWindow(startIndex + shiftCandles, count)
                    crosshairX = null
                    onCrosshairCandle(null)
                    drawEpoch++
                }
            }
            .pointerInput(seriesKey, candles.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        crosshairX = offset.x
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val count = visibleCount(width, candleWidthPx)
                        val start = clampWindow(startIndex, count)
                        onCrosshairCandle(candleAtX(offset.x, start, count, candleWidthPx))
                        drawEpoch++
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val x = change.position.x
                        crosshairX = x
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val count = visibleCount(width, candleWidthPx)
                        val start = clampWindow(startIndex, count)
                        onCrosshairCandle(candleAtX(x, start, count, candleWidthPx))
                        drawEpoch++
                    },
                    onDragEnd = { /* keep selection until Clear / pan-zoom */ },
                    onDragCancel = {
                        crosshairX = null
                        onCrosshairCandle(null)
                        drawEpoch++
                    },
                )
            },
    ) {
        @Suppress("UNUSED_EXPRESSION")
        drawEpoch

        val width = size.width
        val height = size.height
        val volFrac = if (showVolume) 0.22f else 0f
        val priceHeight = height * (1f - volFrac)
        val volTop = priceHeight
        val volHeight = height - priceHeight

        val cw = candleWidthPx
        val count = visibleCount(width, cw)
        val start = clampWindow(startIndex, count)
        val end = min(candles.size, start + count)
        if (start >= end) return@Canvas

        var lo = Float.POSITIVE_INFINITY
        var hi = Float.NEGATIVE_INFINITY
        var maxVol = 0.0
        for (i in start until end) {
            val c = candles[i]
            lo = min(lo, c.low.toFloat())
            hi = max(hi, c.high.toFloat())
            if (c.volume > maxVol) maxVol = c.volume
        }
        val pad = ((hi - lo) * 0.08f).coerceAtLeast(1f)
        lo -= pad
        hi += pad
        val span = (hi - lo).coerceAtLeast(1e-3f)

        fun yFor(price: Float): Float = priceHeight - ((price - lo) / span) * priceHeight

        val gridColor = chartColors.grid
        for (i in 0..3) {
            val gy = priceHeight * i / 4f
            drawLine(gridColor, Offset(0f, gy), Offset(width, gy), strokeWidth = 1f)
        }

        val bodyFrac = 0.62f
        for (i in start until end) {
            val c = candles[i]
            val slot = i - start
            val cx = slot * cw + cw / 2f
            val bull = c.close >= c.open
            val color = if (bull) chartColors.bull else chartColors.bear
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

        if (showVolume && volHeight > 1f && maxVol > 0.0) {
            drawLine(gridColor, Offset(0f, volTop), Offset(width, volTop), strokeWidth = 1f)
            val barW = cw * 0.7f
            for (i in start until end) {
                val c = candles[i]
                val slot = i - start
                val cx = slot * cw + cw / 2f
                val bull = c.close >= c.open
                val color = if (bull) chartColors.bull else chartColors.bear
                val h = (c.volume / maxVol).toFloat().coerceIn(0f, 1f) * (volHeight - 2f)
                drawRect(
                    color = color.copy(alpha = 0.45f),
                    topLeft = Offset(cx - barW / 2f, height - h),
                    size = Size(barW, h.coerceAtLeast(1f)),
                )
            }
        }

        val xh = crosshairX
        if (xh != null) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(
                chartColors.crosshair,
                Offset(xh, 0f),
                Offset(xh, height),
                strokeWidth = 1.5f,
                pathEffect = dash,
            )
            val c = candleAtX(xh, start, count, cw)
            if (c != null) {
                val y = yFor(c.close.toFloat())
                drawCircle(chartColors.crosshair, radius = 5f, center = Offset(xh, y))
            }
        }
    }
}
