package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RsiTest {
    @Test
    fun wilderPeriod2HandVector() {
        val closes = doubleArrayOf(10.0, 12.0, 11.0, 13.0)
        val rsi = Rsi.series(closes, period = 2)
        assertThat(rsi[0]).isNull()
        assertThat(rsi[1]).isNull()
        assertThat(rsi[2]).isWithin(1e-9).of(100.0 - 100.0 / 3.0)
        assertThat(rsi[3]).isWithin(1e-9).of(100.0 - 100.0 / 7.0)
    }

    @Test
    fun allGainsYields100AfterWarmup() {
        val closes = DoubleArray(15) { it.toDouble() + 1.0 }
        val rsi = Rsi.series(closes, period = 14)
        assertThat(rsi.take(14).all { it == null }).isTrue()
        assertThat(rsi[14]).isWithin(1e-9).of(100.0)
    }

    @Test
    fun incrementalMatchesBatch() {
        val closes = doubleArrayOf(44.0, 44.5, 43.9, 44.2, 45.0, 45.1, 44.8, 45.3)
        val batch = Rsi.series(closes, period = 3)
        var acc: Rsi.Accumulator? = null
        val incremental = closes.map { close ->
            val step = Rsi.next(acc, close, 3)
            acc = step.first
            step.second
        }
        assertThat(incremental).isEqualTo(batch)
    }
}
