package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SnapshotDayOneIndicatorsTest {
    private val cutoff = SnapshotFixtures.loadMetaCutoff()

    @Test
    fun packagedMetaMatchesLockedCutoff() {
        assertThat(cutoff.cutoffMs).isEqualTo(1_789_815_599_999L)
        assertThat(cutoff.cutoffUtcLabel).isEqualTo("2026-09-19 10:59 UTC")
        assertThat(cutoff.cutoffTorontoLabel).isEqualTo(
            "2026-09-19 06:59 EDT (America/Toronto, UTC-4)",
        )
        assertThat(cutoff.snapshotVersion).isEqualTo("md_snapshot@2026-09-19")
    }

    @Test
    fun oneMinuteOvershootBarsAreExcluded() {
        val raw = SnapshotFixtures.loadBars("1m")
        val window = CandleFence.apply(raw, cutoff)
        assertThat(window.droppedOvershootCount).isGreaterThan(0)
        assertThat(window.bars).isNotEmpty()
        assertThat(window.bars.all { it.isFinal }).isTrue()
        assertThat(window.bars.all { it.closeTimeMs <= cutoff.cutoffMs }).isTrue()
        assertThat(raw.count { it.isFinal && it.closeTimeMs > cutoff.cutoffMs })
            .isEqualTo(window.droppedOvershootCount)
    }

    @Test
    fun fiveAutosOnFenced1hAreFinite() {
        val raw = SnapshotFixtures.loadBars("1h")
        val out = IndicatorCalc.evaluate(raw, cutoff)
        assertThat(out.fence.droppedOvershootCount).isEqualTo(0)
        assertThat(out.bars).hasSize(raw.size)
        assertThat(out.fence.cutoffUtcLabel).isEqualTo(cutoff.cutoffUtcLabel)
        assertThat(out.fence.cutoffTorontoLabel).isEqualTo(cutoff.cutoffTorontoLabel)
        assertThat(out.fence.lastCloseTimeMs).isAtMost(cutoff.cutoffMs)

        assertFiniteInRange(out.rsi14, 0.0, 100.0, minDefined = 100)
        assertFinitePositive(out.volume, minDefined = out.bars.size)
        assertFinitePositive(out.volumeSma20, minDefined = out.bars.size - 19)
        assertFinitePositive(out.ema9, minDefined = out.bars.size - 8)
        assertFinitePositive(out.ema21, minDefined = out.bars.size - 20)
        assertFinitePositive(out.sma50, minDefined = out.bars.size - 49)
        assertFinitePositive(out.sma200, minDefined = out.bars.size - 199)
        assertThat(out.sma200WarmupIncomplete).isFalse()

        assertThat(out.ichimoku.tenkan.finiteCount()).isAtLeast(out.bars.size - 8)
        assertThat(out.ichimoku.kijun.finiteCount()).isAtLeast(out.bars.size - 25)
        assertThat(out.ichimoku.senkouB.finiteCount()).isGreaterThan(0)
        assertThat(out.ichimoku.forwardCloud).isNotEmpty()
        out.ichimoku.forwardCloud.forEach { point ->
            point.senkouA?.let { assertThat(it).isFinite() }
            point.senkouB?.let { assertThat(it).isFinite() }
        }

        val vp = out.volumeProfile
        assertThat(vp.usedBarCount).isEqualTo(VolumeProfile.LOOKBACK_BARS)
        assertThat(vp.pointOfControl!!).isFinite()
        assertThat(vp.valueAreaHigh!!).isFinite()
        assertThat(vp.valueAreaLow!!).isFinite()
        assertThat(vp.valueAreaLow!!).isAtMost(vp.pointOfControl!!)
        assertThat(vp.valueAreaHigh!!).isAtLeast(vp.pointOfControl!!)
        assertThat(vp.notes).contains("last 24 closed bars")
    }

    @Test
    fun fiveAutosOnFenced1dAnd1wHonesty() {
        val d1 = IndicatorCalc.evaluate(SnapshotFixtures.loadBars("1d"), cutoff)
        assertThat(d1.bars.size).isAtLeast(200)
        val d1Rsi = d1.rsi14.lastFinite()!!
        assertThat(d1Rsi).isAtLeast(0.0)
        assertThat(d1Rsi).isAtMost(100.0)
        assertThat(d1.sma200WarmupIncomplete).isFalse()
        assertThat(d1.sma200.lastFinite()!!).isFinite()
        assertThat(d1.ichimoku.kijun.finiteCount()).isGreaterThan(0)
        assertThat(d1.volumeProfile.pointOfControl!!).isFinite()

        val w1 = IndicatorCalc.evaluate(SnapshotFixtures.loadBars("1w"), cutoff)
        assertThat(w1.bars.size).isEqualTo(199)
        assertThat(w1.sma200WarmupIncomplete).isTrue()
        assertThat(w1.sma50.lastFinite()!!).isFinite()
        val w1Rsi = w1.rsi14.lastFinite()!!
        assertThat(w1Rsi).isAtLeast(0.0)
        assertThat(w1Rsi).isAtMost(100.0)
        assertThat(w1.volumeProfile.pointOfControl!!).isFinite()
    }

    @Test
    fun onClosedBarIgnoresOvershoot() {
        val out = IndicatorCalc.evaluate(SnapshotFixtures.loadBars("1h"), cutoff)
        val rsiBefore = out.rsi14.lastFinite()
        val overshoot = out.bars.last().copy(
            openTimeMs = out.bars.last().openTimeMs + 3_600_000L,
            closeTimeMs = cutoff.cutoffMs + 1,
            isFinal = true,
        )
        val after = IndicatorCalc.onClosedBar(out, overshoot)
        assertThat(after.bars).hasSize(out.bars.size)
        assertThat(after.rsi14.lastFinite()).isEqualTo(rsiBefore)
        assertThat(after.fence.droppedOvershootCount).isEqualTo(out.fence.droppedOvershootCount + 1)
    }

    private fun assertFiniteInRange(
        series: AlignedSeries,
        lo: Double,
        hi: Double,
        minDefined: Int,
    ) {
        val defined = series.values.filter { it.isFiniteNumber() }
        assertThat(defined.size).isAtLeast(minDefined)
        defined.forEach { value ->
            assertThat(value!!).isFinite()
            assertThat(value).isAtLeast(lo)
            assertThat(value).isAtMost(hi)
        }
    }

    private fun assertFinitePositive(series: AlignedSeries, minDefined: Int) {
        val defined = series.values.filter { it.isFiniteNumber() }
        assertThat(defined.size).isAtLeast(minDefined)
        defined.forEach { value ->
            assertThat(value!!).isFinite()
            assertThat(value).isAtLeast(0.0)
        }
    }
}
