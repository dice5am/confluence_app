package com.cavin.confluence.feature.chart

import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.MaSpec
import com.cavin.confluence.indicators.MaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChartIndicatorPreferencesTest {

    @Test
    fun visibilityAndWellsPersist() {
        val ctx = RuntimeEnvironment.getApplication()
        ctx.getSharedPreferences("confluence_chart_indicators", 0).edit().clear().commit()
        val store = ChartIndicatorPreferences(ctx)
        val hidden = ChartOverlayVisibility.AllOn.toggle(ChartIndicatorId.Ichimoku)
        store.saveVisibility(hidden)
        val cycled = ChartIndicatorPalette.Defaults.cycleWell(ChartIndicatorId.Ma0, 0)
        store.savePalette(cycled)
        val custom = IndicatorParams(
            movingAverages = listOf(
                MaSpec(MaType.SMA, 8),
                MaSpec(MaType.EMA, 21),
                MaSpec(MaType.SMA, 50),
                MaSpec(MaType.SMA, 200),
            ),
            rsiPeriod = 7,
            volumeSmaPeriod = 10,
            volumeProfileLookback = 12,
        )
        store.saveParams(custom)
        val loaded = ChartIndicatorPreferences(ctx)
        assertFalse(loaded.loadVisibility().ichimoku)
        assertEquals(cycled.ma0, loaded.loadPalette().ma0)
        assertEquals(ChartIndicatorPalette.Defaults.ma1, loaded.loadPalette().ma1)
        val params = loaded.loadParams()
        assertEquals(7, params.rsiPeriod)
        assertEquals(MaType.SMA, params.movingAverages[0].type)
        assertEquals(8, params.movingAverages[0].period)
        assertEquals(10, params.volumeSmaPeriod)
        assertEquals(12, params.volumeProfileLookback)
    }
}
