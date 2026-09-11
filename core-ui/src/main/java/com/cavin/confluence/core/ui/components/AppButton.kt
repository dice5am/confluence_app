package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens

enum class AppButtonStyle {
    /** Plasma fill + bloom stroke */
    Primary,
    /** Outlined plasma — secondary actions (Retry) */
    Secondary,
    /** Ghost / low emphasis */
    Ghost,
}

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val fillAlpha = when {
        !enabled -> 0.08f
        style == AppButtonStyle.Ghost -> 0f
        style == AppButtonStyle.Secondary && !pressed -> 0.10f
        pressed -> 0.45f
        else -> 0.25f
    }
    val borderColor = when {
        !enabled -> ConfluenceColors.Dim.copy(alpha = 0.35f)
        focused -> ConfluenceColors.Bloom
        style == AppButtonStyle.Ghost -> ConfluenceColors.Plasma.copy(alpha = 0.45f)
        else -> ConfluenceColors.Plasma.copy(alpha = if (pressed) 0.85f else 0.65f)
    }
    val contentColor = when {
        !enabled -> ConfluenceColors.Dim.copy(alpha = 0.45f)
        else -> ConfluenceColors.Ice
    }
    val scale = if (pressed && enabled) ConfluenceDimens.pressScale else 1f
    val ring = ConfluenceDimens.focusRing

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .then(
                if (focused && enabled) {
                    Modifier
                        .drawBehind {
                            drawRoundRect(
                                color = ConfluenceColors.Plasma.copy(alpha = 0.25f),
                                cornerRadius = CornerRadius(15.dp.toPx()),
                            )
                        }
                        .padding(ring)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(ConfluenceColors.Plasma.copy(alpha = fillAlpha), shape)
            .border(ConfluenceDimens.glassBorder, borderColor, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 48.dp)
            .heightIn(min = 48.dp)
            .padding(PaddingValues(horizontal = 20.dp, vertical = 12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}

@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = ConfluenceColors.Plasma),
        content = content,
    )
}
