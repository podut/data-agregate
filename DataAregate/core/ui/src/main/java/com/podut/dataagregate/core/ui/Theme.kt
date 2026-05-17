package com.podut.dataagregate.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val AppPurple     = Color(0xFF7C3AED)
val AppBackground = Color(0xFF0B0B17)
val AppSurface    = Color(0xFF13131F)
val AppSurfaceVar = Color(0xFF1C1C2E)
val AppTextGray   = Color(0xFF9CA3AF)
val AppBorder     = Color(0xFF2D2D3D)
val AppScoreGreen = Color(0xFF22C55E)

private val DarkScheme = darkColorScheme(
    primary          = AppPurple,
    onPrimary        = Color.White,
    background       = AppBackground,
    onBackground     = Color.White,
    surface          = AppSurface,
    onSurface        = Color.White,
    surfaceVariant   = AppSurfaceVar,
    onSurfaceVariant = AppTextGray,
    outline          = AppBorder
)

private val LightScheme = lightColorScheme(
    primary          = AppPurple,
    onPrimary        = Color.White,
    background       = Color(0xFFF5F5FA),
    onBackground     = Color(0xFF0B0B17),
    surface          = Color.White,
    onSurface        = Color(0xFF0B0B17),
    surfaceVariant   = Color(0xFFEFEFF5),
    onSurfaceVariant = Color(0xFF5A5A72),
    outline          = Color(0xFFD0D0E0)
)

/** Propagat prin CompositionLocalProvider de DataAgregateTheme pentru a putea fi citit din helpers. */
val LocalIsDarkTheme = compositionLocalOf { true }

// ─── Theme helpers — colors that auto-switch on light/dark ────────────────────
@Composable @ReadOnlyComposable
fun appBg(): Color = if (LocalIsDarkTheme.current) Color(0xFF0F0F17) else Color(0xFFF5F5FA)

@Composable @ReadOnlyComposable
fun appOnBg(): Color = if (LocalIsDarkTheme.current) Color.White else Color(0xFF0B0B17)

@Composable @ReadOnlyComposable
fun appCardBg(): Color = if (LocalIsDarkTheme.current) Color(0xFF1E1E2E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.95f)

@Composable @ReadOnlyComposable
fun appCardBorder(): Color = if (LocalIsDarkTheme.current) Color.White.copy(alpha = 0.05f) else Color(0xFF0B0B17).copy(alpha = 0.06f)

@Composable @ReadOnlyComposable
fun appTextSec(): Color = if (LocalIsDarkTheme.current) Color.LightGray else Color(0xFF5A5A72)

@Composable @ReadOnlyComposable
fun appTextMuted(): Color = if (LocalIsDarkTheme.current) Color.LightGray.copy(alpha = 0.7f) else Color(0xFF5A5A72).copy(alpha = 0.75f)

@Composable @ReadOnlyComposable
fun appBgImageAlpha(): Float = if (LocalIsDarkTheme.current) 0.4f else 0.10f

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
