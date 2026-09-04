package com.cavin.confluence.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.Spacing

@Composable
fun SnapshotBadge(
    modifier: Modifier = Modifier,
    label: String = "Snapshot",
    pulse: Boolean = true,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.chipRadius)
    val alpha = if (pulse) {
        val t = rememberInfiniteTransition(label = "snap")
        val a by t.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
            label = "dot",
        )
        a
    } else {
        1f
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(ConfluenceColors.Mint.copy(alpha = 0.12f), shape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer { this.alpha = alpha }
                .clip(CircleShape)
                .background(ConfluenceColors.Mint),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ConfluenceColors.Mint,
        )
    }
}
