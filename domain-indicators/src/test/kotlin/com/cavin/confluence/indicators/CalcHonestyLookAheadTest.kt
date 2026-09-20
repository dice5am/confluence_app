package com.cavin.confluence.indicators

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs

/**
 * Phase A calc-honesty: every overlay point at bar `i` must equal
 * [IndicatorCalc] on the fenced window ending at that bar's `closeTimeMs`.
 *
 * Snapshot pin: `md_snapshot@2026-09-19`, cutoffMs = 1789815599999.
 */
class CalcHonestyLookAheadTest {
    private val cutoff = SnapshotFixtures.loadMetaCutoff()

    @Test
    fun packagedSnapshotPinIsLocked() {
        assertThat(cutoff.cutoffMs).isEqualTo(1_789_815_599_999L)
        assertThat(cutoff.snapshotVersion).isEqualTo("md_snapshot@2026-09-19")
    }

    @Test
    fun causalSeriesOnFenced1hMatchPrefixEvaluate() {
        assertPrefixProperty("1h", ONE_HOUR_SAMPLE_INDICES)
    }

    @Test
    fun causalSeriesOnFenced1dMatchPrefixEvaluate() {
        assertPrefixProperty("1d", ONE_DAY_SAMPLE_INDICES)
    }

    @Test
    fun evaluateAsOfLastBarEqualsFullEvaluate() {
        for (tf in listOf("1h", "1d")) {
            val raw = SnapshotFixtures.loadBars(tf)
            val fenced = CandleFence.apply(raw, cutoff).bars
            val full = IndicatorCalc.evaluate(raw, cutoff)
            val asOfLast = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced.last().closeTimeMs)
            assertCausalSeriesEqual(tf, fenced.lastIndex, full, asOfLast)
            assertVolumeProfileEqual(full.volumeProfile, asOfLast.volumeProfile)
            assertThat(full.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced.last().closeTimeMs)
        }
    }

    @Test
    fun ichimokuForwardCloudAtAsOfMatchesLaterPlotAlignedSenkou() {
        for (tf in listOf("1h", "1d")) {
            val raw = SnapshotFixtures.loadBars(tf)
            val fenced = CandleFence.apply(raw, cutoff).bars
            val full = IndicatorCalc.evaluate(raw, cutoff)
            for (i in sampleIndices(fenced.size, if (tf == "1h") ONE_HOUR_SAMPLE_INDICES else ONE_DAY_SAMPLE_INDICES)) {
                val prefix = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs)
                for (point in prefix.ichimoku.forwardCloud) {
                    val futureIndex = i + point.offsetBars
                    if (futureIndex <= fenced.lastIndex) {
                        assertNullableClose(
                            actual = full.ichimoku.senkouA.values[futureIndex],
                            expected = point.senkouA,
                            label = "$tf senkouA plot[i+${point.offsetBars}] vs as-of-$i forwardCloud",
                        )
                        assertNullableClose(
                            actual = full.ichimoku.senkouB.values[futureIndex],
                            expected = point.senkouB,
                            label = "$tf senkouB plot[i+${point.offsetBars}] vs as-of-$i forwardCloud",
                        )
                    }
                    val rawIndex = i + point.offsetBars - Ichimoku.DISPLACEMENT
                    if (rawIndex in 0..i) {
                        assertNullableClose(
                            actual = prefix.ichimoku.senkouARaw.values[rawIndex],
                            expected = point.senkouA,
                            label = "$tf senkouARaw[$rawIndex] vs as-of-$i forwardCloud",
                        )
                        assertNullableClose(
                            actual = prefix.ichimoku.senkouBRaw.values[rawIndex],
                            expected = point.senkouB,
                            label = "$tf senkouBRaw[$rawIndex] vs as-of-$i forwardCloud",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun ichimokuChikouPlotSlotIsKnownOnlyAfterDisplacedClose() {
        for (tf in listOf("1h", "1d")) {
            val raw = SnapshotFixtures.loadBars(tf)
            val fenced = CandleFence.apply(raw, cutoff).bars
            val full = IndicatorCalc.evaluate(raw, cutoff)
            for (i in sampleIndices(fenced.size, if (tf == "1h") ONE_HOUR_SAMPLE_INDICES else ONE_DAY_SAMPLE_INDICES)) {
                val prefix = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs)
                for (j in 0..i) {
                    val known = j + Ichimoku.DISPLACEMENT <= i
                    if (known) {
                        assertNullableClose(
                            actual = prefix.ichimoku.chikou.values[j],
                            expected = fenced[j + Ichimoku.DISPLACEMENT].close,
                            label = "$tf as-of-$i chikou[$j] known close",
                        )
                        assertNullableClose(
                            actual = full.ichimoku.chikou.values[j],
                            expected = prefix.ichimoku.chikou.values[j],
                            label = "$tf full vs prefix chikou[$j] (known at $i)",
                        )
                    } else {
                        assertWithMessage("$tf as-of-$i chikou[$j] must be undefined (needs close of ${j + Ichimoku.DISPLACEMENT})")
                            .that(prefix.ichimoku.chikou.values[j])
                            .isNull()
                    }
                }
                if (i + Ichimoku.DISPLACEMENT <= fenced.lastIndex) {
                    assertWithMessage("$tf full.chikou[$i] uses close[${i + Ichimoku.DISPLACEMENT}] — plot look-ahead vs as-of $i")
                        .that(full.ichimoku.chikou.values[i])
                        .isWithin(1e-9)
                        .of(fenced[i + Ichimoku.DISPLACEMENT].close)
                    assertThat(prefix.ichimoku.chikou.values[i]).isNull()
                }
            }
        }
    }

    @Test
    fun volumeProfileAsOfUsesOnlyLast24EndingAtBar() {
        for (tf in listOf("1h", "1d")) {
            val raw = SnapshotFixtures.loadBars(tf)
            val fenced = CandleFence.apply(raw, cutoff).bars
            val fullVp = IndicatorCalc.evaluate(raw, cutoff).volumeProfile
            for (i in sampleIndices(fenced.size, if (tf == "1h") ONE_HOUR_SAMPLE_INDICES else ONE_DAY_SAMPLE_INDICES)) {
                val asOf = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs)
                val direct = VolumeProfile.computeAsOf(fenced, fenced[i].closeTimeMs)
                val expectedWindow = fenced.subList(maxOf(0, i - VolumeProfile.LOOKBACK_BARS + 1), i + 1)
                assertThat(asOf.volumeProfile.usedBarCount).isEqualTo(expectedWindow.size)
                assertThat(asOf.volumeProfile.windowFirstOpenTimeMs).isEqualTo(expectedWindow.first().openTimeMs)
                assertThat(asOf.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced[i].closeTimeMs)
                assertThat(asOf.volumeProfile.windowLastCloseTimeMs).isAtMost(fenced[i].closeTimeMs)
                assertVolumeProfileEqual(asOf.volumeProfile, direct)
                assertVolumeProfileEqual(asOf.volumeProfile, VolumeProfile.compute(expectedWindow))
                if (i < fenced.lastIndex - VolumeProfile.LOOKBACK_BARS) {
                    assertWithMessage("$tf VP as-of $i must not equal the full-series last-24 snapshot")
                        .that(asOf.volumeProfile.windowLastCloseTimeMs)
                        .isNotEqualTo(fullVp.windowLastCloseTimeMs)
                }
            }
            assertThat(fullVp.windowLastCloseTimeMs).isEqualTo(fenced.last().closeTimeMs)
            assertThat(fullVp.usedBarCount).isEqualTo(VolumeProfile.LOOKBACK_BARS)
        }
    }

    @Test
    fun futureBarShockDoesNotMoveCausalPointsOrAsOfVolumeProfile() {
        val bars = (0 until 80).map { idx ->
            TestBars.bar(
                openTimeMs = 1_000_000L + idx * 3_600_000L,
                open = 100.0 + idx,
                high = 110.0 + idx,
                low = 90.0 + idx,
                close = 105.0 + idx,
                volume = 10.0 + idx,
            )
        }
        val wideCutoff = cutoff.copy(cutoffMs = Long.MAX_VALUE)
        val i = 50
        val asOfMs = bars[i].closeTimeMs
        val shocked = bars.mapIndexed { idx, bar ->
            if (idx <= i) {
                bar
            } else {
                bar.copy(high = 9_999.0, low = 1.0, close = 9_999.0, volume = 1_000_000.0)
            }
        }
        val control = IndicatorCalc.evaluate(bars.take(i + 1), wideCutoff)
        val asOfShocked = IndicatorCalc.evaluateAsOf(shocked, wideCutoff, asOfMs)
        val fullShocked = IndicatorCalc.evaluate(shocked, wideCutoff)

        assertCausalSeriesEqual("synthetic", i, control, asOfShocked)
        assertVolumeProfileEqual(control.volumeProfile, asOfShocked.volumeProfile)
        assertThat(asOfShocked.volumeProfile.windowLastCloseTimeMs).isEqualTo(asOfMs)

        assertWithMessage("full-series VP must peek into shocked future bars")
            .that(abs(fullShocked.volumeProfile.pointOfControl!! - control.volumeProfile.pointOfControl!!))
            .isGreaterThan(1.0)
        assertWithMessage("full-series Chikou plot at i uses shocked close[i+26]")
            .that(fullShocked.ichimoku.chikou.values[i])
            .isWithin(1e-9)
            .of(9_999.0)
        assertThat(asOfShocked.ichimoku.chikou.values[i]).isNull()
        assertThat(control.ichimoku.chikou.values[i]).isNull()
    }

    @Test
    fun sampleBarAudit1hPrefixMatchesFullAtIndex250() {
        val raw = SnapshotFixtures.loadBars("1h")
        val fenced = CandleFence.apply(raw, cutoff).bars
        val i = SAMPLE_1H_AUDIT_INDEX
        assertThat(fenced).hasSize(499)
        assertThat(fenced[i].openTimeMs).isEqualTo(1_788_919_200_000L)
        assertThat(fenced[i].closeTimeMs).isEqualTo(1_788_922_799_999L)
        assertThat(fenced.last().closeTimeMs).isEqualTo(cutoff.cutoffMs)

        val full = IndicatorCalc.evaluate(raw, cutoff)
        val prefix = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs)
        assertCausalSeriesEqual("1h-audit", i, full, prefix)

        assertThat(prefix.rsi14.values[i]).isNotNull()
        assertThat(prefix.rsi14.values[i]!!).isFinite()
        assertThat(prefix.ema9.values[i]!!).isFinite()
        assertThat(prefix.ema21.values[i]!!).isFinite()
        assertThat(prefix.sma50.values[i]!!).isFinite()
        assertThat(prefix.sma200.values[i]!!).isFinite()
        assertThat(prefix.volumeSma20.values[i]!!).isFinite()
        assertThat(prefix.ichimoku.tenkan.values[i]!!).isFinite()
        assertThat(prefix.ichimoku.kijun.values[i]!!).isFinite()
        assertThat(prefix.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced[i].closeTimeMs)
        assertThat(prefix.volumeProfile.usedBarCount).isEqualTo(VolumeProfile.LOOKBACK_BARS)
        assertThat(full.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced.last().closeTimeMs)
        assertThat(full.volumeProfile.windowLastCloseTimeMs).isNotEqualTo(prefix.volumeProfile.windowLastCloseTimeMs)

        assertThat(prefix.rsi14.values[i]!!).isWithin(1e-9).of(50.25641484462052)
        assertThat(prefix.volumeSma20.values[i]!!).isWithin(1e-9).of(747.0739020000003)
        assertThat(prefix.ema9.values[i]!!).isWithin(1e-9).of(78649.15005315424)
        assertThat(prefix.ema21.values[i]!!).isWithin(1e-9).of(78664.64743073574)
        assertThat(prefix.sma50.values[i]!!).isWithin(1e-9).of(78978.23940000003)
        assertThat(prefix.sma200.values[i]!!).isWithin(1e-9).of(79034.74685000003)
        assertThat(prefix.ichimoku.tenkan.values[i]!!).isWithin(1e-9).of(78600.07)
        assertThat(prefix.ichimoku.kijun.values[i]!!).isWithin(1e-9).of(78552.505)
        assertThat(prefix.ichimoku.senkouA.values[i]!!).isWithin(1e-9).of(79360.0575)
        assertThat(prefix.ichimoku.senkouB.values[i]!!).isWithin(1e-9).of(79619.995)
        assertThat(prefix.ichimoku.senkouARaw.values[i]!!).isWithin(1e-9).of(78576.2875)
        assertThat(prefix.ichimoku.senkouBRaw.values[i]!!).isWithin(1e-9).of(79090.0)
        assertThat(prefix.ichimoku.chikou.values[i]).isNull()
        assertThat(prefix.volumeProfile.pointOfControl!!).isWithin(1e-9).of(78463.0239)
        assertThat(prefix.volumeProfile.valueAreaHigh!!).isWithin(1e-9).of(78753.2418)
        assertThat(prefix.volumeProfile.valueAreaLow!!).isWithin(1e-9).of(78228.08559999999)
        assertThat(full.volumeProfile.pointOfControl!!).isWithin(1e-9).of(81100.321)
    }

    @Test
    fun sampleBarAudit1dPrefixMatchesFullAtIndex250() {
        val raw = SnapshotFixtures.loadBars("1d")
        val fenced = CandleFence.apply(raw, cutoff).bars
        val i = SAMPLE_1D_AUDIT_INDEX
        assertThat(fenced).hasSize(399)
        assertThat(fenced[i].openTimeMs).isEqualTo(1_776_902_400_000L)
        assertThat(fenced[i].closeTimeMs).isEqualTo(1_776_988_799_999L)

        val full = IndicatorCalc.evaluate(raw, cutoff)
        val prefix = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs)
        assertCausalSeriesEqual("1d-audit", i, full, prefix)
        assertThat(prefix.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced[i].closeTimeMs)
        assertThat(full.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced.last().closeTimeMs)
    }

    @Test
    fun customParamsAsOfStillRespectsCutoffAndPrefix() {
        val raw = SnapshotFixtures.loadBars("1h")
        val fenced = CandleFence.apply(raw, cutoff).bars
        val params = IndicatorParams(
            movingAverages = listOf(MaSpec(MaType.SMA, 8), MaSpec(MaType.EMA, 21)),
            rsiPeriod = 7,
            volumeSmaPeriod = 10,
            volumeProfileLookback = 10,
            ichimoku = IchimokuParams(tenkanPeriod = 8),
        )
        val i = SAMPLE_1H_AUDIT_INDEX
        val prefix = IndicatorCalc.evaluate(fenced.take(i + 1), cutoff, params)
        val asOf = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs, params)
        assertThat(asOf.bars).hasSize(i + 1)
        assertThat(asOf.rsi.values[i]).isEqualTo(prefix.rsi.values[i])
        assertThat(asOf.ma(MaType.SMA, 8)!!.values[i]).isEqualTo(prefix.ma(MaType.SMA, 8)!!.values[i])
        assertThat(asOf.volumeProfile.windowLastCloseTimeMs).isEqualTo(fenced[i].closeTimeMs)
        assertThat(asOf.volumeProfile.usedBarCount).isEqualTo(10)
        assertThat(asOf.bars.all { it.closeTimeMs <= fenced[i].closeTimeMs }).isTrue()
        assertThat(asOf.rsi.values[i]).isNotEqualTo(
            IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs).rsi14.values[i],
        )
    }

    @Test
    fun asOfDoesNotLiftGlobalCutoffFence() {
        val raw = SnapshotFixtures.loadBars("1m")
        val fenced = CandleFence.apply(raw, cutoff)
        assertThat(fenced.droppedOvershootCount).isGreaterThan(0)
        val lastOvershoot = raw.filter { it.isFinal && it.closeTimeMs > cutoff.cutoffMs }.maxBy { it.closeTimeMs }
        val asOfFuture = IndicatorCalc.evaluateAsOf(raw, cutoff, lastOvershoot.closeTimeMs)
        assertThat(asOfFuture.bars).isNotEmpty()
        assertThat(asOfFuture.bars.all { it.closeTimeMs <= cutoff.cutoffMs }).isTrue()
        assertThat(asOfFuture.fence.lastCloseTimeMs).isAtMost(cutoff.cutoffMs)
        assertThat(asOfFuture.volumeProfile.windowLastCloseTimeMs).isAtMost(cutoff.cutoffMs)
    }

    private fun assertPrefixProperty(timeframe: String, preferred: IntArray) {
        val raw = SnapshotFixtures.loadBars(timeframe)
        val fenced = CandleFence.apply(raw, cutoff).bars
        assertThat(fenced).isNotEmpty()
        assertThat(fenced.all { it.isFinal && it.closeTimeMs <= cutoff.cutoffMs }).isTrue()
        val full = IndicatorCalc.evaluate(raw, cutoff)
        assertThat(full.bars.map { it.openTimeMs }).isEqualTo(fenced.map { it.openTimeMs })

        for (i in sampleIndices(fenced.size, preferred)) {
            val prefixBars = fenced.filter { it.closeTimeMs <= fenced[i].closeTimeMs }
            assertThat(prefixBars).hasSize(i + 1)
            val prefix = IndicatorCalc.evaluate(prefixBars, cutoff)
            val asOf = IndicatorCalc.evaluateAsOf(raw, cutoff, fenced[i].closeTimeMs)
            assertCausalSeriesEqual("$timeframe-prefix", i, full, prefix)
            assertCausalSeriesEqual("$timeframe-asOf", i, full, asOf)
            assertVolumeProfileEqual(prefix.volumeProfile, asOf.volumeProfile)
        }
    }

    private fun assertCausalSeriesEqual(
        label: String,
        i: Int,
        full: DayOneIndicators,
        prefix: DayOneIndicators,
    ) {
        assertWithMessage("$label prefix length").that(prefix.bars).hasSize(i + 1)
        assertThat(prefix.bars[i].openTimeMs).isEqualTo(full.bars[i].openTimeMs)
        assertThat(prefix.bars[i].closeTimeMs).isEqualTo(full.bars[i].closeTimeMs)

        assertSeriesAt(label, "rsi14", full.rsi14, prefix.rsi14, i)
        assertSeriesAt(label, "volume", full.volume, prefix.volume, i)
        assertSeriesAt(label, "volumeSma20", full.volumeSma20, prefix.volumeSma20, i)
        assertSeriesAt(label, "ema9", full.ema9, prefix.ema9, i)
        assertSeriesAt(label, "ema21", full.ema21, prefix.ema21, i)
        assertSeriesAt(label, "sma50", full.sma50, prefix.sma50, i)
        assertSeriesAt(label, "sma200", full.sma200, prefix.sma200, i)
        assertSeriesAt(label, "tenkan", full.ichimoku.tenkan, prefix.ichimoku.tenkan, i)
        assertSeriesAt(label, "kijun", full.ichimoku.kijun, prefix.ichimoku.kijun, i)
        assertSeriesAt(label, "senkouA-plot", full.ichimoku.senkouA, prefix.ichimoku.senkouA, i)
        assertSeriesAt(label, "senkouB-plot", full.ichimoku.senkouB, prefix.ichimoku.senkouB, i)
        assertSeriesAt(label, "senkouARaw", full.ichimoku.senkouARaw, prefix.ichimoku.senkouARaw, i)
        assertSeriesAt(label, "senkouBRaw", full.ichimoku.senkouBRaw, prefix.ichimoku.senkouBRaw, i)

        for (j in 0..i) {
            assertSeriesAt(label, "rsi14[$j]", full.rsi14, prefix.rsi14, j)
            assertSeriesAt(label, "ema9[$j]", full.ema9, prefix.ema9, j)
            assertSeriesAt(label, "tenkan[$j]", full.ichimoku.tenkan, prefix.ichimoku.tenkan, j)
            assertSeriesAt(label, "senkouA-plot[$j]", full.ichimoku.senkouA, prefix.ichimoku.senkouA, j)
            assertSeriesAt(label, "senkouB-plot[$j]", full.ichimoku.senkouB, prefix.ichimoku.senkouB, j)
        }
    }

    private fun assertSeriesAt(
        label: String,
        name: String,
        full: AlignedSeries,
        prefix: AlignedSeries,
        index: Int,
    ) {
        assertThat(prefix.openTimeMs[index]).isEqualTo(full.openTimeMs[index])
        assertNullableClose(
            actual = prefix.values[index],
            expected = full.values[index],
            label = "$label $name@$index",
        )
    }

    private fun assertVolumeProfileEqual(actual: VolumeProfileResult, expected: VolumeProfileResult) {
        assertThat(actual.usedBarCount).isEqualTo(expected.usedBarCount)
        assertThat(actual.windowFirstOpenTimeMs).isEqualTo(expected.windowFirstOpenTimeMs)
        assertThat(actual.windowLastCloseTimeMs).isEqualTo(expected.windowLastCloseTimeMs)
        assertNullableClose(actual.pointOfControl, expected.pointOfControl, "VP.poc")
        assertNullableClose(actual.valueAreaHigh, expected.valueAreaHigh, "VP.vah")
        assertNullableClose(actual.valueAreaLow, expected.valueAreaLow, "VP.val")
        assertThat(actual.totalVolume).isWithin(1e-9).of(expected.totalVolume)
        assertThat(actual.bins.size).isEqualTo(expected.bins.size)
        for (idx in actual.bins.indices) {
            assertThat(actual.bins[idx].volume).isWithin(1e-9).of(expected.bins[idx].volume)
            assertThat(actual.bins[idx].isPointOfControl).isEqualTo(expected.bins[idx].isPointOfControl)
            assertThat(actual.bins[idx].inValueArea).isEqualTo(expected.bins[idx].inValueArea)
        }
    }

    private fun assertNullableClose(actual: Double?, expected: Double?, label: String) {
        if (expected == null) {
            assertWithMessage(label).that(actual).isNull()
            return
        }
        assertWithMessage(label).that(actual).isNotNull()
        assertWithMessage(label).that(actual!!).isWithin(1e-9).of(expected)
    }

    private fun sampleIndices(size: Int, preferred: IntArray): IntArray =
        preferred.filter { it in 0 until size }.distinct().sorted().toIntArray()

    private companion object {
        const val SAMPLE_1H_AUDIT_INDEX: Int = 250
        const val SAMPLE_1D_AUDIT_INDEX: Int = 250
        val ONE_HOUR_SAMPLE_INDICES: IntArray = intArrayOf(0, 20, 50, 199, 250, 350, 472, 498)
        val ONE_DAY_SAMPLE_INDICES: IntArray = intArrayOf(0, 20, 50, 199, 250, 300, 372, 398)
    }
}
