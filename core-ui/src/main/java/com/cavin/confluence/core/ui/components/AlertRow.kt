package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceMono
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (accent) {
        AlertAccent.Confluence -> ConfluenceColors.CyberCyan
        AlertAccent.Warning -> ConfluenceColors.Amber
        AlertAccent.Breakdown -> ConfluenceColors.Rose
    }
    val confColor = when {
        confidencePct >= 75 -> ConfluenceColors.Mint
        confidencePct >= 50 -> ConfluenceColors.Amber
        else -> ConfluenceColors.Slate
    }
    val shape = RoundedCornerShape(ConfluenceDimens.glassCorner)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (unread) ConfluenceColors.CyberCyan.copy(alpha = 0.06f)
                else ConfluenceColors.SurfaceGlassSolid.copy(alpha = 0.75f),
            )
            .clickable(onClick = onClick)
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(ConfluenceDimens.accentBar)
                .fillMaxHeight()
                .background(accentColor),
        )
        Spacer(Modifier.width(Spacing.md))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ConfluenceColors.VoidElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.ShowChart,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ConfluenceColors.TextPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(meta, style = MaterialTheme.typography.bodySmall, color = ConfluenceColors.TextSecondary)
        }
        Text(confidenceLabel, style = ConfluenceMono.Caption.copy(color = confColor))
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = ConfluenceColors.Slate,
        )
        Spacer(Modifier.width(Spacing.md))
    }
}
