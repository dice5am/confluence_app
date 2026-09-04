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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
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
    // Deep-link TF wins when present; else MOB-2.3 last-used / first-open 1h inside VM.
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BTC/USDT") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    TextButton(onClick = onToggleVolume) {
                        Text(if (state.showVolume) "Vol on" else "Vol off")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            // MOB-2.2 TF chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DayOneTimeframes.forEach { tf ->
                    FilterChip(
                        selected = state.timeframe == tf,
                        onClick = { onSelectTf(tf) },
                        label = { Text(tf.wire) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // MOB-2.8 banners
            ChartStatusBanner(
                loading = state.loading,
                error = state.error,
                health = state.health?.status,
                empty = !state.loading && state.error == null && state.candles.isEmpty(),
                onRetry = onRetry,
            )

            OhlcReadout(candle = state.crosshair ?: state.candles.lastOrNull())
            if (state.rawCandleCount > 0) {
                Text(
                    text = buildString {
                        append("drawn ${state.candles.size}/${state.rawCandleCount}")
                        state.lastTfSwitchMs?.let { append(" · TF switch ${it}ms") }
                        state.lastLiveAppendMs?.let { append(" · live ${it}ms") }
                        if (state.usingFixtures) append(" · fixtures")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ConfluenceColors.Surface),
            ) {
                when {
                    state.loading && state.candles.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Text(
                text = "Insight only — never executes trades",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun ChartStatusBanner(
    loading: Boolean,
    error: String?,
    health: HealthStatus?,
    empty: Boolean,
    onRetry: () -> Unit,
) {
    val (bg, msg) = when {
        error != null -> ConfluenceColors.HealthDisconnected.copy(alpha = 0.25f) to "Error: $error"
        empty -> ConfluenceColors.SurfaceVariant to "No candle data"
        health == HealthStatus.STALE ->
            ConfluenceColors.HealthStale.copy(alpha = 0.25f) to "Stale feed — showing last fixtures"
        health == HealthStatus.DISCONNECTED ->
            ConfluenceColors.HealthDisconnected.copy(alpha = 0.25f) to "Disconnected — O1 cache path later (MOB-4.6)"
        health == HealthStatus.DEGRADED ->
            ConfluenceColors.HealthDegraded.copy(alpha = 0.25f) to "Degraded feed"
        loading -> ConfluenceColors.SurfaceVariant to "Loading…"
        else -> return
    }
    Surface(
        color = bg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(msg, style = MaterialTheme.typography.labelMedium)
            if (error != null || empty) {
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun OhlcReadout(candle: Candle?) {
    if (candle == null) {
        Text("OHLC —", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val bull = candle.close >= candle.open
    val color = if (bull) ConfluenceColors.Bull else ConfluenceColors.Bear
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        fun fmt(v: Double) = String.format(Locale.US, "%.2f", v)
        Text("O ${fmt(candle.open)}", color = color, fontWeight = FontWeight.Medium)
        Text("H ${fmt(candle.high)}", color = color, fontWeight = FontWeight.Medium)
        Text("L ${fmt(candle.low)}", color = color, fontWeight = FontWeight.Medium)
        Text("C ${fmt(candle.close)}", color = color, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ChartPreview() {
    ConfluenceTheme {
        ChartScreen(state = ChartUiState(loading = false, candles = emptyList()))
    }
}
