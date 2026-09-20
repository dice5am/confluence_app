package com.cavin.confluence.feature.chart

import androidx.compose.ui.graphics.Color
import com.cavin.confluence.core.ui.theme.ConfluenceColors

/**
 * V2 settings rows (Phase B V2 2026-09-20). SMA 21 is the board label for the
 * engine's EMA-21 overlay — five-autos lock, no SMA-21 formula in UI.
 */
enum class ChartIndicatorId {
    Ema9,
    Sma21,
    Ichimoku,
    VolumeProfile,
    Rsi14,
    VolumeRibbon,
}

enum class ChartIndicatorGroup {
    Overlays,
    Oscillators,
    Volume,
}

fun ChartIndicatorId.group(): ChartIndicatorGroup = when (this) {
    ChartIndicatorId.Ema9,
    ChartIndicatorId.Sma21,
    ChartIndicatorId.Ichimoku,
    ChartIndicatorId.VolumeProfile,
    -> ChartIndicatorGroup.Overlays
    ChartIndicatorId.Rsi14 -> ChartIndicatorGroup.Oscillators
    ChartIndicatorId.VolumeRibbon -> ChartIndicatorGroup.Volume
}

fun ChartIndicatorId.settingsTitle(): String = when (this) {
    ChartIndicatorId.Ema9 -> "EMA 9"
    ChartIndicatorId.Sma21 -> "SMA 21"
    ChartIndicatorId.Ichimoku -> "Ichimoku Cloud"
    ChartIndicatorId.VolumeProfile -> "Volume Profile"
    ChartIndicatorId.Rsi14 -> "RSI 14"
    ChartIndicatorId.VolumeRibbon -> "Volume (ribbon)"
}

fun ChartIndicatorId.legendLabel(): String? = when (this) {
    ChartIndicatorId.Ema9 -> "EMA 9"
    ChartIndicatorId.Sma21 -> "SMA 21"
    ChartIndicatorId.Ichimoku -> "Ichimoku"
    ChartIndicatorId.VolumeProfile -> "VP"
    ChartIndicatorId.Rsi14 -> null
    ChartIndicatorId.VolumeRibbon -> null
}

fun ChartIndicatorId.wellCount(): Int = when (this) {
    ChartIndicatorId.Ema9,
    ChartIndicatorId.Sma21,
    ChartIndicatorId.VolumeProfile,
    -> 1
    ChartIndicatorId.Rsi14,
    ChartIndicatorId.VolumeRibbon,
    -> 2
    ChartIndicatorId.Ichimoku -> 3
}

fun ChartIndicatorGroup.sectionHeader(): String = when (this) {
    ChartIndicatorGroup.Overlays -> "OVERLAYS"
    ChartIndicatorGroup.Oscillators -> "OSCILLATORS"
    ChartIndicatorGroup.Volume -> "VOLUME"
}

/**
 * Per-indicator show/hide. Five autos only (EMA9 + EMA21-as-SMA21, Ichimoku,
 * VP, RSI, volume ribbon). SMA 50/200 stay on the calc result, not V2 chrome.
 */
data class ChartOverlayVisibility(
    val ema9: Boolean = true,
    val sma21: Boolean = true,
    val ichimoku: Boolean = true,
    val volumeProfile: Boolean = true,
    val rsi: Boolean = true,
    val volume: Boolean = true,
) {
    fun isVisible(id: ChartIndicatorId): Boolean = when (id) {
        ChartIndicatorId.Ema9 -> ema9
        ChartIndicatorId.Sma21 -> sma21
        ChartIndicatorId.Ichimoku -> ichimoku
        ChartIndicatorId.VolumeProfile -> volumeProfile
        ChartIndicatorId.Rsi14 -> rsi
        ChartIndicatorId.VolumeRibbon -> volume
    }

    fun toggle(id: ChartIndicatorId): ChartOverlayVisibility = when (id) {
        ChartIndicatorId.Ema9 -> copy(ema9 = !ema9)
        ChartIndicatorId.Sma21 -> copy(sma21 = !sma21)
        ChartIndicatorId.Ichimoku -> copy(ichimoku = !ichimoku)
        ChartIndicatorId.VolumeProfile -> copy(volumeProfile = !volumeProfile)
        ChartIndicatorId.Rsi14 -> copy(rsi = !rsi)
        ChartIndicatorId.VolumeRibbon -> copy(volume = !volume)
    }

    fun activeCount(): Int = ChartIndicatorId.entries.count { isVisible(it) }

    companion object {
        val AllOn: ChartOverlayVisibility = ChartOverlayVisibility()
    }
}

