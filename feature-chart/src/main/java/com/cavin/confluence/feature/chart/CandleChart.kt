package com.cavin.confluence.feature.chart

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import kotlin.math.max
import kotlin.math.min

/**
 * Optional viewport seed for previews / screenshot proof.
 * Production ChartScreen uses defaults; pan/pinch still own the live viewport.
 * [crosshairIndex] paints the same overlay as a long-press (X1 is still
 * long-press-only for users).
 */
data class ChartViewportSeed(
    val candleWidth: Dp = ConfluenceDimens.chartDefaultCandleWidth,
    val visibleCount: Int = ConfluenceDimens.chartDefaultVisibleCandles,
    val crosshairIndex: Int? = null,
)

/**
 * Compose Canvas OHLC + volume — Approach A craft (TradingView-grade).
 *
 * Right price axis + bottom time axis recompute from the visible window
 * (`seriesKey` / pan / zoom). Crosshair is **long-press only** (X1) and snaps
 * to candle center. Last-price hairline is always on (H1); Snapshot chrome
 * remains the honesty signal that this is frozen history.
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    showVolume: Boolean = true,
    seriesKey: String = "",
    onCrosshairCandle: (Candle?) -> Unit = {},
    viewportSeed: ChartViewportSeed = ChartViewportSeed(),
) {
    if (candles.isEmpty()) return

    val chartColors = ConfluenceThemeAccess.chartColors
    val dimens = ConfluenceDimens
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val minCandleWidthPx = with(density) { dimens.chartMinCandleWidth.toPx() }
    val maxCandleWidthPx = with(density) { dimens.chartMaxCandleWidth.toPx() }
    val leftPad = with(density) { dimens.chartPlotLeftPad.toPx() }
    val rightPad = with(density) { dimens.chartPriceAxisWidth.toPx() }
    val topPad = with(density) { dimens.chartPlotTopPad.toPx() }
    val bottomPad = with(density) { dimens.chartTimeAxisHeight.toPx() }
    val wickStroke = with(density) { dimens.chartWickStroke.toPx() }
    val gridStroke = with(density) { dimens.chartGridStroke.toPx() }
    val hairlineStroke = with(density) { dimens.chartHairlineStroke.toPx() }
    val crosshairStroke = with(density) { dimens.chartCrosshairStroke.toPx() }
    val bloomPx = with(density) { dimens.chartBodyBloom.toPx() }
    val minDojiPx = max(with(density) { dimens.chartMinDojiBody.toPx() }, 2f)
    val axisLabelPx = with(density) { dimens.chartAxisLabelSize.toPx() }
    val tagPadH = with(density) { dimens.chartTagPaddingH.toPx() }
    val tagPadV = with(density) { dimens.chartTagPaddingV.toPx() }
    val tagCorner = with(density) { dimens.chartTagCorner.toPx() }
    val axisTick = with(density) { dimens.chartAxisTick.toPx() }
    val minLabelGap = with(density) { dimens.chartMinTimeLabelGap.toPx() }

    var startOffset by remember(seriesKey) {
        mutableFloatStateOf(ChartGeometry.initialStartOffset(candles.size, viewportSeed.visibleCount))
    }
    var candleWidthPx by remember(seriesKey) {
        mutableFloatStateOf(with(density) { viewportSeed.candleWidth.toPx() })
    }
    var crosshairIndex by remember(seriesKey) { mutableStateOf(viewportSeed.crosshairIndex) }
    var drawEpoch by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewportSeed.crosshairIndex, candles) {
        val idx = viewportSeed.crosshairIndex ?: return@LaunchedEffect
        onCrosshairCandle(candles.getOrNull(idx))
    }

    val axisPaint = remember(axisLabelPx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ConfluenceColors.OnSurfaceMuted.toArgb()
            textSize = axisLabelPx
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
        }
    }
    val tagPaint = remember(axisLabelPx) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = axisLabelPx
            typeface = Typeface.MONOSPACE
            fontFeatureSettings = "tnum"
            textAlign = Paint.Align.CENTER
        }
    }

    fun plotWidth(totalW: Float): Float =
        (totalW - leftPad - rightPad).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("candleChart")
            .onSizeChanged { canvasWidthPx = it.width.toFloat() }
            .pointerInput(seriesKey, candles.size) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                    val pw = plotWidth(width)
                    val oldCw = candleWidthPx
                    val newCw = (oldCw * zoom).coerceIn(minCandleWidthPx, maxCandleWidthPx)
                    startOffset = ChartGeometry.zoomAround(
                        startOffset = startOffset,
                        oldCw = oldCw,
                        newCw = newCw,
                        focusPlotX = centroid.x - leftPad,
                        panX = pan.x,
                        candleCount = candles.size,
                        plotWidth = pw,
                    )
                    candleWidthPx = newCw
                    crosshairIndex = null
                    onCrosshairCandle(null)
                    drawEpoch++
                }
            }
            .pointerInput(seriesKey, candles.size) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val pw = plotWidth(width)
                        val idx = ChartGeometry.slotAtX(
                            canvasX = offset.x,
                            plotLeft = leftPad,
                            plotWidth = pw,
                            candleWidthPx = candleWidthPx,
                            startOffset = startOffset,
                            candleCount = candles.size,
                        )
                        crosshairIndex = idx
                        onCrosshairCandle(candles.getOrNull(idx))
                        drawEpoch++
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val width = canvasWidthPx.takeIf { it > 0f } ?: size.width.toFloat()
                        val pw = plotWidth(width)
                        val idx = ChartGeometry.slotAtX(
                            canvasX = change.position.x,
                            plotLeft = leftPad,
                            plotWidth = pw,
                            candleWidthPx = candleWidthPx,
                            startOffset = startOffset,
                            candleCount = candles.size,
                        )
                        crosshairIndex = idx
                        onCrosshairCandle(candles.getOrNull(idx))
                        drawEpoch++
                    },
                    onDragEnd = { },
                    onDragCancel = {
                        crosshairIndex = null
                        onCrosshairCandle(null)
                        drawEpoch++
                    },
                )
            },
    ) {
        @Suppress("UNUSED_EXPRESSION")
        drawEpoch

        val panes = ChartGeometry.panes(
            width = size.width,
            height = size.height,
            leftPad = leftPad,
            rightPad = rightPad,
            topPad = topPad,
            bottomPad = bottomPad,
            showVolume = showVolume,
        )
        val cw = candleWidthPx
        val win = ChartGeometry.window(
            candleCount = candles.size,
            plotWidth = panes.plotWidth,
            candleWidthPx = cw,
            startOffset = startOffset,
        )
        if (win.startIndex >= win.endIndex) return@Canvas

        var lo = Float.POSITIVE_INFINITY
        var hi = Float.NEGATIVE_INFINITY
        var maxVol = 0.0
        for (i in win.startIndex until win.endIndex) {
            val c = candles[i]
            lo = min(lo, c.low.toFloat())
            hi = max(hi, c.high.toFloat())
            if (c.volume > maxVol) maxVol = c.volume
        }
        val domain = ChartGeometry.yDomain(lo, hi)
        val tf = candles[win.startIndex].timeframe
        val lastIndex = candles.lastIndex
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

        fun yPrice(price: Float): Float =
            ChartGeometry.yFor(price, domain, panes.priceTop, panes.priceHeight)

        fun xSlot(index: Int): Float =
            ChartGeometry.slotCenterX(index, win.startIndex, panes.plotLeft, cw, win.pixelShift)

        val yTicks = ChartAxisLabels.priceTicks(domain.lo, domain.hi, targetCount = 5)
        for (price in yTicks) {
            val gy = yPrice(price)
            if (gy < panes.priceTop || gy > panes.priceBottom) continue
            drawLine(
                color = chartColors.grid,
                start = Offset(panes.plotLeft, gy),
                end = Offset(panes.plotRight, gy),
                strokeWidth = gridStroke,
            )
        }

        if (showVolume && panes.volHeight > 1f) {
            drawLine(
                color = chartColors.grid,
                start = Offset(panes.plotLeft, panes.volTop),
                end = Offset(panes.plotRight, panes.volTop),
                strokeWidth = gridStroke,
            )
        }

        if (showVolume && panes.volHeight > 1f && maxVol > 0.0) {
            val barW = cw * ChartGeometry.bodyFraction
            clipRect(panes.plotLeft, panes.volTop, panes.plotRight, panes.volBottom) {
                for (i in win.startIndex until win.endIndex) {
                    val c = candles[i]
                    val cx = xSlot(i)
                    val bull = c.close >= c.open
                    val color = (if (bull) chartColors.bull else chartColors.bear)
                        .copy(alpha = ChartGeometry.volumeAlpha)
                    val h = (c.volume / maxVol).toFloat().coerceIn(0f, 1f) * (panes.volHeight - 2f)
                    drawRect(
                        color = color,
                        topLeft = Offset(cx - barW / 2f, panes.volBottom - h),
                        size = Size(barW, h.coerceAtLeast(1f)),
                    )
                }
            }
        }

        clipRect(panes.plotLeft, panes.priceTop, panes.plotRight, panes.priceBottom) {
            val bodyW = cw * ChartGeometry.bodyFraction
            for (i in win.startIndex until win.endIndex) {
                val c = candles[i]
                val cx = xSlot(i)
                val isLast = i == lastIndex
                val base = if (c.close >= c.open) chartColors.bull else chartColors.bear
                val color = if (isLast) {
                    base.boosted(dimens.chartLastCandleBoost)
                } else {
                    base.copy(alpha = dimens.chartOtherCandleAlpha)
                }
                val yHigh = yPrice(c.high.toFloat())
                val yLow = yPrice(c.low.toFloat())
                val yOpen = yPrice(c.open.toFloat())
                val yClose = yPrice(c.close.toFloat())
                drawLine(
                    color = color,
                    start = Offset(cx, yHigh),
                    end = Offset(cx, yLow),
                    strokeWidth = wickStroke,
                    cap = StrokeCap.Butt,
                )
                val body = ChartGeometry.bodyRect(yOpen, yClose, minDojiPx)
                val bloom = if (isLast) dimens.chartBloomAlphaLast else dimens.chartBloomAlpha
                drawRect(
                    color = color.copy(alpha = bloom),
                    topLeft = Offset(cx - bodyW / 2f - bloomPx, body.top - bloomPx),
                    size = Size(bodyW + bloomPx * 2f, body.height + bloomPx * 2f),
                )
                drawRect(
                    color = color,
                    topLeft = Offset(cx - bodyW / 2f, body.top),
                    size = Size(bodyW, body.height),
                )
            }
        }

        val last = candles.last()
        val lastY = yPrice(last.close.toFloat())
        if (lastY in panes.priceTop..panes.priceBottom) {
            drawLine(
                color = chartColors.lastPriceHairline,
                start = Offset(panes.plotLeft, lastY),
                end = Offset(panes.plotRight, lastY),
                strokeWidth = hairlineStroke,
            )
        }

        drawLine(
            color = ConfluenceColors.BorderSubtle,
            start = Offset(panes.plotRight, panes.priceTop),
            end = Offset(panes.plotRight, panes.volBottom),
            strokeWidth = gridStroke,
        )

        axisPaint.textAlign = Paint.Align.LEFT
        axisPaint.color = ConfluenceColors.OnSurfaceMuted.toArgb()
        val labelX = panes.plotRight + axisTick
        val font = axisPaint.fontMetrics
        for (price in yTicks) {
            val gy = yPrice(price)
            if (gy < panes.priceTop || gy > panes.priceBottom) continue
            val textY = (gy - (font.ascent + font.descent) / 2f).coerceIn(
                panes.priceTop - font.ascent,
                panes.priceBottom - font.descent,
            )
            drawContext.canvas.nativeCanvas.drawText(
                ChartAxisLabels.formatPrice(price),
                labelX,
                textY,
                axisPaint,
            )
        }

        val slots = ChartAxisLabels.timeSlotIndices(
            visibleCount = (win.endIndex - win.startIndex).coerceAtLeast(1),
            minPxBetween = minLabelGap,
            candleWidthPx = cw,
        )
        axisPaint.textAlign = Paint.Align.CENTER
        val timeY = size.height - 4f
        val labelGap = 10f
        var lastLabelRight = panes.plotLeft
        for (slot in slots) {
            val idx = win.startIndex + slot
            val c = candles.getOrNull(idx) ?: continue
            val cx = xSlot(idx)
            if (cx < panes.plotLeft || cx > panes.plotRight) continue
            val label = ChartAxisLabels.formatTime(c.openTimeMs, tf)
            val tw = axisPaint.measureText(label)
            val left = cx - tw / 2f
            val right = cx + tw / 2f
            if (left < lastLabelRight + labelGap) continue
            if (right > panes.plotRight + axisTick) continue
            drawLine(
                color = ConfluenceColors.Outline,
                start = Offset(cx, panes.volBottom),
                end = Offset(cx, panes.volBottom + axisTick),
                strokeWidth = gridStroke,
            )
            drawContext.canvas.nativeCanvas.drawText(label, cx, timeY, axisPaint)
            lastLabelRight = right
        }
        axisPaint.textAlign = Paint.Align.LEFT

        if (lastY in panes.priceTop..panes.priceBottom) {
            drawAxisTag(
                text = ChartAxisLabels.formatPrice(last.close.toFloat()),
                anchorX = panes.plotRight + axisTick / 2f,
                anchorY = lastY.coerceIn(panes.priceTop, panes.priceBottom),
                paint = tagPaint,
                background = ConfluenceColors.VoidElevated,
                textColor = ConfluenceColors.CyberCyan,
                border = ConfluenceColors.CyberCyan.copy(alpha = 0.55f),
                paddingH = tagPadH,
                paddingV = tagPadV,
                corner = tagCorner,
                minX = panes.plotRight,
                maxX = size.width,
                minY = panes.priceTop,
                maxY = panes.priceBottom,
            )
        }

        val xhIndex = crosshairIndex
        if (xhIndex != null) {
            val snapped = candles.getOrNull(xhIndex)
            if (snapped != null) {
                val cx = xSlot(xhIndex).coerceIn(panes.plotLeft, panes.plotRight)
                val cy = yPrice(snapped.close.toFloat()).coerceIn(panes.priceTop, panes.priceBottom)
                drawLine(
                    color = chartColors.crosshair,
                    start = Offset(cx, panes.priceTop),
                    end = Offset(cx, panes.volBottom),
                    strokeWidth = crosshairStroke,
                    pathEffect = dash,
                )
                drawLine(
                    color = chartColors.crosshair,
                    start = Offset(panes.plotLeft, cy),
                    end = Offset(panes.plotRight, cy),
                    strokeWidth = crosshairStroke,
                    pathEffect = dash,
                )
                drawCircle(chartColors.crosshair, radius = 3.5f, center = Offset(cx, cy))
                drawAxisTag(
                    text = ChartAxisLabels.formatPrice(snapped.close.toFloat()),
                    anchorX = panes.plotRight + axisTick / 2f,
                    anchorY = cy,
                    paint = tagPaint,
                    background = ConfluenceColors.CyberCyan,
                    textColor = ConfluenceColors.OnPrimary,
                    border = ConfluenceColors.CyberCyan,
                    paddingH = tagPadH,
                    paddingV = tagPadV,
                    corner = tagCorner,
                    minX = panes.plotRight,
                    maxX = size.width,
                    minY = panes.priceTop,
                    maxY = panes.priceBottom,
                )
                drawAxisTag(
                    text = ChartAxisLabels.formatTime(snapped.openTimeMs, snapped.timeframe),
                    anchorX = cx,
                    anchorY = panes.volBottom + bottomPad / 2f,
                    paint = tagPaint,
                    background = ConfluenceColors.VoidElevated,
                    textColor = ConfluenceColors.CyberCyan,
                    border = ConfluenceColors.CyberCyan,
                    paddingH = tagPadH,
                    paddingV = tagPadV,
                    corner = tagCorner,
                    minX = 0f,
                    maxX = panes.plotRight,
                    minY = panes.volBottom,
                    maxY = size.height,
                    centerHorizontally = true,
                )
            }
        }
    }
}

private fun Color.boosted(amount: Float): Color {
    val t = ConfluenceColors.TextPrimary
    return Color(
        red = red + (t.red - red) * amount,
        green = green + (t.green - green) * amount,
        blue = blue + (t.blue - blue) * amount,
        alpha = 1f,
    )
}

private fun DrawScope.drawAxisTag(
    text: String,
    anchorX: Float,
    anchorY: Float,
    paint: Paint,
    background: Color,
    textColor: Color,
    border: Color,
    paddingH: Float,
    paddingV: Float,
    corner: Float,
    minX: Float,
    maxX: Float,
    minY: Float,
    maxY: Float,
    centerHorizontally: Boolean = false,
) {
    val tw = paint.measureText(text)
    val tagW = tw + paddingH * 2f
    val tagH = paint.textSize + paddingV * 2f
    val left = if (centerHorizontally) {
        (anchorX - tagW / 2f).coerceIn(minX, (maxX - tagW).coerceAtLeast(minX))
    } else {
        anchorX.coerceIn(minX, (maxX - tagW).coerceAtLeast(minX))
    }
    val top = (anchorY - tagH / 2f).coerceIn(minY, (maxY - tagH).coerceAtLeast(minY))
    val radius = CornerRadius(corner, corner)
    drawRoundRect(
        color = background,
        topLeft = Offset(left, top),
        size = Size(tagW, tagH),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = border,
        topLeft = Offset(left, top),
        size = Size(tagW, tagH),
        cornerRadius = radius,
        style = Stroke(width = 1f),
    )
    paint.color = textColor.toArgb()
    val fm = paint.fontMetrics
    val textY = top + tagH / 2f - (fm.ascent + fm.descent) / 2f
    drawContext.canvas.nativeCanvas.drawText(text, left + tagW / 2f, textY, paint)
}

@Preview(showBackground = true, backgroundColor = 0xFF07090E, widthDp = 400, heightDp = 320)
@Composable
private fun CandleChartPreview() {
    ConfluenceTheme {
        CandleChart(
            candles = FakeFixtures.sampleClosedCandles(count = 64),
            seriesKey = "preview:1h",
            modifier = Modifier.height(320.dp),
        )
    }
}
