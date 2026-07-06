package com.streamcast.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    secondary = SecondaryAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = TextPrimary,
    onSecondary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

@Composable
fun StreamCastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // StreamCast is dark-first as per docs, we could enforce dark mode or allow system
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Enforce dark for now

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
