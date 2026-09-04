package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.model.Timeframe
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartAxisLabelsTest {
    @Test
    fun priceTicksCoverRange() {
        val ticks = ChartAxisLabels.priceTicks(95_000f, 100_000f, targetCount = 5)
        assertTrue(ticks.isNotEmpty())
        assertTrue(ticks.first() <= 95_000f + 1f)
        assertTrue(ticks.last() >= 100_000f - 1_000f)
    }

    @Test
    fun timeFormatDependsOnTf() {
        val ms = 1_725_400_800_000L
        val m1 = ChartAxisLabels.formatTime(ms, Timeframe.M1)
        val d1 = ChartAxisLabels.formatTime(ms, Timeframe.D1)
        assertTrue(m1.contains(":"))
        assertTrue(d1.length >= 3)
    }
}
