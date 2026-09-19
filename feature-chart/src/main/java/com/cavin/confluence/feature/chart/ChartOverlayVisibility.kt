package com.cavin.confluence.feature.chart

/**
 * Day-one overlay families the user can show/hide. Five autos only.
 */
enum class ChartOverlayFamily {
    MovingAverages,
    Ichimoku,
    VolumeProfile,
    Rsi,
    Volume,
}

data class ChartOverlayVisibility(
    val movingAverages: Boolean = true,
    val ichimoku: Boolean = true,
    val volumeProfile: Boolean = true,
    val rsi: Boolean = true,
    val volume: Boolean = true,
) {
    fun isVisible(family: ChartOverlayFamily): Boolean = when (family) {
        ChartOverlayFamily.MovingAverages -> movingAverages
        ChartOverlayFamily.Ichimoku -> ichimoku
        ChartOverlayFamily.VolumeProfile -> volumeProfile
        ChartOverlayFamily.Rsi -> rsi
        ChartOverlayFamily.Volume -> volume
    }

    fun toggle(family: ChartOverlayFamily): ChartOverlayVisibility = when (family) {
        ChartOverlayFamily.MovingAverages -> copy(movingAverages = !movingAverages)
        ChartOverlayFamily.Ichimoku -> copy(ichimoku = !ichimoku)
        ChartOverlayFamily.VolumeProfile -> copy(volumeProfile = !volumeProfile)
        ChartOverlayFamily.Rsi -> copy(rsi = !rsi)
        ChartOverlayFamily.Volume -> copy(volume = !volume)
    }

    companion object {
        val AllOn: ChartOverlayVisibility = ChartOverlayVisibility()
    }
}

internal fun ChartOverlayFamily.chipLabel(): String = when (this) {
    ChartOverlayFamily.MovingAverages -> "MA"
    ChartOverlayFamily.Ichimoku -> "ICHI"
    ChartOverlayFamily.VolumeProfile -> "VP"
    ChartOverlayFamily.Rsi -> "RSI"
    ChartOverlayFamily.Volume -> "VOL"
}
