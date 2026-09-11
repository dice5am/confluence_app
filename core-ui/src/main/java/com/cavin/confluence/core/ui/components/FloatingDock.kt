package com.cavin.confluence.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.Spacing

data class FloatingDockItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val badgeCount: Int = 0,
)

/**
 * Three-tab floating dock — plasma frame, filled active cell.
 * H1 brand spot at [ConfluenceDimens.brandMarkDock]; labels remain the tabs.
 */
@Composable
fun FloatingDock(
    items: List<FloatingDockItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    gutter: Boolean = true,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.dockCorner)
    val cellShape = RoundedCornerShape(ConfluenceDimens.dockCellCorner)
    val horizontalGutter = if (gutter) ConfluenceLayout.screenGutter else 0.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = horizontalGutter,
                end = horizontalGutter,
                bottom = ConfluenceDimens.dockElevationGap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ConfluenceDimens.dockHeight)
                .clip(shape)
                .background(ConfluenceColors.VoidElevated.copy(alpha = 0.92f), shape)
                .border(ConfluenceDimens.glassBorder, ConfluenceColors.BorderAccent, shape)
                .padding(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConfluenceMark(
                size = ConfluenceDimens.brandMarkDock,
                modifier = Modifier.padding(start = Spacing.xs, end = Spacing.xs),
            )
            items.forEach { item ->
                val selected = item.id == selectedId
                val tint by animateColorAsState(
                    if (selected) ConfluenceColors.Ice else ConfluenceColors.Dim,
                    tween(200),
                    label = "dock${item.id}",
                )
                val cellBg by animateColorAsState(
                    if (selected) ConfluenceColors.Plasma.copy(alpha = 0.18f)
                    else ConfluenceColors.Void.copy(alpha = 0f),
                    tween(200),
                    label = "dockBg${item.id}",
                )
                val label = buildString {
                    append(item.label.uppercase())
                    if (item.badgeCount > 0) append("  ${item.badgeCount}")
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .clip(cellShape)
                        .background(cellBg, cellShape)
                        .then(
                            if (selected) {
                                Modifier.border(ConfluenceDimens.glassBorder, ConfluenceColors.Plasma, cellShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(item.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = ConfluenceType.Telemetry,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = tint,
                    )
                }
            }
        }
    }
}

val ConfluenceDockItems = listOf(
    FloatingDockItem("home", "Home"),
    FloatingDockItem("chart", "Chart"),
    FloatingDockItem("alerts", "Alerts"),
)
