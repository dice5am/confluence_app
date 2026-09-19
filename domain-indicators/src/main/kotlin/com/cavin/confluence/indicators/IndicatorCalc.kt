package com.cavin.confluence.indicators

/**
 * Public APPLY calc facade: fence closed candles, then the five automatic
 * families (RSI, volume, MAs, Ichimoku, volume profile).
 *
 * VWAP, Fib, discretionary price levels, and pivots are **not** implemented
 * in this pass (deferred, not abandoned).
 *
 * Incremental path: [onClosedBar] appends/replaces one final bar and recomputes
 * from the fenced window (cheap at snapshot depth; same functions work later
 * on live closed bars).
 */
object IndicatorCalc {
    const val ENGINE_ID: String = "alt-apply-calc"
    const val ENGINE_VERSION: String = "0.1.0-five-autos"

    fun evaluate(bars: List<IndicatorBar>, cutoff: SnapshotCutoff): DayOneIndicators {
        val fenced = CandleFence.apply(bars, cutoff)
        return computeFromFenced(fenced)
    }

    /**
     * Causal / as-of evaluation: apply the global [cutoff] fence, then keep only
     * bars with `closeTimeMs <= asOfCloseTimeMs`.
     *
     * Overlay points at bar `i` MUST come from this window (or an equivalent
     * prefix). [evaluate] on the full fenced series is as-of the last kept bar
     * only — it is not a pre-baked historical store. In particular
     * [DayOneIndicators.volumeProfile] on a full evaluate is the last-24
     * snapshot ending at the last fenced bar, not the VP that was known at
     * an earlier bar.
     */
    fun evaluateAsOf(
        bars: List<IndicatorBar>,
        cutoff: SnapshotCutoff,
        asOfCloseTimeMs: Long,
    ): DayOneIndicators {
        val fenced = CandleFence.apply(bars, cutoff)
        val prefix = fenced.bars.filter { it.closeTimeMs <= asOfCloseTimeMs }
        return computeFromFenced(
            FencedWindow(
                cutoff = cutoff,
                inputCount = fenced.inputCount,
                bars = prefix,
                droppedFormingCount = fenced.droppedFormingCount,
                droppedOvershootCount = fenced.droppedOvershootCount,
            ),
        )
    }

    /**
     * Update after a newly closed (or revised-tip) bar. Forming bars and
     * `closeTimeMs > cutoffMs` are ignored, matching [CandleFence].
     */
    fun onClosedBar(current: DayOneIndicators, bar: IndicatorBar): DayOneIndicators {
        if (!bar.isFinal) {
            return current.copy(
                fence = current.fence.copy(
                    inputCount = current.fence.inputCount + 1,
                    droppedFormingCount = current.fence.droppedFormingCount + 1,
                ),
            )
        }
        if (bar.closeTimeMs > current.cutoff.cutoffMs) {
            return current.copy(
                fence = current.fence.copy(
                    inputCount = current.fence.inputCount + 1,
                    droppedOvershootCount = current.fence.droppedOvershootCount + 1,
                ),
            )
        }
        val merged = ArrayList<IndicatorBar>(current.bars.size + 1)
        merged.addAll(current.bars)
        val existing = merged.indexOfLast { it.openTimeMs == bar.openTimeMs }
        if (existing >= 0) {
            merged[existing] = bar
        } else {
            merged.add(bar)
            merged.sortBy { it.openTimeMs }
        }
        return computeFromFenced(
            FencedWindow(
                cutoff = current.cutoff,
                inputCount = current.fence.inputCount + 1,
                bars = merged,
                droppedFormingCount = current.fence.droppedFormingCount,
                droppedOvershootCount = current.fence.droppedOvershootCount,
            ),
        )
    }

    private fun computeFromFenced(fenced: FencedWindow): DayOneIndicators {
        val bars = fenced.bars
        val closes = DoubleArray(bars.size) { bars[it].close }
        val volumes = DoubleArray(bars.size) { bars[it].volume }

        return DayOneIndicators(
            engineId = ENGINE_ID,
            engineVersion = ENGINE_VERSION,
            cutoff = fenced.cutoff,
            fence = fenced.report(),
            bars = bars,
            rsi14 = AlignedSeries.aligned(bars, Rsi.series(closes, Rsi.DEFAULT_PERIOD)),
            volume = AlignedSeries.copies(bars) { it.volume },
            volumeSma20 = AlignedSeries.aligned(bars, Sma.series(volumes, VolumeStats.SMA_PERIOD)),
            ema9 = AlignedSeries.aligned(bars, Ema.series(closes, MovingAverages.EMA_FAST)),
            ema21 = AlignedSeries.aligned(bars, Ema.series(closes, MovingAverages.EMA_SLOW)),
            sma50 = AlignedSeries.aligned(bars, Sma.series(closes, MovingAverages.SMA_MID)),
            sma200 = AlignedSeries.aligned(bars, Sma.series(closes, MovingAverages.SMA_LONG)),
            ichimoku = Ichimoku.compute(bars),
            volumeProfile = VolumeProfile.compute(bars),
        )
    }
}

data class DayOneIndicators(
    val engineId: String,
    val engineVersion: String,
    val cutoff: SnapshotCutoff,
    val fence: FenceReport,
    val bars: List<IndicatorBar>,
    val rsi14: AlignedSeries,
    val volume: AlignedSeries,
    val volumeSma20: AlignedSeries,
    val ema9: AlignedSeries,
    val ema21: AlignedSeries,
    val sma50: AlignedSeries,
    val sma200: AlignedSeries,
    val ichimoku: IchimokuResult,
    /** Last-24 snapshot as-of [fence.lastCloseTimeMs] — not a per-bar series. */
    val volumeProfile: VolumeProfileResult,
) {
    val sma200WarmupIncomplete: Boolean get() = sma200.finiteCount() == 0
}
