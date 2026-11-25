package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark laboratory theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = VoidDark.toArgb()
                window.navigationBarColor = VoidDark.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ProgressorColorScheme,
        typography = Typography,
        content = content
    )
}
