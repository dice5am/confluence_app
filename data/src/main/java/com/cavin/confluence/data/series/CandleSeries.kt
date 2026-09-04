package com.cavin.confluence.data.series

import com.cavin.confluence.data.model.Candle

/**
 * MOB-2.5 — apply a live candle without rebuilding the whole series path.
 *
 * - Same openTimeMs as last → replace last (forming → final / forming update)
 * - Newer openTimeMs → append
 * - Older / foreign key → ignore (no full resort)
 */
object CandleSeries {

    fun applyLive(series: List<Candle>, tick: Candle): List<Candle> {
        if (series.isEmpty()) return listOf(tick)
        val last = series.last()
        if (last.venue != tick.venue ||
            last.symbol != tick.symbol ||
            last.timeframe != tick.timeframe
        ) {
            return series
        }
        return when {
            tick.openTimeMs == last.openTimeMs -> {
                // In-place update of tip — O(1) structural share of prefix
                val out = ArrayList<Candle>(series.size)
                out.addAll(series.subList(0, series.lastIndex))
                out.add(tick)
                out
            }
            tick.openTimeMs > last.openTimeMs -> {
                val out = ArrayList<Candle>(series.size + 1)
                out.addAll(series)
                out.add(tick)
                out
            }
            else -> series
        }
    }
}
