package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.Spacing

data class MeterSignal(
    val name: String,
    val state: String, // Align / Expand / Fade
    val bullish: Boolean?,
)

@Composable
fun ConfluenceMeter(
    scoreBullish: Int = 62,
    signals: List<MeterSignal> = listOf(
        MeterSignal("RSI", "Align", true),
        MeterSignal("MACD", "Expand", true),
        MeterSignal("Volume", "Fade", null),
        MeterSignal("Trend", "Align", true),
    ),
    awaiting: Boolean = false,
    onViewChart: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val bearish = (100 - scoreBullish).coerceIn(0, 100)
    GlassCard(modifier = modifier, accentBorder = true, glow = false) {
        Text(
            "Signal confluence",
            style = MaterialTheme.typography.titleMedium,
            color = ConfluenceColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(Spacing.md))
        if (awaiting) {
            Text(
                "Awaiting indicators",
                style = MaterialTheme.typography.bodyMedium,
                color = ConfluenceColors.Slate,
            )
        } else {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Canvas(Modifier.size(ConfluenceDimens.meterHeight)) {
                    val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = ConfluenceColors.BorderSubtle,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = stroke,
                    )
                    drawArc(
                        color = ConfluenceColors.Mint,
                        startAngle = 135f,
                        sweepAngle = 270f * (scoreBullish / 100f),
                        useCenter = false,
                        style = stroke,
                    )
                }
                Column {
                    Text(
                        "Bullish $scoreBullish",
                        style = ConfluenceMono.Row.copy(color = ConfluenceColors.Mint),
                    )
                    Text(
                        "Bearish $bearish",
                        style = ConfluenceMono.Row.copy(color = ConfluenceColors.Rose),
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            signals.forEach { s ->
                val pill = when (s.bullish) {
                    true -> ConfluenceColors.Mint
                    false -> ConfluenceColors.Rose
                    null -> ConfluenceColors.Slate
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xxs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(s.name, style = ConfluenceMono.Caption, color = ConfluenceColors.TextSecondary)
                    Text(s.state, style = ConfluenceMono.Caption, color = pill)
                }
            }
        }
        if (onViewChart != null) {
            Spacer(Modifier.height(Spacing.sm))
            AppTextButton(onClick = onViewChart) {
                Text("View chart", color = ConfluenceColors.CyberCyan)
            }
        }
    }
}
