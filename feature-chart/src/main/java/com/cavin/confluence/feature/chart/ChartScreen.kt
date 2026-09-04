package com.cavin.confluence.feature.chart

import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.AppCard
import com.cavin.confluence.core.ui.components.AppCardGlow
import com.cavin.confluence.core.ui.components.AppChip
import com.cavin.confluence.core.ui.components.AppChipAccent
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
                            "Insight chart",
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
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
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

            ChartStatusBanner(
                loading = state.loading,
                error = state.error,
                health = state.health?.status,
                healthNote = state.health?.note,
                empty = !state.loading && state.error == null && state.candles.isEmpty(),
                usingFixtures = state.usingFixtures,
                onRetry = onRetry,
            )

            AppCard(glow = AppCardGlow.Blue, contentPadding = spacing.md) {
                OhlcReadout(candle = state.crosshair ?: state.candles.lastOrNull())
                if (state.rawCandleCount > 0) {
                    Spacer(Modifier.height(spacing.xs))
                    Text(
                        text = buildString {
                            append("drawn ${state.candles.size}/${state.rawCandleCount}")
                            state.lastTfSwitchMs?.let { append(" · TF ${it}ms") }
                            state.lastLiveAppendMs?.let { append(" · live ${it}ms") }
                            if (state.usingFixtures) append(" · fixtures")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ConfluenceColors.OnSurfaceMuted,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ConfluenceColors.Surface),
            ) {
                when {
                    state.loading && state.candles.isEmpty() ->
                        CircularProgressIndicator(
                            Modifier.align(Alignment.Center),
                            color = ConfluenceColors.Primary,
                        )
                    state.candles.isNotEmpty() -> CandleChart(
                        candles = state.candles,
                        showVolume = state.showVolume,
                        seriesKey = "${state.venue.wire}:${state.timeframe.wire}",
                        modifier = Modifier.fillMaxSize(),
                        onCrosshairCandle = onCrosshair,
                    )
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
private fun ChartStatusBanner(
    loading: Boolean,
    error: String?,
    health: HealthStatus?,
    healthNote: String?,
    empty: Boolean,
    usingFixtures: Boolean,
    onRetry: () -> Unit,
) {
    val (glow, msg) = when {
        error != null -> AppCardGlow.Orange to "Error: $error"
        empty -> AppCardGlow.Orange to "No candle data"
        usingFixtures -> AppCardGlow.Orange to (
            healthNote?.takeIf { it.isNotBlank() }
                ?: "Fixture / snapshot mode"
            )
        health == HealthStatus.STALE ->
            AppCardGlow.Orange to (healthNote ?: "Stale feed")
        health == HealthStatus.DISCONNECTED ->
            AppCardGlow.Orange to (healthNote ?: "Disconnected")
        health == HealthStatus.DEGRADED ->
            AppCardGlow.Blue to (healthNote ?: "Degraded feed")
        loading -> AppCardGlow.Blue to "Loading…"
        else -> return
    }
    AppCard(glow = glow, contentPadding = 12.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                msg,
                style = MaterialTheme.typography.labelLarge,
                color = ConfluenceColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            if (error != null || empty) {
                AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun OhlcReadout(candle: Candle?) {
    if (candle == null) {
        Text(
            "OHLC —",
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
        Text("O ${fmt(candle.open)}", color = color, fontWeight = FontWeight.SemiBold)
        Text("H ${fmt(candle.high)}", color = color, fontWeight = FontWeight.SemiBold)
        Text("L ${fmt(candle.low)}", color = color, fontWeight = FontWeight.SemiBold)
        Text("C ${fmt(candle.close)}", color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF070B12)
@Composable
private fun ChartPreview() {
    ConfluenceTheme {
        ChartScreen(state = ChartUiState(loading = false, candles = emptyList()))
    }
}
