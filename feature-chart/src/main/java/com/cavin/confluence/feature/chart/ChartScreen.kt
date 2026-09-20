package com.cavin.confluence.feature.chart

import android.app.Application
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.components.PlasmaAcrylicBox
import com.cavin.confluence.core.ui.components.PlasmaSpinner
import com.cavin.confluence.core.ui.components.PreviewAppShell
import com.cavin.confluence.core.ui.components.SnapshotBadge
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing
import com.cavin.confluence.core.ui.theme.confluenceScreenGutter
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.indicators.DayOneIndicators
import com.cavin.confluence.indicators.IndicatorParams
import com.cavin.confluence.indicators.SnapshotCutoff

/** V2 board TF chips (1m / 15m / 1H / 4H / 1D). Extra TFs appear if last-used is 5m/1W. */
internal val V2Timeframes = listOf(
    Timeframe.M1, Timeframe.M15, Timeframe.H1, Timeframe.H4, Timeframe.D1,
)

internal fun v2TfLabel(tf: Timeframe): String = when (tf) {
    Timeframe.M1 -> "1m"
    Timeframe.M5 -> "5m"
    Timeframe.M15 -> "15m"
    Timeframe.H1 -> "1H"
    Timeframe.H4 -> "4H"
    Timeframe.D1 -> "1D"
    Timeframe.W1 -> "1W"
}

internal fun visibleChartTimeframes(current: Timeframe): List<Timeframe> =
    if (current in V2Timeframes) V2Timeframes else V2Timeframes + current

/** Screenshot chrome as-of — packaged meta.json cutoff (fallback when assets aren't loaded). */
internal val ChartProofAsOf: String
    get() = MdSnapshotStore.bannerLabel

@Composable
fun ChartRoute(
    timeframe: String? = null,
    alertId: String? = null,
    onBack: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as Application
    val navTf = timeframe?.takeIf { it.isNotBlank() }?.let {
        runCatching { Timeframe.fromWire(it) }.getOrNull()
    }
    val vm: ChartViewModel = viewModel(factory = ChartViewModel.factory(app, navTf))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var settingsOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        ChartScreen(
            state = state,
            alertId = alertId?.takeIf { it.isNotBlank() },
            onSelectTf = vm::setTimeframe,
            onCrosshair = vm::onCrosshair,
            onOpenSettings = { settingsOpen = true },
            onRetry = vm::refresh,
        )
        if (settingsOpen) {
            Dialog(
                onDismissRequest = { settingsOpen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                ),
            ) {
                ChartIndicatorsSheet(
                    overlays = state.overlays,
                    palette = state.overlayPalette,
                    params = state.params,
                    onToggle = vm::toggleIndicator,
                    onCycleWell = vm::cycleIndicatorWell,
                    onParamsChange = vm::updateParams,
                    onBack = { settingsOpen = false },
                )
            }
        }
    }
}

