package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens

/**
 * F1 Plasma Acrylic chrome: offset acrylic under-layer + plasma bloom on the
 * face + corner brackets. Glow and under-layer travel together.
 */
@Composable
fun PlasmaAcrylicBox(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    brackets: Boolean = true,
    dashed: Boolean = false,
    contentPadding: Dp = ConfluenceDimens.glassPadding,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ConfluenceDimens.glassCorner)
    val under = ConfluenceDimens.acrylicUnderOffset
    val showSystem = glow && !dashed

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showSystem) Modifier.padding(end = under, bottom = under) else Modifier),
    ) {
        if (showSystem) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = under, y = under)
                    .border(
                        ConfluenceDimens.glassBorder,
                        ConfluenceColors.AcrylicEdge.copy(alpha = 0.55f),
                        shape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (showSystem) {
                        Modifier
                            .shadow(
                                elevation = ConfluenceDimens.plasmaGlowPad,
                                shape = shape,
                                ambientColor = ConfluenceColors.Bloom.copy(alpha = 0.40f),
                                spotColor = ConfluenceColors.Plasma.copy(alpha = 0.35f),
                            )
                            .drawBehind {
                                drawPlasmaBloom(ConfluenceDimens.glassCorner.toPx())
                            }
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(plasmaFaceBrush(), shape)
                .then(
                    if (dashed) {
                        Modifier.drawBehind {
                            drawDashedBorder(ConfluenceDimens.glassCorner.toPx())
                        }
                    } else {
                        Modifier.border(
                            ConfluenceDimens.glassBorder,
                            if (showSystem) ConfluenceColors.BorderAccent else ConfluenceColors.BorderSubtle,
                            shape,
                        )
                    },
                )
                .drawWithContent {
                    drawContent()
                    if (brackets && !dashed) {
                        drawCornerBrackets(
                            color = ConfluenceColors.Bracket.copy(alpha = 0.92f),
                            stroke = ConfluenceDimens.bracketStroke.toPx(),
                            length = ConfluenceDimens.bracketLength.toPx(),
                        )
                    }
                }
                .padding(contentPadding),
            content = content,
        )
    }
}

private fun plasmaFaceBrush(): Brush = Brush.verticalGradient(
    0f to ConfluenceColors.Plasma.copy(alpha = 0.16f),
    0.22f to ConfluenceColors.Text.copy(alpha = 0.05f),
    1f to ConfluenceColors.VoidElevated.copy(alpha = 0.94f),
)

internal fun DrawScope.drawPlasmaBloom(cornerPx: Float) {
    val inflate = 14.dp.toPx()
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                ConfluenceColors.Bloom.copy(alpha = 0.32f),
                ConfluenceColors.Plasma.copy(alpha = 0.14f),
                Color.Transparent,
            ),
            center = Offset(size.width / 2f, size.height * 0.12f),
            radius = size.maxDimension * 0.78f,
        ),
        topLeft = Offset(-inflate * 0.25f, -inflate * 0.25f),
        size = Size(size.width + inflate * 0.5f, size.height + inflate * 0.5f),
        cornerRadius = CornerRadius(cornerPx + inflate * 0.15f),
    )
}

internal fun DrawScope.drawCornerBrackets(
    color: Color,
    stroke: Float,
    length: Float,
) {
    val inset = stroke / 2f + 1.5.dp.toPx()
    val cap = StrokeCap.Square
    val w = size.width
    val h = size.height
    fun seg(a: Offset, b: Offset) {
        drawLine(color, a, b, stroke, cap)
    }
    seg(Offset(inset, inset), Offset(inset + length, inset))
    seg(Offset(inset, inset), Offset(inset, inset + length))
    seg(Offset(w - inset, inset), Offset(w - inset - length, inset))
    seg(Offset(w - inset, inset), Offset(w - inset, inset + length))
    seg(Offset(inset, h - inset), Offset(inset + length, h - inset))
    seg(Offset(inset, h - inset), Offset(inset, h - inset - length))
    seg(Offset(w - inset, h - inset), Offset(w - inset - length, h - inset))
    seg(Offset(w - inset, h - inset), Offset(w - inset, h - inset - length))
}

internal fun DrawScope.drawDashedBorder(shapeCorner: Float) {
    drawRoundRect(
        color = ConfluenceColors.Dim.copy(alpha = 0.55f),
        cornerRadius = CornerRadius(shapeCorner),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
        ),
    )
}
