package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Futuristic Terminal tokens (§1 FUTURISTIC_TERMINAL_PASS).
 * Feature screens must use these — no hardcoded hex.
 */
object ConfluenceColors {
    val Void = Color(0xFF07090E)
    val VoidElevated = Color(0xFF0B1220)
    val SurfaceGlass = Color(0x8C121826) // ~0.55 alpha of #121826
    val SurfaceGlassSolid = Color(0xFF121826)
    val BorderSubtle = Color(0x12FFFFFF) // white @ ~7%
    val BorderAccent = Color(0x3800F2FE) // cyberCyan @ ~22%
    val CyberCyan = Color(0xFF00F2FE)
    val Mint = Color(0xFF00E676)
    val Rose = Color(0xFFF43F5E)
    val Amber = Color(0xFFFBBF24)
    val Slate = Color(0xFF64748B)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)

    val CyberCyanGlow = Color(0x1400F2FE) // ~8% for hero shadow
    val CyberCyanSoft = Color(0x3800F2FE)

    // Aliases used across existing call sites (map to terminal palette)
    val Background = Void
    val Surface = VoidElevated
    val SurfaceVariant = SurfaceGlassSolid
    val Outline = BorderSubtle
    val Glass = SurfaceGlass
    val GlassBorder = BorderSubtle
    val GlassHighlight = Color(0x14FFFFFF)
    val OnBackground = TextPrimary
    val OnSurface = TextPrimary
    val OnSurfaceMuted = TextSecondary
    val OnPrimary = Void
    val OnAccent = Void
    val Primary = CyberCyan
    val PrimaryContainer = VoidElevated
    val PrimaryGlow = CyberCyanSoft
    val Accent = Amber
    val AccentContainer = VoidElevated
    val AccentGlow = Color(0x38FBBF24)
    val Secondary = CyberCyan
    val Tertiary = Mint
    val Bull = Mint
    val Bear = Rose
    val Grid = Color(0x0AFFFFFF) // ~4% white grid
    val Crosshair = CyberCyan
    val AlertMarker = Amber
    val HealthOk = Mint
    val HealthDegraded = Amber
    val HealthStale = Amber
    val HealthDisconnected = Rose
    val Error = Rose
    val OnError = TextPrimary
}

object ConfluenceLightColors {
    val Background = Color(0xFFF5F7FA)
    val Surface = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF0B0F14)
    val Primary = Color(0xFF0891B2)
    val Accent = Color(0xFFD97706)
    val Bull = Color(0xFF059669)
    val Bear = Color(0xFFE11D48)
}
