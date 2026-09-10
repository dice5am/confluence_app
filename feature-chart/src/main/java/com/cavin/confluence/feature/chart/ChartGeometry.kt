package com.cavin.confluence.feature.chart

import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Pure layout math for the Compose Canvas chart (Approach A craft).
 *
 * Kept off the draw scope so viewport / snap / pane DoD can be unit-tested.
 */
object ChartGeometry {
    val volumeFraction: Float get() = ConfluenceDimens.chartVolumeFraction
    val bodyFraction: Float get() = ConfluenceDimens.chartBodyFraction
    val yPadFraction: Float get() = ConfluenceDimens.chartYPadFraction
    val volumeAlpha: Float get() = ConfluenceDimens.chartVolumeAlpha
    val lastPriceHairlineAlpha: Float get() = ConfluenceDimens.chartLastPriceHairlineAlpha
    val minVisibleCandles: Int get() = ConfluenceDimens.chartMinVisibleCandles
    val defaultVisibleCandles: Int get() = ConfluenceDimens.chartDefaultVisibleCandles

    data class Panes(
        val plotLeft: Float,
        val plotRight: Float,
        val plotWidth: Float,
        val priceTop: Float,
        val priceBottom: Float,
        val priceHeight: Float,
        val volTop: Float,
        val volBottom: Float,
        val volHeight: Float,
    )

    fun panes(
        width: Float,
        height: Float,
        leftPad: Float,
        rightPad: Float,
        topPad: Float,
        bottomPad: Float,
        showVolume: Boolean,
        volumeFraction: Float = this.volumeFraction,
    ): Panes {
        val plotLeft = leftPad
        val plotRight = (width - rightPad).coerceAtLeast(plotLeft + 1f)
        val plotWidth = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotBottom = (height - bottomPad).coerceAtLeast(topPad + 1f)
        val usable = (plotBottom - topPad).coerceAtLeast(1f)
        val volH = if (showVolume) usable * volumeFraction else 0f
        val priceH = (usable - volH).coerceAtLeast(1f)
        val priceTop = topPad
        val priceBottom = priceTop + priceH
        return Panes(
            plotLeft = plotLeft,
            plotRight = plotRight,
            plotWidth = plotWidth,
            priceTop = priceTop,
            priceBottom = priceBottom,
            priceHeight = priceH,
            volTop = priceBottom,
            volBottom = plotBottom,
            volHeight = volH,
        )
    }

    data class Window(
        val startOffset: Float,
        val count: Int,
        val startIndex: Int,
        val endIndex: Int,
        val pixelShift: Float,
    )

    fun visibleCount(
        plotWidth: Float,
        candleWidthPx: Float,
        minCount: Int = minVisibleCandles,
    ): Int {
        if (plotWidth <= 0f || candleWidthPx <= 0f) return minCount
        return max(minCount, floor(plotWidth / candleWidthPx).toInt())
    }

    fun clampStart(startOffset: Float, candleCount: Int, visibleCount: Int): Float {
        if (candleCount <= 0) return 0f
        val maxStart = max(0f, (candleCount - visibleCount).toFloat())
        return startOffset.coerceIn(0f, maxStart)
    }

    fun initialStartOffset(candleCount: Int, visibleHint: Int = defaultVisibleCandles): Float =
        max(0f, (candleCount - visibleHint).toFloat())

    fun window(
        candleCount: Int,
        plotWidth: Float,
        candleWidthPx: Float,
        startOffset: Float,
    ): Window {
        val count = visibleCount(plotWidth, candleWidthPx).coerceAtMost(candleCount.coerceAtLeast(1))
        val start = clampStart(startOffset, candleCount, count)
        val startIndex = floor(start).toInt().coerceIn(0, max(0, candleCount - 1))
        val endIndex = min(candleCount, startIndex + count + 1)
        val pixelShift = (start - startIndex) * candleWidthPx
        return Window(
            startOffset = start,
            count = count,
            startIndex = startIndex,
            endIndex = endIndex,
            pixelShift = pixelShift,
        )
    }

    fun slotCenterX(
        index: Int,
        startIndex: Int,
        plotLeft: Float,
        candleWidthPx: Float,
        pixelShift: Float,
    ): Float = plotLeft + (index - startIndex) * candleWidthPx + candleWidthPx / 2f - pixelShift

    /**
     * Snap a canvas X to a candle index. Finger positions in the axis gutters
     * still resolve to the nearest in-plot slot (TradingView-style magnet).
     */
    fun slotAtX(
        canvasX: Float,
        plotLeft: Float,
        plotWidth: Float,
        candleWidthPx: Float,
        startOffset: Float,
        candleCount: Int,
    ): Int {
        if (candleCount <= 0 || candleWidthPx <= 0f) return 0
        val plotX = (canvasX - plotLeft).coerceIn(0f, plotWidth)
        val idx = floor(startOffset + plotX / candleWidthPx).toInt()
        return idx.coerceIn(0, candleCount - 1)
    }

    data class YDomain(val lo: Float, val hi: Float, val span: Float)

    fun yDomain(rawLo: Float, rawHi: Float, padFraction: Float = yPadFraction): YDomain {
        var lo = rawLo
        var hi = rawHi
        if (!lo.isFinite() || !hi.isFinite() || hi <= lo) {
            lo = 0f
            hi = 1f
        }
        val pad = ((hi - lo) * padFraction).coerceAtLeast(1f)
        lo -= pad
        hi += pad
        return YDomain(lo = lo, hi = hi, span = (hi - lo).coerceAtLeast(1e-3f))
    }

    fun yFor(price: Float, domain: YDomain, priceTop: Float, priceHeight: Float): Float =
        priceTop + priceHeight - ((price - domain.lo) / domain.span) * priceHeight

    fun bodyHeightPx(openY: Float, closeY: Float, minDojiPx: Float): Float =
        bodyRect(openY, closeY, minDojiPx).height

    data class BodyRect(val top: Float, val height: Float)

    fun bodyRect(openY: Float, closeY: Float, minDojiPx: Float): BodyRect {
        val rawTop = min(openY, closeY)
        val rawH = abs(closeY - openY)
        val height = max(rawH, minDojiPx)
        val top = rawTop - (height - rawH) / 2f
        return BodyRect(top = top, height = height)
    }

    fun zoomAround(
        startOffset: Float,
        oldCw: Float,
        newCw: Float,
        focusPlotX: Float,
        panX: Float,
        candleCount: Int,
        plotWidth: Float,
    ): Float {
        val safeOld = oldCw.coerceAtLeast(1f)
        val safeNew = newCw.coerceAtLeast(1f)
        val focusIndex = startOffset + focusPlotX / safeOld
        val afterZoom = focusIndex - focusPlotX / safeNew
        val afterPan = afterZoom - panX / safeNew
        val count = visibleCount(plotWidth, newCw)
        return clampStart(afterPan, candleCount, count)
    }
}
