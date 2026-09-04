package com.cavin.confluence.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing

data class FloatingDockItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * Floating glass bottom dock — Home / Chart / Alerts.
 * Selected = cyan icon + glow + indicator bar (never brown).
 */
@Composable
fun FloatingDock(
    items: List<FloatingDockItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.dockCorner)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, bottom = ConfluenceDimens.dockElevationGap),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ConfluenceDimens.dockHeight)
                .clip(shape)
                .background(ConfluenceColors.SurfaceGlassSolid.copy(alpha = 0.82f), shape)
                .border(ConfluenceDimens.glassBorder, ConfluenceColors.BorderSubtle, shape)
                .padding(horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                val tint by animateColorAsState(
                    if (selected) ConfluenceColors.CyberCyan else ConfluenceColors.Slate,
                    tween(200),
                    label = "dock${item.id}",
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(item.id) }
                        .padding(vertical = Spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge(
                                    containerColor = ConfluenceColors.CyberCyan,
                                    contentColor = ConfluenceColors.Void,
                                ) { Text(item.badgeCount.toString()) }
                            }
                        },
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = tint,
                            modifier = Modifier
                                .size(ConfluenceDimens.dockIcon)
                                .drawBehind {
                                    if (selected) {
                                        drawRoundRect(
                                            color = ConfluenceColors.CyberCyan.copy(alpha = 0.2f),
                                            cornerRadius = CornerRadius(10.dp.toPx()),
                                        )
                                    }
                                },
                        )
                    }
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = item.label,
                        style = ConfluenceTypography.labelSmall,
                        color = tint,
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Box(
                        Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                if (selected) ConfluenceColors.CyberCyan else ConfluenceColors.Void.copy(alpha = 0f),
                            ),
                    )
                }
            }
        }
    }
}
