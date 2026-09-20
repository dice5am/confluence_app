package com.cavin.confluence.feature.chart

import android.content.Context

/**
 * Persist V2 indicator show/hide + swatch wells. Immediate chart reflect
 * is ViewModel state; this store survives process death.
 */
class ChartIndicatorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadVisibility(): ChartOverlayVisibility = ChartOverlayVisibility(
        ema9 = prefs.getBoolean(keyVisible(ChartIndicatorId.Ema9), true),
        sma21 = prefs.getBoolean(keyVisible(ChartIndicatorId.Sma21), true),
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
            ema9 = swatch(keySwatch("ema9"), defaults.ema9),
            sma21 = swatch(keySwatch("sma21"), defaults.sma21),
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
            .putString(keySwatch("ema9"), palette.ema9.name)
            .putString(keySwatch("sma21"), palette.sma21.name)
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

    companion object {
        private const val PREFS = "confluence_chart_indicators"

        fun keyVisible(id: ChartIndicatorId): String = "visible_${id.name}"

        fun keySwatch(name: String): String = "swatch_$name"
    }
}
