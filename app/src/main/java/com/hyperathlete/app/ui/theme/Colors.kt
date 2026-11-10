package com.hyperathlete.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp

/**
 * Colour tokens. The light palette is the Calisthenics palette (white + orange + neutrals);
 * the dark palette keeps the same hierarchy on near-black surfaces. Accent-derived tones
 * (deep / light / soft) are computed so a custom accent colour re-skins the whole app.
 */
@Immutable
data class HyperColors(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val surfaceRaised: Color,
    val border: Color,
    val borderStrong: Color,
    val text: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentDeep: Color,
    val accentLight: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val secondary: Color,
    val secondarySoft: Color,
    val lock: Color,
    val lockSoft: Color,
    val success: Color,
    val successSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val scrim: Color,
    val shadow: Color,
)

object Palette {
    val Orange = Color(0xFFFF6B00)
    val Blue = Color(0xFF2F6BFF)

    /** Ready-made accent presets offered in Settings. First one is the Calisthenics default. */
    val AccentPresets = listOf(
        Color(0xFFFF6B00), // Calisthenics orange
        Color(0xFFE5484D), // red
        Color(0xFFEC4899), // pink
        Color(0xFF8B5CF6), // violet
        Color(0xFF2F6BFF), // blue
        Color(0xFF0EA5E9), // sky
        Color(0xFF10B981), // emerald
        Color(0xFFF5B400), // amber
    )

    val SecondaryPresets = listOf(
        Color(0xFF2F6BFF),
        Color(0xFF10B981),
        Color(0xFF8B5CF6),
        Color(0xFF0EA5E9),
        Color(0xFFF5B400),
        Color(0xFF64748B),
    )
}

/** Ready-made themes: a light/dark mode plus a main and secondary colour. First = Calisthenics (default). */
data class ThemePreset(val id: String, val mode: com.hyperathlete.app.domain.model.ThemeMode, val accent: Color, val secondary: Color) {
    val accentArgb: Long get() = argb(accent)
    val secondaryArgb: Long get() = argb(secondary)
    fun matches(mode: com.hyperathlete.app.domain.model.ThemeMode, accent: Long, secondary: Long) =
        mode == this.mode && (accent and 0xFFFFFFFFL) == accentArgb && (secondary and 0xFFFFFFFFL) == secondaryArgb

    companion object {
        private fun argb(c: Color): Long = (c.value shr 32).toLong() and 0xFFFFFFFFL
        private val L = com.hyperathlete.app.domain.model.ThemeMode.LIGHT
        private val D = com.hyperathlete.app.domain.model.ThemeMode.DARK
        val All = listOf(
            ThemePreset("calisthenics", L, Color(0xFFFF6B00), Color(0xFF2F6BFF)),
            ThemePreset("night", D, Color(0xFFFF6B00), Color(0xFF2F6BFF)),
            ThemePreset("ocean", L, Color(0xFF0EA5E9), Color(0xFF10B981)),
            ThemePreset("forest", L, Color(0xFF10B981), Color(0xFFF5B400)),
            ThemePreset("midnight", D, Color(0xFF8B5CF6), Color(0xFF0EA5E9)),
            ThemePreset("rose", L, Color(0xFFEC4899), Color(0xFF8B5CF6)),
        )
    }
}

fun hyperColors(
    dark: Boolean,
    accent: Color = Palette.Orange,
    secondary: Color = Palette.Blue,
): HyperColors = if (!dark) {
    HyperColors(
        isDark = false,
        // Liquid Glass canvas (Saja --bg); cards are frosted white on top of it.
        bg = Color(0xFFF2F3F8),
        surface = Color(0xFFFFFFFF),
        surfaceAlt = Color(0xFFF6F7F9),
        surfaceRaised = Color(0xFFFFFFFF),
        border = Color(0xFFEDEEF2),
        borderStrong = Color(0xFFDFE2E8),
        text = Color(0xFF101114),
        // WCAG AA (≥ 4.5:1) on glass over the canvas with any accent preset.
        textSecondary = Color(0xFF4C525D),
        textMuted = Color(0xFF59606B),
        accent = accent,
        accentDeep = lerp(accent, Color.Black, 0.09f),
        accentLight = lerp(accent, Color.White, 0.25f),
        accentSoft = lerp(accent, Color.White, 0.92f),
        onAccent = Color.White,
        secondary = secondary,
        secondarySoft = lerp(secondary, Color.White, 0.9f),
        lock = Color(0xFFB4BAC4),
        lockSoft = Color(0xFFF1F2F5),
        success = Color(0xFF17A34A),
        successSoft = Color(0xFFE9F7EE),
        warn = Color(0xFFB45309),
        warnSoft = Color(0xFFFFF6E0),
        danger = Color(0xFFDC2626),
        dangerSoft = Color(0xFFFDECEC),
        scrim = Color(0x66101114),
        shadow = Color(0x14000000),
    )
} else {
    val surface = Color(0xFF17181C)
    HyperColors(
        isDark = true,
        bg = Color(0xFF08090C),
        surface = surface,
        surfaceAlt = Color(0xFF1F2126),
        surfaceRaised = Color(0xFF1C1D22),
        border = Color(0xFF26282E),
        borderStrong = Color(0xFF33363D),
        text = Color(0xFFF4F5F7),
        textSecondary = Color(0xFFB4B9C2),
        textMuted = Color(0xFF9EA3AD),
        accent = accent,
        accentDeep = lerp(accent, Color.Black, 0.12f),
        accentLight = lerp(accent, Color.White, 0.2f),
        accentSoft = accent.copy(alpha = 0.16f).compositeOver(surface),
        onAccent = Color.White,
        secondary = secondary,
        secondarySoft = secondary.copy(alpha = 0.16f).compositeOver(surface),
        lock = Color(0xFF5B606A),
        lockSoft = Color(0xFF22242A),
        success = Color(0xFF34C46A),
        successSoft = Color(0x2934C46A).compositeOver(surface),
        warn = Color(0xFFF5B400),
        warnSoft = Color(0x24F5B400).compositeOver(surface),
        danger = Color(0xFFF87171),
        dangerSoft = Color(0x26F87171).compositeOver(surface),
        scrim = Color(0x99000000),
        shadow = Color(0x40000000),
    )
}