/**
 * F1×B1 swatches only — cycling wells never invents hex.
 */
enum class OverlaySwatch {
    Plasma,
    Bloom,
    Ice,
    Acrylic,
    Bracket,
    Muted,
    GradEnd,
    Pos,
    Neg,
    Dim,
}

fun OverlaySwatch.toColor(): Color = when (this) {
    OverlaySwatch.Plasma -> ConfluenceColors.Plasma
    OverlaySwatch.Bloom -> ConfluenceColors.Bloom
    OverlaySwatch.Ice -> ConfluenceColors.Ice
    OverlaySwatch.Acrylic -> ConfluenceColors.AcrylicEdge
    OverlaySwatch.Bracket -> ConfluenceColors.Bracket
    OverlaySwatch.Muted -> ConfluenceColors.Muted
    OverlaySwatch.GradEnd -> ConfluenceColors.GradEnd
    OverlaySwatch.Pos -> ConfluenceColors.Pos
    OverlaySwatch.Neg -> ConfluenceColors.Neg
    OverlaySwatch.Dim -> ConfluenceColors.Dim
}

fun OverlaySwatch.next(): OverlaySwatch {
    val all = OverlaySwatch.entries
    return all[(ordinal + 1) % all.size]
}

data class ChartIndicatorPalette(
    val ema9: OverlaySwatch = OverlaySwatch.Plasma,
    val sma21: OverlaySwatch = OverlaySwatch.Bloom,
    val ichimokuTenkan: OverlaySwatch = OverlaySwatch.Ice,
    val ichimokuKijun: OverlaySwatch = OverlaySwatch.Bloom,
    val ichimokuCloud: OverlaySwatch = OverlaySwatch.Acrylic,
    val volumeProfile: OverlaySwatch = OverlaySwatch.Plasma,
    val rsi: OverlaySwatch = OverlaySwatch.Plasma,
    val rsiGuide: OverlaySwatch = OverlaySwatch.Dim,
    val volumeBull: OverlaySwatch = OverlaySwatch.Pos,
    val volumeBear: OverlaySwatch = OverlaySwatch.Neg,
) {
    fun wells(id: ChartIndicatorId): List<OverlaySwatch> = when (id) {
        ChartIndicatorId.Ema9 -> listOf(ema9)
        ChartIndicatorId.Sma21 -> listOf(sma21)
        ChartIndicatorId.Ichimoku -> listOf(ichimokuTenkan, ichimokuKijun, ichimokuCloud)
        ChartIndicatorId.VolumeProfile -> listOf(volumeProfile)
        ChartIndicatorId.Rsi14 -> listOf(rsi, rsiGuide)
        ChartIndicatorId.VolumeRibbon -> listOf(volumeBull, volumeBear)
    }

    fun cycleWell(id: ChartIndicatorId, wellIndex: Int): ChartIndicatorPalette {
        val i = wellIndex.coerceAtLeast(0)
        return when (id) {
            ChartIndicatorId.Ema9 -> copy(ema9 = ema9.next())
            ChartIndicatorId.Sma21 -> copy(sma21 = sma21.next())
            ChartIndicatorId.Ichimoku -> when (i) {
                0 -> copy(ichimokuTenkan = ichimokuTenkan.next())
                1 -> copy(ichimokuKijun = ichimokuKijun.next())
                else -> copy(ichimokuCloud = ichimokuCloud.next())
            }
            ChartIndicatorId.VolumeProfile -> copy(volumeProfile = volumeProfile.next())
            ChartIndicatorId.Rsi14 -> if (i == 0) {
                copy(rsi = rsi.next())
            } else {
                copy(rsiGuide = rsiGuide.next())
            }
            ChartIndicatorId.VolumeRibbon -> if (i == 0) {
                copy(volumeBull = volumeBull.next())
            } else {
                copy(volumeBear = volumeBear.next())
            }
        }
    }

    fun legendColor(id: ChartIndicatorId): Color? = when (id) {
        ChartIndicatorId.Ema9 -> ema9.toColor()
        ChartIndicatorId.Sma21 -> sma21.toColor()
        ChartIndicatorId.Ichimoku -> ichimokuCloud.toColor()
        ChartIndicatorId.VolumeProfile -> volumeProfile.toColor()
        ChartIndicatorId.Rsi14 -> null
        ChartIndicatorId.VolumeRibbon -> null
    }

    companion object {
        val Defaults: ChartIndicatorPalette = ChartIndicatorPalette()
    }
}
