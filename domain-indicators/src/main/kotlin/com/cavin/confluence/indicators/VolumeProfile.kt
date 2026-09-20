package com.cavin.confluence.indicators

import kotlin.math.max
import kotlin.math.min

/**
 * Closed-bar volume profile (POC / VAH / VAL).
 *
 * Deterministic session — **no manual range**:
 * last [LOOKBACK_BARS] fenced closed bars of the input series (default 24;
 * overridable via [compute] / [IndicatorParams.volumeProfileLookback]).
 * Callers should prefer 1h+ (ALT-1.2 §3.7). On 1h this is ~24h of 1h candles,
 * **not** an America/Toronto cash session and not tick/aggTrade data.
 *
 * Each bar's volume is spread uniformly across price bins overlapping `[low, high]`.
 * Value area expands from the POC bin until it holds [VALUE_AREA_FRACTION] of
 * window volume. POC ties pick the lowest-price bin.
 */
object VolumeProfile {
    const val LOOKBACK_BARS: Int = 24
    const val ROW_COUNT: Int = 50
    const val VALUE_AREA_FRACTION: Double = 0.70

    const val SESSION_NOTE: String =
        "Fixed lookback: last $LOOKBACK_BARS closed bars of the provided series " +
            "(prefer 1h+). Not a Toronto session, not a user-drawn range, not tick VP. " +
            "Snapshot 1h depth is ~20d, well under MD-1.1 1m caps; this engine never " +
            "assumes >90d of 1m."

    fun sessionNote(lookbackBars: Int): String =
        "Fixed lookback: last $lookbackBars closed bars of the provided series " +
            "(prefer 1h+). Not a Toronto session, not a user-drawn range, not tick VP. " +
            "Snapshot 1h depth is ~20d, well under MD-1.1 1m caps; this engine never " +
            "assumes >90d of 1m."

    /**
     * Last-[lookbackBars] snapshot of [fencedClosedBars] (as-of the last
     * provided bar). Not a historical series — see [computeAsOf].
     * Default lookback is [LOOKBACK_BARS] (24).
     */
    fun compute(
        fencedClosedBars: List<IndicatorBar>,
        lookbackBars: Int = LOOKBACK_BARS,
    ): VolumeProfileResult {
        require(lookbackBars > 0)
        val window = fencedClosedBars.takeLast(lookbackBars)
        if (window.isEmpty()) {
            return VolumeProfileResult.empty("no closed bars in lookback", lookbackBars)
        }
        var priceLow = Double.POSITIVE_INFINITY
        var priceHigh = Double.NEGATIVE_INFINITY
        var totalVolume = 0.0
        for (bar in window) {
            if (bar.low < priceLow) priceLow = bar.low
            if (bar.high > priceHigh) priceHigh = bar.high
            totalVolume += bar.volume
        }
        if (!priceLow.isFinite() || !priceHigh.isFinite() || priceHigh < priceLow) {
            return VolumeProfileResult.empty("non-finite price range", lookbackBars)
        }

        val rowCount = ROW_COUNT
        val span = priceHigh - priceLow
        val volumes = DoubleArray(rowCount)
        if (span == 0.0) {
            for (bar in window) {
                volumes[0] += bar.volume
            }
        } else {
            val width = span / rowCount
            for (bar in window) {
                distribute(volumes, priceLow, width, bar.low, bar.high, bar.volume)
            }
        }

        var pocIndex = 0
        for (i in 1 until rowCount) {
            if (volumes[i] > volumes[pocIndex]) {
                pocIndex = i
            }
        }

        val inVa = BooleanArray(rowCount)
        expandValueArea(volumes, pocIndex, totalVolume, inVa)

        val width = if (span == 0.0) 0.0 else span / rowCount
        val bins = ArrayList<VolumeBin>(rowCount)
        var vaLowIdx = -1
        var vaHighIdx = -1
        for (i in 0 until rowCount) {
            val low = if (span == 0.0) priceLow else priceLow + i * width
            val high = if (span == 0.0) priceHigh else if (i == rowCount - 1) priceHigh else priceLow + (i + 1) * width
            bins.add(
                VolumeBin(
                    index = i,
                    priceLow = low,
                    priceHigh = high,
                    midpoint = (low + high) / 2.0,
                    volume = volumes[i],
                    isPointOfControl = i == pocIndex,
                    inValueArea = inVa[i],
                ),
            )
            if (inVa[i]) {
                if (vaLowIdx < 0) vaLowIdx = i
                vaHighIdx = i
            }
        }

        val poc = bins[pocIndex].midpoint
        val valLow = if (vaLowIdx >= 0) bins[vaLowIdx].priceLow else null
        val vaHigh = if (vaHighIdx >= 0) bins[vaHighIdx].priceHigh else null

        return VolumeProfileResult(
            lookbackBarsRequested = lookbackBars,
            usedBarCount = window.size,
            lookbackLimited = window.size < lookbackBars,
            windowFirstOpenTimeMs = window.first().openTimeMs,
            windowLastCloseTimeMs = window.last().closeTimeMs,
            rowCount = rowCount,
            valueAreaFraction = VALUE_AREA_FRACTION,
            pointOfControl = poc,
            valueAreaHigh = vaHigh,
            valueAreaLow = valLow,
            totalVolume = totalVolume,
            bins = bins,
            notes = sessionNote(lookbackBars),
        )
    }

