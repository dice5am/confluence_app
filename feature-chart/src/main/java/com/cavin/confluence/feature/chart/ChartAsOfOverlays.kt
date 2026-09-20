package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.indicators.CandleFence
import com.cavin.confluence.indicators.DayOneIndicators
import com.cavin.confluence.indicators.IndicatorBar
import com.cavin.confluence.indicators.IndicatorCalc
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.SnapshotCutoff
import com.cavin.confluence.indicators.VolumeProfile
import com.cavin.confluence.indicators.VolumeProfileResult

/**
 * Chart overlay adapter: every displayed overlay point is sourced from
 * [IndicatorCalc.evaluateAsOf] with [IndicatorParams] (or the equivalent
 * fenced prefix). Defaults ≡ five-auto.
 *
 * A single full [IndicatorCalc.evaluate] is **not** used as a historical
 * store for Chikou or volume profile.
 */
class ChartAsOfOverlays(
    val cutoff: SnapshotCutoff,
    val bars: List<IndicatorBar>,
    val params: IndicatorParams,
    val asOfLast: DayOneIndicators,
) {
    val lastCloseTimeMs: Long
        get() = bars.lastOrNull()?.closeTimeMs ?: cutoff.cutoffMs

    /**
     * Causal overlay snapshot known at bar [index]'s close.
     * Never a full-series peek: Chikou at the prefix tip is undefined;
     * VP is last-N ending at that bar.
     */
    fun evaluateAt(index: Int): DayOneIndicators {
        val bar = bars[index]
        return IndicatorCalc.evaluateAsOf(bars, cutoff, bar.closeTimeMs, params)
    }

    /**
     * Plot-aligned Chikou known at bar [index]. Always `null` at the as-of
     * tip (`chikou[i]=close[i+displacement]` is not known at `i`).
     */
    fun chikouPlotKnownAt(index: Int): Double? {
        val asOf = evaluateAt(index)
        return asOf.ichimoku.chikou.values.lastOrNull()?.takeIf { it.isFinite() }
    }

    fun volumeProfileAt(index: Int): VolumeProfileResult {
        val bar = bars[index]
        return VolumeProfile.computeAsOf(bars, bar.closeTimeMs, params.volumeProfileLookback)
    }

    /** Snapshot VP as-of the last fenced bar — current levels, not a historical store. */
    fun volumeProfileAsOfLast(): VolumeProfileResult = asOfLast.volumeProfile

    companion object {
        fun fromBars(
            rawBars: List<IndicatorBar>,
            cutoff: SnapshotCutoff,
            params: IndicatorParams = IndicatorParams.DEFAULT,
        ): ChartAsOfOverlays {
            val fenced = CandleFence.apply(rawBars, cutoff)
            val lastClose = fenced.bars.lastOrNull()?.closeTimeMs ?: cutoff.cutoffMs
            val asOfLast = IndicatorCalc.evaluateAsOf(rawBars, cutoff, lastClose, params)
            return ChartAsOfOverlays(
                cutoff = cutoff,
                bars = asOfLast.bars,
                params = params,
                asOfLast = asOfLast,
            )
        }

        fun fromCandles(
            candles: List<Candle>,
            cutoff: SnapshotCutoff,
            params: IndicatorParams = IndicatorParams.DEFAULT,
        ): ChartAsOfOverlays = fromBars(candles.map { it.toIndicatorBar() }, cutoff, params)
    }
}
