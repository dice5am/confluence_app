package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class VolumeProfileTest {
    @Test
    fun pocAndValueAreaFromFixedLookback() {
        val bars = (0 until 24).map { i ->
            val concentrated = i == 10
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0,
                high = if (concentrated) 101.0 else 110.0,
                low = if (concentrated) 99.0 else 90.0,
                close = 100.0,
                volume = if (concentrated) 100.0 else 1.0,
            )
        }
        val vp = VolumeProfile.compute(bars)
        assertThat(vp.usedBarCount).isEqualTo(24)
        assertThat(vp.lookbackLimited).isFalse()
        assertThat(vp.pointOfControl!!).isFinite()
        assertThat(vp.valueAreaLow!!).isFinite()
        assertThat(vp.valueAreaHigh!!).isFinite()
        assertThat(vp.valueAreaLow!!).isAtMost(vp.pointOfControl!!)
        assertThat(vp.valueAreaHigh!!).isAtLeast(vp.pointOfControl!!)
        assertThat(vp.pointOfControl!!).isWithin(2.0).of(100.0)
        assertThat(vp.notes).contains("last 24 closed bars")
        assertThat(vp.notes).doesNotContain("manual")
    }

    @Test
    fun computeAsOfIgnoresBarsAfterAsOfClose() {
        val bars = (0 until 30).map { i ->
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0,
                high = if (i > 20) 10_000.0 else 110.0,
                low = 90.0,
                close = 100.0,
                volume = if (i > 20) 1_000.0 else 1.0,
            )
        }
        val asOf = bars[20].closeTimeMs
        val honest = VolumeProfile.computeAsOf(bars, asOf)
        val prefix = VolumeProfile.compute(bars.take(21))
        assertThat(honest.windowLastCloseTimeMs).isEqualTo(asOf)
        assertThat(honest.usedBarCount).isEqualTo(21)
        assertThat(honest.pointOfControl).isWithin(1e-9).of(prefix.pointOfControl!!)
        val peeked = VolumeProfile.compute(bars)
        assertThat(peeked.windowLastCloseTimeMs).isEqualTo(bars.last().closeTimeMs)
        assertThat(abs(peeked.pointOfControl!! - honest.pointOfControl!!)).isGreaterThan(1.0)
    }

    @Test
    fun usesAllBarsWhenShorterThanLookback() {
        val bars = (0 until 5).map { i ->
            TestBars.bar(
                openTimeMs = 1_000L + i,
                open = 50.0 + i,
                high = 51.0 + i,
                low = 49.0 + i,
                close = 50.0 + i,
                volume = 2.0,
            )
        }
        val vp = VolumeProfile.compute(bars)
        assertThat(vp.usedBarCount).isEqualTo(5)
        assertThat(vp.lookbackLimited).isTrue()
        assertThat(vp.pointOfControl!!).isFinite()
    }

    @Test
    fun customLookbackUsesLastNClosedBars() {
        val bars = (0 until 24).map { i ->
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0,
                high = 110.0,
                low = 90.0,
                close = 100.0,
                volume = if (i >= 14) 100.0 else 1.0,
            )
        }
        val last10 = VolumeProfile.compute(bars, lookbackBars = 10)
        assertThat(last10.lookbackBarsRequested).isEqualTo(10)
        assertThat(last10.usedBarCount).isEqualTo(10)
        assertThat(last10.windowFirstOpenTimeMs).isEqualTo(bars[14].openTimeMs)
        val asOf = VolumeProfile.computeAsOf(bars, bars[20].closeTimeMs, lookbackBars = 10)
        assertThat(asOf.usedBarCount).isEqualTo(10)
        assertThat(asOf.windowLastCloseTimeMs).isEqualTo(bars[20].closeTimeMs)
        assertThat(asOf.windowFirstOpenTimeMs).isEqualTo(bars[11].openTimeMs)
    }
}
