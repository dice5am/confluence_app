package com.cavin.confluence.feature.chart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.data.model.Timeframe

/**
 * Chart feature stub (MOB Phase 1).
 * NO Canvas candle engine — that is MOB-2.1+.
 */
@Composable
fun ChartRoute(
    timeframe: String? = null,
    alertId: String? = null,
    onBack: () -> Unit = {},
) {
    ChartScreen(
        timeframeWire = timeframe?.takeIf { it.isNotBlank() },
        alertId = alertId?.takeIf { it.isNotBlank() },
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    timeframeWire: String? = null,
    alertId: String? = null,
    onBack: () -> Unit = {},
) {
    val tfLabel = timeframeWire
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Timeframe.fromWire(it).wire }.getOrDefault(it) }
        ?: "1h (default later)"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chart") },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Chart — Phase 2",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Candle Canvas engine not started (MOB-2.1).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "TF stub: $tfLabel",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!alertId.isNullOrBlank()) {
                Text(
                    text = "alertId stub: $alertId",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Insight view only — no trade execution.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun ChartPreview() {
    ConfluenceTheme {
        ChartScreen(timeframeWire = "1h", alertId = "alert-demo-1")
    }
}
