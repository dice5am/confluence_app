package com.cavin.confluence.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.cavin.confluence.core.ui.R
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceDimens
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.core.ui.theme.ConfluenceType
import com.cavin.confluence.core.ui.theme.ConfluenceTypography
import com.cavin.confluence.core.ui.theme.Spacing

/**
 * Locked H1 Stream Acrylic mark (M1×M3 hybrid). Light-on-dark only.
 * Raster is generated from `app/src/main/assets/brand/confluence-mark.svg`.
 */
@Composable
fun ConfluenceMark(
    modifier: Modifier = Modifier,
    size: Dp = ConfluenceDimens.brandMarkHeader,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(R.drawable.confluence_mark),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

/**
 * Existing header chrome with the H1 mark swapped in — no layout redesign.
 */
@Composable
fun ConfluenceBrandLockup(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = ConfluenceTypography.titleLarge,
    titleColor: Color = ConfluenceColors.Text,
    titleWeight: FontWeight = FontWeight.SemiBold,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ConfluenceMark(size = ConfluenceDimens.brandMarkHeader)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle,
                fontWeight = titleWeight,
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = ConfluenceTypography.labelSmall,
                color = ConfluenceColors.Muted,
            )
        }
        trailing?.invoke(this)
    }
}

/** Cold-start / first-frame splash — H1 centered 96–120dp on void. */
@Composable
fun ConfluenceSplash(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ConfluenceColors.Void),
        contentAlignment = Alignment.Center,
    ) {
        ConfluenceMark(size = ConfluenceDimens.brandMarkSplash)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390)
@Composable
private fun ConfluenceBrandLockupPreview() {
    ConfluenceTheme {
        ConfluenceBrandLockup(
            title = "ICE + BRIGHT BLUE",
            subtitle = "H1 Stream Acrylic",
            titleStyle = ConfluenceType.Eyebrow,
            titleColor = ConfluenceColors.Bloom,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B14, widthDp = 390, heightDp = 780)
@Composable
internal fun ConfluenceSplashPreview() {
    ConfluenceTheme {
        ConfluenceSplash()
    }
}