    /**
     * Volume profile known at [asOfCloseTimeMs]: last [lookbackBars] fenced
     * closed bars with `closeTimeMs <= asOfCloseTimeMs`. Future bars in
     * [fencedClosedBars] are ignored. Default lookback is [LOOKBACK_BARS].
     */
    fun computeAsOf(
        fencedClosedBars: List<IndicatorBar>,
        asOfCloseTimeMs: Long,
        lookbackBars: Int = LOOKBACK_BARS,
    ): VolumeProfileResult = compute(
        fencedClosedBars.filter { it.closeTimeMs <= asOfCloseTimeMs },
        lookbackBars,
    )

    private fun distribute(
        volumes: DoubleArray,
        priceLow: Double,
        width: Double,
        barLow: Double,
        barHigh: Double,
        volume: Double,
    ) {
        if (volume == 0.0) return
        val lo = min(barLow, barHigh)
        val hi = max(barLow, barHigh)
        val range = hi - lo
        if (range == 0.0 || width == 0.0) {
            volumes[binIndex(priceLow, width, volumes.size, lo)] += volume
            return
        }
        for (i in volumes.indices) {
            val binLo = priceLow + i * width
            val binHi = if (i == volumes.lastIndex) priceLow + volumes.size * width else priceLow + (i + 1) * width
            val overlap = min(hi, binHi) - max(lo, binLo)
            if (overlap > 0.0) {
                volumes[i] += volume * (overlap / range)
            }
        }
    }

    private fun binIndex(priceLow: Double, width: Double, rowCount: Int, price: Double): Int {
        if (width == 0.0) return 0
        val raw = ((price - priceLow) / width).toInt()
        return raw.coerceIn(0, rowCount - 1)
    }

    private fun expandValueArea(
        volumes: DoubleArray,
        pocIndex: Int,
        totalVolume: Double,
        inVa: BooleanArray,
    ) {
        if (totalVolume <= 0.0) {
            inVa[pocIndex] = true
            return
        }
        val target = totalVolume * VALUE_AREA_FRACTION
        inVa[pocIndex] = true
        var used = volumes[pocIndex]
        var lo = pocIndex
        var hi = pocIndex
        while (used < target && (lo > 0 || hi < volumes.lastIndex)) {
            val below = if (lo > 0) volumes[lo - 1] else Double.NEGATIVE_INFINITY
            val above = if (hi < volumes.lastIndex) volumes[hi + 1] else Double.NEGATIVE_INFINITY
            when {
                above > below -> {
                    hi += 1
                    inVa[hi] = true
                    used += volumes[hi]
                }
                below > above -> {
                    lo -= 1
                    inVa[lo] = true
                    used += volumes[lo]
                }
                else -> {
                    // Tie: expand both sides when available (deterministic).
                    if (lo > 0) {
                        lo -= 1
                        inVa[lo] = true
                        used += volumes[lo]
                    }
                    if (used < target && hi < volumes.lastIndex) {
                        hi += 1
                        inVa[hi] = true
                        used += volumes[hi]
                    }
                }
            }
        }
    }
}

data class VolumeBin(
    val index: Int,
    val priceLow: Double,
    val priceHigh: Double,
    val midpoint: Double,
    val volume: Double,
    val isPointOfControl: Boolean,
    val inValueArea: Boolean,
)

data class VolumeProfileResult(
    val lookbackBarsRequested: Int,
    val usedBarCount: Int,
    val lookbackLimited: Boolean,
    val windowFirstOpenTimeMs: Long?,
    val windowLastCloseTimeMs: Long?,
    val rowCount: Int,
    val valueAreaFraction: Double,
    val pointOfControl: Double?,
    val valueAreaHigh: Double?,
    val valueAreaLow: Double?,
    val totalVolume: Double,
    val bins: List<VolumeBin>,
    val notes: String,
) {
    companion object {
        fun empty(
            reason: String,
            lookbackBars: Int = VolumeProfile.LOOKBACK_BARS,
        ): VolumeProfileResult = VolumeProfileResult(
            lookbackBarsRequested = lookbackBars,
            usedBarCount = 0,
            lookbackLimited = true,
            windowFirstOpenTimeMs = null,
            windowLastCloseTimeMs = null,
            rowCount = VolumeProfile.ROW_COUNT,
            valueAreaFraction = VolumeProfile.VALUE_AREA_FRACTION,
            pointOfControl = null,
            valueAreaHigh = null,
            valueAreaLow = null,
            totalVolume = 0.0,
            bins = emptyList(),
            notes = "${VolumeProfile.sessionNote(lookbackBars)} ($reason)",
        )
    }
}
