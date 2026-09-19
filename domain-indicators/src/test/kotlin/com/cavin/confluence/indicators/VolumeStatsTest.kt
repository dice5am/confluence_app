package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VolumeStatsTest {
    @Test
    fun volumeSeriesIsPassthroughAndSma20Matches() {
        val bars = (0 until 25).map { i ->
            TestBars.bar(
                openTimeMs = 1_000L + i,
                open = 10.0,
                close = 10.0,
                volume = (i + 1).toDouble(),
            )
        }
        val out = IndicatorCalc.evaluate(
            bars,
            SnapshotCutoff.PACKAGED_2026_09_19.copy(cutoffMs = Long.MAX_VALUE),
        )
        assertThat(out.volume.values).isEqualTo(bars.map { it.volume })
        assertThat(out.volumeSma20.values[18]).isNull()
        assertThat(out.volumeSma20.values[19]).isWithin(1e-9).of((1..20).sum().toDouble() / 20.0)
        assertThat(out.volumeSma20.values[24]).isWithin(1e-9).of((6..25).sum().toDouble() / 20.0)
    }
}