@Composable
fun ChartScreen(
    state: ChartUiState,
    alertId: String? = null,
    onSelectTf: (Timeframe) -> Unit = {},
    onCrosshair: (Candle?) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRetry: () -> Unit = {},
    viewportSeed: ChartViewportSeed = ChartViewportSeed(),
) {
    val spacing = ConfluenceThemeAccess.spacing
    val tfs = visibleChartTimeframes(state.timeframe)
    val candle = state.crosshair ?: state.candles.lastOrNull()
    val lastPrice = candle?.close
    val asOfCaption = ChartHonesty.asOfCaption(
        state.snapshotBanner
            ?.substringAfterLast(" · ")
            ?.takeIf { it.contains("UTC") }
            ?: ChartHonesty.packagedUtcLabel,
    )
    val overlaySeriesKey = indicatorSeriesKey(state.timeframe.wire, state.candles, state.params)
    val indicators = remember(overlaySeriesKey, state.indicators) {
        when {
            state.indicators != null -> state.indicators
            state.candles.isEmpty() -> null
            else -> evaluateDayOneIndicators(
                state.candles,
                SnapshotCutoff.PACKAGED_2026_09_19,
                state.params,
            )
        }
    }
    val overlays = state.overlays.copy(volume = state.showVolume)
    val palette = state.overlayPalette

    Column(
        modifier = Modifier
            .fillMaxSize()
            .confluenceScreenGutter(
                top = ConfluenceLayout.screenTop,
                bottom = ConfluenceLayout.screenBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ChartHonesty.PAIR,
                style = ConfluenceTypography.titleLarge,
                color = ConfluenceColors.Text,
                modifier = Modifier.testTag("chartPair"),
            )
            Spacer(Modifier.padding(start = Spacing.sm))
            Text(
                text = lastPrice?.let { ChartHonesty.formatLastPrice(it) } ?: "—",
                style = ConfluenceMono.Hud,
                color = ConfluenceColors.Neg.takeIf {
                    candle != null && candle.close < candle.open
                } ?: ConfluenceColors.Pos,
                modifier = Modifier.testTag("chartLastPrice"),
            )
            Spacer(Modifier.weight(1f))
            SnapshotBadge(
                label = ChartHonesty.SNAPSHOT_BADGE,
                pulse = false,
                modifier = Modifier.testTag("snapshotBadge"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                for (tf in tfs) {
                    TfChip(
                        label = v2TfLabel(tf),
                        selected = tf == state.timeframe,
                        onClick = { onSelectTf(tf) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("chartSettingsGear"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Chart indicators",
                    tint = ConfluenceColors.Ice,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                asOfCaption,
                style = ConfluenceMono.Caption,
                color = ConfluenceColors.Ice,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chartHonestyAsOf"),
            )
            Text(
                ChartHonesty.MODE_CAPTION,
                style = ConfluenceMono.Caption,
                color = ConfluenceColors.Muted,
                modifier = Modifier.testTag("chartHonestyMode"),
            )
        }

        OverlayHonestyCaption(indicators = indicators, overlays = overlays)

        ChartStatusBanner(
            loading = state.loading,
            error = state.error,
            health = state.health?.status,
            healthNote = state.health?.note?.takeUnless { it.startsWith("Historical snapshot") },
            empty = !state.loading && state.error == null && state.candles.isEmpty(),
            onRetry = onRetry,
        )

        PlasmaAcrylicBox(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            glow = true,
            brackets = true,
            contentPadding = spacing.xs,
        ) {
            Box(Modifier.fillMaxSize()) {
                when {
                    state.loading && state.candles.isEmpty() ->
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PlasmaSpinner()
                            Spacer(Modifier.height(spacing.sm))
                            Text("Loading candles…", color = ConfluenceColors.Dim)
                        }
                    state.candles.isNotEmpty() -> {
                        CandleChart(
                            candles = state.candles,
                            showVolume = overlays.volume,
                            seriesKey = "${state.venue.wire}:${state.timeframe.wire}",
                            modifier = Modifier.fillMaxSize(),
                            onCrosshairCandle = onCrosshair,
                            viewportSeed = viewportSeed,
                            indicators = indicators,
                            overlays = overlays,
                            palette = palette,
                        )
                        OverlayLegendRow(
                            overlays = overlays,
                            palette = palette,
                            params = state.params,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = Spacing.xs, top = Spacing.xs),
                        )
                    }
                    else -> Text(
                        "No series for ${state.timeframe.wire}",
                        modifier = Modifier.align(Alignment.Center),
                        color = ConfluenceColors.Dim,
                    )
                }
            }
        }

        if (!alertId.isNullOrBlank()) {
            Text(
                "Opened from alert · $alertId",
                style = ConfluenceTypography.labelSmall,
                color = ConfluenceColors.Dim,
            )
        }
    }
}

@Composable
private fun TfChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val border = if (selected) ConfluenceColors.Plasma else ConfluenceColors.BorderSubtle
    val bg = if (selected) {
        ConfluenceColors.Plasma.copy(alpha = 0.25f)
    } else {
        ConfluenceColors.VoidElevated.copy(alpha = 0.55f)
    }
    val fg = if (selected) ConfluenceColors.Ice else ConfluenceColors.Muted
    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .testTag("tfChip-$label")
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
        )
    }
}

@Composable
private fun OverlayHonestyCaption(
    indicators: DayOneIndicators?,
    overlays: ChartOverlayVisibility,
) {
    if (indicators == null) return
    val parts = buildList {
        val fence = indicators.fence
        if (fence.droppedOvershootCount > 0) {
            add("Fence dropped ${fence.droppedOvershootCount} bars after cutoff")
        }
        if ((overlays.ma0 || overlays.ma1 || overlays.ma2 || overlays.ma3) &&
            indicators.sma200WarmupIncomplete &&
            indicators.params.movingAverages.any { it.period >= 200 }
        ) {
            add("SMA200 undefined (${indicators.bars.size} < 200)")
        }
    }
    if (parts.isEmpty()) return
    Text(
        parts.joinToString(" · "),
        style = ConfluenceMono.Caption,
        color = ConfluenceColors.Dim,
        modifier = Modifier.testTag("chartFenceCaption"),
    )
}

