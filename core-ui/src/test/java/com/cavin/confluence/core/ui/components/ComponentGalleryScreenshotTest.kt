package com.cavin.confluence.core.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h860dp-xxhdpi")
class ComponentGalleryScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun componentGalleryB1() {
        snap("b1-components", heightDp = 860) { ComponentGalleryPreview() }
    }

    @Test
    fun h1SplashOnVoid() {
        snap("h1-splash", heightDp = 780) { ConfluenceSplashPreview() }
    }

    private fun snap(name: String, heightDp: Int, content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent(content)
        composeRule.mainClock.advanceTimeBy(400)
        val view = composeRule.activity.findViewById<View>(android.R.id.content)
        val widthPx = 390 * 3
        val heightPx = heightDp * 3
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
        listOf(
            File("/opt/cursor/artifacts/screenshots"),
            File("build/b1-proof"),
        ).forEach { dir ->
            dir.mkdirs()
            File(dir, "$name.png").outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        check(bitmap.width > 100 && bitmap.height > 100) { "empty screenshot $name" }
    }
}
