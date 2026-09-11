package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceType

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
        style = ConfluenceType.Eyebrow,
        fontWeight = FontWeight.SemiBold,
        color = if (accent) ConfluenceColors.Plasma else ConfluenceColors.Dim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
