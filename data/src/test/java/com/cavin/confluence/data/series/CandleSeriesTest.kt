package com.cavin.confluence.data.series

import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Timeframe
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CandleSeriesTest {
    @Test
    fun replaceTip_sameOpenTime() {
        val series = FakeFixtures.sampleClosedCandles(timeframe = Timeframe.H1, count = 3)
        val tip = series.last().copy(close = series.last().close + 10.0, isFinal = false)
        val out = CandleSeries.applyLive(series, tip)
        assertThat(out).hasSize(3)
        assertThat(out.last().close).isEqualTo(tip.close)
        assertThat(out.last().isFinal).isFalse()
        assertThat(out[0]).isEqualTo(series[0])
    }

    @Test
    fun append_newerOpenTime() {
        val series = FakeFixtures.sampleClosedCandles(timeframe = Timeframe.H1, count = 2)
        val last = series.last()
        val next = last.copy(
            openTimeMs = last.openTimeMs + 3_600_000L,
            closeTimeMs = last.closeTimeMs + 3_600_000L,
            isFinal = false,
        )
        val out = CandleSeries.applyLive(series, next)
        assertThat(out).hasSize(3)
        assertThat(out.last().openTimeMs).isEqualTo(next.openTimeMs)
    }

    @Test
    fun ignore_olderOpenTime() {
        val series = FakeFixtures.sampleClosedCandles(count = 2)
        val older = series.first()
        val out = CandleSeries.applyLive(series, older)
        assertThat(out).isEqualTo(series)
    }
}
