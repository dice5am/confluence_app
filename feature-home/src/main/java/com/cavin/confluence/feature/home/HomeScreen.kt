package com.cavin.confluence.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.AppCard
import com.cavin.confluence.core.ui.components.AppCardGlow
import com.cavin.confluence.core.ui.components.AppStatusChip
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.Spacing
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.HealthStatus
import java.util.Locale

/**
 * Home hub — futuristic glass dashboard (Phase 2 revision).
 * Insight only; no Buy/Sell CTAs.
 */
@Composable
fun HomeRoute(
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenChart = onOpenChart,
        onOpenAlerts = onOpenAlerts,
        onOpenSettings = onOpenSettings,
        onRetry = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
) {
    val spacing = ConfluenceThemeAccess.spacing
    val chartColors = ConfluenceThemeAccess.chartColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Confluence",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "BTC insight hub",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.OnSurfaceMuted,
                        )
                    }
                },
                actions = {
                    val badgeCount = (state as? HomeUiState.Ready)?.unreadAlertCount ?: 0
                    IconButton(onClick = onOpenAlerts) {
                        BadgedBox(
                            badge = {
                                if (badgeCount > 0) {
                                    Badge(
                                        containerColor = ConfluenceColors.Accent,
                                        contentColor = ConfluenceColors.OnAccent,
                                    ) { Text(badgeCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Insight alerts",
                                tint = ConfluenceColors.Primary,
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings placeholder",
                            tint = ConfluenceColors.OnSurfaceMuted,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConfluenceColors.Background,
                ),
            )
        },
        containerColor = ConfluenceColors.Background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.lg),
        ) {
            when (state) {
                HomeUiState.Loading -> LoadingState()
                HomeUiState.Empty -> EmptyState(onRetry = onRetry)
                is HomeUiState.Error -> ErrorState(message = state.message, onRetry = onRetry)
                is HomeUiState.Ready -> ReadyContent(
                    state = state,
                    spacing = spacing,
                    bull = chartColors.bull,
                    bear = chartColors.bear,
                    onOpenChart = onOpenChart,
                    onOpenAlerts = onOpenAlerts,
                    onOpenSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    spacing: Spacing,
    bull: Color,
    bear: Color,
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val quote = state.quote
    val pct = quote.percentChange
    val pctColor = when {
        pct == null -> ConfluenceColors.OnSurfaceMuted
        pct >= 0 -> bull
        else -> bear
    }
    val pctText = pct?.let {
        String.format(Locale.US, "%+.2f%%", it)
    } ?: "—%"

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AppCard(glow = AppCardGlow.Blue) {
            Text(
                text = "BTC / USDT",
                style = MaterialTheme.typography.labelLarge,
                color = ConfluenceColors.Primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = formatPrice(quote.lastPrice),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = ConfluenceColors.OnBackground,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = pctText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = pctColor,
                )
                FreshnessChip(status = quote.health.status)
            }
            Spacer(Modifier.height(spacing.sm))
            Text(
                text = "Insight only — never executes trades",
                style = MaterialTheme.typography.labelSmall,
                color = ConfluenceColors.OnSurfaceMuted,
            )
        }

        AppButton(
            onClick = onOpenChart,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.Primary,
        ) {
            Icon(Icons.Outlined.ShowChart, contentDescription = null)
            Spacer(Modifier.width(spacing.sm))
            Text("Open chart")
        }

        AppButton(
            onClick = onOpenAlerts,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.Secondary,
        ) {
            Text(
                if (state.unreadAlertCount > 0) {
                    "Alerts (${state.unreadAlertCount} unread)"
                } else {
                    "Alerts"
                },
            )
        }

        AppButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
            style = AppButtonStyle.Ghost,
        ) {
            Text("Settings (placeholder)")
        }
    }
}

@Composable
fun FreshnessChip(status: HealthStatus) {
    val chart = ConfluenceThemeAccess.chartColors
    val (label, color) = when (status) {
        HealthStatus.OK -> status.toChipLabel() to chart.healthOk
        HealthStatus.DEGRADED -> status.toChipLabel() to chart.healthDegraded
        HealthStatus.STALE -> status.toChipLabel() to chart.healthStale
        HealthStatus.DISCONNECTED -> status.toChipLabel() to chart.healthDisconnected
    }
    AppStatusChip(label = label, color = color)
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ConfluenceColors.Primary)
            Spacer(Modifier.height(12.dp))
            Text("Loading market snapshot…", color = ConfluenceColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppCard(glow = AppCardGlow.Orange, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("No market data yet", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Fixtures or API have not provided a BTC/USDT snapshot.",
                color = ConfluenceColors.OnSurfaceMuted,
            )
            Spacer(Modifier.height(16.dp))
            AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppCard(glow = AppCardGlow.Orange, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(message, color = ConfluenceColors.Error)
            Spacer(Modifier.height(16.dp))
            AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
        }
    }
}

private fun formatPrice(price: Double): String =
    String.format(Locale.US, "$%,.2f", price)

@Preview(showBackground = true, backgroundColor = 0xFF070B12)
@Composable
private fun HomeReadyPreview() {
    ConfluenceTheme {
        HomeScreen(
            state = HomeUiState.Ready(
                quote = FakeFixtures.sampleQuote(),
                unreadAlertCount = 2,
            ),
            onOpenChart = {},
            onOpenAlerts = {},
            onOpenSettings = {},
            onRetry = {},
        )
    }
}
