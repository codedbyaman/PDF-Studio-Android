package com.example.androidpdfstudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary            = StudioAccent,
    secondary          = StudioTeal,
    tertiary           = Color(0xFF8B5CF6),
    background         = NavyDark,
    surface            = NavyMedium,
    surfaceVariant     = Color(0xFF1E293B),
    onPrimary          = Color.White,
    onSecondary        = Color.White,
    onTertiary         = Color.White,
    onBackground       = Color.White,
    onSurface          = Color.White,
    onSurfaceVariant   = Color(0xFFCBD5E1),
    outline            = Color(0xFF334155),
    error              = StudioRed,
    onError            = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary            = StudioAccent,
    secondary          = StudioTeal,
    tertiary           = Color(0xFF7C3AED),
    background         = Color(0xFFF8FAFC),
    surface            = Color.White,
    surfaceVariant     = Color(0xFFF1F5F9),
    onPrimary          = Color.White,
    onSecondary        = Color.White,
    onTertiary         = Color.White,
    onBackground       = Color(0xFF0F172A),
    onSurface          = Color(0xFF0F172A),
    onSurfaceVariant   = Color(0xFF64748B),
    outline            = Color(0xFFE2E8F0),
    error              = StudioRed,
    onError            = Color.White,
)

@Composable
fun AndroidPDFStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
