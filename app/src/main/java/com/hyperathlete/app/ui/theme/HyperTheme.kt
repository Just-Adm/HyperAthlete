package com.hyperathlete.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.hyperathlete.app.domain.model.ThemeMode

val LocalHyperColors = staticCompositionLocalOf { hyperColors(dark = false) }
val LocalHyperType = staticCompositionLocalOf { hyperType() }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Kilograms or pounds for every weight on screen (the database always keeps kg). */
val LocalWeightUnit = staticCompositionLocalOf { com.hyperathlete.app.domain.model.WeightUnit.KG }

/** Short accessors: `Hyper.colors.accent`, `Hyper.type.title`. */
object Hyper {
    val colors: HyperColors @Composable get() = LocalHyperColors.current
    val type: HyperType @Composable get() = LocalHyperType.current
    val glass: HyperGlass @Composable get() = LocalHyperGlass.current
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun HyperTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accent: Color = Palette.Orange,
    secondary: Color = Palette.Blue,
    textScale: Float = 1f,
    hapticsEnabled: Boolean = true,
    weightUnit: com.hyperathlete.app.domain.model.WeightUnit = com.hyperathlete.app.domain.model.WeightUnit.KG,
    content: @Composable () -> Unit,
) {
    val dark = isDarkTheme(themeMode)
    val colors = remember(dark, accent, secondary) { hyperColors(dark, accent, secondary) }
    val glass = remember(dark, accent, secondary) { hyperGlass(dark, accent, secondary) }
    val type = remember { hyperType() }
    val density = LocalDensity.current
    val scaledDensity = remember(density, textScale) {
        Density(density.density, density.fontScale * textScale)
    }
    MaterialTheme(
        colorScheme = colors.toMaterial(),
        typography = type.toMaterial(),
    ) {
        CompositionLocalProvider(
            LocalHyperColors provides colors,
            LocalHyperType provides type,
            LocalHyperGlass provides glass,
            LocalHapticsEnabled provides hapticsEnabled,
            LocalWeightUnit provides weightUnit,
            LocalDensity provides scaledDensity,
            content = content,
        )
    }
}

/** Map our tokens onto Material 3 so stock pickers and sheets inherit the theme. */
private fun HyperColors.toMaterial(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accentDeep,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = accentSoft,
        onSecondaryContainer = text,
        tertiary = secondary,
        background = bg,
        onBackground = text,
        surface = surface,
        onSurface = text,
        surfaceVariant = surfaceAlt,
        onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceAlt,
        surfaceTint = Color.Transparent,
        outline = borderStrong,
        outlineVariant = border,
        error = danger,
        onError = Color.White,
        scrim = scrim,
    )
}

private fun HyperType.toMaterial(): Typography = Typography(
    displayLarge = display, displayMedium = display, displaySmall = h1,
    headlineLarge = h1, headlineMedium = h1, headlineSmall = h2,
    titleLarge = h2, titleMedium = title, titleSmall = smallMedium,
    bodyLarge = body, bodyMedium = body, bodySmall = small,
    labelLarge = bodyMedium, labelMedium = smallMedium, labelSmall = micro,
)
