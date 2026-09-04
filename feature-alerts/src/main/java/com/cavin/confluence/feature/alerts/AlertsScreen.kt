package com.cavin.confluence.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cavin.confluence.core.ui.components.AlertAccent
import com.cavin.confluence.core.ui.components.AlertRow
import com.cavin.confluence.core.ui.components.Disclaimer
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess

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
    val spacing = ConfluenceThemeAccess.spacing

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Insight alerts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ConfluenceColors.TextPrimary,
                        )
                        Text(
                            "Advisory only · no buy/sell",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.Slate,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConfluenceColors.Void,
                ),
            )
        },
        containerColor = ConfluenceColors.Void,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.lg, vertical = spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            AlertRow(
                title = "Sample confluence",
                meta = "12:04 UTC  ·  [EMA-CROSS]  ·  [VOL-SPIKE]",
                confidenceLabel = "88%",
                confidencePct = 88,
                accent = AlertAccent.Confluence,
                unread = true,
                onClick = { onOpenAlert("alert-demo-1") },
            )

            GlassCard {
                Text(
                    "No other alerts right now.",
                    style = MaterialTheme.typography.titleMedium,
                    color = ConfluenceColors.TextPrimary,
                )
                Spacer(Modifier.height(spacing.xs))
                Text(
                    "New confluence insights will appear here as they surface.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ConfluenceColors.Slate,
                )
            }

            Disclaimer(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF07090E)
@Composable
private fun AlertsPreview() {
    ConfluenceTheme { AlertsScreen() }
}
