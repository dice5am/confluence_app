package com.cavin.confluence.core.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors

enum class AppCardGlow {
    Blue,
    Orange,
    None,
}

/**
 * Glass-morphism panel — frosted fill, neon rim, optional outer glow.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    glow: AppCardGlow = AppCardGlow.Blue,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val glowColor = when (glow) {
        AppCardGlow.Blue -> ConfluenceColors.PrimaryGlow
        AppCardGlow.Orange -> ConfluenceColors.AccentGlow
        AppCardGlow.None -> Color.Transparent
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (glow != AppCardGlow.None) {
                    drawRoundRect(
                        color = glowColor,
                        cornerRadius = CornerRadius(20.dp.toPx()),
                    )
                }
            }
            .padding(if (glow != AppCardGlow.None) 2.dp else 0.dp)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        ConfluenceColors.GlassHighlight,
                        ConfluenceColors.Glass,
                    ),
                ),
                shape = shape,
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        ConfluenceColors.GlassBorder,
                        when (glow) {
                            AppCardGlow.Orange -> ConfluenceColors.Accent.copy(alpha = 0.45f)
                            AppCardGlow.Blue -> ConfluenceColors.Primary.copy(alpha = 0.35f)
                            AppCardGlow.None -> ConfluenceColors.Outline
                        },
                    ),
                ),
                shape = shape,
            )
            .padding(contentPadding),
    ) {
        Column(content = content)
    }
}
