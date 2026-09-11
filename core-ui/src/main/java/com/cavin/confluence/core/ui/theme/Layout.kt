package com.cavin.confluence.core.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Shared screen rhythm (B1 layout polish).
 *
 * Faces, chips, HUD, and dock share [screenGutter]. Acrylic under-layers peek
 * equally into a reserved inner inset (not a SE translation) so scroll
 * containers can clip without cutting the halo.
 */
object ConfluenceLayout {
    val screenGutter: Dp = Spacing.lg
    val screenTop: Dp = Spacing.md
    val screenBottom: Dp = Spacing.lg
    val stackGap: Dp = Spacing.lg
    val chromeGap: Dp = Spacing.sm

    val peekReserve: Dp = ConfluenceDimens.acrylicUnderInset
    val outerGutter: Dp = screenGutter - peekReserve
}

/** Status bar + outer gutter. Pair with [confluenceScreenInner] around content. */
fun Modifier.confluenceScreenOuter(): Modifier =
    this
        .statusBarsPadding()
        .padding(horizontal = ConfluenceLayout.outerGutter)

/**
 * Inner inset matching the acrylic peek so under-layers stay inside clip
 * bounds while faces still sit on [ConfluenceLayout.screenGutter].
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
