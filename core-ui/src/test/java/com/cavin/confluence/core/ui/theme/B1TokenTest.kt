package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class B1TokenTest {
    @Test
    fun lockedPaletteHex() {
        assertEquals(0xFF060B14.toInt(), ConfluenceColors.Void.toArgb())
        assertEquals(0xFFF0F9FF.toInt(), ConfluenceColors.Text.toArgb())
        assertEquals(0xFF0EA5E9.toInt(), ConfluenceColors.Plasma.toArgb())
        assertEquals(0xFF38BDF8.toInt(), ConfluenceColors.Bloom.toArgb())
        assertEquals(0xFFBAE6FD.toInt(), ConfluenceColors.Ice.toArgb())
        assertEquals(0xFFE0F2FE.toInt(), ConfluenceColors.Bracket.toArgb())
        assertEquals(0xFF7DD3FC.toInt(), ConfluenceColors.Muted.toArgb())
        assertEquals(0xFFBAE6FD.toInt(), ConfluenceColors.Muted2.toArgb())
        assertEquals(0xFF22D3EE.toInt(), ConfluenceColors.AcrylicEdge.toArgb())
        assertEquals(0xFF64748B.toInt(), ConfluenceColors.Dim.toArgb())
        assertEquals(0xFFFB7185.toInt(), ConfluenceColors.Neg.toArgb())
        assertEquals(0xFF4ADE80.toInt(), ConfluenceColors.Pos.toArgb())
        assertEquals(0xFFFBBF24.toInt(), ConfluenceColors.Warn.toArgb())
        assertEquals(0xFF67E8F9.toInt(), ConfluenceColors.GradEnd.toArgb())
    }

    @Test
    fun aliasesMapToB1() {
        assertEquals(ConfluenceColors.Plasma, ConfluenceColors.CyberCyan)
        assertEquals(ConfluenceColors.Plasma, ConfluenceColors.Primary)
        assertEquals(ConfluenceColors.Pos, ConfluenceColors.Bull)
        assertEquals(ConfluenceColors.Neg, ConfluenceColors.Bear)
        assertEquals(ConfluenceColors.Void, ConfluenceColors.Background)
        assertEquals(ConfluenceColors.Ice, ConfluenceColors.Accent)
        assertEquals(ConfluenceColors.Dim, ConfluenceColors.Slate)
    }

    @Test
    fun acrylicUnderLayerIsCenteredEqualInset() {
        // Single equal-inset token — not an asymmetric SE translation.
        assertEquals(7f, ConfluenceDimens.acrylicUnderInset.value)
        assertEquals(7f, ConfluenceDimens.acrylicHaloStroke.value)
        assertEquals(14f, ConfluenceDimens.acrylicHaloReserve.value)
        assertTrue(ConfluenceDimens.acrylicEdgeAlpha in 0.20f..0.28f)
        assertEquals(24f, ConfluenceLayout.screenGutter.value)
        assertEquals(32f, ConfluenceLayout.stackGap.value)
        assertEquals(8f, ConfluenceLayout.inlineGap.value)
        assertEquals(14f, ConfluenceLayout.peekReserve.value)
        assertEquals(10f, ConfluenceLayout.outerGutter.value)
        assertEquals(24f, ConfluenceDimens.dockElevationGap.value)
        assertEquals(ConfluenceLayout.screenGutter, Spacing.xl)
        assertEquals(ConfluenceLayout.stackGap, Spacing.xxl)
        assertEquals(ConfluenceDimens.acrylicHaloReserve, ConfluenceLayout.peekReserve)
        assertEquals(28f, ConfluenceDimens.brandMarkHeader.value)
        assertEquals(22f, ConfluenceDimens.brandMarkDock.value)
        assertEquals(22f, ConfluenceDimens.dockIcon.value)
        assertEquals(108f, ConfluenceDimens.brandMarkSplash.value)
        assertTrue(
            "H1 header mark stays in the 24–28dp HUD lock",
            ConfluenceDimens.brandMarkHeader.value in 24f..28f,
        )
        assertTrue(
            "H1 dock/tab mark stays in the 22–24dp lock",
            ConfluenceDimens.brandMarkDock.value in 22f..24f,
        )
        assertTrue(
            "H1 splash mark stays in the 96–120dp lock",
            ConfluenceDimens.brandMarkSplash.value in 96f..120f,
        )
    }

    @Test
    fun stackGapClearsNeighboringAcrylicHalos() {
        val collision = ConfluenceDimens.acrylicHaloReserve.value * 2f
        assertTrue(
            "stackGap must be >= 2× acrylicHaloReserve so glows do not overlap at ~390dp",
            ConfluenceLayout.stackGap.value >= collision,
        )
        assertTrue(
            "screenGutter must be >= peekReserve so side halos stay inside the clip",
            ConfluenceLayout.screenGutter.value >= ConfluenceLayout.peekReserve.value,
        )
        assertTrue(
            "outerGutter must stay non-negative (gutter minus halo reserve)",
            ConfluenceLayout.outerGutter.value >= 0f,
        )
        assertTrue(
            "screenTop/Bottom must clear a single halo so scroll clip does not crop it",
            ConfluenceLayout.screenTop.value >= ConfluenceDimens.acrylicHaloReserve.value &&
                ConfluenceLayout.screenBottom.value >= ConfluenceDimens.acrylicHaloReserve.value,
        )
    }
}
