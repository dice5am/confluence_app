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
 *
 * Parameterized overloads take [IndicatorParams] so Mobile can recompute on
 * settings / TF change. Omitting params uses [IndicatorParams.DEFAULT] (the
 * day-one five-auto set) and is equivalent to the no-params overloads.
 */
object IndicatorCalc {
    const val ENGINE_ID: String = "alt-apply-calc"
    const val ENGINE_VERSION: String = "0.1.0-five-autos"

    fun evaluate(bars: List<IndicatorBar>, cutoff: SnapshotCutoff): DayOneIndicators =
        evaluate(bars, cutoff, IndicatorParams.DEFAULT)

    fun evaluate(
        bars: List<IndicatorBar>,
        cutoff: SnapshotCutoff,
        params: IndicatorParams,
    ): DayOneIndicators {
        val fenced = CandleFence.apply(bars, cutoff)
        return computeFromFenced(fenced, params)
    }

    /**
     * Causal / as-of evaluation: apply the global [cutoff] fence, then keep only
     * bars with `closeTimeMs <= asOfCloseTimeMs`.
     *
     * Overlay points at bar `i` MUST come from this window (or an equivalent
     * prefix). [evaluate] on the full fenced series is as-of the last kept bar
     * only — it is not a pre-baked historical store. In particular
     * [DayOneIndicators.volumeProfile] on a full evaluate is the last-N
     * snapshot ending at the last fenced bar, not the VP that was known at
     * an earlier bar.
     *
     * Omitting [params] uses [IndicatorParams.DEFAULT]. As-of fence semantics
     * are independent of params.
     */
    fun evaluateAsOf(
        bars: List<IndicatorBar>,
        cutoff: SnapshotCutoff,
        asOfCloseTimeMs: Long,
    ): DayOneIndicators = evaluateAsOf(bars, cutoff, asOfCloseTimeMs, IndicatorParams.DEFAULT)

    fun evaluateAsOf(
        bars: List<IndicatorBar>,
        cutoff: SnapshotCutoff,
        asOfCloseTimeMs: Long,
        params: IndicatorParams,
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
            params,
        )
    }

    /**
     * Update after a newly closed (or revised-tip) bar. Forming bars and
     * `closeTimeMs > cutoffMs` are ignored, matching [CandleFence].
     *
     * The no-params overload keeps [DayOneIndicators.params] from [current].
     * Pass [params] to recompute the existing window under a new settings pack.
     */
    fun onClosedBar(current: DayOneIndicators, bar: IndicatorBar): DayOneIndicators =
        onClosedBar(current, bar, current.params)

    fun onClosedBar(
        current: DayOneIndicators,
        bar: IndicatorBar,
        params: IndicatorParams,
    ): DayOneIndicators {
        if (!bar.isFinal) {
            return dropBar(current, params, forming = true)
        }
        if (bar.closeTimeMs > current.cutoff.cutoffMs) {
            return dropBar(current, params, forming = false)
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
            params,
        )
    }

    private fun dropBar(
        current: DayOneIndicators,
        params: IndicatorParams,
        forming: Boolean,
    ): DayOneIndicators {
        val nextFence = if (forming) {
            current.fence.copy(
                inputCount = current.fence.inputCount + 1,
                droppedFormingCount = current.fence.droppedFormingCount + 1,
            )
        } else {
            current.fence.copy(
                inputCount = current.fence.inputCount + 1,
                droppedOvershootCount = current.fence.droppedOvershootCount + 1,
            )
        }
        if (params == current.params) {
            return current.copy(fence = nextFence)
        }
        return computeFromFenced(
            FencedWindow(
                cutoff = current.cutoff,
                inputCount = nextFence.inputCount,
                bars = current.bars,
                droppedFormingCount = nextFence.droppedFormingCount,
                droppedOvershootCount = nextFence.droppedOvershootCount,
            ),
            params,
        )
    }

    private fun computeFromFenced(
        fenced: FencedWindow,
        params: IndicatorParams,
    ): DayOneIndicators {
        val bars = fenced.bars
        val closes = DoubleArray(bars.size) { bars[it].close }
        val volumes = DoubleArray(bars.size) { bars[it].volume }
        val movingAverages = params.movingAverages.map { spec ->
            MovingAverageSeries(
                spec = spec,
                series = AlignedSeries.aligned(bars, maSeries(closes, spec)),
            )
        }

        return DayOneIndicators(
            engineId = ENGINE_ID,
            engineVersion = ENGINE_VERSION,
            cutoff = fenced.cutoff,
            fence = fenced.report(),
            bars = bars,
            params = params,
            rsi14 = AlignedSeries.aligned(bars, Rsi.series(closes, params.rsiPeriod)),
            volume = AlignedSeries.copies(bars) { it.volume },
            volumeSma20 = AlignedSeries.aligned(bars, Sma.series(volumes, params.volumeSmaPeriod)),
            ema9 = lookupMa(movingAverages, bars, MaType.EMA, MovingAverages.EMA_FAST),
            ema21 = lookupMa(movingAverages, bars, MaType.EMA, MovingAverages.EMA_SLOW),
            sma50 = lookupMa(movingAverages, bars, MaType.SMA, MovingAverages.SMA_MID),
            sma200 = lookupMa(movingAverages, bars, MaType.SMA, MovingAverages.SMA_LONG),
            movingAverages = movingAverages,
            ichimoku = Ichimoku.compute(bars, params.ichimoku),
            volumeProfile = VolumeProfile.compute(bars, params.volumeProfileLookback),
        )
    }

    private fun maSeries(closes: DoubleArray, spec: MaSpec): List<Double?> {
        return when (spec.type) {
            MaType.SMA -> Sma.series(closes, spec.period)
            MaType.EMA -> Ema.series(closes, spec.period)
        }
    }

    private fun lookupMa(
        movingAverages: List<MovingAverageSeries>,
        bars: List<IndicatorBar>,
        type: MaType,
        period: Int,
    ): AlignedSeries {
        val hit = movingAverages.firstOrNull { it.spec.type == type && it.spec.period == period }
        return hit?.series ?: AlignedSeries.aligned(bars, List(bars.size) { null })
    }
}

data class DayOneIndicators(
    val engineId: String,
    val engineVersion: String,
    val cutoff: SnapshotCutoff,
    val fence: FenceReport,
    val bars: List<IndicatorBar>,
    val params: IndicatorParams,
    val rsi14: AlignedSeries,
    val volume: AlignedSeries,
    val volumeSma20: AlignedSeries,
    val ema9: AlignedSeries,
    val ema21: AlignedSeries,
    val sma50: AlignedSeries,
    val sma200: AlignedSeries,
    val movingAverages: List<MovingAverageSeries>,
    val ichimoku: IchimokuResult,
    /** Last-N snapshot as-of [fence.lastCloseTimeMs] — not a per-bar series. */
    val volumeProfile: VolumeProfileResult,
) {
    val sma200WarmupIncomplete: Boolean get() = sma200.finiteCount() == 0

    /** RSI series at [params]`.rsiPeriod` (legacy field name is `rsi14`). */
    val rsi: AlignedSeries get() = rsi14

    /** Volume SMA at [params]`.volumeSmaPeriod` (legacy field name is `volumeSma20`). */
    val volumeSma: AlignedSeries get() = volumeSma20

    fun ma(type: MaType, period: Int): AlignedSeries? =
        movingAverages.firstOrNull { it.spec.type == type && it.spec.period == period }?.series

    fun ma(key: String): AlignedSeries? =
        movingAverages.firstOrNull { it.key == key }?.series
}
