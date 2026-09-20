package com.cavin.confluence.feature.chart

import androidx.compose.ui.graphics.Color
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.IchimokuParams
import com.cavin.confluence.indicators.MaSpec
import com.cavin.confluence.indicators.MaType

/**
 * V2 settings rows (Phase B V2 2026-09-20). Four MA slots bind to
 * [IndicatorParams.movingAverages] (defaults EMA 9/21, SMA 50/200).
 */
enum class ChartIndicatorId {
    Ma0,
    Ma1,
    Ma2,
    Ma3,
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
    ChartIndicatorId.Ma0,
    ChartIndicatorId.Ma1,
    ChartIndicatorId.Ma2,
    ChartIndicatorId.Ma3,
    ChartIndicatorId.Ichimoku,
    ChartIndicatorId.VolumeProfile,
    -> ChartIndicatorGroup.Overlays
    ChartIndicatorId.Rsi14 -> ChartIndicatorGroup.Oscillators
    ChartIndicatorId.VolumeRibbon -> ChartIndicatorGroup.Volume
}

fun ChartIndicatorId.maIndex(): Int? = when (this) {
    ChartIndicatorId.Ma0 -> 0
    ChartIndicatorId.Ma1 -> 1
    ChartIndicatorId.Ma2 -> 2
    ChartIndicatorId.Ma3 -> 3
    ChartIndicatorId.Ichimoku,
    ChartIndicatorId.VolumeProfile,
    ChartIndicatorId.Rsi14,
    ChartIndicatorId.VolumeRibbon,
    -> null
}

fun ChartIndicatorId.settingsTitle(params: IndicatorParams): String = when (this) {
    ChartIndicatorId.Ma0,
    ChartIndicatorId.Ma1,
    ChartIndicatorId.Ma2,
    ChartIndicatorId.Ma3,
    -> params.movingAverages.getOrNull(maIndex()!!)?.label() ?: "MA"
    ChartIndicatorId.Ichimoku -> "Ichimoku Cloud"
    ChartIndicatorId.VolumeProfile -> "Volume Profile"
    ChartIndicatorId.Rsi14 -> "RSI ${params.rsiPeriod}"
    ChartIndicatorId.VolumeRibbon -> "Volume (ribbon)"
}

fun MaSpec.label(): String = "${type.name} $period"

fun ChartIndicatorGroup.sectionHeader(): String = when (this) {
    ChartIndicatorGroup.Overlays -> "OVERLAYS"
    ChartIndicatorGroup.Oscillators -> "OSCILLATORS"
    ChartIndicatorGroup.Volume -> "VOLUME"
}

/**
 * Per-indicator show/hide. MA slots 0–1 default on (V2 overlay-first density);
 * 50/200 stay in [IndicatorParams] and can be turned on in settings.
 */
