package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors

enum class AppChipAccent {
    Blue,
    Orange,
}

/**
 * Selectable chip — selected state uses blue or orange neon fill.
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: AppChipAccent = AppChipAccent.Blue,
) {
    val shape = RoundedCornerShape(999.dp)
    val accentColor = when (accent) {
        AppChipAccent.Blue -> ConfluenceColors.Primary
        AppChipAccent.Orange -> ConfluenceColors.Accent
    }
    val bg = if (selected) {
        Brush.horizontalGradient(
            listOf(accentColor.copy(alpha = 0.35f), accentColor.copy(alpha = 0.18f)),
        )
    } else {
        Brush.horizontalGradient(
            listOf(ConfluenceColors.SurfaceVariant, ConfluenceColors.Surface),
        )
    }
    val border = if (selected) accentColor.copy(alpha = 0.85f) else ConfluenceColors.Outline
    val fg = if (selected) accentColor else ConfluenceColors.OnSurfaceMuted

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
        )
    }
}

/** Non-interactive status / freshness pill. */
@Composable
fun AppStatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.18f), shape)
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}
