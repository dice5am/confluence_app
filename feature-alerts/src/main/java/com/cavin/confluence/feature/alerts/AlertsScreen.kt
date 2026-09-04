package com.cavin.confluence.feature.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

/**
 * Alerts inbox stub (MOB Phase 1). Full inbox is MOB-4.1.
 * Advisory / confluence insight only — no order actions.
 */
@Composable
fun AlertsRoute(
    onOpenAlert: (alertId: String) -> Unit = {},
) {
    AlertsScreen(onOpenAlert = onOpenAlert)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onOpenAlert: (alertId: String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Insight alerts",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Inbox placeholder — confluence explainability arrives in Phase 4.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "No Buy / Sell actions. Advisory only.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // Light stub rows so deep-link wiring is exercisable
            StubAlertRow(
                title = "Sample confluence (stub)",
                subtitle = "alert-demo-1 · tap opens chart deep-link",
                onClick = { onOpenAlert("alert-demo-1") },
            )
            StubAlertRow(
                title = "Empty-state preview",
                subtitle = "TODO(MOB-4.1): real payload list",
                onClick = { onOpenAlert("alert-demo-2") },
            )
        }
    }
}

@Composable
private fun StubAlertRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F14)
@Composable
private fun AlertsPreview() {
    ConfluenceTheme { AlertsScreen() }
}
