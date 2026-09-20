package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.indicators.DayOneIndicators
import com.cavin.confluence.indicators.IndicatorBar
import com.cavin.confluence.indicators.IndicatorCalc
import com.cavin.confluence.indicators.SnapshotCutoff

/**
 * 1:1 MD-1.1 [Candle] → [IndicatorBar] adapter. No resample, no fake series.
 */
fun Candle.toIndicatorBar(): IndicatorBar = IndicatorBar(
    openTimeMs = openTimeMs,
    closeTimeMs = closeTimeMs,
    open = open,
    high = high,
    low = low,
    close = close,
    volume = volume,
    isFinal = isFinal,
)

/**
 * Cutoff labels pass through from packaged `meta.json` when the store is loaded;
 * otherwise the locked PR #29 pin on [SnapshotCutoff.PACKAGED_2026_09_19].
 */
fun resolveSnapshotCutoff(): SnapshotCutoff {
    val packaged = SnapshotCutoff.PACKAGED_2026_09_19
    if (!MdSnapshotStore.isLoaded()) return packaged
    return SnapshotCutoff(
        cutoffMs = MdSnapshotStore.cutoffMs,
        cutoffUtcLabel = MdSnapshotStore.cutoffUtcLabel.ifBlank { packaged.cutoffUtcLabel },
        cutoffTorontoLabel = packaged.cutoffTorontoLabel,
        snapshotVersion = packaged.snapshotVersion,
    )
}

/**
 * Chart path always binds through [IndicatorCalc.evaluateAsOf] at the last
 * fenced close (or [cutoff.cutoffMs] when the window is empty). Never uses a
 * full [IndicatorCalc.evaluate] as a historical Chikou / VP store.
 */
fun evaluateDayOneIndicators(
    candles: List<Candle>,
    cutoff: SnapshotCutoff = resolveSnapshotCutoff(),
): DayOneIndicators {
    val bars = candles.map { it.toIndicatorBar() }
    val lastClose = bars
        .filter { it.isFinal && it.closeTimeMs <= cutoff.cutoffMs }
        .maxOfOrNull { it.closeTimeMs }
        ?: cutoff.cutoffMs
    return IndicatorCalc.evaluateAsOf(bars, cutoff, lastClose)
}

fun chartAsOfOverlays(
    candles: List<Candle>,
    cutoff: SnapshotCutoff = resolveSnapshotCutoff(),
): ChartAsOfOverlays = ChartAsOfOverlays.fromCandles(candles, cutoff)

/**
 * Keep displayed candles aligned to the fenced IndicatorCalc window (same openTimeMs).
 * A forming tip that survives the cutoff may trail the closed window.
 */
fun candlesAlignedToIndicators(raw: List<Candle>, indicators: DayOneIndicators): List<Candle> {
    if (indicators.bars.isEmpty()) return emptyList()
    val byOpen = HashMap<Long, Candle>(raw.size)
    for (candle in raw) {
        byOpen[candle.openTimeMs] = candle
    }
    val aligned = ArrayList<Candle>(indicators.bars.size + 1)
    for (bar in indicators.bars) {
        val candle = byOpen[bar.openTimeMs] ?: continue
        aligned.add(candle)
    }
    val tip = raw.lastOrNull()
    if (
        tip != null &&
        !tip.isFinal &&
        tip.closeTimeMs <= indicators.cutoff.cutoffMs &&
        aligned.none { it.openTimeMs == tip.openTimeMs }
    ) {
        aligned.add(tip)
    }
    return aligned
}

internal fun indicatorSeriesKey(timeframeWire: String, candles: List<Candle>): String {
    val first = candles.firstOrNull()?.openTimeMs ?: 0L
    val last = candles.lastOrNull()?.closeTimeMs ?: 0L
    return "$timeframeWire:$first:$last:${candles.size}"
}