@Composable
private fun ChartStatusBanner(
    loading: Boolean,
    error: String?,
    health: HealthStatus?,
    healthNote: String?,
    empty: Boolean,
    onRetry: () -> Unit,
) {
    val (title, body) = when {
        error != null -> "Error" to error
        empty -> "Empty" to "No candle data"
        health == HealthStatus.STALE -> "Stale" to (healthNote ?: "Stale feed")
        health == HealthStatus.DISCONNECTED -> "Offline" to (healthNote ?: "Disconnected")
        health == HealthStatus.DEGRADED -> "Degraded" to (healthNote ?: "Degraded feed")
        loading -> "Loading" to "Fetching series…"
        else -> return
    }
    GlassCard(contentPadding = ConfluenceDimens.glassPaddingTight) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = ConfluenceTypography.labelSmall, color = ConfluenceColors.Dim)
                Text(body, style = ConfluenceTypography.bodyMedium, color = ConfluenceColors.Text)
            }
            if (error != null || empty) {
                AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
            }
        }
    }
}

internal fun chartProofUiState(
    timeframe: Timeframe,
    count: Int = 80,
    showVolume: Boolean = true,
    crosshair: Candle? = null,
    overlays: ChartOverlayVisibility = ChartOverlayVisibility.Defaults,
    palette: ChartIndicatorPalette = ChartIndicatorPalette.Defaults,
    params: IndicatorParams = IndicatorParams.DEFAULT,
): ChartUiState {
    val candles = FakeFixtures.sampleClosedCandles(count = count, timeframe = timeframe)
    val indicators = evaluateDayOneIndicators(candles, SnapshotCutoff.PACKAGED_2026_09_19, params)
    val visibility = overlays.copy(volume = showVolume)
    return ChartUiState(
        loading = false,
        candles = candles,
        timeframe = timeframe,
        snapshotBanner = ChartProofAsOf,
        showVolume = showVolume,
        overlays = visibility,
        overlayPalette = palette,
        params = params,
        indicators = indicators,
        crosshair = crosshair ?: candles.lastOrNull(),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
private fun ChartPreview() {
    ConfluenceTheme {
        PreviewAppShell(selectedId = "chart") {
            ChartScreen(state = ChartUiState(loading = false, candles = emptyList()))
        }
    }
}

@Preview(name = "zoomed out + volume", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartPreviewZoomedOut() {
    val state = remember { chartProofUiState(Timeframe.W1, count = 120) }
    ConfluenceTheme {
        PreviewAppShell(selectedId = "chart") {
            ChartScreen(
                state = state,
                viewportSeed = ChartViewportSeed(candleWidth = ConfluenceDimens.chartMinCandleWidth),
            )
        }
    }
}

@Preview(name = "zoomed in bull+bear", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartPreviewZoomedIn() {
    val state = remember { chartProofUiState(Timeframe.H1, count = 48) }
    ConfluenceTheme {
        PreviewAppShell(selectedId = "chart") {
            ChartScreen(
                state = state,
                viewportSeed = ChartViewportSeed(candleWidth = ConfluenceDimens.chartMaxCandleWidth, visibleCount = 12),
            )
        }
    }
}

@Preview(name = "crosshair on", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartPreviewCrosshair() {
    val state = remember { chartProofUiState(Timeframe.H1, count = 48) }
    val idx = state.candles.lastIndex - 4
    ConfluenceTheme {
        PreviewAppShell(selectedId = "chart") {
            ChartScreen(
                state = state.copy(crosshair = state.candles.getOrNull(idx)),
                viewportSeed = ChartViewportSeed(crosshairIndex = idx),
            )
        }
    }
}

@Preview(name = "TF 1D switched", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartPreviewTfSwitched() {
    val state = remember { chartProofUiState(Timeframe.D1, count = 64) }
    ConfluenceTheme {
        PreviewAppShell(selectedId = "chart") {
            ChartScreen(state = state)
        }
    }
}

@Preview(name = "V2 overlay-first 1H", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartPreviewOverlays() {
    val state = remember { chartProofUiState(Timeframe.H1, count = 220) }
    ConfluenceTheme {
        PreviewAppShell(selectedId = "chart") {
            ChartScreen(
                state = state,
                viewportSeed = ChartViewportSeed(visibleCount = 48),
            )
        }
    }
}

@Preview(name = "V2 settings", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ChartPreviewIndicatorsSheet() {
    ConfluenceTheme {
        Box(Modifier.fillMaxSize()) {
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
}
