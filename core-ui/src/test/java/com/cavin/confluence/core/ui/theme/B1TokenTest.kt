package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
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
}
