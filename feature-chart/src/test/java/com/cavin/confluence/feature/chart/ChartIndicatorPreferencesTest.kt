package com.cavin.confluence.feature.chart

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
        val cycled = ChartIndicatorPalette.Defaults.cycleWell(ChartIndicatorId.Ema9, 0)
        store.savePalette(cycled)
        val loaded = ChartIndicatorPreferences(ctx)
        assertFalse(loaded.loadVisibility().ichimoku)
        assertEquals(cycled.ema9, loaded.loadPalette().ema9)
        assertEquals(ChartIndicatorPalette.Defaults.sma21, loaded.loadPalette().sma21)
    }
}
