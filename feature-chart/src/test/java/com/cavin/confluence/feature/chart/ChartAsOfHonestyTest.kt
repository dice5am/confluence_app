package com.cavin.confluence.feature.chart

import com.cavin.confluence.indicators.IndicatorBar
import com.cavin.confluence.indicators.IndicatorCalc
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.SnapshotCutoff
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Phase A′: chart adapter must not display full-series Chikou / VP peek.
 * Pinned to md_snapshot@2026-09-19 1h index 250 (PR #32 sample bar).
 */
class ChartAsOfHonestyTest {

    private val cutoff = SnapshotCutoff.PACKAGED_2026_09_19

    @Test
    fun chikouAtHistoricalIndexDoesNotPeekFutureClose() {
        val bars = load1h()
        val fenced = IndicatorCalc.evaluateAsOf(bars, cutoff, cutoff.cutoffMs).bars
        assertTrue(fenced.size > 276)
        val i = 250
        val adapter = ChartAsOfOverlays.fromBars(bars, cutoff)
        assertEquals(IndicatorParams.DEFAULT, adapter.params)
        val known = adapter.chikouPlotKnownAt(i)
        assertNull(known)
        val full = IndicatorCalc.evaluate(bars, cutoff)
        val peek = full.ichimoku.chikou.values[i]
        assertTrue(peek != null && peek.isFinite())
        assertEquals(fenced[i + 26].close, peek!!, 1e-9)
        assertNotEquals(peek, known)
        val asOf = adapter.evaluateAt(i)
        assertNull(asOf.ichimoku.chikou.values.lastOrNull())
        assertEquals(fenced[i].closeTimeMs, asOf.bars.last().closeTimeMs)
    }

    @Test
    fun volumeProfileAtHistoricalIndexIsNotFullSeriesVp() {
        val bars = load1h()
        val adapter = ChartAsOfOverlays.fromBars(bars, cutoff)
        val i = 250
        val vpAtI = adapter.volumeProfileAt(i)
        val full = IndicatorCalc.evaluate(bars, cutoff).volumeProfile
        val asOf = IndicatorCalc.evaluateAsOf(bars, cutoff, adapter.bars[i].closeTimeMs)
        assertEquals(asOf.volumeProfile.pointOfControl!!, vpAtI.pointOfControl!!, 1e-6)
        assertNotEquals(full.pointOfControl!!, vpAtI.pointOfControl!!, 1.0)
        assertEquals(1_788_922_799_999L, vpAtI.windowLastCloseTimeMs)
        assertEquals(1_789_815_599_999L, full.windowLastCloseTimeMs)
        assertEquals(full.pointOfControl, adapter.volumeProfileAsOfLast().pointOfControl)
    }

    @Test
    fun prefixStableSeriesAtIndexMatchEvaluateAsOf() {
        val bars = load1h()
        val adapter = ChartAsOfOverlays.fromBars(bars, cutoff)
        val i = 250
        val asOf = adapter.evaluateAt(i)
        val last = adapter.asOfLast
        assertEquals(asOf.rsi14.values[i], last.rsi14.values[i])
        assertEquals(asOf.ema9.values[i], last.ema9.values[i])
        assertEquals(asOf.ema21.values[i], last.ema21.values[i])
        assertEquals(asOf.ichimoku.tenkan.values[i], last.ichimoku.tenkan.values[i])
        assertEquals(asOf.ichimoku.senkouA.values[i], last.ichimoku.senkouA.values[i])
    }

    @Test
    fun customParamsFlowThroughEvaluateAsOfWithoutPeek() {
        val bars = load1h()
        val custom = IndicatorParams(rsiPeriod = 7, volumeProfileLookback = 12)
        val adapter = ChartAsOfOverlays.fromBars(bars, cutoff, custom)
        assertEquals(custom, adapter.params)
        val i = 250
        val asOf = adapter.evaluateAt(i)
        assertEquals(7, asOf.params.rsiPeriod)
        assertEquals(12, asOf.volumeProfile.lookbackBarsRequested)
        assertNull(adapter.chikouPlotKnownAt(i))
        val full = IndicatorCalc.evaluate(bars, cutoff, custom)
        assertNotEquals(full.volumeProfile.pointOfControl!!, adapter.volumeProfileAt(i).pointOfControl!!, 1.0)
        val last = adapter.asOfLast
        assertEquals(asOf.rsi.values[i], last.rsi.values[i])
    }

    @Test
    fun fenceStillDropsOvershootOnAsOfPath() {
        val bars = load1h()
        val overshootClose = cutoff.cutoffMs + 60_000L
        val asOfFuture = IndicatorCalc.evaluateAsOf(bars, cutoff, overshootClose)
        assertTrue(asOfFuture.bars.all { it.closeTimeMs <= cutoff.cutoffMs })
        assertEquals(cutoff.cutoffMs, asOfFuture.fence.cutoffMs)
        val adapter = ChartAsOfOverlays.fromBars(bars, cutoff)
        assertTrue(adapter.bars.all { it.closeTimeMs <= cutoff.cutoffMs })
        assertEquals(cutoff.snapshotVersion, adapter.asOfLast.fence.snapshotVersion)
        assertFalse(ChartHonesty.packagedAsOfCaption.contains(ChartHonesty.FORBIDDEN_LIVE))
    }

    private fun load1h(): List<IndicatorBar> {
        val json = readSnapshot("BTCUSDT_1h.json")
        val root = JSONObject(json)
        val arr = root.getJSONArray("candles")
        val out = ArrayList<IndicatorBar>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                IndicatorBar(
                    openTimeMs = o.getLong("openTimeMs"),
                    closeTimeMs = o.getLong("closeTimeMs"),
                    open = o.getDouble("open"),
                    high = o.getDouble("high"),
                    low = o.getDouble("low"),
                    close = o.getDouble("close"),
                    volume = o.getDouble("volume"),
                    isFinal = o.optBoolean("isFinal", true),
                ),
            )
        }
        return out
    }

    private fun readSnapshot(name: String): String {
        val fromClasspath = ChartAsOfHonestyTest::class.java.classLoader.getResourceAsStream(name)
        if (fromClasspath != null) {
            return fromClasspath.bufferedReader().use { it.readText() }
        }
        val candidates = listOf(
            Path.of("data/src/main/assets/md_snapshot").resolve(name),
            Path.of("../data/src/main/assets/md_snapshot").resolve(name),
            Path.of("../../data/src/main/assets/md_snapshot").resolve(name),
        )
        val path = candidates.firstOrNull { Files.isRegularFile(it) }
            ?: error("md_snapshot/$name not found")
        return Files.readString(path)
    }
}
