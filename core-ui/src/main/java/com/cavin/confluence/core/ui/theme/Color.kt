package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Dark-first futuristic crypto dashboard tokens (Phase 2 revision).
 * Glass panels + neon blue / orange accents.
 */
object ConfluenceColors {
    // Core surfaces (deep navy-black)
    val Background = Color(0xFF070B12)
    val Surface = Color(0xFF0E1520)
    val SurfaceVariant = Color(0xFF162032)
    val Outline = Color(0xFF2A3A52)

    // Glass fills
    val Glass = Color(0xCC121A28)
    val GlassBorder = Color(0x55A8C4FF)
    val GlassHighlight = Color(0x22FFFFFF)

    // Text
    val OnBackground = Color(0xFFEAF0FA)
    val OnSurface = Color(0xFFEAF0FA)
    val OnSurfaceMuted = Color(0xFF8FA3BD)
    val OnPrimary = Color(0xFF041018)
    val OnAccent = Color(0xFF140A00)

    // Neon accents — blue AND orange (mandatory)
    val Primary = Color(0xFF4DB8FF)
    val PrimaryContainer = Color(0xFF123048)
    val PrimaryGlow = Color(0x664DB8FF)
    val Accent = Color(0xFFFF8A3D)
    val AccentContainer = Color(0xFF3A2210)
    val AccentGlow = Color(0x66FF8A3D)

    val Secondary = Color(0xFF8B7CFF)
    val Tertiary = Color(0xFF3DD6C6)

    // Chart-safe semantic
    val Bull = Color(0xFF3DDC97)
    val Bear = Color(0xFFFF6B7A)
    val Grid = Color(0xFF1E2A3C)
    val Crosshair = Color(0xFFD0D9E6)
    val AlertMarker = Color(0xFFFFC14D)

    // Health
    val HealthOk = Color(0xFF3DDC97)
    val HealthDegraded = Color(0xFFFFC14D)
    val HealthStale = Color(0xFFFF8A3D)
    val HealthDisconnected = Color(0xFFFF6B7A)

    val Error = Color(0xFFFF6B7A)
    val OnError = Color(0xFF2A0A0E)
}

/** Optional light tokens — stub; app defaults dark. */
object ConfluenceLightColors {
    val Background = Color(0xFFF5F7FA)
    val Surface = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF0B0F14)
    val Primary = Color(0xFF2F6FED)
    val Accent = Color(0xFFE67E22)
    val Bull = Color(0xFF0F9F6E)
    val Bear = Color(0xFFD6455D)
}
