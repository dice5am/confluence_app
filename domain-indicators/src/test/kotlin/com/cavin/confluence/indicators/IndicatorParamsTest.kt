package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IndicatorParamsTest {
    private val cutoff = SnapshotFixtures.loadMetaCutoff()
    private val wideCutoff = cutoff.copy(cutoffMs = Long.MAX_VALUE)

    @Test
    fun defaultParamsMatchNoArgEvaluateOnFenced1h() {
        val raw = SnapshotFixtures.loadBars("1h")
        val baseline = IndicatorCalc.evaluate(raw, cutoff)
        val explicit = IndicatorCalc.evaluate(raw, cutoff, IndicatorParams.DEFAULT)
        assertThat(explicit).isEqualTo(baseline)
        assertThat(baseline.params).isEqualTo(IndicatorParams.DEFAULT)
        assertThat(baseline.movingAverages.map { it.key })
            .isEqualTo(listOf("EMA:9", "EMA:21", "SMA:50", "SMA:200"))
        assertThat(baseline.ma(MaType.EMA, 9)?.values).isEqualTo(baseline.ema9.values)
        assertThat(baseline.ma("EMA:21")?.values).isEqualTo(baseline.ema21.values)
        assertThat(baseline.ma(MaType.SMA, 50)?.values).isEqualTo(baseline.sma50.values)
        assertThat(baseline.ma("SMA:200")?.values).isEqualTo(baseline.sma200.values)
        assertThat(baseline.rsi.values).isEqualTo(baseline.rsi14.values)
        assertThat(baseline.volumeSma.values).isEqualTo(baseline.volumeSma20.values)
        assertThat(baseline.volumeProfile.lookbackBarsRequested).isEqualTo(24)
        assertThat(baseline.ichimoku.displacement).isEqualTo(26)
    }

    @Test
    fun defaultParamsMatchNoArgEvaluateAsOfAndOnClosedBar() {
        val raw = SnapshotFixtures.loadBars("1h")
        val fenced = CandleFence.apply(raw, cutoff).bars
        val asOfMs = fenced[250].closeTimeMs
        val baseline = IndicatorCalc.evaluateAsOf(raw, cutoff, asOfMs)
        val explicit = IndicatorCalc.evaluateAsOf(raw, cutoff, asOfMs, IndicatorParams.DEFAULT)
        assertThat(explicit).isEqualTo(baseline)

        val current = IndicatorCalc.evaluate(raw, cutoff)
        val overshoot = current.bars.last().copy(
            openTimeMs = current.bars.last().openTimeMs + 3_600_000L,
            closeTimeMs = cutoff.cutoffMs + 1,
            isFinal = true,
        )
        assertThat(IndicatorCalc.onClosedBar(current, overshoot))
            .isEqualTo(IndicatorCalc.onClosedBar(current, overshoot, IndicatorParams.DEFAULT))
    }

    @Test
    fun customRsiPeriodChangesOutput() {
        val bars = mixedCloses(40)
        val rsi14 = IndicatorCalc.evaluate(bars, wideCutoff)
        val rsi7 = IndicatorCalc.evaluate(bars, wideCutoff, IndicatorParams(rsiPeriod = 7))
        assertThat(rsi14.rsi14.values[7]).isNull()
        assertThat(rsi7.rsi.values[7]).isNotNull()
        assertThat(rsi7.rsi.values.last()).isNotEqualTo(rsi14.rsi14.values.last())
        assertThat(rsi7.params.rsiPeriod).isEqualTo(7)
    }

    @Test
    fun customMovingAveragePeriodsSurfaceInKeyedList() {
        val bars = TestBars.closes(DoubleArray(220) { 50.0 + it })
        val params = IndicatorParams(
            movingAverages = listOf(
                MaSpec(MaType.SMA, 8),
                MaSpec(MaType.EMA, 21),
                MaSpec(MaType.SMA, 50),
                MaSpec(MaType.SMA, 200),
            ),
        )
        val out = IndicatorCalc.evaluate(bars, wideCutoff, params)
        assertThat(out.movingAverages.map { it.key })
            .isEqualTo(listOf("SMA:8", "EMA:21", "SMA:50", "SMA:200"))
        assertThat(out.ma(MaType.SMA, 8)!!.values[6]).isNull()
        assertThat(out.ma(MaType.SMA, 8)!!.values[7]).isNotNull()
        assertThat(out.ema9.values.all { it == null }).isTrue()
        assertThat(out.ema21.values[20]).isNotNull()
        assertThat(out.sma50.values[49]).isNotNull()
        assertThat(out.sma200.values[199]).isNotNull()
        val defaultEma9 = IndicatorCalc.evaluate(bars, wideCutoff).ema9
        assertThat(out.ma(MaType.SMA, 8)!!.values.last()).isNotEqualTo(defaultEma9.values.last())
    }

    @Test
    fun customIchimokuAndVolumeSmaAndVpLookbackChangeOutputs() {
        val bars = (0 until 80).map { i ->
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0 + i,
                high = 120.0 + i,
                low = 80.0 + i,
                close = 110.0 + i,
                volume = 10.0 + i,
            )
        }
        val baseline = IndicatorCalc.evaluate(bars, wideCutoff)
        val custom = IndicatorCalc.evaluate(
            bars,
            wideCutoff,
            IndicatorParams(
                rsiPeriod = 14,
                ichimoku = IchimokuParams(
                    tenkanPeriod = 8,
                    kijunPeriod = 21,
                    senkouBPeriod = 50,
                    displacement = 21,
                ),
                volumeSmaPeriod = 10,
                volumeProfileLookback = 10,
            ),
        )
        assertThat(custom.ichimoku.tenkan.values[7]).isNotNull()
        assertThat(baseline.ichimoku.tenkan.values[7]).isNull()
        assertThat(custom.ichimoku.displacement).isEqualTo(21)
        assertThat(custom.ichimoku.forwardCloud).hasSize(21)
        assertThat(custom.volumeSma.values[9]).isNotNull()
        assertThat(baseline.volumeSma20.values[9]).isNull()
        assertThat(custom.volumeProfile.lookbackBarsRequested).isEqualTo(10)
        assertThat(custom.volumeProfile.usedBarCount).isEqualTo(10)
        assertThat(custom.volumeProfile.windowFirstOpenTimeMs)
            .isNotEqualTo(baseline.volumeProfile.windowFirstOpenTimeMs)
        assertThat(custom.volumeProfile.pointOfControl)
            .isNotEqualTo(baseline.volumeProfile.pointOfControl)
        assertThat(custom.volumeProfile.notes).contains("last 10 closed bars")
    }

    @Test
    fun evaluateAsOfWithCustomParamsStaysCausal() {
        val bars = (0 until 80).map { i ->
            TestBars.bar(
                openTimeMs = 1_000_000L + i * 3_600_000L,
                open = 100.0 + i,
                high = 110.0 + i,
                low = 90.0 + i,
                close = 105.0 + i,
                volume = 10.0 + i,
            )
        }
        val params = IndicatorParams(
            movingAverages = listOf(MaSpec(MaType.SMA, 8), MaSpec(MaType.EMA, 21)),
            rsiPeriod = 7,
            ichimoku = IchimokuParams(tenkanPeriod = 8, kijunPeriod = 21, senkouBPeriod = 50, displacement = 21),
            volumeSmaPeriod = 10,
            volumeProfileLookback = 10,
        )
        val i = 50
        val asOfMs = bars[i].closeTimeMs
        val shocked = bars.mapIndexed { idx, bar ->
            if (idx <= i) bar else bar.copy(high = 9_999.0, low = 1.0, close = 9_999.0, volume = 1_000_000.0)
        }
        val prefix = IndicatorCalc.evaluate(bars.take(i + 1), wideCutoff, params)
        val asOf = IndicatorCalc.evaluateAsOf(shocked, wideCutoff, asOfMs, params)
        val fullShocked = IndicatorCalc.evaluate(shocked, wideCutoff, params)

        assertThat(asOf.bars).hasSize(i + 1)
        assertThat(asOf.rsi.values[i]).isEqualTo(prefix.rsi.values[i])
        assertThat(asOf.ma(MaType.SMA, 8)!!.values[i]).isEqualTo(prefix.ma(MaType.SMA, 8)!!.values[i])
        assertThat(asOf.volumeSma.values[i]).isEqualTo(prefix.volumeSma.values[i])
        assertThat(asOf.ichimoku.tenkan.values[i]).isEqualTo(prefix.ichimoku.tenkan.values[i])
        assertThat(asOf.volumeProfile.usedBarCount).isEqualTo(10)
        assertThat(asOf.volumeProfile.windowLastCloseTimeMs).isEqualTo(asOfMs)
        assertThat(asOf.volumeProfile.pointOfControl).isEqualTo(prefix.volumeProfile.pointOfControl)
        assertThat(asOf.volumeProfile.pointOfControl)
            .isNotEqualTo(fullShocked.volumeProfile.pointOfControl)
        assertThat(asOf.ichimoku.chikou.values[i]).isNull()
        assertThat(fullShocked.ichimoku.chikou.values[i]).isEqualTo(9_999.0)
    }

    @Test
    fun onClosedBarRecomputesWithNewParams() {
        val bars = mixedCloses(40)
        val current = IndicatorCalc.evaluate(bars.dropLast(1), wideCutoff)
        val last = bars.last()
        val same = IndicatorCalc.onClosedBar(current, last)
        val custom = IndicatorCalc.onClosedBar(current, last, IndicatorParams(rsiPeriod = 7))
        assertThat(same.params).isEqualTo(IndicatorParams.DEFAULT)
        assertThat(custom.params.rsiPeriod).isEqualTo(7)
        assertThat(custom.rsi.values.last()).isNotEqualTo(same.rsi14.values.last())
        assertThat(custom.bars.last().openTimeMs).isEqualTo(last.openTimeMs)
    }

    @Test
    fun rejectNonPositivePeriods() {
        assertThrowsIllegal { IndicatorParams(rsiPeriod = 0) }
        assertThrowsIllegal { IndicatorParams(volumeSmaPeriod = -1) }
        assertThrowsIllegal { IndicatorParams(volumeProfileLookback = 0) }
        assertThrowsIllegal { MaSpec(MaType.EMA, 0) }
        assertThrowsIllegal { IchimokuParams(tenkanPeriod = 0) }
        assertThrowsIllegal { IchimokuParams(displacement = -1) }
    }

    private fun mixedCloses(count: Int): List<IndicatorBar> =
        TestBars.closes(
            DoubleArray(count) { i -> 100.0 + (i % 7) - (i % 5) + i * 0.25 },
        )

    private fun assertThrowsIllegal(block: () -> Unit) {
        try {
            block()
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // expected
        }
    }
}
