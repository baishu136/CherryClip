package com.cherryclip.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cherryclip.app.model.ThemeMode

private val CherryTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
    )
)

object CherryTheme {
    val palette: CherryPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalCherryPalette.current
}

@Composable
fun CherryClipTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    updateSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val palette = if (isDark) DarkCherryPalette else LightCherryPalette

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.primaryForeground,
            secondary = palette.secondary,
            onSecondary = palette.foreground,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.card,
            onSurface = palette.foreground,
            surfaceVariant = palette.codeBlock,
            onSurfaceVariant = palette.mutedForeground,
            outline = palette.border,
            error = palette.destructive
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.primaryForeground,
            secondary = palette.secondary,
            onSecondary = palette.foreground,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.card,
            onSurface = palette.foreground,
            surfaceVariant = palette.codeBlock,
            onSurfaceVariant = palette.mutedForeground,
            outline = palette.border,
            error = palette.destructive
        )
    }

    if (updateSystemBars) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalCherryPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CherryTypography,
            content = content
        )
    }
}
