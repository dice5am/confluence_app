package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors

enum class AppButtonStyle {
    /** Neon blue filled CTA */
    Primary,
    /** Neon orange filled CTA */
    Secondary,
    /** Ghost / text — low emphasis */
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
    val shape = RoundedCornerShape(14.dp)
    val min = Modifier
        .then(modifier)
        .defaultMinSize(minHeight = 48.dp)
        .heightIn(min = 48.dp)

    when (style) {
        AppButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = min,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ConfluenceColors.Primary,
                contentColor = ConfluenceColors.OnPrimary,
                disabledContainerColor = ConfluenceColors.PrimaryContainer,
                disabledContentColor = ConfluenceColors.OnSurfaceMuted,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            content = content,
        )
        AppButtonStyle.Secondary -> Button(
            onClick = onClick,
            modifier = min,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ConfluenceColors.Accent,
                contentColor = ConfluenceColors.OnAccent,
                disabledContainerColor = ConfluenceColors.AccentContainer,
                disabledContentColor = ConfluenceColors.OnSurfaceMuted,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            content = content,
        )
        AppButtonStyle.Ghost -> OutlinedButton(
            onClick = onClick,
            modifier = min,
            enabled = enabled,
            shape = shape,
            border = BorderStroke(1.dp, ConfluenceColors.Primary.copy(alpha = 0.45f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ConfluenceColors.Primary,
                disabledContentColor = ConfluenceColors.OnSurfaceMuted,
                containerColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            content = content,
        )
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
        colors = ButtonDefaults.textButtonColors(contentColor = ConfluenceColors.Primary),
        content = content,
    )
}
