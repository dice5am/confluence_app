package com.cavin.confluence.feature.chart.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.data.model.Candle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * MOB-2.1 Compose Canvas OHLC candle spike.
 *
 * Gestures:
 * - 1-finger drag → horizontal pan (time axis)
 * - Pinch → zoom time axis about centroid
 * - Long-press (+ drag) → crosshair + nearest-candle OHLC readout
 *
 * No volume pane, TF chips, overlays, or live append (later MOB tickets).
 */
@Composable
fun CandleChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    /** Optional seed so TF remounts reset viewport; null = keep. */
    seriesKey: Any? = null,
) {
    val chartColors = ConfluenceThemeAccess.chartColors
    val surface = MaterialTheme.colorScheme.surface
    val onMuted = MaterialTheme.colorScheme.onSurfaceVariant

    val viewport = remember { CandleChartViewport() }
    var viewportEpoch by remember { mutableIntStateOf(0) }
    var crosshairIndex by remember { mutableStateOf<Int?>(null) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(seriesKey, candles.size) {
        viewport.resetToEnd(candles.size)
        crosshairIndex = null
        viewportEpoch++
    }

    val readout = crosshairIndex?.let { idx ->
        candles.getOrNull(idx)?.let { formatOhlcReadout(it) }
    }

    Column(modifier = modifier.background(surface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = readout ?: "Long-press a candle for OHLC",
                style = MaterialTheme.typography.labelMedium,
                color = if (readout != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    onMuted
                },
                fontFamily = FontFamily.Monospace,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        val density = LocalDensity.current
        val minTouchSlop = with(density) { 8.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(candles.size, seriesKey) {
                    // Pan + pinch on the time axis (does not steal long-press).
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.fastAny { it.pressed }
                            if (!pressed) break
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            var used = false
                            if (abs(zoom - 1f) > 0.001f) {
                                viewport.applyZoom(zoom, centroid.x, size.width.toFloat(), candles.size)
                                used = true
                            }
                            if (abs(pan.x) > 0.5f && event.changes.size == 1) {
                                // Single-finger pan only when not in a multi-touch zoom.
                                viewport.applyPan(pan.x, size.width.toFloat(), candles.size)
                                used = true
                            } else if (abs(pan.x) > 0.5f && event.changes.size > 1) {
                                viewport.applyPan(pan.x, size.width.toFloat(), candles.size)
                                used = true
                            }
                            if (used) {
                                event.changes.fastForEach {
                                    if (it.positionChanged()) it.consume()
                                }
                                viewportEpoch++
                            }
                        } while (true)
                    }
                }
                .pointerInput(candles.size, seriesKey) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            canvasWidthPx = size.width.toFloat()
                            crosshairIndex = viewport.nearestIndex(
                                offset.x,
                                size.width.toFloat(),
                                candles.size,
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            crosshairIndex = viewport.nearestIndex(
                                change.position.x,
                                size.width.toFloat(),
                                candles.size,
                            )
                        },
                        onDragEnd = {
                            // Keep crosshair until next long-press elsewhere / series change.
                        },
                        onDragCancel = { },
                    )
                },
        ) {
            // Read epoch so pan/zoom invalidates draw.
            @Suppress("UNUSED_EXPRESSION")
            viewportEpoch
            @Suppress("UNUSED_EXPRESSION")
            minTouchSlop

            canvasWidthPx = size.width
            drawCandleChart(
                candles = candles,
                viewport = viewport,
                bull = chartColors.bull,
                bear = chartColors.bear,
                grid = chartColors.grid,
                crosshair = chartColors.crosshair,
                crosshairIndex = crosshairIndex,
            )
        }
    }
}

private fun DrawScope.drawCandleChart(
    candles: List<Candle>,
    viewport: CandleChartViewport,
    bull: Color,
    bear: Color,
    grid: Color,
    crosshair: Color,
    crosshairIndex: Int?,
) {
    if (candles.isEmpty()) return
    val bounds = viewport.priceBounds(candles) ?: return
    val w = size.width
    val h = size.height
    val slot = viewport.candleSlotWidth(w)
    val bodyWidth = (slot * 0.62f).coerceAtLeast(1f)
    val wickStroke = (slot * 0.08f).coerceIn(1f, 3f)

    // Light horizontal grid (3 lines) — chart-safe token.
    for (i in 1..3) {
        val y = h * i / 4f
        drawLine(
            color = grid,
            start = Offset(0f, y),
            end = Offset(w, y),
            strokeWidth = 1f,
        )
    }

    val range = viewport.drawIndexRange(candles.size)
    for (i in range) {
        val c = candles[i]
        val cx = viewport.xForIndex(i + 0.5f, w)
        if (cx < -slot || cx > w + slot) continue

        val yHigh = bounds.yFor(c.high, h)
        val yLow = bounds.yFor(c.low, h)
        val yOpen = bounds.yFor(c.open, h)
        val yClose = bounds.yFor(c.close, h)
        val up = c.close >= c.open
        val color = if (up) bull else bear

        drawLine(
            color = color,
            start = Offset(cx, yHigh),
            end = Offset(cx, yLow),
            strokeWidth = wickStroke,
        )

        val top = minOf(yOpen, yClose)
        val bottom = maxOf(yOpen, yClose)
        val bodyH = (bottom - top).coerceAtLeast(1f)
        drawRect(
            color = color,
            topLeft = Offset(cx - bodyWidth / 2f, top),
            size = Size(bodyWidth, bodyH),
        )
    }

    val idx = crosshairIndex
    if (idx != null && idx in candles.indices) {
        val c = candles[idx]
        val cx = viewport.xForIndex(idx + 0.5f, w)
        val cy = bounds.yFor(c.close, h)
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        drawLine(
            color = crosshair.copy(alpha = 0.85f),
            start = Offset(cx, 0f),
            end = Offset(cx, h),
            strokeWidth = 1.5f,
            pathEffect = dash,
        )
        drawLine(
            color = crosshair.copy(alpha = 0.55f),
            start = Offset(0f, cy),
            end = Offset(w, cy),
            strokeWidth = 1f,
            pathEffect = dash,
        )
    }
}

private val TorontoZone: ZoneId = ZoneId.of("America/Toronto")
private val TimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z", Locale.US)

private fun formatOhlcReadout(c: Candle): String {
    val whenStr = Instant.ofEpochMilli(c.openTimeMs).atZone(TorontoZone).format(TimeFmt)
    return buildString {
        append(whenStr)
        append("  O ").append(fmtPx(c.open))
        append("  H ").append(fmtPx(c.high))
        append("  L ").append(fmtPx(c.low))
        append("  C ").append(fmtPx(c.close))
    }
}

private fun fmtPx(v: Double): String = String.format(Locale.US, "%.2f", v)
