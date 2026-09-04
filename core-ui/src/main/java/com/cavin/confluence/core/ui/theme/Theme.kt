package com.cavin.confluence.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkColorScheme = darkColorScheme(
    primary = ConfluenceColors.Primary,
    onPrimary = ConfluenceColors.OnPrimary,
    primaryContainer = ConfluenceColors.PrimaryContainer,
    secondary = ConfluenceColors.Secondary,
    tertiary = ConfluenceColors.Tertiary,
    background = ConfluenceColors.Background,
    onBackground = ConfluenceColors.OnBackground,
    surface = ConfluenceColors.Surface,
    onSurface = ConfluenceColors.OnSurface,
    surfaceVariant = ConfluenceColors.SurfaceVariant,
    onSurfaceVariant = ConfluenceColors.OnSurfaceMuted,
    outline = ConfluenceColors.Outline,
    error = ConfluenceColors.Error,
    onError = ConfluenceColors.OnError,
)

/** Light scheme stub — app defaults to dark (MOB-1.2). */
private val LightColorScheme = lightColorScheme(
    primary = ConfluenceLightColors.Primary,
    background = ConfluenceLightColors.Background,
    surface = ConfluenceLightColors.Surface,
    onBackground = ConfluenceLightColors.OnBackground,
    onSurface = ConfluenceLightColors.OnBackground,
)

data class ChartSafeColors(
    val bull: androidx.compose.ui.graphics.Color = ConfluenceColors.Bull,
    val bear: androidx.compose.ui.graphics.Color = ConfluenceColors.Bear,
    val grid: androidx.compose.ui.graphics.Color = ConfluenceColors.Grid,
    val crosshair: androidx.compose.ui.graphics.Color = ConfluenceColors.Crosshair,
    val alertMarker: androidx.compose.ui.graphics.Color = ConfluenceColors.AlertMarker,
    val healthOk: androidx.compose.ui.graphics.Color = ConfluenceColors.HealthOk,
    val healthDegraded: androidx.compose.ui.graphics.Color = ConfluenceColors.HealthDegraded,
    val healthStale: androidx.compose.ui.graphics.Color = ConfluenceColors.HealthStale,
    val healthDisconnected: androidx.compose.ui.graphics.Color = ConfluenceColors.HealthDisconnected,
)

val LocalChartSafeColors = staticCompositionLocalOf { ChartSafeColors() }
val LocalSpacing = staticCompositionLocalOf { Spacing }

@Composable
fun ConfluenceTheme(
    /**
     * Dark is default. Pass false only for the optional light stub.
     * System light is NOT auto-applied in Phase 1.
     */
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Intentionally ignore system light by default (dark-first).
    @Suppress("UNUSED_VARIABLE")
    val systemDark = isSystemInDarkTheme()

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val chartColors = if (darkTheme) {
        ChartSafeColors()
    } else {
        ChartSafeColors(
            bull = ConfluenceLightColors.Bull,
            bear = ConfluenceLightColors.Bear,
        )
    }

    CompositionLocalProvider(
        LocalChartSafeColors provides chartColors,
        LocalSpacing provides Spacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ConfluenceTypography,
            content = content,
        )
    }
}

object ConfluenceThemeAccess {
    val chartColors: ChartSafeColors
        @Composable get() = LocalChartSafeColors.current
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
}