data class ChartOverlayVisibility(
    val ma0: Boolean = true,
    val ma1: Boolean = true,
    val ma2: Boolean = false,
    val ma3: Boolean = false,
    val ichimoku: Boolean = true,
    val volumeProfile: Boolean = true,
    val rsi: Boolean = true,
    val volume: Boolean = true,
) {
    fun isVisible(id: ChartIndicatorId): Boolean = when (id) {
        ChartIndicatorId.Ma0 -> ma0
        ChartIndicatorId.Ma1 -> ma1
        ChartIndicatorId.Ma2 -> ma2
        ChartIndicatorId.Ma3 -> ma3
        ChartIndicatorId.Ichimoku -> ichimoku
        ChartIndicatorId.VolumeProfile -> volumeProfile
        ChartIndicatorId.Rsi14 -> rsi
        ChartIndicatorId.VolumeRibbon -> volume
    }

    fun isMaVisible(index: Int): Boolean = when (index) {
        0 -> ma0
        1 -> ma1
        2 -> ma2
        3 -> ma3
        else -> false
    }

    fun toggle(id: ChartIndicatorId): ChartOverlayVisibility = when (id) {
        ChartIndicatorId.Ma0 -> copy(ma0 = !ma0)
        ChartIndicatorId.Ma1 -> copy(ma1 = !ma1)
        ChartIndicatorId.Ma2 -> copy(ma2 = !ma2)
        ChartIndicatorId.Ma3 -> copy(ma3 = !ma3)
        ChartIndicatorId.Ichimoku -> copy(ichimoku = !ichimoku)
        ChartIndicatorId.VolumeProfile -> copy(volumeProfile = !volumeProfile)
        ChartIndicatorId.Rsi14 -> copy(rsi = !rsi)
        ChartIndicatorId.VolumeRibbon -> copy(volume = !volume)
    }

    fun activeCount(): Int = ChartIndicatorId.entries.count { isVisible(it) }

    companion object {
        val Defaults: ChartOverlayVisibility = ChartOverlayVisibility()
        val AllOn: ChartOverlayVisibility = ChartOverlayVisibility(
            ma2 = true,
            ma3 = true,
        )
    }
}

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
    val ma0: OverlaySwatch = OverlaySwatch.Plasma,
    val ma1: OverlaySwatch = OverlaySwatch.Bloom,
    val ma2: OverlaySwatch = OverlaySwatch.Ice,
    val ma3: OverlaySwatch = OverlaySwatch.Muted,
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
        ChartIndicatorId.Ma0 -> listOf(ma0)
        ChartIndicatorId.Ma1 -> listOf(ma1)
        ChartIndicatorId.Ma2 -> listOf(ma2)
        ChartIndicatorId.Ma3 -> listOf(ma3)
        ChartIndicatorId.Ichimoku -> listOf(ichimokuTenkan, ichimokuKijun, ichimokuCloud)
        ChartIndicatorId.VolumeProfile -> listOf(volumeProfile)
        ChartIndicatorId.Rsi14 -> listOf(rsi, rsiGuide)
        ChartIndicatorId.VolumeRibbon -> listOf(volumeBull, volumeBear)
    }

    fun maSwatch(index: Int): OverlaySwatch = when (index) {
        0 -> ma0
        1 -> ma1
        2 -> ma2
        3 -> ma3
        else -> OverlaySwatch.Plasma
    }

    fun cycleWell(id: ChartIndicatorId, wellIndex: Int): ChartIndicatorPalette {
        val i = wellIndex.coerceAtLeast(0)
        return when (id) {
            ChartIndicatorId.Ma0 -> copy(ma0 = ma0.next())
            ChartIndicatorId.Ma1 -> copy(ma1 = ma1.next())
            ChartIndicatorId.Ma2 -> copy(ma2 = ma2.next())
            ChartIndicatorId.Ma3 -> copy(ma3 = ma3.next())
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

    companion object {
        val Defaults: ChartIndicatorPalette = ChartIndicatorPalette()
    }
}

/** Short, decisive period/type choices — not a full TradingView menu. */
object ChartParamChoices {
    val maTypes: List<MaType> = listOf(MaType.EMA, MaType.SMA)
    val maPeriods: List<Int> = listOf(8, 9, 21, 50, 200)
    val rsiPeriods: List<Int> = listOf(7, 14, 21)
    val ichimokuTenkan: List<Int> = listOf(7, 9, 12)
    val ichimokuKijun: List<Int> = listOf(22, 26, 33)
    val ichimokuSenkou: List<Int> = listOf(44, 52, 66)
    val volumeSmaPeriods: List<Int> = listOf(10, 20, 50)
    val vpLookbacks: List<Int> = listOf(12, 24, 48)
}

fun IndicatorParams.withMa(index: Int, spec: MaSpec): IndicatorParams {
    val next = movingAverages.toMutableList()
    if (index !in next.indices) return this
    next[index] = spec
    return copy(movingAverages = next)
}

fun IndicatorParams.withIchimoku(block: (IchimokuParams) -> IchimokuParams): IndicatorParams =
    copy(ichimoku = block(ichimoku))
