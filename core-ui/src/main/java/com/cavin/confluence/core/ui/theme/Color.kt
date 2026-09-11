package com.cavin.confluence.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * B1 Ice + Bright Blue tokens (Product Designer lock).
 * F1 Plasma Acrylic materials — purple DNA retinted to these blues.
 * Feature screens must use these names; do not hardcode hex.
 */
object ConfluenceColors {
    // Locked palette (palettes.json b1) — use exactly
    val Void = Color(0xFF060B14)
    val Text = Color(0xFFF0F9FF)
    val Plasma = Color(0xFF0EA5E9)
    val Bloom = Color(0xFF38BDF8)
    val Ice = Color(0xFFBAE6FD)
    val Bracket = Color(0xFFE0F2FE)
    val Muted = Color(0xFF7DD3FC)
    val Muted2 = Color(0xFFBAE6FD)
    val AcrylicEdge = Color(0xFF22D3EE)
    val Dim = Color(0xFF64748B)
    val Neg = Color(0xFFFB7185)
    val Pos = Color(0xFF4ADE80)
    val Warn = Color(0xFFFBBF24)
    val GradEnd = Color(0xFF67E8F9)

    // Surfaces derived from void (not a second palette)
    val VoidElevated = Color(0xFF0A1522)
    val SurfaceGlass = Color(0x14F0F9FF)
    val SurfaceGlassSolid = Color(0xFF0C1824)
    val BorderSubtle = Color(0x33BAE6FD)
    val BorderAccent = Color(0x730EA5E9)
    val GlassFill = Color(0x0AF0F9FF)
    val FaceBloom = Color(0x3D0EA5E9)

    val PlasmaGlow = Color(0x380EA5E9)
    val PlasmaSoft = Color(0x590EA5E9)
    val BloomGlow = Color(0x5238BDF8)

    // Aliases so existing call sites keep compiling
    val CyberCyan = Plasma
    val CyberCyanGlow = PlasmaGlow
    val CyberCyanSoft = PlasmaSoft
    val Mint = Pos
    val Rose = Neg
    val Amber = Warn
    val Slate = Dim
    val TextPrimary = Text
    val TextSecondary = Muted
    val Background = Void
    val Surface = VoidElevated
    val SurfaceVariant = SurfaceGlassSolid
    val Outline = BorderSubtle
    val Glass = SurfaceGlass
    val GlassBorder = BorderSubtle
    val GlassHighlight = Color(0x14F0F9FF)
    val OnBackground = Text
    val OnSurface = Text
    val OnSurfaceMuted = Muted
    val OnPrimary = Void
    val OnAccent = Void
    val Primary = Plasma
    val PrimaryContainer = VoidElevated
    val PrimaryGlow = PlasmaSoft
    val Accent = Ice
    val AccentContainer = VoidElevated
    val AccentGlow = Color(0x38BAE6FD)
    val Secondary = Plasma
    val Tertiary = Ice
    val Bull = Pos
    val Bear = Neg
    val Grid = Color(0x14BAE6FD)
    val Crosshair = Plasma
    val LastPriceHairline = Color(0x330EA5E9)
    val AlertMarker = Warn
    val HealthOk = Pos
    val HealthDegraded = Warn
    val HealthStale = Warn
    val HealthDisconnected = Neg
    val Error = Neg
    val OnError = Text
}

object ConfluenceLightColors {
    val Background = Color(0xFFF0F9FF)
    val Surface = Color(0xFFFFFFFF)
    val OnBackground = Color(0xFF060B14)
    val Primary = Color(0xFF0284C7)
    val Accent = Color(0xFF0EA5E9)
    val Bull = Color(0xFF16A34A)
    val Bear = Color(0xFFE11D48)
}
