package com.cavin.confluence.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceMono

enum class PriceTextVariant { Hero, Hud, Row }

@Composable
fun PriceText(
    text: String,
    modifier: Modifier = Modifier,
    variant: PriceTextVariant = PriceTextVariant.Hero,
    color: Color = ConfluenceColors.TextPrimary,
    cyanShadow: Boolean = false,
) {
    val base = when (variant) {
        PriceTextVariant.Hero -> ConfluenceMono.Hero
        PriceTextVariant.Hud -> ConfluenceMono.Hud
        PriceTextVariant.Row -> ConfluenceMono.Row
    }
    val style = if (cyanShadow && variant == PriceTextVariant.Hero) {
        base.copy(
            color = color,
            shadow = Shadow(
                color = ConfluenceColors.CyberCyan.copy(alpha = 0.08f),
                offset = Offset.Zero,
                blurRadius = 30f,
            ),
        )
    } else {
        base.copy(color = color)
    }
    Text(text = text, modifier = modifier, style = style)
}
