package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IchimokuTest {
    @Test
    fun tenkanIsDonchianMidpointOverNineBars() {
        val bars = (0 until 9).map { i ->
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0,
                high = 110.0 + i,
                low = 90.0,
                close = 100.0,
            )
        }
        val result = Ichimoku.compute(bars)
        assertThat(result.tenkan.values.take(8).all { it == null }).isTrue()
        val expected = (118.0 + 90.0) / 2.0
        assertThat(result.tenkan.values[8]).isWithin(1e-9).of(expected)
        assertThat(result.kijun.values.all { it == null }).isTrue()
        assertThat(result.senkouB.values.all { it == null }).isTrue()
    }

    @Test
    fun displacementPutsRawSpansForward() {
        val bars = (0 until 60).map { i ->
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0,
                high = 120.0,
                low = 80.0,
                close = 100.0 + i,
            )
        }
        val result = Ichimoku.compute(bars)
        val last = bars.lastIndex
        val tenkanLast = result.tenkan.values[last]!!
        val kijunLast = result.kijun.values[last]!!
        val rawA = (tenkanLast + kijunLast) / 2.0
        assertThat(result.forwardCloud).hasSize(Ichimoku.DISPLACEMENT)
        assertThat(result.forwardCloud.last().senkouA).isWithin(1e-9).of(rawA)
        assertThat(result.senkouARaw.values[last]).isWithin(1e-9).of(rawA)
        assertThat(result.senkouA.values[last]).isWithin(1e-9).of(result.senkouARaw.values[last - Ichimoku.DISPLACEMENT]!!)
        assertThat(result.chikou.values[last - Ichimoku.DISPLACEMENT]).isWithin(1e-9).of(bars.last().close)
        assertThat(result.chikou.values[last]).isNull()
    }
}
