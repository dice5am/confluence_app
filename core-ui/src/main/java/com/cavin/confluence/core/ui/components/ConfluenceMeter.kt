package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.Spacing

data class MeterSignal(
    val name: String,
    val state: String,
    val bullish: Boolean?,
)

@Composable
fun ConfluenceMeter(
    scoreBullish: Int = 62,
    signals: List<MeterSignal> = emptyList(),
    awaiting: Boolean = false,
    onViewChart: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val score = scoreBullish.coerceIn(0, 100)
    GlassCard(modifier = modifier, accentBorder = true, glow = true) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "CONFLUENCE",
                style = ConfluenceType.Eyebrow,
                color = ConfluenceColors.Muted,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                score.toString(),
                style = ConfluenceMono.Row.copy(color = ConfluenceColors.Ice),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        if (awaiting) {
            Text(
                "Awaiting indicators",
                style = ConfluenceType.Telemetry,
                color = ConfluenceColors.Dim,
            )
        } else {
            val barShape = RoundedCornerShape(ConfluenceDimens.chipRadius)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ConfluenceDimens.meterBarHeight)
                    .clip(barShape)
                    .background(ConfluenceColors.Dim.copy(alpha = 0.35f), barShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(score / 100f)
                        .height(ConfluenceDimens.meterBarHeight)
                        .clip(barShape)
                        .background(ConfluenceColors.Plasma, barShape),
                )
            }
            if (signals.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.md))
                signals.forEach { s ->
                    val pill = when (s.bullish) {
                        true -> ConfluenceColors.Pos
                        false -> ConfluenceColors.Neg
                        null -> ConfluenceColors.Dim
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(s.name, style = ConfluenceMono.Caption, color = ConfluenceColors.Muted)
                        Text(s.state, style = ConfluenceMono.Caption, color = pill)
                    }
                }
            }
        }
        if (onViewChart != null) {
            Spacer(Modifier.height(Spacing.sm))
            AppTextButton(onClick = onViewChart) {
                Text("View chart", color = ConfluenceColors.Plasma)
            }
        }
    }
}
