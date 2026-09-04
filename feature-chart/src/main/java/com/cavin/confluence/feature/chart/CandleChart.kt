package com.cavin.confluence.feature.chart

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.data.model.Candle
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Compose Canvas OHLC + volume + **dynamic X/Y axes**.
 *
 * X = time (visible window labels update on pan/zoom/TF via [seriesKey]).
 * Y = price (nice ticks from visible high/low).
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
    val leftPad = with(density) { 56.dp.toPx() }
    val rightPad = with(density) { 10.dp.toPx() }
    val topPad = with(density) { 10.dp.toPx() }
    val bottomPad = with(density) { 26.dp.toPx() }

    var startIndex by remember(seriesKey, candles.size) {
        mutableIntStateOf(max(0, candles.size - 48))
    }
    var candleWidthPx by remember(seriesKey) {
        mutableFloatStateOf(with(density) { 10.dp.toPx() })
    }
    var crosshairX by remember { mutableStateOf<Float?>(null) }
    var drawEpoch by remember { mutableIntStateOf(0) }

    val labelPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ConfluenceColors.OnSurfaceMuted.toArgb()
            textSize = with(density) { 10.dp.toPx() }
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }
    fun plotWidth(totalW: Float): Float = (totalW - leftPad - rightPad).coerceAtLeast(1f)

    fun visibleCount(plotW: Float, cw: Float): Int {
        if (plotW <= 0f || cw <= 0f) return 1
        return max(8, floor(plotW / cw).toInt())
    }

    fun clampWindow(start: Int, count: Int): Int {
        val c = count.coerceAtMost(candles.size)
        return start.coerceIn(0, max(0, candles.size - c))
    }

    fun candleAtPlotX(plotX: Float, start: Int, count: Int, cw: Float): Candle? {
        if (count <= 0 || cw <= 0f) return null
        val idx = start + (plotX / cw).toInt().coerceIn(0, count - 1)
        return candles.getOrNull(idx)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasWidthPx = it.width.toFloat() }
            .pointerInput(seriesKey, candles.size) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                    val pw = plotWidth(width)
                    val cw = (candleWidthPx * zoom).coerceIn(minCandleWidthPx, maxCandleWidthPx)
                    val count = visibleCount(pw, cw)
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
                        val pw = plotWidth(width)
                        val count = visibleCount(pw, candleWidthPx)
                        val start = clampWindow(startIndex, count)
                        val plotX = offset.x - leftPad
                        onCrosshairCandle(candleAtPlotX(plotX, start, count, candleWidthPx))
                        drawEpoch++
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val x = change.position.x
                        crosshairX = x
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val pw = plotWidth(width)
                        val count = visibleCount(pw, candleWidthPx)
                        val start = clampWindow(startIndex, count)
                        onCrosshairCandle(candleAtPlotX(x - leftPad, start, count, candleWidthPx))
                        drawEpoch++
                    },
                    onDragEnd = { },
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
        val plotW = plotWidth(width)
        val plotBottom = height - bottomPad
        val volFrac = if (showVolume) 0.20f else 0f
        val usableH = (plotBottom - topPad).coerceAtLeast(1f)
        val volHeight = usableH * volFrac
        val priceHeight = usableH - volHeight
        val priceTop = topPad
        val volTop = priceTop + priceHeight

        val cw = candleWidthPx
        val count = visibleCount(plotW, cw)
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

        fun yFor(price: Float): Float =
            priceTop + priceHeight - ((price - lo) / span) * priceHeight

        fun xForSlot(slot: Int): Float = leftPad + slot * cw + cw / 2f

        val tf = candles[start].timeframe
        val gridColor = chartColors.grid
        val yTicks = ChartAxisLabels.priceTicks(lo, hi, targetCount = 5)

        // Y grid + labels
        for (price in yTicks) {
            val gy = yFor(price)
            if (gy < priceTop || gy > priceTop + priceHeight) continue
            drawLine(
                gridColor,
                Offset(leftPad, gy),
                Offset(width - rightPad, gy),
                strokeWidth = 1f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                ChartAxisLabels.formatPrice(price),
                4f,
                gy + labelPaint.textSize * 0.35f,
                labelPaint,
            )
        }

        // Plot frame
        drawLine(ConfluenceColors.Primary.copy(alpha = 0.35f), Offset(leftPad, priceTop), Offset(leftPad, plotBottom), strokeWidth = 1.5f)
        drawLine(ConfluenceColors.Primary.copy(alpha = 0.35f), Offset(leftPad, plotBottom), Offset(width - rightPad, plotBottom), strokeWidth = 1.5f)

        val bodyFrac = 0.62f
        for (i in start until end) {
            val c = candles[i]
            val slot = i - start
            val cx = xForSlot(slot)
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
            drawLine(gridColor, Offset(leftPad, volTop), Offset(width - rightPad, volTop), strokeWidth = 1f)
            val barW = cw * 0.7f
            for (i in start until end) {
                val c = candles[i]
                val slot = i - start
                val cx = xForSlot(slot)
                val bull = c.close >= c.open
                val color = if (bull) chartColors.bull else chartColors.bear
                val h = (c.volume / maxVol).toFloat().coerceIn(0f, 1f) * (volHeight - 2f)
                drawRect(
                    color = color.copy(alpha = 0.45f),
                    topLeft = Offset(cx - barW / 2f, plotBottom - h),
                    size = Size(barW, h.coerceAtLeast(1f)),
                )
            }
        }

        // X time labels (update with pan/zoom/TF)
        val slots = ChartAxisLabels.timeSlotIndices(
            visibleCount = end - start,
            minPxBetween = with(density) { 56.dp.toPx() },
            candleWidthPx = cw,
        )
        labelPaint.textAlign = Paint.Align.CENTER
        for (slot in slots) {
            val idx = start + slot
            val c = candles.getOrNull(idx) ?: continue
            val cx = xForSlot(slot)
            drawLine(
                ConfluenceColors.Outline,
                Offset(cx, plotBottom),
                Offset(cx, plotBottom + 4f),
                strokeWidth = 1f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                ChartAxisLabels.formatTime(c.openTimeMs, tf),
                cx,
                height - 6f,
                labelPaint,
            )
        }
        labelPaint.textAlign = Paint.Align.LEFT

        val xh = crosshairX
        if (xh != null && xh >= leftPad && xh <= width - rightPad) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(
                chartColors.crosshair,
                Offset(xh, priceTop),
                Offset(xh, plotBottom),
                strokeWidth = 1.5f,
                pathEffect = dash,
            )
            val c = candleAtPlotX(xh - leftPad, start, count, cw)
            if (c != null) {
                val y = yFor(c.close.toFloat())
                drawCircle(chartColors.crosshair, radius = 5f, center = Offset(xh, y))
                drawLine(
                    chartColors.crosshair.copy(alpha = 0.5f),
                    Offset(leftPad, y),
                    Offset(width - rightPad, y),
                    strokeWidth = 1f,
                    pathEffect = dash,
                )
                drawContext.canvas.nativeCanvas.drawText(
                    ChartAxisLabels.formatPrice(c.close.toFloat()),
                    leftPad + 4f,
                    (y - 6f).coerceAtLeast(priceTop + labelPaint.textSize),
                    labelPaint.apply { color = ConfluenceColors.CyberCyan.toArgb() },
                )
                labelPaint.color = ConfluenceColors.OnSurfaceMuted.toArgb()
            }
        }
    }
}
