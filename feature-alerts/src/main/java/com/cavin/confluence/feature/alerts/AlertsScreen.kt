package com.cavin.confluence.feature.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.cavin.confluence.core.ui.components.AlertAccent
import com.cavin.confluence.core.ui.components.AlertRow
import com.cavin.confluence.core.ui.components.Disclaimer
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.components.PreviewAppShell
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.ConfluenceTypography

@Composable
fun AlertsRoute(
    onOpenAlert: (alertId: String) -> Unit = {},
) {
    AlertsScreen(onOpenAlert = onOpenAlert)
}

@Composable
fun AlertsScreen(
    onOpenAlert: (alertId: String) -> Unit = {},
) {
    val spacing = ConfluenceThemeAccess.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column {
            Text(
                "Insight alerts",
                style = ConfluenceTypography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = ConfluenceColors.Text,
            )
            Text(
                "Advisory only · Ice + Bright Blue",
                style = ConfluenceTypography.labelSmall,
                color = ConfluenceColors.Muted,
            )
        }

        AlertRow(
            title = "Sample confluence",
            meta = "12:04 UTC · EMA-CROSS · VOL-SPIKE",
            confidenceLabel = "88%",
            confidencePct = 88,
            accent = AlertAccent.Confluence,
            unread = true,
            detail = "Unread · open chart →",
            onClick = { onOpenAlert("alert-demo-1") },
        )

        AlertRow(
            title = "Volume fade",
            meta = "Advisory insight",
            confidenceLabel = "71%",
            confidencePct = 71,
            accent = AlertAccent.Warning,
            unread = false,
            onClick = { onOpenAlert("alert-demo-2") },
        )

        GlassCard(dashed = true, glow = false, accentBorder = false, brackets = false) {
            Text(
                "No other alerts right now.",
                style = ConfluenceTypography.bodyMedium,
                color = ConfluenceColors.Dim,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Disclaimer(modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 400, heightDp = 780)
@Composable
internal fun AlertsPreview() {
    ConfluenceTheme {
        PreviewAppShell(selectedId = "alerts") {
            AlertsScreen()
        }
    }
}
