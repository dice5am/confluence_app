package com.cavin.confluence.feature.chart

import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.model.Venue
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.indicators.IndicatorCalc
import com.cavin.confluence.indicators.SnapshotCutoff
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

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
    fun evaluateDelegatesToIndicatorCalcOnFencedSnapshot1h() {
        val candles = loadSnapshotCandles("1h")
        val fromAdapter = evaluateDayOneIndicators(candles, cutoff)
        val fromEngine = IndicatorCalc.evaluate(candles.map { it.toIndicatorBar() }, cutoff)
        assertEquals(fromEngine.engineId, fromAdapter.engineId)
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
        val aligned = candlesAlignedToIndicators(candles, fromAdapter)
        assertEquals(fromAdapter.bars.size, aligned.size)
        assertEquals(fromAdapter.bars.last().openTimeMs, aligned.last().openTimeMs)
        assertTrue(aligned.all { it.closeTimeMs <= cutoff.cutoffMs })
        assertTrue(aligned.all { it.isFinal })
    }

    @Test
    fun oneMinuteFenceDropsOvershootBeforeOverlays() {
        val candles = loadSnapshotCandles("1m")
        val rawOvershoot = candles.count { it.isFinal && it.closeTimeMs > cutoff.cutoffMs }
        assertTrue(rawOvershoot > 0)
        val indicators = evaluateDayOneIndicators(candles, cutoff)
        assertEquals(rawOvershoot, indicators.fence.droppedOvershootCount)
        assertTrue(indicators.bars.all { it.closeTimeMs <= cutoff.cutoffMs })
        val aligned = candlesAlignedToIndicators(candles, indicators)
        assertEquals(indicators.bars.size, aligned.size)
        assertTrue(aligned.last().closeTimeMs <= cutoff.cutoffMs)
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
        val indicators = evaluateDayOneIndicators(loadSnapshotCandles("1h"), cutoff)
        val names = indicators::class.java.declaredFields.map { it.name }.toSet()
        assertFalse(names.any { it.contains("vwap", ignoreCase = true) })
        assertFalse(names.any { it.contains("fib", ignoreCase = true) })
        assertFalse(names.any { it.contains("pivot", ignoreCase = true) })
        assertTrue(names.contains("rsi14"))
        assertTrue(names.contains("volumeSma20"))
        assertTrue(names.contains("ema9"))
        assertTrue(names.contains("ichimoku"))
        assertTrue(names.contains("volumeProfile"))
    }

    @Test
    fun overlayFamiliesToggleIndependently() {
        var vis = ChartOverlayVisibility.AllOn
        vis = vis.toggle(ChartOverlayFamily.Ichimoku)
        assertFalse(vis.ichimoku)
        assertTrue(vis.movingAverages)
        vis = vis.toggle(ChartOverlayFamily.Volume)
        assertFalse(vis.volume)
        vis = vis.toggle(ChartOverlayFamily.Ichimoku)
        assertTrue(vis.ichimoku)
    }

    @Test
    fun weeklySma200WarmupIsHonest() {
        val w1 = evaluateDayOneIndicators(loadSnapshotCandles("1w"), cutoff)
        assertTrue(w1.sma200WarmupIncomplete)
        assertNull(w1.sma200.lastFinite())
        assertTrue(w1.sma50.finiteCount() > 0)
    }

    private fun loadSnapshotCandles(timeframeWire: String): List<Candle> {
        val tf = Timeframe.fromWire(timeframeWire)
        val root = JSONObject(Files.readString(snapshotDir().resolve("BTCUSDT_$timeframeWire.json")))
        val arr = root.getJSONArray("candles")
        val out = ArrayList<Candle>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Candle(
                    venue = Venue.BINANCE,
                    symbol = Candle.SYMBOL_BTCUSDT,
                    timeframe = tf,
                    openTimeMs = o.getLong("openTimeMs"),
                    closeTimeMs = o.getLong("closeTimeMs"),
                    open = o.getDouble("open"),
                    high = o.getDouble("high"),
                    low = o.getDouble("low"),
                    close = o.getDouble("close"),
                    volume = o.getDouble("volume"),
                    isFinal = o.optBoolean("isFinal", true),
                    sourceTsMs = o.optLong("sourceTsMs", o.getLong("closeTimeMs")),
                    ingestTsMs = cutoff.cutoffMs,
                ),
            )
        }
        return out
    }

    private fun snapshotDir(): Path {
        val candidates = listOf(
            Path.of("data/src/main/assets/md_snapshot"),
            Path.of("../data/src/main/assets/md_snapshot"),
        )
        return candidates.firstOrNull { Files.isRegularFile(it.resolve("meta.json")) }
            ?: error("md_snapshot fixtures not found")
    }
}
