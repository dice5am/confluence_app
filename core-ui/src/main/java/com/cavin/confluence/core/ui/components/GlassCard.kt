package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.cavin.confluence.core.ui.theme.ConfluenceDimens

/**
 * Plasma Acrylic card — translucent face + acrylic under-layer + brackets.
 * [glow] enables the full F1 system (under-layer + bloom). They travel together.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    glow: Boolean = false,
    dashed: Boolean = false,
    brackets: Boolean = glow || accentBorder,
    contentPadding: Dp = ConfluenceDimens.glassPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    PlasmaAcrylicBox(
        modifier = modifier,
        glow = glow || accentBorder,
        brackets = brackets,
        dashed = dashed,
        contentPadding = contentPadding,
    ) {
        Column(content = content)
    }
}

enum class AppCardGlow { Plasma, None }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    glow: AppCardGlow = AppCardGlow.Plasma,
    contentPadding: Dp = ConfluenceDimens.glassPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        accentBorder = glow != AppCardGlow.None,
        glow = glow != AppCardGlow.None,
        contentPadding = contentPadding,
        content = content,
    )
}
