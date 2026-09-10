package com.cavin.confluence.feature.chart

import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartGeometryTest {
    @Test
    fun volumePaneIsEighteenPercentUnderPrice() {
        assertEquals(0.18f, ChartGeometry.volumeFraction, 0.0001f)
        val panes = ChartGeometry.panes(
            width = 400f,
            height = 1000f,
            leftPad = 8f,
            rightPad = 56f,
            topPad = 8f,
            bottomPad = 22f,
            showVolume = true,
        )
        val usable = panes.priceHeight + panes.volHeight
        assertEquals(0.18f, panes.volHeight / usable, 0.001f)
        assertEquals(panes.priceBottom, panes.volTop, 0.01f)
        assertTrue(panes.volTop > panes.priceTop)
    }

    @Test
    fun bodyFractionIsTradingViewSlotRange() {
        assertTrue(ChartGeometry.bodyFraction in 0.55f..0.70f)
        assertEquals(ConfluenceDimens.chartBodyFraction, ChartGeometry.bodyFraction, 0f)
    }

    @Test
    fun yDomainPadsFiveToEightPercent() {
        assertTrue(ChartGeometry.yPadFraction in 0.05f..0.08f)
        val domain = ChartGeometry.yDomain(100f, 200f)
        val pad = (200f - 100f) * ChartGeometry.yPadFraction
        assertEquals(100f - pad, domain.lo, 0.01f)
        assertEquals(200f + pad, domain.hi, 0.01f)
        assertTrue(100f in domain.lo..domain.hi)
    }

    @Test
    fun slotAtXSnapsToCandleIndex() {
        val idx = ChartGeometry.slotAtX(
            canvasX = 8f + 25f,
            plotLeft = 8f,
            plotWidth = 300f,
            candleWidthPx = 10f,
            startOffset = 0f,
            candleCount = 50,
        )
        assertEquals(2, idx)
    }

    @Test
    fun slotCenterIsMidSlot() {
        val cx = ChartGeometry.slotCenterX(
            index = 3,
            startIndex = 0,
            plotLeft = 10f,
            candleWidthPx = 10f,
            pixelShift = 0f,
        )
        assertEquals(10f + 3 * 10f + 5f, cx, 0.01f)
    }

    @Test
    fun visibleWindowClampsToSeries() {
        val win = ChartGeometry.window(
            candleCount = 20,
            plotWidth = 400f,
            candleWidthPx = 10f,
            startOffset = 100f,
        )
        assertEquals(0f, win.startOffset, 0.01f)
        assertTrue(win.endIndex <= 20)
    }

    @Test
    fun zoomAroundKeepsFocusIndex() {
        val start = ChartGeometry.zoomAround(
            startOffset = 0f,
            oldCw = 10f,
            newCw = 20f,
            focusPlotX = 100f,
            panX = 0f,
            candleCount = 200,
            plotWidth = 400f,
        )
        assertEquals(5f, start, 0.05f)
    }

    @Test
    fun dojiBodyNeverCollapses() {
        assertEquals(2f, ChartGeometry.bodyHeightPx(10f, 10f, minDojiPx = 2f), 0f)
        assertEquals(8f, ChartGeometry.bodyHeightPx(10f, 18f, minDojiPx = 2f), 0f)
        val doji = ChartGeometry.bodyRect(10f, 10f, minDojiPx = 2f)
        assertEquals(9f, doji.top, 0.01f)
        assertEquals(2f, doji.height, 0f)
    }

    @Test
    fun lastPriceHairlineAlphaIsTwentyPercent() {
        assertEquals(0.20f, ChartGeometry.lastPriceHairlineAlpha, 0.001f)
    }

    @Test
    fun volumeBarsUseLowAlpha() {
        assertEquals(0.35f, ChartGeometry.volumeAlpha, 0.001f)
    }
}
