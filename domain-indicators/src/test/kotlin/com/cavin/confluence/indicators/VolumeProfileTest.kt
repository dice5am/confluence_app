package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
