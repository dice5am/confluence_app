package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dark-first chart-safe contrast tokens (MOB-1.2).
 *
 * Designed for OHLC readability on dark canvases in Phase 2:
 * bull/bear candles, grid, crosshair, and alert markers need
 * WCAG-ish contrast against [Background] / [Surface].
 */
object ConfluenceColors {
    // Core surfaces
    val Background = Color(0xFF0B0F14)
    val Surface = Color(0xFF121821)
    val SurfaceVariant = Color(0xFF1A2330)
    val Outline = Color(0xFF2A3544)

    // Text
    val OnBackground = Color(0xFFE8EEF6)
    val OnSurface = Color(0xFFE8EEF6)
    val OnSurfaceMuted = Color(0xFF9AA8B8)
    val OnPrimary = Color(0xFF041018)

    // Brand / accent (insight UI — not buy/sell)
    val Primary = Color(0xFF5B9FFF)
    val PrimaryContainer = Color(0xFF1A3358)
    val Secondary = Color(0xFF8B7CFF)
    val Tertiary = Color(0xFF3DD6C6)

    // Chart-safe semantic (Phase 2 Canvas will reuse)
    val Bull = Color(0xFF3DDC97)      // up / positive Δ
    val Bear = Color(0xFFFF6B7A)      // down / negative Δ
    val Grid = Color(0xFF243041)
    val Crosshair = Color(0xFFD0D9E6)
    val AlertMarker = Color(0xFFFFC14D)

    // Health freshness chips
    val HealthOk = Color(0xFF3DDC97)
    val HealthDegraded = Color(0xFFFFC14D)
    val HealthStale = Color(0xFFFF9F43)
    val HealthDisconnected = Color(0xFFFF6B7A)

    // Error / empty
    val Error = Color(0xFFFF6B7A)
    val OnError = Color(0xFF2A0A0E)
}

/** Optional light tokens — stub for later; app defaults dark. */
object ConfluenceLightColors {
    val Background = Color(0xFFF5F7FA)
    val Surface = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF0B0F14)
    val Primary = Color(0xFF2F6FED)
    val Bull = Color(0xFF0F9F6E)
    val Bear = Color(0xFFD6455D)
}
