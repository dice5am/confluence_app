package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Timeframe
import org.junit.Assert.assertTrue
import org.junit.Test

class CandleLodTest {
    @Test
    fun decimatesWhenOverMaxPoints() {
        val raw = FakeFixtures.sampleClosedCandles(timeframe = Timeframe.M1, count = 5_000)
        val out = CandleLod.maybeDecimate(raw, Timeframe.M1, maxPoints = 2_000)
        assertTrue(out.size <= 2_000)
        assertTrue(out.isNotEmpty())
        assertTrue(out.first().openTimeMs <= out.last().openTimeMs)
    }

    @Test
    fun passthroughWhenSmall() {
        val raw = FakeFixtures.sampleClosedCandles(count = 48)
        val out = CandleLod.maybeDecimate(raw, Timeframe.H1, maxPoints = 2_000)
        assertTrue(out.size == raw.size)
    }
}
