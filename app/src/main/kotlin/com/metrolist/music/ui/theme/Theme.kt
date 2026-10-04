/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

/** LoveMusic signature pink. Also the sentinel meaning "use the built-in LoveMusic palette". */
val DefaultThemeColor = Color(0xFFFF4D8D)

/** Hand-tuned LoveMusic palettes (plum night / blush day) used when no custom seed colour is chosen. */
val LoveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF7AA8),
    onPrimary = Color(0xFF4A0A27),
    primaryContainer = Color(0xFF6B1A3D),
    onPrimaryContainer = Color(0xFFFFD9E5),
    secondary = Color(0xFFCBB2F5),
    onSecondary = Color(0xFF2E1A52),
    secondaryContainer = Color(0xFF433069),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = Color(0xFFFFB784),
    onTertiary = Color(0xFF4A2500),
    tertiaryContainer = Color(0xFF6A3A12),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF120816),
    onBackground = Color(0xFFF4E6F0),
    surface = Color(0xFF120816),
    onSurface = Color(0xFFF4E6F0),
    surfaceVariant = Color(0xFF3F2E48),
    onSurfaceVariant = Color(0xFFD3BDD0),
    surfaceTint = Color(0xFFFF7AA8),
    inverseSurface = Color(0xFFF4E6F0),
    inverseOnSurface = Color(0xFF35243B),
    inversePrimary = Color(0xFFB4275C),
    outline = Color(0xFF9B8499),
    outlineVariant = Color(0xFF4F3C57),
    surfaceDim = Color(0xFF120816),
    surfaceBright = Color(0xFF3A2849),
    surfaceContainerLowest = Color(0xFF0E0511),
    surfaceContainerLow = Color(0xFF1A0E20),
    surfaceContainer = Color(0xFF20132A),
    surfaceContainerHigh = Color(0xFF2A1B36),
    surfaceContainerHighest = Color(0xFF352445),
)

val LoveLightColorScheme = lightColorScheme(
    primary = Color(0xFFB8174F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E4),
    onPrimaryContainer = Color(0xFF3F0020),
    secondary = Color(0xFF6B4F9A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBDDFF),
    onSecondaryContainer = Color(0xFF25104A),
    tertiary = Color(0xFF8B4E1A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2F1500),
    background = Color(0xFFFFF7FA),
    onBackground = Color(0xFF26151F),
    surface = Color(0xFFFFF7FA),
    onSurface = Color(0xFF26151F),
    surfaceVariant = Color(0xFFF4DCE6),
    onSurfaceVariant = Color(0xFF52404B),
    surfaceTint = Color(0xFFB8174F),
    inverseSurface = Color(0xFF3B292F),
    inverseOnSurface = Color(0xFFFFECF3),
    inversePrimary = Color(0xFFFF7AA8),
    outline = Color(0xFF85707B),
    outlineVariant = Color(0xFFD7C0CB),
    surfaceDim = Color(0xFFEBD6DF),
    surfaceBright = Color(0xFFFFF7FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF0F5),
    surfaceContainer = Color(0xFFFBE8F0),
    surfaceContainerHigh = Color(0xFFF5DFEA),
    surfaceContainerHighest = Color(0xFFEFD7E4),
)

/** Rounder, softer corners across cards, sheets and dialogs. */
val LoveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val BaseTypography = Typography()

/** Bolder titles, tighter tracking: a more editorial, "music app" voice. */
val LoveTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    displayMedium = BaseTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun LoveMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // Default seed -> the hand-tuned LoveMusic palette. Any other seed (palette pick or album art) -> generated scheme.
    val baseColorScheme = if (themeColor == DefaultThemeColor) {
        if (darkTheme) LoveDarkColorScheme else LoveLightColorScheme
    } else {
        rememberDynamicColorScheme(
            seedColor = themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LoveTypography,
        shapes = LoveShapes,
        content = content,
    )
}

/** Kept so existing call sites keep compiling. */
@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) = LoveMusicTheme(darkTheme, pureBlack, themeColor, content)

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
