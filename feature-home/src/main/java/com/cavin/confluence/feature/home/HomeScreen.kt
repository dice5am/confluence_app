package com.cavin.confluence.feature.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.AppSectionLabel
import com.cavin.confluence.core.ui.components.AppStatusChip
import com.cavin.confluence.core.ui.components.ConfluenceMeter
import com.cavin.confluence.core.ui.components.DeltaChip
import com.cavin.confluence.core.ui.components.Disclaimer
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.components.MiniSparkline
import com.cavin.confluence.core.ui.components.PriceText
import com.cavin.confluence.core.ui.components.PriceTextVariant
import com.cavin.confluence.core.ui.components.SnapshotBadge
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.Spacing
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.model.HealthStatus
import java.util.Locale
import kotlin.math.sin

/**
 * Home hub — Futuristic Terminal. Dock owns Chart/Alerts; no NAVIGATE stack.
 */
@Composable
fun HomeRoute(
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app = app))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Confluence",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ConfluenceColors.TextPrimary,
                        )
                        Text(
                            "BTC insight hub",
                            style = MaterialTheme.typography.labelSmall,
                            color = ConfluenceColors.Slate,
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
                                        containerColor = ConfluenceColors.CyberCyan,
                                        contentColor = ConfluenceColors.Void,
                                    ) { Text(badgeCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Insight alerts",
                                tint = ConfluenceColors.CyberCyan,
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = ConfluenceColors.Slate,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .drawBehind {
                    // Faint grid + soft cyan radial wash behind hero
                    val step = 48f
                    val grid = ConfluenceColors.Grid
                    var x = 0f
                    while (x < size.width) {
                        drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                ConfluenceColors.CyberCyan.copy(alpha = 0.08f),
                                ConfluenceColors.Void.copy(alpha = 0f),
                            ),
                        ),
                        radius = size.minDimension * 0.55f,
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                    )
                }
                .padding(horizontal = spacing.lg),
        ) {
            when (state) {
                HomeUiState.Loading -> LoadingState()
                HomeUiState.Empty -> EmptyState(onRetry = onRetry)
                is HomeUiState.Error -> ErrorState(message = state.message, onRetry = onRetry)
                is HomeUiState.Ready -> ReadyContent(
                    state = state,
                    spacing = spacing,
                    onOpenChart = onOpenChart,
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    spacing: Spacing,
    onOpenChart: () -> Unit,
) {
    val quote = state.quote
    val spark = rememberSparkFromPrice(quote.lastPrice, quote.percentChange)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AppSectionLabel("Market", accent = true)
        GlassCard(accentBorder = true, glow = true) {
            Text(
                text = "BTC / USDT",
                style = MaterialTheme.typography.labelLarge,
                color = ConfluenceColors.CyberCyan,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(spacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                PriceText(
                    text = formatPrice(quote.lastPrice),
                    variant = PriceTextVariant.Hero,
                    cyanShadow = true,
                    modifier = Modifier.weight(1f),
                )
                DeltaChip(percent = quote.percentChange)
            }
            Spacer(Modifier.height(spacing.sm))
            MiniSparkline(values = spark)
            Spacer(Modifier.height(spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SnapshotBadge()
                FreshnessChip(
                    status = quote.health.status,
                    snapshot = quote.health.note?.startsWith("Historical snapshot") == true,
                )
            }
            quote.health.note?.takeIf { it.isNotBlank() }?.let { note ->
                Spacer(Modifier.height(spacing.sm))
                Text(
                    text = note,
                    style = MaterialTheme.typography.labelSmall,
                    color = ConfluenceColors.Slate,
                )
            }
        }

        ConfluenceMeter(
            scoreBullish = 62,
            awaiting = false,
            onViewChart = onOpenChart,
        )

        Disclaimer(
            modifier = Modifier.fillMaxWidth(),
            text = "Advisory only · no buy/sell",
        )
    }
}

@Composable
private fun rememberSparkFromPrice(last: Double, pct: Double?): List<Float> {
    return remember(last, pct) {
        val base = last.toFloat()
        val drift = ((pct ?: -0.34) / 100.0).toFloat()
        List(32) { i ->
            val t = i / 31f
            val wave = sin(t * 6.2f) * base * 0.004f
            base * (1f - drift * (1f - t)) + wave
        }
    }
}

@Composable
fun FreshnessChip(status: HealthStatus, snapshot: Boolean = false) {
    val chart = ConfluenceThemeAccess.chartColors
    val (label, color) = when (status) {
        HealthStatus.OK -> status.toChipLabel(snapshot) to chart.healthOk
        HealthStatus.DEGRADED -> status.toChipLabel(snapshot) to chart.healthDegraded
        HealthStatus.STALE -> status.toChipLabel(snapshot) to chart.healthStale
        HealthStatus.DISCONNECTED -> status.toChipLabel(snapshot) to chart.healthDisconnected
    }
    AppStatusChip(label = label, color = color)
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(accentBorder = true) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator(color = ConfluenceColors.CyberCyan)
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Loading market snapshot…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ConfluenceColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(accentBorder = true) {
            Text("No market data yet", style = MaterialTheme.typography.titleLarge, color = ConfluenceColors.TextPrimary)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                "No BTC/USDT snapshot available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = ConfluenceColors.Slate,
            )
            Spacer(Modifier.height(Spacing.lg))
            AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(accentBorder = true) {
            Text("Something went wrong", style = MaterialTheme.typography.titleLarge, color = ConfluenceColors.TextPrimary)
            Spacer(Modifier.height(Spacing.sm))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ConfluenceColors.Rose)
            Spacer(Modifier.height(Spacing.lg))
            AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
        }
    }
}

private fun formatPrice(price: Double): String =
    String.format(Locale.US, "$%,.2f", price)

@Preview(showBackground = true, backgroundColor = 0xFF07090E)
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
