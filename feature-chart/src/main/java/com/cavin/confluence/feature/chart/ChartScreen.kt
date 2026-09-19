package com.cavin.confluence.feature.chart

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.ConfluenceBrandLockup
import com.cavin.confluence.core.ui.components.Disclaimer
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.components.HudOhlc
import com.cavin.confluence.core.ui.components.HudStrip
import com.cavin.confluence.core.ui.components.PlasmaAcrylicBox
import com.cavin.confluence.core.ui.components.PlasmaSpinner
import com.cavin.confluence.core.ui.components.PreviewAppShell
import com.cavin.confluence.core.ui.components.SegmentedControl
import com.cavin.confluence.core.ui.components.SnapshotBadge
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.confluenceScreenGutter
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import com.cavin.confluence.indicators.DayOneIndicators
import com.cavin.confluence.indicators.SnapshotCutoff

internal val DayOneTimeframes = listOf(
    Timeframe.M1, Timeframe.M5, Timeframe.M15,
    Timeframe.H1, Timeframe.H4, Timeframe.D1, Timeframe.W1,
)

internal val TfLabels = listOf("1m", "5m", "15m", "1h", "4h", "1D", "1W")

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

    ChartScreen(
        state = state,
        alertId = alertId?.takeIf { it.isNotBlank() },
        onSelectTf = vm::setTimeframe,
        onCrosshair = vm::onCrosshair,
        onToggleOverlay = vm::toggleOverlay,
        onRetry = vm::refresh,
    )
}

@Composable
fun ChartScreen(
    state: ChartUiState,
    alertId: String? = null,
    onSelectTf: (Timeframe) -> Unit = {},
    onCrosshair: (Candle?) -> Unit = {},
    onToggleOverlay: (ChartOverlayFamily) -> Unit = {},
    onRetry: () -> Unit = {},
    viewportSeed: ChartViewportSeed = ChartViewportSeed(),
) {
    val spacing = ConfluenceThemeAccess.spacing
    val selectedIndex = DayOneTimeframes.indexOf(state.timeframe).coerceAtLeast(0)
    val candle = state.crosshair ?: state.candles.lastOrNull()
    val tfLabel = TfLabels.getOrElse(selectedIndex) { state.timeframe.wire }
    val banner = state.snapshotBanner
        ?: state.health?.note?.takeIf { it.startsWith("Historical snapshot") }
        ?: ChartProofAsOf
    val overlaySeriesKey = indicatorSeriesKey(state.timeframe.wire, state.candles)
    val indicators = remember(overlaySeriesKey, state.indicators) {
        when {
            state.indicators != null -> state.indicators
            state.candles.isEmpty() -> null
            else -> evaluateDayOneIndicators(state.candles, SnapshotCutoff.PACKAGED_2026_09_19)
        }
    }
    val overlays = state.overlays.copy(volume = state.showVolume)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .confluenceScreenGutter(
                top = ConfluenceLayout.screenTop,
                bottom = ConfluenceLayout.screenBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.stackGap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
            ConfluenceBrandLockup(
                title = "BTC / USDT",
                subtitle = "Ice + Bright Blue · $tfLabel",
            )

            SegmentedControl(
                options = TfLabels,
                selectedIndex = selectedIndex,
                onSelect = { idx -> onSelectTf(DayOneTimeframes[idx]) },
            )

            OverlayFamilyChipRow(
                overlays = overlays,
                onToggle = onToggleOverlay,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
            ) {
                SnapshotBadge(label = "Snapshot", pulse = false)
                Text(
                    banner,
                    style = ConfluenceMono.Caption,
                    color = ConfluenceColors.Ice,
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
        }

        HudStrip(
            ohlc = candle?.let {
                HudOhlc(open = it.open, high = it.high, low = it.low, close = it.close)
            },
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
                    state.candles.isNotEmpty() -> CandleChart(
                        candles = state.candles,
                        showVolume = overlays.volume,
                        seriesKey = "${state.venue.wire}:${state.timeframe.wire}",
                        modifier = Modifier.fillMaxSize(),
                        onCrosshairCandle = onCrosshair,
                        viewportSeed = viewportSeed,
                        indicators = indicators,
                        overlays = overlays,
                    )
                    else -> Text(
                        "No series for ${state.timeframe.wire}",
                        modifier = Modifier.align(Alignment.Center),
                        color = ConfluenceColors.Dim,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.chromeGap)) {
            if (!alertId.isNullOrBlank()) {
                Text(
                    "Opened from alert · $alertId",
                    style = ConfluenceTypography.labelSmall,
                    color = ConfluenceColors.Dim,
                )
            }
            Disclaimer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun OverlayFamilyChipRow(
    overlays: ChartOverlayVisibility,
    onToggle: (ChartOverlayFamily) -> Unit,
) {
    val families = ChartOverlayFamily.entries
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overlayFamilyChips"),
        horizontalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
    ) {
        for (family in families) {
            OverlayFamilyChip(
                family = family,
                selected = overlays.isVisible(family),
                onClick = { onToggle(family) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OverlayFamilyChip(
    family: ChartOverlayFamily,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
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
            .testTag("overlayChip-${family.chipLabel()}")
            .padding(horizontal = ConfluenceThemeAccess.spacing.xs, vertical = ConfluenceThemeAccess.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = family.chipLabel(),
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
        if (overlays.movingAverages && indicators.sma200WarmupIncomplete) {
            add("SMA200 undefined (${indicators.bars.size} < 200)")
        }
    }
    if (parts.isEmpty()) return
    Text(
        parts.joinToString(" · "),
        style = ConfluenceMono.Caption,
        color = ConfluenceColors.Dim,
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
    overlays: ChartOverlayVisibility = ChartOverlayVisibility.AllOn,
): ChartUiState {
    val candles = FakeFixtures.sampleClosedCandles(count = count, timeframe = timeframe)
    val indicators = evaluateDayOneIndicators(candles, SnapshotCutoff.PACKAGED_2026_09_19)
    val visibility = overlays.copy(volume = showVolume)
    return ChartUiState(
        loading = false,
        candles = candles,
        timeframe = timeframe,
        snapshotBanner = ChartProofAsOf,
        showVolume = showVolume,
        overlays = visibility,
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

@Preview(name = "overlays five autos", showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
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
