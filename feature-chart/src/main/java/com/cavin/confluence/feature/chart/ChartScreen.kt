package com.cavin.confluence.feature.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.AppCard
import com.cavin.confluence.core.ui.components.AppCardGlow
import com.cavin.confluence.core.ui.components.AppChip
import com.cavin.confluence.core.ui.components.AppChipAccent
import com.cavin.confluence.core.ui.components.AppSectionLabel
import com.cavin.confluence.core.ui.components.AppTextButton
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.HealthStatus
import com.cavin.confluence.data.model.Timeframe
import java.util.Locale

private val DayOneTimeframes = listOf(
    Timeframe.M1, Timeframe.M5, Timeframe.M15,
    Timeframe.H1, Timeframe.H4, Timeframe.D1, Timeframe.W1,
)

@Composable
fun ChartRoute(
    timeframe: String? = null,
    alertId: String? = null,
    onBack: () -> Unit = {},
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
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
    val chartShape = RoundedCornerShape(18.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "BTC / USDT",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Insight chart · ${state.timeframe.wire}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.OnSurfaceMuted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConfluenceColors.Background,
                ),
                actions = {
                    AppTextButton(onClick = onToggleVolume) {
                        Text(if (state.showVolume) "Vol on" else "Vol off")
                    }
                },
            )
        },
        containerColor = ConfluenceColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Column {
                AppSectionLabel("Timeframe", accent = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    DayOneTimeframes.forEachIndexed { index, tf ->
                        AppChip(
                            label = tf.wire,
                            selected = state.timeframe == tf,
                            onClick = { onSelectTf(tf) },
                            accent = if (index % 2 == 0) AppChipAccent.Blue else AppChipAccent.Orange,
                        )
                    }
                }
            }

            state.snapshotBanner?.let { note ->
                StatusBannerCard(
                    title = "Snapshot",
                    body = note,
                    glow = AppCardGlow.Orange,
                )
            }

            ChartStatusBanner(
                loading = state.loading,
                error = state.error,
                health = state.health?.status,
                healthNote = state.health?.note,
                empty = !state.loading && state.error == null && state.candles.isEmpty(),
                usingFixtures = state.usingFixtures && state.snapshotBanner == null,
                onRetry = onRetry,
            )

            Column {
                AppSectionLabel("OHLC")
                AppCard(glow = AppCardGlow.Blue, contentPadding = spacing.md) {
                    OhlcReadout(candle = state.crosshair ?: state.candles.lastOrNull())
                    if (state.rawCandleCount > 0) {
                        Spacer(Modifier.height(spacing.sm))
                        Text(
                            text = buildString {
                                append("drawn ${state.candles.size}/${state.rawCandleCount}")
                                state.lastTfSwitchMs?.let { append(" · TF ${it}ms") }
                                state.lastLiveAppendMs?.let { append(" · live ${it}ms") }
                                when {
                                    state.snapshotBanner != null -> append(" · snapshot")
                                    state.usingFixtures -> append(" · fixtures")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.OnSurfaceMuted,
                        )
                    }
                }
            }

            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppSectionLabel("Chart · Y price", accent = true)
                    Text(
                        "X · time · ${state.timeframe.wire}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConfluenceColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(chartShape)
                        .background(ConfluenceColors.Surface)
                        .border(1.dp, ConfluenceColors.Primary.copy(alpha = 0.35f), chartShape)
                        .padding(3.dp),
                ) {
                    when {
                        state.loading && state.candles.isEmpty() ->
                            Column(
                                Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                CircularProgressIndicator(color = ConfluenceColors.Primary)
                                Spacer(Modifier.height(spacing.sm))
                                Text(
                                    "Loading candles…",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ConfluenceColors.OnSurfaceMuted,
                                )
                            }
                        state.candles.isNotEmpty() -> CandleChart(
                            candles = state.candles,
                            showVolume = state.showVolume,
                            seriesKey = "${state.venue.wire}:${state.timeframe.wire}",
                            modifier = Modifier.fillMaxSize(),
                            onCrosshairCandle = onCrosshair,
                        )
                        else -> Column(
                            Modifier
                                .align(Alignment.Center)
                                .padding(spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "No series",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ConfluenceColors.OnSurface,
                            )
                            Spacer(Modifier.height(spacing.xs))
                            Text(
                                "Nothing for ${state.timeframe.wire} yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ConfluenceColors.OnSurfaceMuted,
                            )
                        }
                    }
                }
            }

            if (!alertId.isNullOrBlank()) {
                Text(
                    text = "alertId deep-link stub: $alertId",
                    style = MaterialTheme.typography.labelSmall,
                    color = ConfluenceColors.OnSurfaceMuted,
                )
            }
            Text(
                text = "Insight only — never executes trades",
                style = MaterialTheme.typography.labelSmall,
                color = ConfluenceColors.OnSurfaceMuted,
            )
        }
    }
}

@Composable
private fun StatusBannerCard(
    title: String,
    body: String,
    glow: AppCardGlow,
    trailing: (@Composable () -> Unit)? = null,
) {
    AppCard(glow = glow, contentPadding = 12.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ConfluenceColors.OnSurfaceMuted,
                    letterSpacing = 0.6.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ConfluenceColors.OnSurface,
                )
            }
            trailing?.invoke()
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
    usingFixtures: Boolean,
    onRetry: () -> Unit,
) {
    val triple = when {
        error != null -> Triple(AppCardGlow.Orange, "Error", error)
        empty -> Triple(AppCardGlow.Orange, "Empty", "No candle data")
        usingFixtures -> Triple(
            AppCardGlow.Orange,
            "Mode",
            healthNote?.takeIf { it.isNotBlank() } ?: "Fixture / snapshot mode",
        )
        health == HealthStatus.STALE ->
            Triple(AppCardGlow.Orange, "Stale", healthNote ?: "Stale feed")
        health == HealthStatus.DISCONNECTED ->
            Triple(AppCardGlow.Orange, "Offline", healthNote ?: "Disconnected")
        health == HealthStatus.DEGRADED ->
            Triple(AppCardGlow.Blue, "Degraded", healthNote ?: "Degraded feed")
        loading -> Triple(AppCardGlow.Blue, "Loading", "Fetching series…")
        else -> return
    }
    StatusBannerCard(
        title = triple.second,
        body = triple.third,
        glow = triple.first,
        trailing = if (error != null || empty) {
            {
                AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) {
                    Text("Retry")
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun OhlcReadout(candle: Candle?) {
    if (candle == null) {
        Text(
            "Crosshair or last bar —",
            style = MaterialTheme.typography.titleMedium,
            color = ConfluenceColors.OnSurfaceMuted,
        )
        return
    }
    val bull = candle.close >= candle.open
    val color = if (bull) ConfluenceColors.Bull else ConfluenceColors.Bear
    fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OhlcCell("Open", fmt(candle.open), color)
        OhlcCell("High", fmt(candle.high), color)
        OhlcCell("Low", fmt(candle.low), color)
        OhlcCell("Close", fmt(candle.close), color)
    }
}

@Composable
private fun OhlcCell(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = ConfluenceColors.OnSurfaceMuted,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B12)
@Composable
private fun ChartPreview() {
    ConfluenceTheme {
        ChartScreen(state = ChartUiState(loading = false, candles = emptyList()))
    }
}
