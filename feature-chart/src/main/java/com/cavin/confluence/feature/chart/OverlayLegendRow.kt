package com.cavin.confluence.feature.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.Spacing
import com.cavin.confluence.indicators.IndicatorParams

/** Floating legend chips on the price pane (V2 overlay-first). */
@Composable
fun OverlayLegendRow(
    overlays: ChartOverlayVisibility,
    palette: ChartIndicatorPalette,
    params: IndicatorParams,
    modifier: Modifier = Modifier,
) {
    data class Chip(val tag: String, val label: String, val color: Color)
    val chips = buildList {
        params.movingAverages.forEachIndexed { index, spec ->
            if (overlays.isMaVisible(index)) {
                add(Chip("ma$index", spec.label(), palette.maSwatch(index).toColor()))
            }
        }
        if (overlays.ichimoku) {
            add(Chip("Ichimoku", "Ichimoku", palette.ichimokuCloud.toColor()))
        }
        if (overlays.volumeProfile) {
            add(Chip("VP", "VP", palette.volumeProfile.toColor()))
        }
    }
    if (chips.isEmpty()) return
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .testTag("overlayLegend"),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (chip in chips) {
            val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(ConfluenceColors.VoidElevated.copy(alpha = 0.82f), shape)
                    .border(1.dp, ConfluenceColors.BorderSubtle, shape)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                    .testTag("legendChip-${chip.tag}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(chip.color),
                )
                Text(
                    text = chip.label,
                    style = ConfluenceType.Telemetry,
                    color = ConfluenceColors.Ice,
                )
            }
        }
    }
}
