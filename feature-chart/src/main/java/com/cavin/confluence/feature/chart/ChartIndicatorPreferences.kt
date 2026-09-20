package com.cavin.confluence.feature.chart

import android.content.Context
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.IchimokuParams
import com.cavin.confluence.indicators.MaSpec
import com.cavin.confluence.indicators.MaType

/**
 * Persist V2 show/hide, swatch wells, and [IndicatorParams]. Immediate chart
 * reflect is ViewModel state; this store survives process death.
 */
class ChartIndicatorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadVisibility(): ChartOverlayVisibility = ChartOverlayVisibility(
        ma0 = prefs.getBoolean(keyVisible(ChartIndicatorId.Ma0), prefs.getBoolean("visible_Ema9", true)),
        ma1 = prefs.getBoolean(keyVisible(ChartIndicatorId.Ma1), prefs.getBoolean("visible_Sma21", true)),
        ma2 = prefs.getBoolean(keyVisible(ChartIndicatorId.Ma2), false),
        ma3 = prefs.getBoolean(keyVisible(ChartIndicatorId.Ma3), false),
        ichimoku = prefs.getBoolean(keyVisible(ChartIndicatorId.Ichimoku), true),
        volumeProfile = prefs.getBoolean(keyVisible(ChartIndicatorId.VolumeProfile), true),
        rsi = prefs.getBoolean(keyVisible(ChartIndicatorId.Rsi14), true),
        volume = prefs.getBoolean(keyVisible(ChartIndicatorId.VolumeRibbon), true),
    )

    fun saveVisibility(visibility: ChartOverlayVisibility) {
        val editor = prefs.edit()
        for (id in ChartIndicatorId.entries) {
            editor.putBoolean(keyVisible(id), visibility.isVisible(id))
        }
        editor.apply()
    }

    fun loadPalette(): ChartIndicatorPalette {
        fun swatch(key: String, fallback: OverlaySwatch): OverlaySwatch {
            val raw = prefs.getString(key, null) ?: return fallback
            return OverlaySwatch.entries.firstOrNull { it.name == raw } ?: fallback
        }
        val defaults = ChartIndicatorPalette.Defaults
        return ChartIndicatorPalette(
            ma0 = swatch(keySwatch("ma0"), swatch(keySwatch("ema9"), defaults.ma0)),
            ma1 = swatch(keySwatch("ma1"), swatch(keySwatch("sma21"), defaults.ma1)),
            ma2 = swatch(keySwatch("ma2"), defaults.ma2),
            ma3 = swatch(keySwatch("ma3"), defaults.ma3),
            ichimokuTenkan = swatch(keySwatch("ichiTenkan"), defaults.ichimokuTenkan),
            ichimokuKijun = swatch(keySwatch("ichiKijun"), defaults.ichimokuKijun),
            ichimokuCloud = swatch(keySwatch("ichiCloud"), defaults.ichimokuCloud),
            volumeProfile = swatch(keySwatch("vp"), defaults.volumeProfile),
            rsi = swatch(keySwatch("rsi"), defaults.rsi),
            rsiGuide = swatch(keySwatch("rsiGuide"), defaults.rsiGuide),
            volumeBull = swatch(keySwatch("volBull"), defaults.volumeBull),
            volumeBear = swatch(keySwatch("volBear"), defaults.volumeBear),
        )
    }

    fun savePalette(palette: ChartIndicatorPalette) {
        prefs.edit()
            .putString(keySwatch("ma0"), palette.ma0.name)
            .putString(keySwatch("ma1"), palette.ma1.name)
            .putString(keySwatch("ma2"), palette.ma2.name)
            .putString(keySwatch("ma3"), palette.ma3.name)
            .putString(keySwatch("ichiTenkan"), palette.ichimokuTenkan.name)
            .putString(keySwatch("ichiKijun"), palette.ichimokuKijun.name)
            .putString(keySwatch("ichiCloud"), palette.ichimokuCloud.name)
            .putString(keySwatch("vp"), palette.volumeProfile.name)
            .putString(keySwatch("rsi"), palette.rsi.name)
            .putString(keySwatch("rsiGuide"), palette.rsiGuide.name)
            .putString(keySwatch("volBull"), palette.volumeBull.name)
            .putString(keySwatch("volBear"), palette.volumeBear.name)
            .apply()
    }

    fun loadParams(): IndicatorParams {
        val defaults = IndicatorParams.DEFAULT
        fun ma(index: Int): MaSpec {
            val fallback = defaults.movingAverages.getOrElse(index) { MaSpec(MaType.EMA, 9) }
            val typeName = prefs.getString("ma${index}_type", fallback.type.name)
            val type = MaType.entries.firstOrNull { it.name == typeName } ?: fallback.type
            val period = prefs.getInt("ma${index}_period", fallback.period).coerceAtLeast(1)
            return MaSpec(type, period)
        }
        val mas = List(defaults.movingAverages.size) { ma(it) }
        val ichi = IchimokuParams(
            tenkanPeriod = prefs.getInt("ichi_tenkan", defaults.ichimoku.tenkanPeriod).coerceAtLeast(1),
            kijunPeriod = prefs.getInt("ichi_kijun", defaults.ichimoku.kijunPeriod).coerceAtLeast(1),
            senkouBPeriod = prefs.getInt("ichi_senkou", defaults.ichimoku.senkouBPeriod).coerceAtLeast(1),
            displacement = defaults.ichimoku.displacement,
        )
        return IndicatorParams(
            movingAverages = mas,
            rsiPeriod = prefs.getInt("rsi_period", defaults.rsiPeriod).coerceAtLeast(1),
            ichimoku = ichi,
            volumeSmaPeriod = prefs.getInt("vol_sma", defaults.volumeSmaPeriod).coerceAtLeast(1),
            volumeProfileLookback = prefs.getInt("vp_lookback", defaults.volumeProfileLookback).coerceAtLeast(1),
        )
    }

    fun saveParams(params: IndicatorParams) {
        val editor = prefs.edit()
        params.movingAverages.forEachIndexed { index, spec ->
            editor.putString("ma${index}_type", spec.type.name)
            editor.putInt("ma${index}_period", spec.period)
        }
        editor.putInt("rsi_period", params.rsiPeriod)
        editor.putInt("ichi_tenkan", params.ichimoku.tenkanPeriod)
        editor.putInt("ichi_kijun", params.ichimoku.kijunPeriod)
        editor.putInt("ichi_senkou", params.ichimoku.senkouBPeriod)
        editor.putInt("vol_sma", params.volumeSmaPeriod)
        editor.putInt("vp_lookback", params.volumeProfileLookback)
        editor.apply()
    }

    companion object {
        private const val PREFS = "confluence_chart_indicators"

        fun keyVisible(id: ChartIndicatorId): String = "visible_${id.name}"

        fun keySwatch(name: String): String = "swatch_$name"
    }
}
