package com.buysloans.hub

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shared high-contrast visual tokens for the Morley Android client.
 * These are presentation-only: pricing, valuation, authentication, NFC and data boundaries stay unchanged.
 */
internal val MorleyBackground = Color(0xFFF5F8FC)
internal val MorleySurface = Color(0xFFFFFFFF)
internal val MorleySurfaceRaised = Color(0xFFF0F6FF)
internal val MorleySurfaceSoft = Color(0xFFEAF4FF)
internal val MorleyAccent = Color(0xFF0878F9)
internal val MorleyAccentStrong = Color(0xFF0566D8)
internal val MorleyAccentSoft = Color(0xFFEAF4FF)
internal val MorleyTextPrimary = Color(0xFF102A43)
internal val MorleyTextSecondary = Color(0xFF52677C)
internal val MorleyTextMuted = Color(0xFF71869A)
internal val MorleyBorder = Color(0xFFC9DAEC)
internal val MorleySuccess = Color(0xFF238A63)
internal val MorleyWarning = Color(0xFFA86A12)
internal val MorleyDanger = Color(0xFFC74755)

internal val MorleyColorScheme = lightColorScheme(
    primary = MorleyAccent,
    onPrimary = Color.White,
    primaryContainer = MorleyAccentSoft,
    onPrimaryContainer = MorleyTextPrimary,
    secondary = MorleyAccentStrong,
    onSecondary = Color.White,
    background = MorleyBackground,
    onBackground = MorleyTextPrimary,
    surface = MorleySurface,
    onSurface = MorleyTextPrimary,
    surfaceVariant = MorleySurfaceRaised,
    onSurfaceVariant = MorleyTextSecondary,
    outline = MorleyBorder,
    error = MorleyDanger,
    onError = Color.White
)
