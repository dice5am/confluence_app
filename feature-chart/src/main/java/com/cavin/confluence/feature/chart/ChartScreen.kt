package com.cavin.confluence.feature.chart

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.data.model.Candle
import com.cavin.confluence.data.model.Timeframe
import java.util.Locale

/**
 * MOB-2.1 Chart screen — Canvas OHLC on fixtures.
 * TF chips / volume pane / banners = later MOB-2.x.
 */
@Composable
fun ChartRoute(
    timeframe: String? = null,
    alertId: String? = null,
    onBack: () -> Unit = {},
) {
    val tf = timeframe?.takeIf { it.isNotBlank() }?.let {
        runCatching { Timeframe.fromWire(it) }.getOrNull()
    } ?: Timeframe.H1

    val vm: ChartViewModel = viewModel(factory = ChartViewModel.factory(tf))
    val state by vm.uiState.collectAsStateWithLifecycle()

    ChartScreen(
        state = state,
        alertId = alertId?.takeIf { it.isNotBlank() },
        onCrosshair = vm::onCrosshair,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    state: ChartUiState,
    alertId: String? = null,
    onCrosshair: (Candle?) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BTC/USDT · ${state.timeframe.wire}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
            OhlcReadout(candle = state.crosshair ?: state.candles.lastOrNull())
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(ConfluenceColors.Surface),
            ) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> CandleChart(
                        candles = state.candles,
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
                text = "Pinch zoom · drag pan · long-press crosshair · fixtures only (MOB-2.1)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = "Insight only — never executes trades",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
