package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Dimens tokens — screens use these, not magic numbers. */
object ConfluenceDimens {
    val glassCorner: Dp = 14.dp
    val glassBorder: Dp = 1.dp
    val glassBlur: Dp = 18.dp
    val glassPadding: Dp = 16.dp
    val glassPaddingTight: Dp = 12.dp
    /** Equal peek on every side; under-layer is centered on the face (SE offset superseded). */
    val acrylicUnderInset: Dp = 7.dp
    /**
     * Soft halo stroke around the acrylic pane. PlasmaAcrylic's centered
     * under-layer uses this as stroke width; keep equal to [acrylicUnderInset]
     * so the pane and halo stay a centered pair.
     */
    val acrylicHaloStroke: Dp = acrylicUnderInset
    /**
     * Draw extent beyond the face (inset + outer half of halo stroke, twice).
     * With stroke = inset this is 2× [acrylicUnderInset] (14dp). Stack gaps
     * must be at least twice this or neighboring glows collide at ~390dp.
     */
    val acrylicHaloReserve: Dp = acrylicUnderInset + acrylicHaloStroke
    /** Acrylic-edge stroke alpha — locked 20–28%. */
    const val acrylicEdgeAlpha = 0.24f
    val bracketLength: Dp = 16.dp
    val bracketStroke: Dp = 1.5.dp
    val plasmaGlowPad: Dp = 12.dp
    val focusRing: Dp = 3.dp
    val dockCorner: Dp = 14.dp
    val dockCellCorner: Dp = 10.dp
    val dockElevationGap: Dp = 24.dp
    val dockHeight: Dp = 60.dp
    /** Dock / tab brand spot (22–24dp lock). */
    val brandMarkDock: Dp = 22.dp
    val dockIcon: Dp = brandMarkDock
    /** In-app HUD mark in screen headers (24–28dp lock). */
    val brandMarkHeader: Dp = 28.dp
    /** Splash mark on void (96–120dp lock). */
    val brandMarkSplash: Dp = 108.dp
    val heroPriceShadow: Dp = 24.dp
    val sparklineHeight: Dp = 64.dp
    val meterHeight: Dp = 88.dp
    val meterBarHeight: Dp = 6.dp
    val hudDivider: Dp = 1.dp
    val accentBar: Dp = 2.dp
    val chipRadius: Dp = 999.dp
    val pressScale = 0.98f

    // Chart craft — Approach A Compose Canvas (TradingView-grade)
    val chartMinCandleWidth: Dp = 4.dp
    val chartMaxCandleWidth: Dp = 28.dp
    val chartDefaultCandleWidth: Dp = 10.dp
    val chartPlotLeftPad: Dp = 8.dp
    val chartPriceAxisWidth: Dp = 56.dp
    val chartPlotTopPad: Dp = 8.dp
    val chartTimeAxisHeight: Dp = 22.dp
    val chartBodyBloom: Dp = 2.dp
    val chartWickStroke: Dp = 1.dp
    val chartGridStroke: Dp = 1.dp
    val chartHairlineStroke: Dp = 1.dp
    val chartCrosshairStroke: Dp = 1.dp
    val chartMinDojiBody: Dp = 1.5.dp
    val chartAxisLabelSize: Dp = 10.dp
    val chartTagPaddingH: Dp = 5.dp
    val chartTagPaddingV: Dp = 3.dp
    val chartTagCorner: Dp = 4.dp
    val chartAxisTick: Dp = 4.dp
    val chartMinTimeLabelGap: Dp = 56.dp
    val chartDefaultVisibleCandles: Int = 48
    const val chartVolumeFraction = 0.18f // V1 lock: volume pane is exactly 18% of price+volume plot
    const val chartBodyFraction = 0.62f
    const val chartYPadFraction = 0.06f
    const val chartVolumeAlpha = 0.35f
    const val chartLastPriceHairlineAlpha = 0.20f
    const val chartLastCandleBoost = 0.14f
    const val chartOtherCandleAlpha = 0.90f
    const val chartBloomAlpha = 0.16f
    const val chartBloomAlphaLast = 0.30f
    const val chartMinVisibleCandles = 8
}
