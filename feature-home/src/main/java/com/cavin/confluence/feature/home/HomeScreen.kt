package com.cavin.confluence.feature.home

import android.app.Application
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cavin.confluence.core.ui.components.AlertAccent
import com.cavin.confluence.core.ui.components.AlertRow
import com.cavin.confluence.core.ui.components.AppButton
import com.cavin.confluence.core.ui.components.AppButtonStyle
import com.cavin.confluence.core.ui.components.AppChip
import com.cavin.confluence.core.ui.components.ConfluenceMeter
import com.cavin.confluence.core.ui.components.DeltaChip
import com.cavin.confluence.core.ui.components.Disclaimer
import com.cavin.confluence.core.ui.components.GlassCard
import com.cavin.confluence.core.ui.components.PlasmaSpinner
import com.cavin.confluence.core.ui.components.PreviewAppShell
import com.cavin.confluence.core.ui.components.PriceText
import com.cavin.confluence.core.ui.components.PriceTextVariant
import com.cavin.confluence.core.ui.components.SnapshotBadge
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceThemeAccess
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing
import com.cavin.confluence.core.ui.theme.confluenceScreenGutter
import com.cavin.confluence.data.fake.FakeFixtures
import com.cavin.confluence.data.snapshot.MdSnapshotStore
import java.util.Locale

/**
 * Home hub — F1 Plasma Acrylic / B1 Ice + Bright Blue.
 * Dock owns Chart/Alerts; no NAVIGATE stack.
 */
@Composable
fun HomeRoute(
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app = app))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenChart = onOpenChart,
        onOpenAlerts = onOpenAlerts,
        onRetry = viewModel::refresh,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
    onRetry: () -> Unit,
) {
    val spacing = ConfluenceThemeAccess.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .confluenceScreenGutter()
            .verticalScroll(rememberScrollState(), clip = false)
            .padding(top = ConfluenceLayout.screenTop, bottom = ConfluenceLayout.screenBottom),
        verticalArrangement = Arrangement.spacedBy(ConfluenceLayout.stackGap),
    ) {
        Column {
            Text(
                "ICE + BRIGHT BLUE",
                style = ConfluenceType.Eyebrow,
                color = ConfluenceColors.Bloom,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                "Blue base · brighter ice-blue accent (no warm hues)",
                style = ConfluenceTypography.labelSmall,
                color = ConfluenceColors.Muted,
            )
        }

        when (state) {
            HomeUiState.Loading -> LoadingState()
            HomeUiState.Empty -> EmptyState(onRetry = onRetry)
            is HomeUiState.Error -> ErrorState(message = state.message, onRetry = onRetry)
            is HomeUiState.Ready -> ReadyContent(
                state = state,
                spacing = spacing,
                onOpenChart = onOpenChart,
                onOpenAlerts = onOpenAlerts,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    state: HomeUiState.Ready,
    spacing: Spacing,
    onOpenChart: () -> Unit,
    onOpenAlerts: () -> Unit,
) {
    val quote = state.quote
    val confScore = 62
    val asOf = quote.health.note?.takeIf { it.startsWith("Historical snapshot") }
        ?: MdSnapshotStore.bannerLabel

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        AppChip(
            label = "SNAP",
            selected = true,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        AppChip(
            label = "CONF $confScore",
            selected = false,
            onClick = {},
            modifier = Modifier.weight(1f),
        )
        AppChip(
            label = "ALRT ${state.unreadAlertCount}",
            selected = false,
            onClick = onOpenAlerts,
            modifier = Modifier.weight(1f),
        )
    }

    GlassCard(accentBorder = true, glow = true) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "BTC / USDT",
                style = ConfluenceType.Telemetry,
                color = ConfluenceColors.Plasma,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(spacing.sm))
            PriceText(
                text = formatPrice(quote.lastPrice),
                variant = PriceTextVariant.Hero,
                cyanShadow = true,
            )
            Spacer(Modifier.height(spacing.sm))
            DeltaChip(percent = quote.percentChange)
            Spacer(Modifier.height(spacing.md))
            SnapshotBadge(label = "Snapshot · glow through acrylic")
            Spacer(Modifier.height(spacing.xs))
            Text(
                text = asOf,
                style = ConfluenceTypography.labelSmall,
                color = ConfluenceColors.Dim,
            )
        }
    }

    ConfluenceMeter(scoreBullish = confScore, awaiting = false)

    AlertRow(
        title = "Sample confluence",
        meta = "12:04 · EMA-CROSS",
        confidenceLabel = "88%",
        confidencePct = 88,
        accent = AlertAccent.Confluence,
        unread = true,
        onClick = onOpenChart,
    )

    Disclaimer(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassCard(accentBorder = true, glow = true) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                PlasmaSpinner()
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "Loading market snapshot…",
                    style = ConfluenceTypography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ConfluenceColors.Text,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onRetry: () -> Unit) {
    GlassCard(accentBorder = true, glow = true) {
        Text("No market data yet", style = ConfluenceTypography.titleLarge, color = ConfluenceColors.Text)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "No BTC/USDT snapshot available yet.",
            style = ConfluenceTypography.bodyMedium,
            color = ConfluenceColors.Dim,
        )
        Spacer(Modifier.height(Spacing.lg))
        AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    GlassCard(accentBorder = true, glow = true) {
        Text("Something went wrong", style = ConfluenceTypography.titleLarge, color = ConfluenceColors.Text)
        Spacer(Modifier.height(Spacing.sm))
        Text(message, style = ConfluenceTypography.bodyMedium, color = ConfluenceColors.Neg)
        Spacer(Modifier.height(Spacing.lg))
        AppButton(onClick = onRetry, style = AppButtonStyle.Secondary) { Text("Retry") }
    }
}

private fun formatPrice(price: Double): String =
    String.format(Locale.US, "$%,.2f", price)

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun HomeReadyPreview() {
    ConfluenceTheme {
        PreviewAppShell(selectedId = "home") {
            HomeScreen(
                state = HomeUiState.Ready(
                    quote = FakeFixtures.sampleQuote(),
                    unreadAlertCount = 2,
                ),
                onOpenChart = {},
                onOpenAlerts = {},
                onRetry = {},
            )
        }
    }
}
