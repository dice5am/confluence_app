package com.cavin.confluence.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cavin.confluence.core.ui.theme.ConfluenceColors

/** Loading spinner — dim ring with plasma top edge (F1 lock). */
@Composable
fun PlasmaSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val t = rememberInfiniteTransition(label = "plasmaSpin")
    val rot by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "rot",
    )
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = ConfluenceColors.Dim.copy(alpha = 0.35f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        rotate(rot) {
            drawArc(
                color = ConfluenceColors.Plasma,
                startAngle = -90f,
                sweepAngle = 110f,
                useCenter = false,
                style = stroke,
            )
        }
    }
}
