package com.gfpgan_android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Dark Mode Color Scheme
 * Background: #131313
 * Text: #FEFBEB
 */
private val DarkColorScheme = darkColorScheme(
    primary = DarkText,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkBackground,
    onSurface = DarkText,
    secondary = DarkText,
    onSecondary = DarkBackground,
    tertiary = DarkText,
    onTertiary = DarkBackground
)

/**
 * Light Mode Color Scheme  
 * Background: #FEFBEB
 * Text: #131313
 */
private val LightColorScheme = lightColorScheme(
    primary = LightText,
    onPrimary = LightBackground,
    background = LightBackground,
    onBackground = LightText,
    surface = LightBackground,
    onSurface = LightText,
    secondary = LightText,
    onSecondary = LightBackground,
    tertiary = LightText,
    onTertiary = LightBackground
)

/**
 * GFPGAN App Theme
 * 
 * @param darkTheme Dark mode aktif mi? (default: sistem ayarı)
 * @param content Composable content
 */
@Composable
fun GfpganTheme(
    darkTheme: Boolean = true, // Force Dark Mode default
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Always use Dark Scheme

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as android.app.Activity).window
            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false // Dark background -> Light icons
                isAppearanceLightNavigationBars = false // Dark background -> Light icons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}