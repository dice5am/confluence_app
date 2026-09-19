package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovingAveragesTest {
    @Test
    fun smaPeriod3() {
        val sma = Sma.series(doubleArrayOf(1.0, 2.0, 3.0, 4.0), period = 3)
        assertThat(sma[0]).isNull()
        assertThat(sma[1]).isNull()
        assertThat(sma[2]).isWithin(1e-9).of(2.0)
        assertThat(sma[3]).isWithin(1e-9).of(3.0)
    }

    @Test
    fun emaSeedsFromSmaThenAppliesMultiplier() {
        val ema = Ema.series(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0), period = 3)
        assertThat(ema[2]).isWithin(1e-9).of(2.0)
        // k = 2/4 = 0.5 → ema3 = 4*0.5 + 2*0.5 = 3; ema4 = 5*0.5 + 3*0.5 = 4
        assertThat(ema[3]).isWithin(1e-9).of(3.0)
        assertThat(ema[4]).isWithin(1e-9).of(4.0)
    }

    @Test
    fun sma200UndefinedWhenDepthShort() {
        val values = DoubleArray(199) { 100.0 + it }
        val sma = Sma.series(values, period = 200)
        assertThat(sma.all { it == null }).isTrue()
    }

    @Test
    fun incrementalSmaAndEmaMatchBatch() {
        val values = doubleArrayOf(10.0, 11.0, 12.5, 12.0, 13.0, 14.0)
        val smaBatch = Sma.series(values, 4)
        var smaAcc = Sma.Accumulator.empty(4)
        val smaInc = values.map {
            val step = Sma.next(smaAcc, it)
            smaAcc = step.first
            step.second
        }
        assertThat(smaInc).isEqualTo(smaBatch)

        val emaBatch = Ema.series(values, 3)
        var emaAcc = Ema.Accumulator.empty(3)
        val emaInc = values.map {
            val step = Ema.next(emaAcc, it)
            emaAcc = step.first
            step.second
        }
        assertThat(emaInc).isEqualTo(emaBatch)
    }
}
