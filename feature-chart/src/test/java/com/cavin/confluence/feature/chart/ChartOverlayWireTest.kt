package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.indicators.IndicatorCalc
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.SnapshotCutoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartOverlayWireTest {

    private val cutoff = SnapshotCutoff.PACKAGED_2026_09_19

    @Test
    fun candleMapsOneToOneOntoIndicatorBar() {
        val candle = FakeFixtures.sampleClosedCandles(count = 1).first()
        val bar = candle.toIndicatorBar()
        assertEquals(candle.openTimeMs, bar.openTimeMs)
        assertEquals(candle.closeTimeMs, bar.closeTimeMs)
        assertEquals(candle.open, bar.open, 0.0)
        assertEquals(candle.high, bar.high, 0.0)
        assertEquals(candle.low, bar.low, 0.0)
        assertEquals(candle.close, bar.close, 0.0)
        assertEquals(candle.volume, bar.volume, 0.0)
        assertEquals(candle.isFinal, bar.isFinal)
    }

    @Test
    fun evaluateDelegatesToEvaluateAsOfNotAParallelSeries() {
        val candles = FakeFixtures.sampleClosedCandles(count = 220, timeframe = Timeframe.H1)
        val fromAdapter = evaluateDayOneIndicators(candles, cutoff)
        val bars = candles.map { it.toIndicatorBar() }
        val lastClose = bars.filter { it.isFinal && it.closeTimeMs <= cutoff.cutoffMs }
            .maxOf { it.closeTimeMs }
        val fromEngine = IndicatorCalc.evaluateAsOf(bars, cutoff, lastClose)
        assertEquals(IndicatorCalc.ENGINE_ID, fromAdapter.engineId)
        assertEquals(fromEngine.bars.size, fromAdapter.bars.size)
        assertEquals(fromEngine.rsi14.lastFinite(), fromAdapter.rsi14.lastFinite())
        assertEquals(fromEngine.ema9.lastFinite(), fromAdapter.ema9.lastFinite())
        assertEquals(fromEngine.ema21.lastFinite(), fromAdapter.ema21.lastFinite())
        assertEquals(fromEngine.sma50.lastFinite(), fromAdapter.sma50.lastFinite())
        assertEquals(fromEngine.sma200.lastFinite(), fromAdapter.sma200.lastFinite())
        assertEquals(fromEngine.volumeSma20.lastFinite(), fromAdapter.volumeSma20.lastFinite())
        assertEquals(fromEngine.ichimoku.kijun.lastFinite(), fromAdapter.ichimoku.kijun.lastFinite())
        assertEquals(fromEngine.volumeProfile.pointOfControl, fromAdapter.volumeProfile.pointOfControl)
        assertEquals(cutoff.cutoffMs, fromAdapter.fence.cutoffMs)
        assertEquals(1_789_815_599_999L, fromAdapter.fence.cutoffMs)
        assertEquals("2026-09-19 10:59 UTC", fromAdapter.fence.cutoffUtcLabel)
        assertEquals("md_snapshot@2026-09-19", fromAdapter.fence.snapshotVersion)
        assertTrue(fromAdapter.rsi14.finiteCount() > 0)
        assertTrue(fromAdapter.ema9.finiteCount() > 0)
        assertFalse(fromAdapter.sma200WarmupIncomplete)
        assertNotNull(fromAdapter.volumeProfile.pointOfControl)
        assertNotNull(fromAdapter.volumeProfile.valueAreaHigh)
        assertNotNull(fromAdapter.volumeProfile.valueAreaLow)
        assertEquals(fromEngine.params, fromAdapter.params)
        assertEquals(IndicatorParams.DEFAULT, fromAdapter.params)
        val custom = evaluateDayOneIndicators(
            candles,
            cutoff,
            IndicatorParams(rsiPeriod = 7),
        )
        assertEquals(7, custom.params.rsiPeriod)
        assertTrue(custom.rsi.lastFinite() != fromAdapter.rsi.lastFinite())
        val aligned = candlesAlignedToIndicators(candles, fromAdapter)
        assertEquals(fromAdapter.bars.size, aligned.size)
        assertEquals(fromAdapter.bars.last().openTimeMs, aligned.last().openTimeMs)
        assertTrue(aligned.all { it.closeTimeMs <= cutoff.cutoffMs })
        val full = IndicatorCalc.evaluate(bars, cutoff, IndicatorParams.DEFAULT)
        assertEquals(full.volumeProfile.pointOfControl, fromAdapter.volumeProfile.pointOfControl)
    }

    @Test
    fun fenceDropsOvershootBeforeOverlays() {
        val kept = FakeFixtures.sampleClosedCandles(count = 40, timeframe = Timeframe.M1)
        val tip = kept.last()
        val overshoot = tip.copy(
            openTimeMs = cutoff.cutoffMs + 60_000L,
            closeTimeMs = cutoff.cutoffMs + 119_999L,
            isFinal = true,
        )
        val indicators = evaluateDayOneIndicators(kept + overshoot, cutoff)
        assertEquals(1, indicators.fence.droppedOvershootCount)
        assertTrue(indicators.bars.all { it.closeTimeMs <= cutoff.cutoffMs })
        val aligned = candlesAlignedToIndicators(kept + overshoot, indicators)
        assertEquals(indicators.bars.size, aligned.size)
        assertTrue(aligned.none { it.openTimeMs == overshoot.openTimeMs })
    }

    @Test
    fun honestyLabelsMatchPackagedCutoff() {
        assertEquals("md_snapshot@2026-09-19", MdSnapshotStore.PACKAGED_SNAPSHOT_VERSION)
        assertEquals("2026-09-19 10:59 UTC", MdSnapshotStore.PACKAGED_CUTOFF_UTC)
        assertEquals(1_789_815_599_999L, MdSnapshotStore.PACKAGED_CUTOFF_MS)
        assertEquals(cutoff.cutoffMs, MdSnapshotStore.PACKAGED_CUTOFF_MS)
        assertTrue(ChartProofAsOf.contains("md_snapshot@2026-09-19"))
        assertTrue(ChartProofAsOf.contains("2026-09-19 10:59 UTC"))
        val resolved = resolveSnapshotCutoff()
        assertEquals(cutoff.cutoffMs, resolved.cutoffMs)
        assertEquals(cutoff.cutoffUtcLabel, resolved.cutoffUtcLabel)
        assertEquals(cutoff.snapshotVersion, resolved.snapshotVersion)
    }

    @Test
    fun dayOneResultHasNoVwapFibOrPivots() {
        val indicators = evaluateDayOneIndicators(
            FakeFixtures.sampleClosedCandles(count = 48, timeframe = Timeframe.H1),
            cutoff,
        )
        val names = indicators::class.java.declaredFields.map { it.name }.toSet()
        assertFalse(names.any { it.contains("vwap", ignoreCase = true) })
        assertFalse(names.any { it.contains("fib", ignoreCase = true) })
        assertFalse(names.any { it.contains("pivot", ignoreCase = true) })
        assertTrue(names.contains("rsi14"))
        assertTrue(names.contains("volumeSma20"))
        assertTrue(names.contains("ema9"))
        assertTrue(names.contains("params"))
        assertTrue(names.contains("movingAverages"))
    }

    @Test
    fun overlayRowsToggleIndependently() {
        var vis = ChartOverlayVisibility.AllOn
        vis = vis.toggle(ChartIndicatorId.Ichimoku)
        assertFalse(vis.ichimoku)
        assertTrue(vis.ma0)
        assertTrue(vis.ma1)
        vis = vis.toggle(ChartIndicatorId.VolumeRibbon)
        assertFalse(vis.volume)
        vis = vis.toggle(ChartIndicatorId.Ichimoku)
        assertTrue(vis.ichimoku)
        assertEquals(7, vis.activeCount())
    }

    @Test
    fun weeklySma200WarmupIsHonest() {
        val w1 = evaluateDayOneIndicators(
            FakeFixtures.sampleClosedCandles(count = 199, timeframe = Timeframe.W1),
            cutoff,
        )
        assertTrue(w1.sma200WarmupIncomplete)
        assertNull(w1.sma200.lastFinite())
        assertTrue(w1.sma50.finiteCount() > 0)
    }
}
