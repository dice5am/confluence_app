package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.model.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

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

    @Test
    fun timeLabelsUseAmericaToronto() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2024, Calendar.SEPTEMBER, 4, 16, 30, 0)
        }.timeInMillis
        assertEquals("12:30", ChartAxisLabels.formatTime(utc, Timeframe.M1))
    }

    @Test
    fun priceFormatUsesGroupedFigures() {
        val label = ChartAxisLabels.formatPrice(77_734f)
        assertTrue(label.contains("77"))
        assertTrue(label.contains(","))
    }
}
