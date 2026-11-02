package com.digitaldost.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun DigitalDostTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Color(0xFF0B6B5A),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE2F4EF),
        onPrimaryContainer = Color(0xFF083E34),
        secondary = Color(0xFFF5A623),
        secondaryContainer = Color(0xFFFFEED4),
        onSecondaryContainer = Color(0xFF4A2B00),
        tertiary = Color(0xFF8D6E63),
        background = Color(0xFFF7F9F8),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEAF0EE),
        onSurface = Color(0xFF17201E),
        onSurfaceVariant = Color(0xFF626C69),
        outline = Color(0xFFD9E1DF),
        outlineVariant = Color(0xFFE7ECEB),
        error = Color(0xFFC84A4A),
        errorContainer = Color(0xFFFFE8E6)
    )
    val typography = Typography(
        headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
