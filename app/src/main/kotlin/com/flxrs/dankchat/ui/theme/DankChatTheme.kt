package com.flxrs.dankchat.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flxrs.dankchat.preferences.appearance.AccentColor
import com.flxrs.dankchat.preferences.appearance.AppearanceSettings
import com.flxrs.dankchat.preferences.appearance.AppearanceSettingsDataStore
import com.flxrs.dankchat.preferences.appearance.PaletteStylePreference
import com.flxrs.dankchat.preferences.appearance.ThemePreference
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import org.koin.compose.koinInject

data class AdaptiveColors(
    val onSurfaceLight: Color,
    val onSurfaceDark: Color,
)

val LocalAdaptiveColors =
    staticCompositionLocalOf {
        AdaptiveColors(
            onSurfaceLight = lightColorScheme().onSurface,
            onSurfaceDark = darkColorScheme().onSurface,
        )
    }

@Composable
fun DankChatTheme(content: @Composable () -> Unit) {
    val inspectionMode = LocalInspectionMode.current
    val appearanceSettings = if (!inspectionMode) koinInject<AppearanceSettingsDataStore>() else null
    val settings by appearanceSettings?.settings?.collectAsStateWithLifecycle(
        initialValue = remember { appearanceSettings.current() },
    ) ?: remember { androidx.compose.runtime.mutableStateOf(AppearanceSettings()) }

    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (settings.theme) {
        ThemePreference.System -> systemDarkTheme
        ThemePreference.Dark -> true
        ThemePreference.Light -> false
    }
    val accentColor = settings.accentColor
    val trueDarkTheme = settings.trueDarkTheme && darkTheme
    val paletteStyle = settings.paletteStyle.toPaletteStyle()
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val useSystemColors = accentColor == null && settings.paletteStyle == PaletteStylePreference.SystemDefault
    val seedColor = accentColor?.seedColor
        ?: if (dynamicColor) dynamicLightColorScheme(LocalContext.current).primary else null

    val lightColorScheme = when {
        seedColor != null && !useSystemColors -> rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = false,
            style = paletteStyle,
        )

        dynamicColor -> dynamicLightColorScheme(LocalContext.current)

        else -> expressiveLightColorScheme()
    }

    val darkColorScheme = when {
        seedColor != null && !useSystemColors -> {
            val seededScheme = rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = true,
                isAmoled = trueDarkTheme,
                style = paletteStyle,
            )
            // A tonal palette would mute the accent, so the default look keeps its exact colors
            val isCitrusLook = accentColor == AccentColor.Citrus && paletteStyle == PaletteStyle.TonalSpot
            remember(seededScheme, isCitrusLook, trueDarkTheme) {
                when {
                    isCitrusLook -> seededScheme.withCitrusDarkColors(trueDarkTheme)
                    else -> seededScheme
                }
            }
        }

        dynamicColor && trueDarkTheme -> dynamicDarkColorScheme(LocalContext.current).copy(
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF222222),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF0E0E0E),
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1C1C1C),
            background = Color.Black,
            onSurface = Color.White,
            onBackground = Color.White,
        )

        dynamicColor -> dynamicDarkColorScheme(LocalContext.current)

        else -> darkColorScheme()
    }

    val adaptiveColors =
        AdaptiveColors(
            onSurfaceLight = lightColorScheme.onSurface,
            onSurfaceDark = darkColorScheme.onSurface,
        )
    val colors = if (darkTheme) darkColorScheme else lightColorScheme
    MaterialExpressiveTheme(
        colorScheme = colors,
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(LocalAdaptiveColors provides adaptiveColors) {
            content()
        }
    }
}

val ColorScheme.toolbarPillColor: Color
    get() = surfaceContainerHigh

private val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(10.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )

private fun ColorScheme.withCitrusDarkColors(trueDark: Boolean): ColorScheme {
    val ground = if (trueDark) Color.Black else Color(0xFF101214)
    return copy(
        primary = Color(0xFFC6E34B),
        onPrimary = Color(0xFF14160C),
        primaryContainer = Color(0xFF3A4310),
        onPrimaryContainer = Color(0xFFE3F59B),
        background = ground,
        onBackground = Color(0xFFE7E9EC),
        surface = ground,
        onSurface = Color(0xFFE7E9EC),
        surfaceVariant = Color(0xFF2A2E34),
        onSurfaceVariant = Color(0xFFAEB4BD),
        surfaceDim = ground,
        surfaceBright = Color(0xFF2A2E34),
        surfaceContainerLowest = if (trueDark) Color.Black else Color(0xFF0B0C0E),
        surfaceContainerLow = if (trueDark) Color(0xFF0A0A0A) else Color(0xFF14171A),
        surfaceContainer = if (trueDark) Color(0xFF0E0E0E) else Color(0xFF181B1F),
        surfaceContainerHigh = if (trueDark) Color(0xFF141414) else Color(0xFF1E2227),
        surfaceContainerHighest = if (trueDark) Color(0xFF1C1C1C) else Color(0xFF23272C),
        outline = Color(0xFF5A616B),
        outlineVariant = Color(0xFF2A2E34),
    )
}

private fun PaletteStylePreference.toPaletteStyle(): PaletteStyle = when (this) {
    PaletteStylePreference.SystemDefault -> PaletteStyle.TonalSpot
    PaletteStylePreference.TonalSpot -> PaletteStyle.TonalSpot
    PaletteStylePreference.Neutral -> PaletteStyle.Neutral
    PaletteStylePreference.Vibrant -> PaletteStyle.Vibrant
    PaletteStylePreference.Expressive -> PaletteStyle.Expressive
    PaletteStylePreference.Rainbow -> PaletteStyle.Rainbow
    PaletteStylePreference.FruitSalad -> PaletteStyle.FruitSalad
    PaletteStylePreference.Monochrome -> PaletteStyle.Monochrome
    PaletteStylePreference.Fidelity -> PaletteStyle.Fidelity
    PaletteStylePreference.Content -> PaletteStyle.Content
}
