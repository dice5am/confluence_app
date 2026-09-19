package com.cavin.confluence.indicators

/**
 * Ichimoku with standard defaults 9 / 26 / 52, displacement 26 (ALT-1.2 §3.6).
 *
 * Tenkan / Kijun / Senkou-B raw are Donchian midpoints at calculation time
 * (known at bar `i`'s `closeTimeMs`).
 *
 * Senkou A/B in [IchimokuResult.senkouA] / [IchimokuResult.senkouB] are
 * **plot-aligned** (raw value from `i - displacement`). Those plot values
 * are known at bar `i` because they were computed 26 bars earlier.
 * [IchimokuResult.senkouARaw] / [IchimokuResult.senkouBRaw] are the same
 * numbers **before** the +26 shift — known at `i`, drawn at `i + 26`
 * (see [IchimokuResult.forwardCloud] past the last bar).
 *
 * Chikou is plot-aligned as `close[i + displacement]`. That plot slot is
 * **not** known at bar `i`; it becomes known when bar `i + 26` closes.
 * The close that *is* known at `i` is `bars[i].close`, drawn at plot
 * index `i - 26`. Use [IndicatorCalc.evaluateAsOf] for causal reads.
 *
 * Forward cloud points beyond the last bar use projected timestamps
 * (`lastOpen + k * barDuration`) — they are displaced plots, not invented candles.
 */
object Ichimoku {
    const val TENKAN_PERIOD: Int = 9
    const val KIJUN_PERIOD: Int = 26
    const val SENKOU_B_PERIOD: Int = 52
    const val DISPLACEMENT: Int = 26

    fun compute(bars: List<IndicatorBar>): IchimokuResult {
        val n = bars.size
        val times = bars.map { it.openTimeMs }
        if (n == 0) {
            return IchimokuResult(
                tenkan = AlignedSeries(emptyList(), emptyList()),
                kijun = AlignedSeries(emptyList(), emptyList()),
                senkouA = AlignedSeries(emptyList(), emptyList()),
                senkouB = AlignedSeries(emptyList(), emptyList()),
                senkouARaw = AlignedSeries(emptyList(), emptyList()),
                senkouBRaw = AlignedSeries(emptyList(), emptyList()),
                chikou = AlignedSeries(emptyList(), emptyList()),
                displacement = DISPLACEMENT,
                forwardCloud = emptyList(),
            )
        }
        val highs = DoubleArray(n) { bars[it].high }
        val lows = DoubleArray(n) { bars[it].low }
        val closes = DoubleArray(n) { bars[it].close }

        val tenkanRaw = Array<Double?>(n) { donchianMid(highs, lows, it, TENKAN_PERIOD) }
        val kijunRaw = Array<Double?>(n) { donchianMid(highs, lows, it, KIJUN_PERIOD) }
        val senkouBRaw = Array<Double?>(n) { donchianMid(highs, lows, it, SENKOU_B_PERIOD) }
        val senkouARaw = Array<Double?>(n) { i ->
            val t = tenkanRaw[i]
            val k = kijunRaw[i]
            if (t != null && k != null) (t + k) / 2.0 else null
        }

        val senkouAPlot = Array<Double?>(n) { i ->
            val src = i - DISPLACEMENT
            if (src >= 0) senkouARaw[src] else null
        }
        val senkouBPlot = Array<Double?>(n) { i ->
            val src = i - DISPLACEMENT
            if (src >= 0) senkouBRaw[src] else null
        }
        val chikouPlot = Array<Double?>(n) { i ->
            val src = i + DISPLACEMENT
            if (src < n) closes[src] else null
        }

        val durationMs = barDurationMs(bars)
        val lastOpen = bars.last().openTimeMs
        val forward = ArrayList<IchimokuForwardPoint>(DISPLACEMENT)
        for (k in 1..DISPLACEMENT) {
            val src = n - 1 + k - DISPLACEMENT
            if (src < 0) continue
            val a = senkouARaw[src]
            val b = senkouBRaw[src]
            if (a == null && b == null) continue
            forward.add(
                IchimokuForwardPoint(
                    offsetBars = k,
                    openTimeMs = lastOpen + k * durationMs,
                    senkouA = a,
                    senkouB = b,
                ),
            )
        }

        return IchimokuResult(
            tenkan = AlignedSeries(times, tenkanRaw.toList()),
            kijun = AlignedSeries(times, kijunRaw.toList()),
            senkouA = AlignedSeries(times, senkouAPlot.toList()),
            senkouB = AlignedSeries(times, senkouBPlot.toList()),
            senkouARaw = AlignedSeries(times, senkouARaw.toList()),
            senkouBRaw = AlignedSeries(times, senkouBRaw.toList()),
            chikou = AlignedSeries(times, chikouPlot.toList()),
            displacement = DISPLACEMENT,
            forwardCloud = forward,
        )
    }

    internal fun donchianMid(
        highs: DoubleArray,
        lows: DoubleArray,
        endInclusive: Int,
        period: Int,
    ): Double? {
        val start = endInclusive - period + 1
        if (start < 0) return null
        var max = Double.NEGATIVE_INFINITY
        var min = Double.POSITIVE_INFINITY
        for (i in start..endInclusive) {
            val h = highs[i]
            val l = lows[i]
            if (h > max) max = h
            if (l < min) min = l
        }
        return (max + min) / 2.0
    }

    internal fun barDurationMs(bars: List<IndicatorBar>): Long {
        val last = bars.last()
        val fromClose = last.closeTimeMs - last.openTimeMs + 1
        if (fromClose > 0) return fromClose
        if (bars.size >= 2) {
            val delta = bars.last().openTimeMs - bars[bars.lastIndex - 1].openTimeMs
            if (delta > 0) return delta
        }
        return 0L
    }
}

data class IchimokuResult(
    val tenkan: AlignedSeries,
    val kijun: AlignedSeries,
    val senkouA: AlignedSeries,
    val senkouB: AlignedSeries,
    /** Donchian / midpoint known at this bar; plotted [displacement] bars forward. */
    val senkouARaw: AlignedSeries,
    /** Donchian-52 midpoint known at this bar; plotted [displacement] bars forward. */
    val senkouBRaw: AlignedSeries,
    /**
     * Plot-aligned Chikou: `values[i] = close[i + displacement]`.
     * Known at bar `i + displacement`, not at bar `i`.
     */
    val chikou: AlignedSeries,
    val displacement: Int,
    val forwardCloud: List<IchimokuForwardPoint>,
)

data class IchimokuForwardPoint(
    val offsetBars: Int,
    val openTimeMs: Long,
    val senkouA: Double?,
    val senkouB: Double?,
)
