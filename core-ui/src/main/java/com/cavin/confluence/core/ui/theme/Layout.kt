package com.cavin.confluence.core.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Shared screen rhythm (B1 layout polish).
 *
 * Faces, chips, HUD, and dock share [screenGutter]. Acrylic under-layers stay
 * equal-inset / centered (not a SE translation). [peekReserve] is the full
 * halo extent so scroll clip and neighboring cards leave void between glows
 * at ~390dp.
 */
object ConfluenceLayout {
    val screenGutter: Dp = Spacing.xl
    val screenTop: Dp = Spacing.lg
    val screenBottom: Dp = Spacing.xl
    /** Face-to-face gap; must stay >= 2× [ConfluenceDimens.acrylicHaloReserve]. */
    val stackGap: Dp = Spacing.xxl
    val chromeGap: Dp = Spacing.sm
    /** Sibling chips / HUD cells — horizontal breathing without acrylic collision. */
    val inlineGap: Dp = Spacing.sm

    val peekReserve: Dp = ConfluenceDimens.acrylicHaloReserve
    val outerGutter: Dp = screenGutter - peekReserve
}

/** Status bar + outer gutter. Pair with [confluenceScreenInner] around content. */
fun Modifier.confluenceScreenOuter(): Modifier =
    this
        .statusBarsPadding()
        .padding(horizontal = ConfluenceLayout.outerGutter)

/**
 * Inner inset matching the acrylic halo reserve so under-layers and glows
 * stay inside clip bounds while faces still sit on [ConfluenceLayout.screenGutter].
 */
fun Modifier.confluenceScreenInner(
    top: Dp = ConfluenceLayout.screenTop,
    bottom: Dp = ConfluenceLayout.screenBottom,
): Modifier =
    this.padding(
        start = ConfluenceLayout.peekReserve,
        end = ConfluenceLayout.peekReserve,
        top = top,
        bottom = bottom,
    )

fun Modifier.confluenceScreenGutter(
    top: Dp = ConfluenceLayout.screenTop,
    bottom: Dp = ConfluenceLayout.screenBottom,
): Modifier =
    this.confluenceScreenOuter().confluenceScreenInner(top = top, bottom = bottom)
