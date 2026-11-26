package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class SpatialUiConfig(
    val isSpatialModeActive: Boolean = false,
    val isTrueBlackOled: Boolean = false,
    val overscanPadding: Dp = 0.dp,
    val isZeroGravityLocked: Boolean = false,
    val isEyesFreeFeedbackEnabled: Boolean = true,
    val hasExternalDisplay: Boolean = false,
    val externalDisplayName: String = "",
    val isInMultiWindow: Boolean = false
)

val LocalSpatialConfig = staticCompositionLocalOf { SpatialUiConfig() }

private val ProgressorColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = VoidDark,
    primaryContainer = SurfaceCardHighlight,
    onPrimaryContainer = CyanAccent,
    secondary = PurpleTheorist,
    onSecondary = VoidDark,
    secondaryContainer = SurfaceCard,
    onSecondaryContainer = PurpleTheoristLight,
    tertiary = AmberSkeptic,
    onTertiary = VoidDark,
    tertiaryContainer = SurfaceCard,
    onTertiaryContainer = AmberSkepticLight,
    background = VoidDark,
    onBackground = TextPrimary,
    surface = DeepSpace,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle,
    error = RoseWarning,
    onError = VoidDark
)

// Strict #000000 True Black palette for Micro-OLED Birdbath Optics (XREAL Air 2 Pro)
private val TrueBlackOledColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = TrueBlackOled,
    primaryContainer = TrueBlackOled,
    onPrimaryContainer = CyanAccent,
    secondary = PurpleTheorist,
    onSecondary = TrueBlackOled,
    secondaryContainer = TrueBlackOled,
    onSecondaryContainer = PurpleTheoristLight,
    tertiary = AmberSkeptic,
    onTertiary = TrueBlackOled,
    tertiaryContainer = TrueBlackOled,
    onTertiaryContainer = AmberSkepticLight,
    background = TrueBlackOled,
    onBackground = TextPrimary,
    surface = TrueBlackOled,
    onSurface = TextPrimary,
    surfaceVariant = TrueBlackOled,
    onSurfaceVariant = TextSecondary,
    outline = OledBorderCrisp,
    outlineVariant = OledBorderCrisp,
    error = RoseWarning,
    onError = TrueBlackOled
)

@Composable
fun spatialCanvasColor(): Color {
    return if (LocalSpatialConfig.current.isTrueBlackOled) TrueBlackOled else VoidDark
}

@Composable
fun spatialSurfaceColor(): Color {
    return if (LocalSpatialConfig.current.isTrueBlackOled) TrueBlackOled else DeepSpace
}

@Composable
fun spatialCardColor(): Color {
    return if (LocalSpatialConfig.current.isTrueBlackOled) TrueBlackOled else SurfaceCard
}

@Composable
fun spatialCardHighlightColor(): Color {
    return if (LocalSpatialConfig.current.isTrueBlackOled) TrueBlackOled else SurfaceCardHighlight
}

@Composable
fun spatialBorderColor(defaultColor: Color = BorderSubtle): Color {
    return if (LocalSpatialConfig.current.isTrueBlackOled) {
        if (defaultColor == BorderSubtle) OledBorderCrisp else defaultColor
    } else {
        defaultColor
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    spatialConfig: SpatialUiConfig = SpatialUiConfig(),
    content: @Composable () -> Unit
) {
    val activeColorScheme = if (spatialConfig.isTrueBlackOled) {
        TrueBlackOledColorScheme
    } else {
        ProgressorColorScheme
    }

    val barColor = if (spatialConfig.isTrueBlackOled) TrueBlackOled else VoidDark
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = barColor.toArgb()
                window.navigationBarColor = barColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(LocalSpatialConfig provides spatialConfig) {
        MaterialTheme(
            colorScheme = activeColorScheme,
            typography = Typography,
            content = content
        )
    }
}
