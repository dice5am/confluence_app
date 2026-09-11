package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceLayout
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.Spacing
import java.util.Locale

data class HudOhlc(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
)

@Composable
fun HudStrip(
    ohlc: HudOhlc?,
    modifier: Modifier = Modifier,
) {
    if (ohlc == null) {
        Text("OHLC —", style = ConfluenceMono.Hud, color = ConfluenceColors.Dim, modifier = modifier)
        return
    }
    fun fmt(v: Double) = String.format(Locale.US, "%,.2f", v)
    val closeColor = if (ohlc.close >= ohlc.open) ConfluenceColors.Pos else ConfluenceColors.Neg
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ConfluenceLayout.inlineGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudCell("O", fmt(ohlc.open), ConfluenceColors.Text, Modifier.weight(1f))
        HudCell("H", fmt(ohlc.high), ConfluenceColors.Pos, Modifier.weight(1f))
        HudCell("L", fmt(ohlc.low), ConfluenceColors.Neg, Modifier.weight(1f))
        HudCell("C", fmt(ohlc.close), closeColor, Modifier.weight(1f))
    }
}

@Composable
private fun HudCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .background(ConfluenceColors.VoidElevated.copy(alpha = 0.85f), shape)
            .border(ConfluenceDimens.glassBorder, ConfluenceColors.BorderSubtle, shape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(label, style = ConfluenceType.Telemetry, color = ConfluenceColors.Dim, maxLines = 1)
        Text(
            value,
            style = ConfluenceMono.Hud.copy(color = valueColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
