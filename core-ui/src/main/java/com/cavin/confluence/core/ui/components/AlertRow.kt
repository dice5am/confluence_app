package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing

enum class AlertAccent { Confluence, Warning, Breakdown }

@Composable
fun AlertRow(
    title: String,
    meta: String,
    confidenceLabel: String,
    confidencePct: Int,
    accent: AlertAccent = AlertAccent.Confluence,
    unread: Boolean = true,
    detail: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (accent) {
        AlertAccent.Confluence -> ConfluenceColors.Plasma
        AlertAccent.Warning -> ConfluenceColors.Warn
        AlertAccent.Breakdown -> ConfluenceColors.Neg
    }
    val confColor = when {
        confidencePct >= 75 -> ConfluenceColors.Pos
        confidencePct >= 50 -> ConfluenceColors.Warn
        else -> ConfluenceColors.Dim
    }
    PlasmaAcrylicBox(
        modifier = modifier.clickable(onClick = onClick),
        glow = unread,
        brackets = unread,
        dashed = false,
        contentPadding = ConfluenceDimens.glassPaddingTight,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(ConfluenceDimens.accentBar)
                    .height(Spacing.xxxl)
                    .background(accentColor),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        style = ConfluenceTypography.titleMedium,
                        color = ConfluenceColors.Text,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        confidenceLabel,
                        style = ConfluenceMono.Row.copy(color = confColor),
                    )
                }
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    meta,
                    style = ConfluenceMono.Caption,
                    color = ConfluenceColors.Muted,
                )
                if (!detail.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        detail,
                        style = ConfluenceTypography.labelSmall,
                        color = ConfluenceColors.Ice,
                    )
                }
            }
        }
    }
}
