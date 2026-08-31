package com.buysloans.hub

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shared high-contrast visual tokens for the Morley Android client.
 * These are presentation-only: pricing, valuation, authentication, NFC and data boundaries stay unchanged.
 */
internal val MorleyBackground = Color(0xFF080B0D)
internal val MorleySurface = Color(0xFF101619)
internal val MorleySurfaceRaised = Color(0xFF151D20)
internal val MorleySurfaceSoft = Color(0xFF0C1214)
internal val MorleyAccent = Color(0xFF38D6A3)
internal val MorleyAccentStrong = Color(0xFF1FB887)
internal val MorleyAccentSoft = Color(0xFF173C32)
internal val MorleyTextPrimary = Color(0xFFF4F7F6)
internal val MorleyTextSecondary = Color(0xFFB2C0BC)
internal val MorleyTextMuted = Color(0xFF81918C)
internal val MorleyBorder = Color(0xFF2B4540)
internal val MorleySuccess = Color(0xFF63E6A6)
internal val MorleyWarning = Color(0xFFF5C76B)
internal val MorleyDanger = Color(0xFFFF7B86)

internal val MorleyColorScheme = darkColorScheme(
    primary = MorleyAccent,
    onPrimary = Color(0xFF06251B),
    primaryContainer = MorleyAccentSoft,
    onPrimaryContainer = MorleyTextPrimary,
    secondary = MorleyAccentStrong,
    onSecondary = Color(0xFF031E16),
    background = MorleyBackground,
    onBackground = MorleyTextPrimary,
    surface = MorleySurface,
    onSurface = MorleyTextPrimary,
    surfaceVariant = MorleySurfaceRaised,
    onSurfaceVariant = MorleyTextSecondary,
    outline = MorleyBorder,
    error = MorleyDanger,
    onError = Color(0xFF2A0408)
)
