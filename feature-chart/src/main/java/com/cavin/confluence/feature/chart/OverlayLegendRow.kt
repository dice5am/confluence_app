package com.cavin.confluence.feature.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.Spacing

/** Floating legend chips on the price pane (V2 overlay-first). */
@Composable
fun OverlayLegendRow(
    overlays: ChartOverlayVisibility,
    palette: ChartIndicatorPalette,
    modifier: Modifier = Modifier,
) {
    val ids = listOf(
        ChartIndicatorId.Ema9,
        ChartIndicatorId.Sma21,
        ChartIndicatorId.Ichimoku,
        ChartIndicatorId.VolumeProfile,
    ).filter { overlays.isVisible(it) }
    if (ids.isEmpty()) return
    Row(
        modifier = modifier.testTag("overlayLegend"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (id in ids) {
            val color = palette.legendColor(id) ?: continue
            val label = id.legendLabel() ?: continue
            val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(ConfluenceColors.VoidElevated.copy(alpha = 0.82f), shape)
                    .border(1.dp, ConfluenceColors.BorderSubtle, shape)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    .testTag("legendChip-${id.name}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Text(
                    text = label,
                    style = ConfluenceType.Telemetry,
                    color = ConfluenceColors.Ice,
                )
            }
        }
    }
}
