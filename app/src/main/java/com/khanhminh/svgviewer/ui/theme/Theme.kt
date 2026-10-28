package com.khanhminh.svgviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5A54E8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E3FF),
    onPrimaryContainer = Color(0xFF16124A),
    secondary = Color(0xFF0E9F9A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC9F5F0),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFFE0577A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF111118),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111118),
    surfaceVariant = Color(0xFFEDECF6),
    onSurfaceVariant = Color(0xFF4B4A5C),
    outline = Color(0xFFC7C6D6),
    outlineVariant = Color(0xFFDEDDEA),
    error = Color(0xFFC0392B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4B0FF),
    onPrimary = Color(0xFF1A1642),
    primaryContainer = Color(0xFF322B7A),
    onPrimaryContainer = Color(0xFFE6E3FF),
    secondary = Color(0xFF6FE3D6),
    onSecondary = Color(0xFF00312D),
    secondaryContainer = Color(0xFF134943),
    onSecondaryContainer = Color(0xFFC9F5F0),
    tertiary = Color(0xFFFFA0B8),
    onTertiary = Color(0xFF3A0B1C),
    background = Color(0xFF08080D),
    onBackground = Color(0xFFEDEDF5),
    surface = Color(0xFF101018),
    onSurface = Color(0xFFEDEDF5),
    surfaceVariant = Color(0xFF1B1B26),
    onSurfaceVariant = Color(0xFFB6B5C8),
    outline = Color(0xFF3B3A4B),
    outlineVariant = Color(0xFF26252F),
    error = Color(0xFFFF8A9B)
)

/** Bảng màu trang trí dùng cho gradient / hiệu ứng nền. */
data class AccentPalette(
    val violet: Color,
    val indigo: Color,
    val cyan: Color,
    val mint: Color,
    val rose: Color,
    val amber: Color,
    val glow: Color
)

val LocalAccent = staticCompositionLocalOf {
    AccentPalette(
        violet = Color(0xFF7C6BFF),
        indigo = Color(0xFF4A3BD6),
        cyan = Color(0xFF3BC9E8),
        mint = Color(0xFF4FE0C4),
        rose = Color(0xFFFF8FB0),
        amber = Color(0xFFFFC46B),
        glow = Color(0x335A54E8)
    )
}

private val DarkAccent = AccentPalette(
    violet = Color(0xFF9C8CFF),
    indigo = Color(0xFF5A4AE8),
    cyan = Color(0xFF54D8F0),
    mint = Color(0xFF63EFD2),
    rose = Color(0xFFFF9EBC),
    amber = Color(0xFFFFD08A),
    glow = Color(0x447C6BFF)
)

private val LightAccent = AccentPalette(
    violet = Color(0xFF6C5CE7),
    indigo = Color(0xFF4A3BD6),
    cyan = Color(0xFF0EA5C4),
    mint = Color(0xFF12B39B),
    rose = Color(0xFFE0577A),
    amber = Color(0xFFE09A34),
    glow = Color(0x225A54E8)
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

@Composable
fun SvgViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalAccent provides if (darkTheme) DarkAccent else LightAccent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
