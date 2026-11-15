package com.cherryclip.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CherryPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundSubtle: Color,
    val card: Color,
    val codeBlock: Color,
    val foreground: Color,
    val mutedForeground: Color,
    val foregroundTertiary: Color,
    val primary: Color,
    val primaryForeground: Color,
    val primarySoft: Color,
    val brand: Color,
    val brandSoft: Color,
    val border: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val secondary: Color,
    val secondaryActive: Color,
    val destructive: Color,
    val destructiveSoft: Color,
    val tagBlue: Color,
    val tagBlueFg: Color,
    val tagGreen: Color,
    val tagGreenFg: Color,
    val tagAmber: Color,
    val tagAmberFg: Color,
    val tagRed: Color,
    val tagRedFg: Color,
    val tagViolet: Color,
    val tagVioletFg: Color,
    val scrim: Color
)

val LightCherryPalette = CherryPalette(
    isDark = false,
    background = Color(0xFFF5F5F5),
    backgroundSubtle = Color(0xFFFAFAFA),
    card = Color(0xFFFFFFFF),
    codeBlock = Color(0xFFF2F2F2),
    foreground = Color(0xFF171717),
    mutedForeground = Color(0xFF525252),
    foregroundTertiary = Color(0xFF858585),
    primary = Color(0xFF128846),
    primaryForeground = Color(0xFFFFFFFF),
    primarySoft = Color(0x1A128846),
    brand = Color(0xFFFF5757),
    brandSoft = Color(0x1AFF5757),
    border = Color(0x14000000),
    borderSubtle = Color(0x0D000000),
    borderStrong = Color(0x36000000),
    secondary = Color(0x0D000000),
    secondaryActive = Color(0x1A000000),
    destructive = Color(0xFFDC2626),
    destructiveSoft = Color(0x14DC2626),
    tagBlue = Color(0xFFEEF5FF),
    tagBlueFg = Color(0xFF1D4ED8),
    tagGreen = Color(0xFFECFDF3),
    tagGreenFg = Color(0xFF128846),
    tagAmber = Color(0xFFFFFBEB),
    tagAmberFg = Color(0xFFB45309),
    tagRed = Color(0xFFFEF2F2),
    tagRedFg = Color(0xFFDC2626),
    tagViolet = Color(0xFFF5F3FF),
    tagVioletFg = Color(0xFF6D28D9),
    scrim = Color(0x66000000)
)

val DarkCherryPalette = CherryPalette(
    isDark = true,
    background = Color(0xFF000000),
    backgroundSubtle = Color(0xFF0A0A0A),
    card = Color(0xFF171717),
    codeBlock = Color(0xFF202020),
    foreground = Color(0xFFF0F0F0),
    mutedForeground = Color(0xFFA1A1A1),
    foregroundTertiary = Color(0xFF737373),
    primary = Color(0xFF28C96D),
    primaryForeground = Color(0xFF000000),
    primarySoft = Color(0x2428C96D),
    brand = Color(0xFFFF5757),
    brandSoft = Color(0x24FF5757),
    border = Color(0x1AFFFFFF),
    borderSubtle = Color(0x12FFFFFF),
    borderStrong = Color(0x3DFFFFFF),
    secondary = Color(0x14FFFFFF),
    secondaryActive = Color(0x24FFFFFF),
    destructive = Color(0xFFF87171),
    destructiveSoft = Color(0x24EF4444),
    tagBlue = Color(0xFF132238),
    tagBlueFg = Color(0xFF60A5FA),
    tagGreen = Color(0xFF102A1C),
    tagGreenFg = Color(0xFF4ADE80),
    tagAmber = Color(0xFF2B1F0E),
    tagAmberFg = Color(0xFFFBBF24),
    tagRed = Color(0xFF2E1416),
    tagRedFg = Color(0xFFF87171),
    tagViolet = Color(0xFF211738),
    tagVioletFg = Color(0xFFA78BFA),
    scrim = Color(0x88000000)
)

object CherryDimens {
    val RadiusSm = 6.dp
    val RadiusMd = 10.dp
    val RadiusLg = 14.dp
    val RadiusXl = 16.dp
    val RadiusSheet = 24.dp
    val RadiusFull = 999.dp
}

val LocalCherryPalette = staticCompositionLocalOf { LightCherryPalette }
