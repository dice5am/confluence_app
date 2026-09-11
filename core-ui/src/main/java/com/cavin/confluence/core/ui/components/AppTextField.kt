package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val border = when {
        isError -> ConfluenceColors.Neg
        focused -> ConfluenceColors.Bloom
        else -> ConfluenceColors.BorderSubtle
    }
    val fg = when {
        !enabled -> ConfluenceColors.Dim.copy(alpha = 0.45f)
        isError -> ConfluenceColors.Neg
        else -> ConfluenceColors.Text
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ConfluenceColors.VoidElevated.copy(alpha = if (enabled) 0.9f else 0.45f), shape)
            .border(ConfluenceDimens.glassBorder, border, shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        enabled = enabled,
        textStyle = ConfluenceTypography.bodyMedium.copy(color = fg),
        cursorBrush = SolidColor(ConfluenceColors.Plasma),
        interactionSource = interaction,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = ConfluenceTypography.bodyMedium, color = ConfluenceColors.Dim)
                }
                inner()
            }
        },
    )
}
