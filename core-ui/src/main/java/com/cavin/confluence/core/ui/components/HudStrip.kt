package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceMono
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
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
        Text("OHLC —", style = ConfluenceMono.Hud, color = ConfluenceColors.Slate, modifier = modifier)
        return
    }
    fun fmt(v: Double) = String.format(Locale.US, "%,.2f", v)
    val closeColor = if (ohlc.close >= ohlc.open) ConfluenceColors.Mint else ConfluenceColors.Rose
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudCell("O", fmt(ohlc.open), ConfluenceColors.TextPrimary)
        HudCell("H", fmt(ohlc.high), ConfluenceColors.TextPrimary)
        HudCell("L", fmt(ohlc.low), ConfluenceColors.TextPrimary)
        HudCell("C", fmt(ohlc.close), closeColor)
    }
}

@Composable
private fun HudCell(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, style = ConfluenceTypography.labelSmall, color = ConfluenceColors.Slate)
        Text(value, style = ConfluenceMono.Hud.copy(color = valueColor))
    }
}
