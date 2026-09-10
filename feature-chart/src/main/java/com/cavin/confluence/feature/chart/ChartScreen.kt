package com.cavin.confluence.feature.chart

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.Disclaimer
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.components.HudOhlc
import com.cavin.confluence.core.ui.components.HudStrip
import com.cavin.confluence.core.ui.components.SegmentedControl
import com.cavin.confluence.core.ui.components.SnapshotBadge
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe

private val DayOneTimeframes = listOf(
    Timeframe.M1, Timeframe.M5, Timeframe.M15,
    Timeframe.H1, Timeframe.H4, Timeframe.D1, Timeframe.W1,
)

private val TfLabels = listOf("1m", "5m", "15m", "1h", "4h", "1D", "1W")

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
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    state: ChartUiState,
    alertId: String? = null,
    onSelectTf: (Timeframe) -> Unit = {},
    onCrosshair: (Candle?) -> Unit = {},
    onToggleVolume: () -> Unit = {},
    onRetry: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val spacing = ConfluenceThemeAccess.spacing
    val selectedIndex = DayOneTimeframes.indexOf(state.timeframe).coerceAtLeast(0)
    val candle = state.crosshair ?: state.candles.lastOrNull()
    val tfLabel = TfLabels.getOrElse(selectedIndex) { state.timeframe.wire }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "BTC / USDT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ConfluenceColors.TextPrimary,
                        )
                        Text(
                            "Insight chart · $tfLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.Slate,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConfluenceColors.Void,
                ),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = spacing.sm),
                    ) {
                        Text(
                            "Vol",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.TextSecondary,
                        )
                        Switch(
                            checked = state.showVolume,
                            onCheckedChange = { onToggleVolume() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ConfluenceColors.CyberCyan,
                                checkedTrackColor = ConfluenceColors.CyberCyan.copy(alpha = 0.35f),
                                uncheckedThumbColor = ConfluenceColors.Slate,
                                uncheckedTrackColor = ConfluenceColors.VoidElevated,
                            ),
                        )
                    }
                },
            )
        },
        containerColor = ConfluenceColors.Void,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            SegmentedControl(
                options = TfLabels,
                selectedIndex = selectedIndex,
                onSelect = { idx -> onSelectTf(DayOneTimeframes[idx]) },
            )

            val banner = state.snapshotBanner
                ?: state.health?.note?.takeIf { it.startsWith("Historical snapshot") }
            if (banner != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    SnapshotBadge(label = "Snapshot")
                    Text(
                        banner,
                        style = MaterialTheme.typography.labelSmall,
                        color = ConfluenceColors.Slate,
                    )
                }
            }

            ChartStatusBanner(
                loading = state.loading,
                error = state.error,
                health = state.health?.status,
                healthNote = state.health?.note?.takeUnless { it.startsWith("Historical snapshot") },
                empty = !state.loading && state.error == null && state.candles.isEmpty(),
                onRetry = onRetry,
            )

            HudStrip(
                ohlc = candle?.let {
                    HudOhlc(open = it.open, high = it.high, low = it.low, close = it.close)
                },
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ConfluenceDimens.glassCorner))
                    .background(ConfluenceColors.Void),
            ) {
                when {
                    state.loading && state.candles.isEmpty() ->
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(color = ConfluenceColors.CyberCyan)
                            Spacer(Modifier.height(spacing.sm))
                            Text("Loading candles…", color = ConfluenceColors.Slate)
                        }
                    state.candles.isNotEmpty() -> CandleChart(
                        candles = state.candles,
                        showVolume = state.showVolume,
                        seriesKey = "${state.venue.wire}:${state.timeframe.wire}",
                        modifier = Modifier.fillMaxSize(),
                        onCrosshairCandle = onCrosshair,
                    )
                    else -> Text(
                        "No series for ${state.timeframe.wire}",
                        modifier = Modifier.align(Alignment.Center),
                        color = ConfluenceColors.Slate,
                    )
                }
            }

            if (!alertId.isNullOrBlank()) {
                Text(
                    "Opened from alert · $alertId",
                    style = MaterialTheme.typography.labelSmall,
                    color = ConfluenceColors.Slate,
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
                Text(title, style = MaterialTheme.typography.labelSmall, color = ConfluenceColors.Slate)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = ConfluenceColors.TextPrimary)
            }
            if (error != null || empty) {
                AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07090E, widthDp = 400, heightDp = 780)
@Composable
private fun ChartPreview() {
    ConfluenceTheme {
        ChartScreen(state = ChartUiState(loading = false, candles = emptyList()))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07090E, widthDp = 400, heightDp = 780)
@Composable
private fun ChartPreviewPopulated() {
    ConfluenceTheme {
        ChartScreen(
            state = ChartUiState(
                loading = false,
                candles = FakeFixtures.sampleClosedCandles(count = 80, timeframe = Timeframe.W1),
                timeframe = Timeframe.W1,
                snapshotBanner = "Historical snapshot · as of 2025-01-01T00:00:00Z",
                showVolume = true,
            ),
        )
    }
}
