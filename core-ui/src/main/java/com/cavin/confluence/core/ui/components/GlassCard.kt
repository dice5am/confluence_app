package com.cavin.confluence.core.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.Dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens

/**
 * Glass panel — translucent fill + 1dp border + optional cyan glow.
 * Non-blur fallback uses denser fill on API < 31 (blur backdrop optional later).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    glow: Boolean = false,
    contentPadding: Dp = ConfluenceDimens.glassPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.glassCorner)
    val borderColor = if (accentBorder) ConfluenceColors.BorderAccent else ConfluenceColors.BorderSubtle
    val fill = if (Build.VERSION.SDK_INT >= 31) {
        ConfluenceColors.SurfaceGlass
    } else {
        ConfluenceColors.SurfaceGlassSolid.copy(alpha = 0.92f)
    }
    val corner = ConfluenceDimens.glassCorner

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (glow) {
                    drawRoundRect(
                        color = ConfluenceColors.CyberCyanGlow,
                        cornerRadius = CornerRadius((corner + ConfluenceDimens.glassBorder * 4).toPx()),
                    )
                }
            }
            .then(if (glow) Modifier.padding(ConfluenceDimens.glassBorder * 2) else Modifier)
            .clip(shape)
            .background(fill, shape)
            .border(ConfluenceDimens.glassBorder, borderColor, shape)
            .padding(contentPadding),
    ) {
        Column(content = content)
    }
}

enum class AppCardGlow { Blue, Orange, None }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    glow: AppCardGlow = AppCardGlow.Blue,
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
