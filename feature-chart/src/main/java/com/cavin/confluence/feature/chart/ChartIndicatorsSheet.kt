package com.cavin.confluence.feature.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.components.AppSectionLabel
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing
import com.cavin.confluence.core.ui.theme.confluenceScreenGutter

/**
 * V2 denser full-height push sheet — section headers, per-indicator toggle,
 * larger color wells, segmented "On chart" chip. F1×B1 acrylic cards.
 */
@Composable
fun ChartIndicatorsSheet(
    overlays: ChartOverlayVisibility,
    palette: ChartIndicatorPalette,
    onToggle: (ChartIndicatorId) -> Unit,
    onCycleWell: (ChartIndicatorId, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ConfluenceThemeAccess.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConfluenceColors.Void)
            .statusBarsPadding()
            .confluenceScreenGutter(
                top = ConfluenceLayout.screenTop,
                bottom = ConfluenceLayout.screenBottom,
            )
            .testTag("chartIndicatorsSheet"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("chartIndicatorsBack"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Back",
                    tint = ConfluenceColors.Ice,
                )
            }
            Text(
                text = "Chart indicators",
                style = ConfluenceTypography.titleLarge,
                color = ConfluenceColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${overlays.activeCount()} active",
                style = ConfluenceType.Telemetry,
                color = ConfluenceColors.Bloom,
                modifier = Modifier.testTag("chartIndicatorsActiveCount"),
            )
        }

        Spacer(Modifier.height(spacing.sm))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap),
        ) {
            for (group in ChartIndicatorGroup.entries) {
                AppSectionLabel(group.sectionHeader())
                val ids = ChartIndicatorId.entries.filter { it.group() == group }
                Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                    for (id in ids) {
                        IndicatorSettingsCard(
                            id = id,
                            visible = overlays.isVisible(id),
                            wells = palette.wells(id),
                            onToggle = { onToggle(id) },
                            onCycleWell = { well -> onCycleWell(id, well) },
                        )
                    }
                }
                Spacer(Modifier.height(spacing.xs))
            }
        }
    }
}

@Composable
private fun IndicatorSettingsCard(
    id: ChartIndicatorId,
    visible: Boolean,
    wells: List<OverlaySwatch>,
    onToggle: () -> Unit,
    onCycleWell: (Int) -> Unit,
) {
    GlassCard(
        modifier = Modifier.testTag("indicatorRow-${id.name}"),
        glow = true,
        contentPadding = ConfluenceDimens.glassPaddingTight,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = id.settingsTitle(),
                    style = ConfluenceTypography.titleMedium,
                    color = ConfluenceColors.Text,
                )
                Spacer(Modifier.height(Spacing.xs))
                VisibleOnChartChip(visible = visible, onClick = onToggle)
            }
            Switch(
                checked = visible,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("indicatorToggle-${id.name}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ConfluenceColors.Ice,
                    checkedTrackColor = ConfluenceColors.Plasma,
                    checkedBorderColor = ConfluenceColors.Plasma,
                    uncheckedThumbColor = ConfluenceColors.Muted,
                    uncheckedTrackColor = ConfluenceColors.VoidElevated,
                    uncheckedBorderColor = ConfluenceColors.BorderSubtle,
                ),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            wells.forEachIndexed { index, swatch ->
                ColorWell(
                    swatch = swatch,
                    onClick = { onCycleWell(index) },
                    modifier = Modifier.testTag("indicatorWell-${id.name}-$index"),
                )
            }
        }
    }
}

@Composable
private fun VisibleOnChartChip(
    visible: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
    val fg = if (visible) ConfluenceColors.Pos else ConfluenceColors.Dim
    val label = if (visible) "On chart" else "Off chart"
    Box(
        modifier = Modifier
            .clip(shape)
            .background(fg.copy(alpha = 0.14f), shape)
            .border(1.dp, fg.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .testTag("visibleOnChartChip"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ConfluenceType.Telemetry,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun ColorWell(
    swatch: OverlaySwatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = swatch.toColor()
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f), CircleShape)
            .border(2.dp, color, CircleShape)
            .clickable(onClick = onClick),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartIndicatorsSheetPreview() {
    ConfluenceTheme {
        ChartIndicatorsSheet(
            overlays = ChartOverlayVisibility.AllOn,
            palette = ChartIndicatorPalette.Defaults,
            onToggle = {},
            onCycleWell = { _, _ -> },
            onBack = {},
        )
    }
}
