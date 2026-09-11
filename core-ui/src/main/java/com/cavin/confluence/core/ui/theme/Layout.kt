package com.cavin.confluence.core.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Shared screen rhythm (B1 layout polish).
 * Faces, chips, HUD, and dock share [screenGutter]; acrylic under-layers
 * peek equally into that gutter (centered — not a SE translation).
 */
object ConfluenceLayout {
    val screenGutter: Dp = Spacing.lg
    val screenTop: Dp = Spacing.md
    val screenBottom: Dp = Spacing.lg
    val stackGap: Dp = Spacing.lg
    val chromeGap: Dp = Spacing.sm
}

fun Modifier.confluenceScreenGutter(): Modifier =
    this
        .statusBarsPadding()
        .padding(horizontal = ConfluenceLayout.screenGutter)
