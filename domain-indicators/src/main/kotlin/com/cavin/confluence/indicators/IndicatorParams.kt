package com.cavin.confluence.indicators

/**
 * Parameter pack for [IndicatorCalc] so Mobile V2 can recompute on settings / TF
 * change. Omitting params (or passing [DEFAULT]) is the day-one five-auto set:
 * EMA 9/21, SMA 50/200, RSI 14, Ichimoku 9/26/52/26, volume SMA 20, VP last-24.
 */
data class IndicatorParams(
    val movingAverages: List<MaSpec> = DEFAULT_MOVING_AVERAGES,
    val rsiPeriod: Int = Rsi.DEFAULT_PERIOD,
    val ichimoku: IchimokuParams = IchimokuParams.DEFAULT,
    val volumeSmaPeriod: Int = VolumeStats.SMA_PERIOD,
    val volumeProfileLookback: Int = VolumeProfile.LOOKBACK_BARS,
) {
    init {
        require(rsiPeriod > 0)
        require(volumeSmaPeriod > 0)
        require(volumeProfileLookback > 0)
    }

    companion object {
        val DEFAULT_MOVING_AVERAGES: List<MaSpec> = listOf(
            MaSpec(MaType.EMA, MovingAverages.EMA_FAST),
            MaSpec(MaType.EMA, MovingAverages.EMA_SLOW),
            MaSpec(MaType.SMA, MovingAverages.SMA_MID),
            MaSpec(MaType.SMA, MovingAverages.SMA_LONG),
        )

        val DEFAULT: IndicatorParams = IndicatorParams()
    }
}

enum class MaType {
    SMA,
    EMA,
}

data class MaSpec(
    val type: MaType,
    val period: Int,
) {
    init {
        require(period > 0)
    }

    /** Stable Mobile key, e.g. `EMA:9`, `SMA:50`. */
    val key: String get() = "${type.name}:$period"
}

data class MovingAverageSeries(
    val spec: MaSpec,
    val series: AlignedSeries,
) {
    val type: MaType get() = spec.type
    val period: Int get() = spec.period
    val key: String get() = spec.key
}

data class IchimokuParams(
    val tenkanPeriod: Int = Ichimoku.TENKAN_PERIOD,
    val kijunPeriod: Int = Ichimoku.KIJUN_PERIOD,
    val senkouBPeriod: Int = Ichimoku.SENKOU_B_PERIOD,
    val displacement: Int = Ichimoku.DISPLACEMENT,
) {
    init {
        require(tenkanPeriod > 0)
        require(kijunPeriod > 0)
        require(senkouBPeriod > 0)
        require(displacement >= 0)
    }

    companion object {
        val DEFAULT: IchimokuParams = IchimokuParams()
    }
}
