package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cavin.confluence.core.ui.theme.ConfluenceColors

/** Small hierarchy label above a chrome block (chips, cards, chart pane). */
@Composable
fun AppSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = if (accent) ConfluenceColors.Primary else ConfluenceColors.OnSurfaceMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
