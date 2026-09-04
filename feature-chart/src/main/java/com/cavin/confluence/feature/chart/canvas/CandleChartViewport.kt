package com.cavin.confluence.feature.chart.canvas

import com.cavin.confluence.data.model.Candle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Time-axis viewport for the MOB-2.1 candle Canvas spike.
 *
 * X is indexed in candle space (0..n-1). Y is auto-fit to the visible window.
 * Steady pan/zoom mutates this holder — drawing iterates indices only (no
 * per-frame List allocation of candles).
 */
class CandleChartViewport(
    initialVisibleCount: Float = DEFAULT_VISIBLE,
) {
    /** Left edge in candle-index space (may be fractional while panning). */
    var startIndex: Float = 0f
        private set

    /** How many candle slots span the chart width. */
    var visibleCount: Float = initialVisibleCount
        private set

    fun resetToEnd(candleCount: Int, preferredVisible: Float = DEFAULT_VISIBLE) {
        visibleCount = preferredVisible.coerceIn(MIN_VISIBLE, maxVisibleFor(candleCount))
        startIndex = max(0f, candleCount - visibleCount)
    }

    fun applyPan(deltaPx: Float, widthPx: Float, candleCount: Int) {
        if (widthPx <= 0f || candleCount <= 0) return
        val deltaCandles = -deltaPx / widthPx * visibleCount
        startIndex = (startIndex + deltaCandles).coerceIn(0f, maxStart(candleCount))
    }

    /**
     * Pinch-zoom about [focalX] (px from left). Zoom > 1 = zoom in (fewer candles).
     */
    fun applyZoom(zoom: Float, focalX: Float, widthPx: Float, candleCount: Int) {
        if (widthPx <= 0f || candleCount <= 0 || zoom == 1f) return
        val focalIndex = startIndex + (focalX / widthPx) * visibleCount
        val nextVisible = (visibleCount / zoom).coerceIn(MIN_VISIBLE, maxVisibleFor(candleCount))
        visibleCount = nextVisible
        startIndex = (focalIndex - (focalX / widthPx) * visibleCount)
            .coerceIn(0f, maxStart(candleCount))
    }

    fun nearestIndex(xPx: Float, widthPx: Float, candleCount: Int): Int? {
        if (candleCount <= 0 || widthPx <= 0f) return null
        val raw = startIndex + (xPx / widthPx) * visibleCount
        return raw.toInt().coerceIn(0, candleCount - 1)
    }

    fun xForIndex(index: Float, widthPx: Float): Float =
        ((index - startIndex) / visibleCount) * widthPx

    fun candleSlotWidth(widthPx: Float): Float = widthPx / visibleCount

    /** Inclusive index range that may paint (with one-candle pad). */
    fun drawIndexRange(candleCount: Int): IntRange {
        if (candleCount <= 0) return IntRange.EMPTY
        val from = floor(startIndex).toInt().coerceIn(0, candleCount - 1)
        val to = ceil(startIndex + visibleCount).toInt().coerceIn(0, candleCount - 1)
        return from..to
    }

    fun priceBounds(candles: List<Candle>): PriceBounds? {
        val range = drawIndexRange(candles.size)
        if (range.isEmpty()) return null
        var lo = Double.POSITIVE_INFINITY
        var hi = Double.NEGATIVE_INFINITY
        for (i in range) {
            val c = candles[i]
            lo = min(lo, c.low)
            hi = max(hi, c.high)
        }
        if (!lo.isFinite() || !hi.isFinite()) return null
        if (hi <= lo) {
            val pad = max(1.0, lo * 0.001)
            return PriceBounds(lo - pad, hi + pad)
        }
        val pad = (hi - lo) * Y_PAD_FRACTION
        return PriceBounds(lo - pad, hi + pad)
    }

    private fun maxStart(candleCount: Int): Float =
        max(0f, candleCount - visibleCount)

    private fun maxVisibleFor(candleCount: Int): Float =
        max(MIN_VISIBLE, min(MAX_VISIBLE, candleCount.toFloat().coerceAtLeast(MIN_VISIBLE)))

    data class PriceBounds(val min: Double, val max: Double) {
        fun yFor(price: Double, heightPx: Float): Float {
            val t = ((price - min) / (max - min)).toFloat().coerceIn(0f, 1f)
            return heightPx * (1f - t)
        }
    }

    companion object {
        const val DEFAULT_VISIBLE = 48f
        const val MIN_VISIBLE = 8f
        const val MAX_VISIBLE = 180f
        private const val Y_PAD_FRACTION = 0.08
    }
}
