package com.cavin.confluence.feature.chart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.cavin.confluence.core.ui.theme.ChartSafeColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.indicators.AlignedSeries
import com.cavin.confluence.indicators.DayOneIndicators
import com.cavin.confluence.indicators.IchimokuResult

/**
 * Draws IndicatorCalc families onto the existing Canvas panes.
 * Values come from [DayOneIndicators] — this file does not recompute formulas.
 */
internal object ChartOverlayRenderer {

    fun expandPriceRange(
        candles: List<Candle>,
        indicators: DayOneIndicators,
        overlays: ChartOverlayVisibility,
        startIndex: Int,
        endIndex: Int,
        rawLo: Float,
        rawHi: Float,
    ): Pair<Float, Float> {
        var lo = rawLo
        var hi = rawHi
        val openTimes = LongArray(candles.size) { candles[it].openTimeMs }
        fun consider(series: AlignedSeries) {
            for (i in series.openTimeMs.indices) {
                val idx = ChartGeometry.slotIndexOf(openTimes, series.openTimeMs[i])
                if (idx < startIndex || idx >= endIndex) continue
                val expanded = ChartGeometry.expandVisibleRange(lo, hi, series.values[i])
                lo = expanded.first
                hi = expanded.second
            }
        }
        if (overlays.ema9) consider(indicators.ema9)
        if (overlays.sma21) consider(indicators.ema21)
        if (overlays.ichimoku) {
            consider(indicators.ichimoku.tenkan)
            consider(indicators.ichimoku.kijun)
            consider(indicators.ichimoku.senkouA)
            consider(indicators.ichimoku.senkouB)
            // Chikou is not a causal overlay at plot index i — do not expand
            // the window from full-series chikou[i] = close[i+26].
        }
        if (overlays.volumeProfile) {
            val vp = indicators.volumeProfile
            val poc = ChartGeometry.expandVisibleRange(lo, hi, vp.pointOfControl)
            lo = poc.first
            hi = poc.second
            val vah = ChartGeometry.expandVisibleRange(lo, hi, vp.valueAreaHigh)
            lo = vah.first
            hi = vah.second
            val valLevel = ChartGeometry.expandVisibleRange(lo, hi, vp.valueAreaLow)
            lo = valLevel.first
            hi = valLevel.second
        }
        return lo to hi
    }

    fun maxVolumeInWindow(
        candles: List<Candle>,
        indicators: DayOneIndicators,
        startIndex: Int,
        endIndex: Int,
        includeSma: Boolean,
    ): Double {
        var maxVol = 0.0
        for (i in startIndex until endIndex) {
            val v = candles[i].volume
            if (v > maxVol) maxVol = v
        }
        if (!includeSma) return maxVol
        val openTimes = LongArray(candles.size) { candles[it].openTimeMs }
        val sma = indicators.volumeSma20
        for (i in sma.openTimeMs.indices) {
            val idx = ChartGeometry.slotIndexOf(openTimes, sma.openTimeMs[i])
            if (idx < startIndex || idx >= endIndex) continue
            val v = sma.values[i] ?: continue
            if (v > maxVol) maxVol = v
        }
        return maxVol
    }

