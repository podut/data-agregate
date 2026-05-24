package com.podut.dataagregate.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.podut.dataagregate.core.ui.R

val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_bold, FontWeight.Bold),
    Font(R.font.sora_extrabold, FontWeight.ExtraBold)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = Sora),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = Sora),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = Sora),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = Sora),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = Sora),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = Sora),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = Sora),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = Sora),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = Sora),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = Sora),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = Sora),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = Sora),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = Sora),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = Sora),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = Sora)
)
