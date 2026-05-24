package com.podut.dataagregate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val AppPurple        = Color(0xFF7C3AED)
val AppBackground    = Color(0xFF0B0B17)
val AppSurface       = Color(0xFF13131F)
val AppSurfaceVar    = Color(0xFF1C1C2E)
val AppTextGray      = Color(0xFF9CA3AF)
val AppBorder        = Color(0xFF2D2D3D)
val AppScoreGreen    = Color(0xFF22C55E)

private val DarkScheme = darkColorScheme(
    primary           = AppPurple,
    onPrimary         = Color.White,
    background        = AppBackground,
    onBackground      = Color.White,
    surface           = AppSurface,
    onSurface         = Color.White,
    surfaceVariant    = AppSurfaceVar,
    onSurfaceVariant  = AppTextGray,
    outline           = AppBorder
)

private val LightScheme = lightColorScheme(
    primary           = AppPurple,
    onPrimary         = Color.White,
    background        = Color(0xFFF5F5FA),
    onBackground      = Color(0xFF0B0B17),
    surface           = Color.White,
    onSurface         = Color(0xFF0B0B17),
    surfaceVariant    = Color(0xFFEFEFF5),
    onSurfaceVariant  = Color(0xFF5A5A72),
    outline           = Color(0xFFD0D0E0)
)

val LocalIsDarkTheme = compositionLocalOf { true }

@Composable
fun DataAgregateTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        content     = content
    )
}