    fun DrawScope.drawPriceOverlays(
        candles: List<Candle>,
        indicators: DayOneIndicators,
        overlays: ChartOverlayVisibility,
        palette: ChartIndicatorPalette,
        colors: ChartSafeColors,
        panes: ChartGeometry.Panes,
        win: ChartGeometry.Window,
        candleWidthPx: Float,
        stroke: Float,
        yPrice: (Float) -> Float,
        xSlot: (Int) -> Float,
    ) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        clipRect(panes.plotLeft, panes.priceTop, panes.plotRight, panes.priceBottom) {
            if (overlays.ichimoku) {
                drawIchimokuCloud(
                    candles = candles,
                    ichimoku = indicators.ichimoku,
                    cloud = palette.ichimokuCloud.toColor(),
                    colors = colors,
                    win = win,
                    yPrice = yPrice,
                    xSlot = xSlot,
                )
                drawSeries(
                    candles, indicators.ichimoku.senkouA, palette.ichimokuTenkan.toColor(), stroke,
                    win, yPrice, xSlot, dash,
                )
                drawSeries(
                    candles, indicators.ichimoku.senkouB, palette.ichimokuKijun.toColor(), stroke,
                    win, yPrice, xSlot, dash,
                )
                drawSeries(
                    candles, indicators.ichimoku.tenkan, palette.ichimokuTenkan.toColor(), stroke,
                    win, yPrice, xSlot,
                )
                drawSeries(
                    candles, indicators.ichimoku.kijun, palette.ichimokuKijun.toColor(), stroke,
                    win, yPrice, xSlot,
                )
                // No Chikou stroke: plot-aligned chikou[i]=close[i+26] peeks.
                drawForwardCloud(
                    candles = candles,
                    ichimoku = indicators.ichimoku,
                    cloud = palette.ichimokuCloud.toColor(),
                    colors = colors,
                    yPrice = yPrice,
                    xSlot = xSlot,
                    lastIndex = candles.lastIndex,
                    candleWidthPx = candleWidthPx,
                )
            }
            if (overlays.sma21) {
                drawSeries(candles, indicators.ema21, palette.sma21.toColor(), stroke, win, yPrice, xSlot)
            }
            if (overlays.ema9) {
                drawSeries(candles, indicators.ema9, palette.ema9.toColor(), stroke, win, yPrice, xSlot)
            }
            if (overlays.volumeProfile) {
                drawVolumeProfileLevels(
                    indicators = indicators,
                    color = palette.volumeProfile.toColor(),
                    panes = panes,
                    yPrice = yPrice,
                    stroke = stroke,
                    dash = dash,
                )
            }
        }
    }

    fun DrawScope.drawVolumeSma(
        candles: List<Candle>,
        indicators: DayOneIndicators,
        colors: ChartSafeColors,
        panes: ChartGeometry.Panes,
        win: ChartGeometry.Window,
        maxVol: Double,
        stroke: Float,
        xSlot: (Int) -> Float,
    ) {
        if (panes.volHeight <= 1f || maxVol <= 0.0) return
        clipRect(panes.plotLeft, panes.volTop, panes.plotRight, panes.volBottom) {
            drawSeries(
                candles = candles,
                series = indicators.volumeSma20,
                color = colors.volumeSma,
                stroke = stroke,
                win = win,
                yOf = { value ->
                    val h = (value / maxVol.toFloat()).coerceIn(0f, 1f) * (panes.volHeight - 2f)
                    panes.volBottom - h
                },
                xSlot = xSlot,
            )
        }
    }

    fun DrawScope.drawRsiPane(
        candles: List<Candle>,
        indicators: DayOneIndicators,
        colors: ChartSafeColors,
        palette: ChartIndicatorPalette,
        panes: ChartGeometry.Panes,
        win: ChartGeometry.Window,
        stroke: Float,
        gridStroke: Float,
        xSlot: (Int) -> Float,
    ) {
        if (panes.rsiHeight <= 1f) return
        drawLine(
            color = colors.grid,
            start = Offset(panes.plotLeft, panes.rsiTop),
            end = Offset(panes.plotRight, panes.rsiTop),
            strokeWidth = gridStroke,
        )
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        fun yRsi(value: Float): Float = ChartGeometry.yForRsi(value, panes.rsiTop, panes.rsiHeight)
        clipRect(panes.plotLeft, panes.rsiTop, panes.plotRight, panes.rsiBottom) {
            for (guide in floatArrayOf(30f, 70f)) {
                val gy = yRsi(guide)
                drawLine(
                    color = palette.rsiGuide.toColor().copy(alpha = 0.45f),
                    start = Offset(panes.plotLeft, gy),
                    end = Offset(panes.plotRight, gy),
                    strokeWidth = gridStroke,
                    pathEffect = dash,
                )
            }
            val mid = yRsi(50f)
            drawLine(
                color = palette.rsiGuide.toColor().copy(alpha = 0.45f),
                start = Offset(panes.plotLeft, mid),
                end = Offset(panes.plotRight, mid),
                strokeWidth = gridStroke,
            )
            drawSeries(
                candles = candles,
                series = indicators.rsi14,
                color = palette.rsi.toColor(),
                stroke = stroke,
                win = win,
                yOf = { yRsi(it) },
                xSlot = xSlot,
            )
        }
    }

    private fun DrawScope.drawSeries(
        candles: List<Candle>,
        series: AlignedSeries,
        color: Color,
        stroke: Float,
        win: ChartGeometry.Window,
        yOf: (Float) -> Float,
        xSlot: (Int) -> Float,
        pathEffect: PathEffect? = null,
    ) {
        if (series.size == 0) return
        val openTimes = LongArray(candles.size) { candles[it].openTimeMs }
        val path = Path()
        var started = false
        for (i in series.openTimeMs.indices) {
            val value = series.values[i]
            if (value == null || !value.isFinite()) {
                started = false
                continue
            }
            val idx = ChartGeometry.slotIndexOf(openTimes, series.openTimeMs[i])
            if (idx < 0) {
                started = false
                continue
            }
            if (idx < win.startIndex - 1 || idx > win.endIndex) {
                started = false
                continue
            }
            val x = xSlot(idx)
            val y = yOf(value.toFloat())
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = pathEffect,
            ),
        )
    }

    private fun DrawScope.drawIchimokuCloud(
        candles: List<Candle>,
        ichimoku: IchimokuResult,
        cloud: Color,
        colors: ChartSafeColors,
        win: ChartGeometry.Window,
        yPrice: (Float) -> Float,
        xSlot: (Int) -> Float,
    ) {
        val openTimes = LongArray(candles.size) { candles[it].openTimeMs }
        val a = ichimoku.senkouA
        val b = ichimoku.senkouB
        var prevIdx = -1
        var prevA: Double? = null
        var prevB: Double? = null
        for (i in a.openTimeMs.indices) {
            val idx = ChartGeometry.slotIndexOf(openTimes, a.openTimeMs[i])
            val va = a.values[i]
            val vb = b.values.getOrNull(i)
            if (idx < 0 || va == null || vb == null || !va.isFinite() || !vb.isFinite()) {
                prevIdx = -1
                prevA = null
                prevB = null
                continue
            }
            if (prevIdx >= 0 && prevA != null && prevB != null &&
                idx >= win.startIndex - 1 && prevIdx <= win.endIndex
            ) {
                val bull = va >= vb
                val fill = (if (bull) cloud else colors.ichimokuCloudBear)
                    .copy(alpha = ConfluenceDimens.chartOverlayCloudAlpha)
                val path = Path()
                path.moveTo(xSlot(prevIdx), yPrice(prevA.toFloat()))
                path.lineTo(xSlot(idx), yPrice(va.toFloat()))
                path.lineTo(xSlot(idx), yPrice(vb.toFloat()))
                path.lineTo(xSlot(prevIdx), yPrice(prevB.toFloat()))
                path.close()
                drawPath(path, fill)
            }
            prevIdx = idx
            prevA = va
            prevB = vb
        }
    }

    private fun DrawScope.drawForwardCloud(
        candles: List<Candle>,
        ichimoku: IchimokuResult,
        cloud: Color,
        colors: ChartSafeColors,
        yPrice: (Float) -> Float,
        xSlot: (Int) -> Float,
        lastIndex: Int,
        candleWidthPx: Float,
    ) {
        if (ichimoku.forwardCloud.isEmpty() || candles.isEmpty()) return
        val lastA = ichimoku.senkouA.values.lastOrNull()
        val lastB = ichimoku.senkouB.values.lastOrNull()
        var prevOffset = 0
        var prevA = lastA
        var prevB = lastB
        for (point in ichimoku.forwardCloud) {
            val va = point.senkouA
            val vb = point.senkouB
            if (va == null || vb == null || prevA == null || prevB == null) {
                prevOffset = point.offsetBars
                prevA = va
                prevB = vb
                continue
            }
            val bull = va >= vb
            val fill = (if (bull) cloud else colors.ichimokuCloudBear)
                .copy(alpha = ConfluenceDimens.chartOverlayCloudAlpha)
            val x0 = xSlot(lastIndex) + prevOffset * candleWidthPx
            val x1 = xSlot(lastIndex) + point.offsetBars * candleWidthPx
            val path = Path()
            path.moveTo(x0, yPrice(prevA.toFloat()))
            path.lineTo(x1, yPrice(va.toFloat()))
            path.lineTo(x1, yPrice(vb.toFloat()))
            path.lineTo(x0, yPrice(prevB.toFloat()))
            path.close()
            drawPath(path, fill)
            prevOffset = point.offsetBars
            prevA = va
            prevB = vb
        }
    }

    private fun DrawScope.drawVolumeProfileLevels(
        indicators: DayOneIndicators,
        color: Color,
        panes: ChartGeometry.Panes,
        yPrice: (Float) -> Float,
        stroke: Float,
        dash: PathEffect,
    ) {
        val vp = indicators.volumeProfile
        fun level(price: Double?, effect: PathEffect?) {
            if (price == null || !price.isFinite()) return
            val y = yPrice(price.toFloat())
            if (y !in panes.priceTop..panes.priceBottom) return
            drawLine(
                color = color,
                start = Offset(panes.plotLeft, y),
                end = Offset(panes.plotRight, y),
                strokeWidth = stroke,
                pathEffect = effect,
            )
        }
        level(vp.valueAreaHigh, dash)
        level(vp.valueAreaLow, dash)
        level(vp.pointOfControl, null)
    }
}
