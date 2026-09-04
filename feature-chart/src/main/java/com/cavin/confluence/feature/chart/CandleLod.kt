package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.Timeframe

/**
 * MOB-2.4 — level-of-detail / decimation for deep history.
 *
 * Depth lock (consumer): 1m ~60d (cap ≤90d), 5m 180d, 15m 1y, 1h+ full.
 * When the series is denser than [maxPoints] for the viewport budget, bucket
 * adjacent candles into OHLC aggregates (volume summed) so zoomed-out 1m
 * remains scrollable without OOM.
 */
object CandleLod {

    /** Soft target for points passed to Canvas for a full-series overview. */
    const val DEFAULT_MAX_POINTS = 2_000

    fun maybeDecimate(
        candles: List<Candle>,
        timeframe: Timeframe,
        maxPoints: Int = DEFAULT_MAX_POINTS,
    ): List<Candle> {
        if (candles.size <= maxPoints) return candles
        val bucket = ((candles.size + maxPoints - 1) / maxPoints).coerceAtLeast(2)
        return aggregate(candles, bucket)
    }

    /**
     * Further decimate based on visible candle width: if many candles would
     * share a pixel column, aggregate before draw.
     */
    fun forVisibleWidth(
        candles: List<Candle>,
        canvasWidthPx: Float,
        minPxPerCandle: Float = 2f,
    ): List<Candle> {
        if (candles.isEmpty() || canvasWidthPx <= 0f) return candles
        val maxPoints = (canvasWidthPx / minPxPerCandle).toInt().coerceAtLeast(64)
        return maybeDecimate(candles, candles.first().timeframe, maxPoints)
    }

    private fun aggregate(candles: List<Candle>, bucket: Int): List<Candle> {
        if (bucket <= 1) return candles
        val out = ArrayList<Candle>((candles.size + bucket - 1) / bucket)
        var i = 0
        while (i < candles.size) {
            val end = minOf(candles.size, i + bucket)
            val slice = candles.subList(i, end)
            val first = slice.first()
            val last = slice.last()
            var high = first.high
            var low = first.low
            var vol = 0.0
            for (c in slice) {
                if (c.high > high) high = c.high
                if (c.low < low) low = c.low
                vol += c.volume
            }
            out.add(
                first.copy(
                    openTimeMs = first.openTimeMs,
                    closeTimeMs = last.closeTimeMs,
                    open = first.open,
                    high = high,
                    low = low,
                    close = last.close,
                    volume = vol,
                    isFinal = slice.all { it.isFinal },
                    sourceTsMs = last.sourceTsMs,
                    ingestTsMs = last.ingestTsMs,
                ),
            )
            i = end
        }
        return out
    }

    /** Documented depth caps for planning / harness (not enforced here). */
    fun depthCapHint(tf: Timeframe): String = when (tf) {
        Timeframe.M1 -> "~60d (never assume >90d)"
        Timeframe.M5 -> "180d"
        Timeframe.M15 -> "1y"
        Timeframe.H1, Timeframe.H4, Timeframe.D1, Timeframe.W1 -> "full available"
    }
}
