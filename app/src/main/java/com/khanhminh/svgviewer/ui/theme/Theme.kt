package com.khanhminh.svgviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = SurfaceDarkRaised,
    onPrimaryContainer = Paper,
    secondary = Paper,
    onSecondary = Ink,
    secondaryContainer = SurfaceDarkRaised,
    onSecondaryContainer = Paper,
    tertiary = Paper,
    onTertiary = Ink,
    tertiaryContainer = SurfaceDarkRaised,
    onTertiaryContainer = Paper,
    background = Ink,
    onBackground = Paper,
    surface = SurfaceDark,
    onSurface = Paper,
    surfaceDim = Ink,
    surfaceBright = SurfaceDarkRaised,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Ink,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceDark,
    surfaceContainerHighest = SurfaceDarkRaised,
    surfaceTint = Paper,
    surfaceVariant = SurfaceDarkRaised,
    onSurfaceVariant = MutedDark,
    outline = BorderDark,
    outlineVariant = BorderDark,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    inversePrimary = InkNear,
    error = ErrorDark,
    onError = Ink,
    errorContainer = SurfaceDarkRaised,
    onErrorContainer = ErrorDark,
    scrim = Ink
)

private val LightScheme = lightColorScheme(
    primary = InkNear,
    onPrimary = Paper,
    primaryContainer = SurfaceLightRaised,
    onPrimaryContainer = InkNear,
    secondary = InkNear,
    onSecondary = Paper,
    secondaryContainer = SurfaceLightRaised,
    onSecondaryContainer = InkNear,
    tertiary = InkNear,
    onTertiary = Paper,
    tertiaryContainer = SurfaceLightRaised,
    onTertiaryContainer = InkNear,
    background = Paper,
    onBackground = InkNear,
    surface = SurfaceLight,
    onSurface = InkNear,
    surfaceDim = SurfaceLightRaised,
    surfaceBright = Paper,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = SurfaceLight,
    surfaceContainerHigh = SurfaceLight,
    surfaceContainerHighest = SurfaceLightRaised,
    surfaceTint = InkNear,
    surfaceVariant = SurfaceLightRaised,
    onSurfaceVariant = MutedLight,
    outline = BorderLight,
    outlineVariant = BorderLight,
    inverseSurface = InkNear,
    inverseOnSurface = Paper,
    inversePrimary = Paper,
    error = ErrorLight,
    onError = Paper,
    errorContainer = Color(0xFFFFE9E7),
    onErrorContainer = ErrorLight,
    scrim = Ink
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Theme duy nhất của app: đen - trắng - xám, không dynamic color.
 * Cả Light và Dark đều được thiết kế riêng, không phải đảo màu.
 */
@Composable
fun SvgViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
