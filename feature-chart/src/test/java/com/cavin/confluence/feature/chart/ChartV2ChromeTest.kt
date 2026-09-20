package com.cavin.confluence.feature.chart

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h780dp-xxhdpi")
class ChartV2ChromeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun snapshotBadgeAndAsOfNeverLive() {
        composeRule.setContent { ChartPreviewOverlays() }
        composeRule.onNodeWithText("LIVE", substring = true, ignoreCase = false).assertDoesNotExist()
        composeRule.onNodeWithTag("snapshotBadge").assertIsDisplayed()
        composeRule.onNodeWithTag("chartHonestyAsOf")
            .assertTextContains("Snapshot · 2026-09-19 10:59 UTC")
        composeRule.onNodeWithTag("chartHonestyMode")
            .assertTextContains("Overlay-first · RSI only")
        composeRule.onNodeWithTag("chartPair").assertTextContains("BTCUSDT")
        composeRule.onNodeWithTag("overlayLegend").assertExists()
        composeRule.onNodeWithTag("chartSettingsGear").assertExists()
    }

    @Test
    fun settingsSheetHasV2GroupsAndOnChartChips() {
        composeRule.setContent { ChartPreviewIndicatorsSheet() }
        composeRule.onNodeWithText("Chart indicators").assertIsDisplayed()
        composeRule.onNodeWithText("OVERLAYS").assertIsDisplayed()
        composeRule.onNodeWithText("OSCILLATORS").assertIsDisplayed()
        composeRule.onNodeWithText("VOLUME").assertIsDisplayed()
        composeRule.onNodeWithText("EMA 9").assertIsDisplayed()
        composeRule.onNodeWithText("EMA 21").assertIsDisplayed()
        composeRule.onNodeWithText("SMA 50").assertIsDisplayed()
        composeRule.onNodeWithText("SMA 200").assertIsDisplayed()
        composeRule.onNodeWithText("Ichimoku Cloud").assertIsDisplayed()
        composeRule.onNodeWithText("Volume Profile").assertIsDisplayed()
        composeRule.onNodeWithText("RSI 14").assertIsDisplayed()
        composeRule.onNodeWithText("Volume (ribbon)").assertIsDisplayed()
        composeRule.onNodeWithText("Type").assertIsDisplayed()
        composeRule.onNodeWithText("Length").assertIsDisplayed()
        composeRule.onNodeWithText("Lookback").assertIsDisplayed()
        composeRule.onNodeWithTag("chartIndicatorsActiveCount").assertTextContains("6 active")
    }
}
