package com.cavin.confluence.feature.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.MaSpec
import com.cavin.confluence.indicators.MaType

/**
 * V2 denser full-height push sheet — section headers, per-indicator toggle,
 * color wells, short param chips (type / period / lookback). F1×B1 acrylic.
 */
@Composable
fun ChartIndicatorsSheet(
    overlays: ChartOverlayVisibility,
    palette: ChartIndicatorPalette,
    params: IndicatorParams,
    onToggle: (ChartIndicatorId) -> Unit,
    onCycleWell: (ChartIndicatorId, Int) -> Unit,
    onParamsChange: (IndicatorParams) -> Unit,
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
                AppSectionLabel(
                    group.sectionHeader(),
                    modifier = Modifier.testTag("indicatorGroup-${group.name}"),
                )
                val ids = ChartIndicatorId.entries.filter { it.group() == group }
                Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
                    for (id in ids) {
                        IndicatorSettingsCard(
                            id = id,
                            params = params,
                            visible = overlays.isVisible(id),
                            wells = palette.wells(id),
                            onToggle = { onToggle(id) },
                            onCycleWell = { well -> onCycleWell(id, well) },
                            onParamsChange = onParamsChange,
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
    params: IndicatorParams,
    visible: Boolean,
    wells: List<OverlaySwatch>,
    onToggle: () -> Unit,
    onCycleWell: (Int) -> Unit,
    onParamsChange: (IndicatorParams) -> Unit,
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
                    text = id.settingsTitle(params),
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
        Spacer(Modifier.height(Spacing.sm))
        IndicatorParamControls(id = id, params = params, onParamsChange = onParamsChange)
    }
}

@Composable
private fun IndicatorParamControls(
    id: ChartIndicatorId,
    params: IndicatorParams,
    onParamsChange: (IndicatorParams) -> Unit,
) {
    val maIndex = id.maIndex()
    when {
        maIndex != null -> {
            val spec = params.movingAverages.getOrNull(maIndex) ?: return
            ParamChipRow(
                label = "Type",
                options = ChartParamChoices.maTypes.map { it.name },
                selected = spec.type.name,
                onSelect = { name ->
                    val type = MaType.entries.first { it.name == name }
                    onParamsChange(params.withMa(maIndex, MaSpec(type, spec.period)))
                },
                testTag = "maType-$maIndex",
            )
            Spacer(Modifier.height(Spacing.xs))
            ParamChipRow(
                label = "Period",
                options = mergeChoice(ChartParamChoices.maPeriods, spec.period).map { it.toString() },
                selected = spec.period.toString(),
                onSelect = { value ->
                    onParamsChange(params.withMa(maIndex, MaSpec(spec.type, value.toInt())))
                },
                testTag = "maPeriod-$maIndex",
            )
        }
        id == ChartIndicatorId.Rsi14 -> {
            ParamChipRow(
                label = "Length",
                options = mergeChoice(ChartParamChoices.rsiPeriods, params.rsiPeriod).map { it.toString() },
                selected = params.rsiPeriod.toString(),
                onSelect = { onParamsChange(params.copy(rsiPeriod = it.toInt())) },
                testTag = "rsiPeriod",
            )
        }
        id == ChartIndicatorId.Ichimoku -> {
            ParamChipRow(
                label = "Tenkan",
                options = mergeChoice(ChartParamChoices.ichimokuTenkan, params.ichimoku.tenkanPeriod)
                    .map { it.toString() },
                selected = params.ichimoku.tenkanPeriod.toString(),
                onSelect = { value ->
                    onParamsChange(params.withIchimoku { it.copy(tenkanPeriod = value.toInt()) })
                },
                testTag = "ichiTenkan",
            )
            Spacer(Modifier.height(Spacing.xs))
            ParamChipRow(
                label = "Kijun",
                options = mergeChoice(ChartParamChoices.ichimokuKijun, params.ichimoku.kijunPeriod)
                    .map { it.toString() },
                selected = params.ichimoku.kijunPeriod.toString(),
                onSelect = { value ->
                    onParamsChange(params.withIchimoku { it.copy(kijunPeriod = value.toInt()) })
                },
                testTag = "ichiKijun",
            )
            Spacer(Modifier.height(Spacing.xs))
            ParamChipRow(
                label = "Senkou",
                options = mergeChoice(ChartParamChoices.ichimokuSenkou, params.ichimoku.senkouBPeriod)
                    .map { it.toString() },
                selected = params.ichimoku.senkouBPeriod.toString(),
                onSelect = { value ->
                    onParamsChange(params.withIchimoku { it.copy(senkouBPeriod = value.toInt()) })
                },
                testTag = "ichiSenkou",
            )
        }
        id == ChartIndicatorId.VolumeRibbon -> {
            ParamChipRow(
                label = "Vol SMA",
                options = mergeChoice(ChartParamChoices.volumeSmaPeriods, params.volumeSmaPeriod)
                    .map { it.toString() },
                selected = params.volumeSmaPeriod.toString(),
                onSelect = { onParamsChange(params.copy(volumeSmaPeriod = it.toInt())) },
                testTag = "volSma",
            )
        }
        id == ChartIndicatorId.VolumeProfile -> {
            ParamChipRow(
                label = "Lookback",
                options = mergeChoice(ChartParamChoices.vpLookbacks, params.volumeProfileLookback)
                    .map { it.toString() },
                selected = params.volumeProfileLookback.toString(),
                onSelect = { onParamsChange(params.copy(volumeProfileLookback = it.toInt())) },
                testTag = "vpLookback",
            )
        }
    }
}

private fun mergeChoice(choices: List<Int>, current: Int): List<Int> =
    if (current in choices) choices else (choices + current).sorted()

@Composable
private fun ParamChipRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = ConfluenceType.Telemetry,
            color = ConfluenceColors.Dim,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .testTag(testTag),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            for (option in options) {
                val on = option == selected
                val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(
                            if (on) ConfluenceColors.Plasma.copy(alpha = 0.25f)
                            else ConfluenceColors.VoidElevated.copy(alpha = 0.55f),
                            shape,
                        )
                        .border(
                            1.dp,
                            if (on) ConfluenceColors.Plasma else ConfluenceColors.BorderSubtle,
                            shape,
                        )
                        .clickable { onSelect(option) }
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                        .testTag("$testTag-$option"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = ConfluenceType.Telemetry,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (on) ConfluenceColors.Ice else ConfluenceColors.Muted,
                    )
                }
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
            overlays = ChartOverlayVisibility.Defaults,
            palette = ChartIndicatorPalette.Defaults,
            params = IndicatorParams.DEFAULT,
            onToggle = {},
            onCycleWell = { _, _ -> },
            onParamsChange = {},
            onBack = {},
        )
    }
}
