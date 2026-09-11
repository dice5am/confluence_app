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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.AppChip
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
import com.cavin.confluence.core.ui.theme.confluenceScreenGutter
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe
import com.cavin.confluence.data.snapshot.MdSnapshotStore

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
        onToggleVolume = vm::toggleVolume,
        onRetry = vm::refresh,
    )
}

@Composable
fun ChartScreen(
    state: ChartUiState,
    alertId: String? = null,
    onSelectTf: (Timeframe) -> Unit = {},
    onCrosshair: (Candle?) -> Unit = {},
    onToggleVolume: () -> Unit = {},
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "BTC / USDT",
                        style = ConfluenceTypography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = ConfluenceColors.Text,
                    )
                    Text(
                        "Ice + Bright Blue · $tfLabel",
                        style = ConfluenceTypography.labelSmall,
                        color = ConfluenceColors.Muted,
                    )
                }
                AppChip(
                    label = if (state.showVolume) "Vol on" else "Vol off",
                    selected = state.showVolume,
                    onClick = onToggleVolume,
                )
            }

            SegmentedControl(
                options = TfLabels,
                selectedIndex = selectedIndex,
                onSelect = { idx -> onSelectTf(DayOneTimeframes[idx]) },
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
                        showVolume = state.showVolume,
                        seriesKey = "${state.venue.wire}:${state.timeframe.wire}",
                        modifier = Modifier.fillMaxSize(),
                        onCrosshairCandle = onCrosshair,
                        viewportSeed = viewportSeed,
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
): ChartUiState {
    val candles = FakeFixtures.sampleClosedCandles(count = count, timeframe = timeframe)
    return ChartUiState(
        loading = false,
        candles = candles,
        timeframe = timeframe,
        snapshotBanner = ChartProofAsOf,
        showVolume = showVolume,
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
