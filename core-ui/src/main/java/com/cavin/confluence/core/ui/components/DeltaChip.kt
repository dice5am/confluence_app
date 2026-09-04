package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.Spacing
import java.util.Locale

@Composable
fun DeltaChip(
    percent: Double?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
    val (label, color) = when {
        percent == null -> "—%" to ConfluenceColors.Slate
        percent >= 0 -> String.format(Locale.US, "%+.2f%%", percent) to ConfluenceColors.Mint
        else -> String.format(Locale.US, "%+.2f%%", percent) to ConfluenceColors.Rose
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f), shape)
            .border(ConfluenceDimens.glassBorder, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(text = label, style = ConfluenceMono.Row.copy(color = color))
    }
}
