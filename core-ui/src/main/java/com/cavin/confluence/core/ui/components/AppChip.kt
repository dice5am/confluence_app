package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceType

enum class AppChipAccent {
    Plasma,
    Acrylic,
    Ice,
}

/**
 * Telemetry / TF chip — selected = plasma fill + glow stroke; acrylic = cyan edge.
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: AppChipAccent = AppChipAccent.Plasma,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    val border = when {
        !enabled -> ConfluenceColors.Dim.copy(alpha = 0.35f)
        accent == AppChipAccent.Acrylic -> ConfluenceColors.AcrylicEdge.copy(alpha = 0.75f)
        selected -> ConfluenceColors.Plasma
        else -> ConfluenceColors.BorderSubtle
    }
    val bg = when {
        !enabled -> ConfluenceColors.VoidElevated.copy(alpha = 0.4f)
        selected -> ConfluenceColors.Plasma.copy(alpha = 0.25f)
        accent == AppChipAccent.Acrylic -> ConfluenceColors.VoidElevated.copy(alpha = 0.7f)
        else -> ConfluenceColors.VoidElevated.copy(alpha = 0.55f)
    }
    val fg = when {
        !enabled -> ConfluenceColors.Dim.copy(alpha = 0.45f)
        selected -> ConfluenceColors.Ice
        accent == AppChipAccent.Ice -> ConfluenceColors.Ice
        else -> ConfluenceColors.Muted
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = ConfluenceType.Telemetry,
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
            style = ConfluenceType.Telemetry,
            color = color,
            fontWeight = FontWeight.Medium,
        )
    }
}
