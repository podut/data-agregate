package com.podut.dataagregate.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

// ─── Brand palette — single source of truth ───────────────────────────────────
// To change the entire palette, update these constants and both color schemes.

val AppPurple      = Color(0xFF7C3AED)
val AppPurpleLight = Color(0xFF9F67FF)
val AppPurpleDeep  = Color(0xFF5B21B6)
val AppPurpleDark  = Color(0xFF1A0A2E)
val AppBlueDeep    = Color(0xFF1D4ED8)
val AppScoreGreen  = Color(0xFF22C55E)
val AppScoreGreenLight = Color(0xFF4ADE80)
val AppError       = Color(0xFFEF4444)
val AppCyan        = Color(0xFF06B6D4)
val AppAmber       = Color(0xFFF59E0B)
val AppViolet      = Color(0xFF8B5CF6)

// Dark scheme raw colors
private val DarkBg         = Color(0xFF0B0B17)
private val DarkSurface    = Color(0xFF13131F)
private val DarkSurfaceVar = Color(0xFF1C1C2E)
private val DarkCard       = Color(0xFF1E1E2E)
private val DarkText       = Color.White
private val DarkTextSec    = Color(0xFF9CA3AF)
private val DarkBorder     = Color(0xFF2D2D3D)
private val DarkNavBg      = Color(0xFF0F0F17)

// Light scheme raw colors
private val LightBg         = Color(0xFFF5F5FA)
private val LightSurface    = Color(0xFFFFFFFF)
private val LightSurfaceVar = Color(0xFFEFEFF5)
private val LightCard       = Color(0xFFFFFFFF)
private val LightText       = Color(0xFF0B0B17)
private val LightTextSec    = Color(0xFF5A5A72)
private val LightBorder     = Color(0xFFD0D0E0)
private val LightNavBg      = Color(0xFFFFFFFF)

// ─── Color schemes ────────────────────────────────────────────────────────────

private val DarkScheme = darkColorScheme(
    primary           = AppPurple,
    onPrimary         = Color.White,
    primaryContainer  = AppPurpleDeep,
    secondary         = AppPurpleDeep,
    onSecondary       = Color.White,
    background        = DarkBg,
    onBackground      = DarkText,
    surface           = DarkSurface,
    onSurface         = DarkText,
    surfaceVariant    = DarkSurfaceVar,
    onSurfaceVariant  = DarkTextSec,
    outline           = DarkBorder,
    error             = AppError,
    onError           = Color.White,
    scrim             = Color.Black.copy(alpha = 0.5f)
)

private val LightScheme = lightColorScheme(
    primary           = AppPurple,
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFF0EAFF),
    secondary         = AppPurpleDeep,
    onSecondary       = Color.White,
    background        = LightBg,
    onBackground      = LightText,
    surface           = LightSurface,
    onSurface         = LightText,
    surfaceVariant    = LightSurfaceVar,
    onSurfaceVariant  = LightTextSec,
    outline           = LightBorder,
    error             = AppError,
    onError           = Color.White,
    scrim             = Color.Black.copy(alpha = 0.3f)
)

/** Provided by DataAgregateTheme — read from any composable to know current mode. */
val LocalIsDarkTheme = compositionLocalOf { true }

// ─── Semantic helpers ─────────────────────────────────────────────────────────
// These delegate to MaterialTheme.colorScheme so screens don't need to
// know which exact Color value is used — just swap the scheme to retheme.

@Composable @ReadOnlyComposable
fun appBg(): Color = MaterialTheme.colorScheme.background

@Composable @ReadOnlyComposable
fun appOnBg(): Color = MaterialTheme.colorScheme.onBackground

@Composable @ReadOnlyComposable
fun appSurface(): Color = MaterialTheme.colorScheme.surface

@Composable @ReadOnlyComposable
fun appOnSurface(): Color = MaterialTheme.colorScheme.onSurface

/** Semi-transparent card background — dark gets dark glass, light gets white glass. */
@Composable @ReadOnlyComposable
fun appCardBg(): Color = if (LocalIsDarkTheme.current)
    DarkCard.copy(alpha = 0.75f) else LightCard.copy(alpha = 0.97f)

@Composable @ReadOnlyComposable
fun appCardBorder(): Color = if (LocalIsDarkTheme.current)
    Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.07f)

@Composable @ReadOnlyComposable
fun appTextSec(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable @ReadOnlyComposable
fun appTextMuted(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

/** Navigation bar / rail background. */
@Composable @ReadOnlyComposable
fun appNavBg(): Color = if (LocalIsDarkTheme.current) DarkNavBg else LightNavBg

/** Thin divider / separator line. */
@Composable @ReadOnlyComposable
fun appDivider(): Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)

/** Chip / tag background for interests, category labels. */
@Composable @ReadOnlyComposable
fun appChipBg(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

@Composable @ReadOnlyComposable
fun appChipBorder(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

/** Hero card gradient — stays purple/blue regardless of theme (brand element). */
val AppHeroGradient: Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF5B21B6), Color(0xFF1D4ED8)),
    start  = Offset(0f, 0f),
    end    = Offset(900f, 200f)
)

/** Header gradient behind the digest title bar. */
val AppHeaderGradient: Brush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF1A0A2E),
        0.6f to Color(0xFF1A0A2E).copy(alpha = 0.85f),
        1.0f to Color.Transparent
    )
)

/** Opacity for the decorative background image — subtle in light mode. */
@Composable @ReadOnlyComposable
fun appBgImageAlpha(): Float = if (LocalIsDarkTheme.current) 0.40f else 0.08f

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun DataAgregateTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography  = Typography,
            content     = content
        )
    }
}
