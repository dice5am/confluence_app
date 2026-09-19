package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CandleFenceTest {
    private val cutoff = SnapshotCutoff.PACKAGED_2026_09_19

    @Test
    fun dropsBarsWithCloseTimeAfterCutoff() {
        val kept = TestBars.bar(
            openTimeMs = cutoff.cutoffMs - 3_600_000L,
            closeTimeMs = cutoff.cutoffMs,
            open = 100.0,
            close = 100.0,
        )
        val overshoot = TestBars.bar(
            openTimeMs = cutoff.cutoffMs + 1,
            closeTimeMs = cutoff.cutoffMs + 60_000L,
            open = 101.0,
            close = 101.0,
        )
        val window = CandleFence.apply(listOf(kept, overshoot), cutoff)
        assertThat(window.droppedOvershootCount).isEqualTo(1)
        assertThat(window.bars).hasSize(1)
        assertThat(window.bars.single().closeTimeMs).isEqualTo(cutoff.cutoffMs)
        assertThat(window.bars.none { it.closeTimeMs > cutoff.cutoffMs }).isTrue()
    }

    @Test
    fun dropsFormingBarsEvenIfInsideCutoff() {
        val forming = TestBars.bar(
            openTimeMs = cutoff.cutoffMs - 3_600_000L,
            closeTimeMs = cutoff.cutoffMs,
            open = 100.0,
            close = 100.0,
            isFinal = false,
        )
        val window = CandleFence.apply(listOf(forming), cutoff)
        assertThat(window.droppedFormingCount).isEqualTo(1)
        assertThat(window.bars).isEmpty()
    }

    @Test
    fun exposesCutoffLabelsFromMeta() {
        val window = CandleFence.apply(emptyList(), cutoff)
        val report = window.report()
        assertThat(report.cutoffMs).isEqualTo(cutoff.cutoffMs)
        assertThat(report.cutoffUtcLabel).isEqualTo("2026-09-19 10:59 UTC")
        assertThat(report.cutoffTorontoLabel).contains("America/Toronto")
        assertThat(report.snapshotVersion).isEqualTo("md_snapshot@2026-09-19")
    }
}
