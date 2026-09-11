package com.cavin.confluence.feature.chart

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.components.PreviewAppShell
import com.cavin.confluence.data.model.Timeframe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Craft proof PNGs for the PR (zoomed out, zoomed in, bull+bear, crosshair,
 * TF switch, volume visible). Writes to /opt/cursor/artifacts/screenshots
 * when that directory exists, plus the module build dir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h780dp-xxhdpi")
class ChartCraftScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun zoomedOutVolumeVisible() {
        snap("chart-zoomed-out-volume") {
            ChartPreviewZoomedOut()
        }
    }

    @Test
    fun zoomedInBullAndBear() {
        snap("chart-zoomed-in-bull-bear") {
            ChartPreviewZoomedIn()
        }
    }

    @Test
    fun crosshairOn() {
        snap("chart-crosshair-on") {
            ChartPreviewCrosshair()
        }
    }

    @Test
    fun timeframeSwitchedTo1d() {
        snap("chart-tf-switched-1d") {
            ChartPreviewTfSwitched()
        }
    }

    @Test
    fun default1hWithHairline() {
        val state = chartProofUiState(Timeframe.H1, count = 64)
        snap("chart-1h-hairline-snapshot") {
            ConfluenceTheme {
                PreviewAppShell(selectedId = "chart") {
                    ChartScreen(
                        state = state,
                        viewportSeed = ChartViewportSeed(
                            candleWidth = ConfluenceDimens.chartDefaultCandleWidth,
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun b1ChartShellCraft() {
        snap("b1-chart") { ChartPreviewZoomedOut() }
    }

    private fun snap(name: String, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent(content)
        composeRule.mainClock.advanceTimeBy(400)
        val view = composeRule.activity.findViewById<View>(android.R.id.content)
        val widthPx = 400 * 3
        val heightPx = 780 * 3
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth.coerceAtLeast(1),
            view.measuredHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        view.draw(AndroidCanvas(bitmap))
        val dirs = listOf(
            File("/opt/cursor/artifacts/screenshots"),
            File("build/chart-craft-proof"),
        )
        dirs.forEach { dir ->
            dir.mkdirs()
            File(dir, "$name.png").outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        check(bitmap.width > 100 && bitmap.height > 100) { "empty screenshot $name" }
        val pixels = IntArray(64)
        bitmap.getPixels(pixels, 0, 8, bitmap.width / 2, bitmap.height / 2, 8, 8)
        check(pixels.any { it != 0 }) { "blank screenshot $name" }
    }
}
