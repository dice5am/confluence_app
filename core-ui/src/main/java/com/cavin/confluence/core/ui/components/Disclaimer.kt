package com.cavin.confluence.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTypography

@Composable
fun Disclaimer(
    modifier: Modifier = Modifier,
    text: String = "Insight only — never executes trades.",
    centered: Boolean = true,
) {
    Text(
        text = text,
        modifier = modifier,
        style = ConfluenceTypography.labelSmall,
        color = ConfluenceColors.Slate,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
    )
}
