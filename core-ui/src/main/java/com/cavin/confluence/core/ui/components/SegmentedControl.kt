package com.cavin.confluence.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.Spacing

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(ConfluenceDimens.glassCorner)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(barShape)
            .background(ConfluenceColors.VoidElevated.copy(alpha = 0.85f), barShape)
            .border(ConfluenceDimens.glassBorder, ConfluenceColors.BorderSubtle, barShape)
            .padding(Spacing.xs),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) ConfluenceColors.Plasma.copy(alpha = 0.25f) else ConfluenceColors.Void.copy(alpha = 0f),
                tween(200),
                label = "seg$index",
            )
            val fg by animateColorAsState(
                if (selected) ConfluenceColors.Ice else ConfluenceColors.Muted,
                tween(200),
                label = "segFg$index",
            )
            val segShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(segShape)
                    .background(bg, segShape)
                    .then(
                        if (selected) {
                            Modifier.border(ConfluenceDimens.glassBorder, ConfluenceColors.Plasma, segShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = ConfluenceType.Telemetry,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = fg,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
