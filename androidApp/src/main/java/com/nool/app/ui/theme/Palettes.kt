package com.nool.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.nool.app.settings.ThemePalette

/**
 * Returns representative preview accent color for the given palette.
 */
fun ThemePalette.previewColor(darkTheme: Boolean): Color = when (this) {
    ThemePalette.DEFAULT -> if (darkTheme) Color(0xFFB8C4FF) else Color(0xFF4C5B92)
    ThemePalette.EMERALD -> if (darkTheme) Color(0xFF89D7B0) else Color(0xFF1B6D4F)
    ThemePalette.ROSE -> if (darkTheme) Color(0xFFFFB1C8) else Color(0xFF984061)
    ThemePalette.AMBER -> if (darkTheme) Color(0xFFFFB958) else Color(0xFF835400)
    ThemePalette.OBSIDIAN -> if (darkTheme) Color(0xFFD7BAFF) else Color(0xFF6E4FA3)
    ThemePalette.NORD -> if (darkTheme) Color(0xFF99CEEB) else Color(0xFF2F637C)
    ThemePalette.OCEAN -> if (darkTheme) Color(0xFF8CCEFC) else Color(0xFF006494)
}

/**
 * Returns the complete Material 3 ColorScheme for each ThemePalette.
 */
fun getPaletteColorScheme(palette: ThemePalette, darkTheme: Boolean): ColorScheme {
    return when (palette) {
        ThemePalette.DEFAULT -> if (darkTheme) DefaultDarkScheme else DefaultLightScheme
        ThemePalette.EMERALD -> if (darkTheme) EmeraldDarkScheme else EmeraldLightScheme
        ThemePalette.ROSE -> if (darkTheme) RoseDarkScheme else RoseLightScheme
        ThemePalette.AMBER -> if (darkTheme) AmberDarkScheme else AmberLightScheme
        ThemePalette.OBSIDIAN -> if (darkTheme) ObsidianDarkScheme else ObsidianLightScheme
        ThemePalette.NORD -> if (darkTheme) NordDarkScheme else NordLightScheme
        ThemePalette.OCEAN -> if (darkTheme) OceanDarkScheme else OceanLightScheme
    }
}

// 1. DEFAULT (Indigo / Modern Minimal)
private val DefaultLightScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight
)

private val DefaultDarkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantLightDark,
    outline = OutlineDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark
)

// 2. EMERALD (Lush Forest & Mint)
private val EmeraldLightScheme = lightColorScheme(
    primary = Color(0xFF1B6D4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA5F3CB),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4F6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D5),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF3E6473),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC2E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF5FBF5),
    onBackground = Color(0xFF171D19),
    surface = Color(0xFFF5FBF5),
    onSurface = Color(0xFF171D19),
    surfaceVariant = Color(0xFFDBE5DC),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5EF),
    surfaceContainer = Color(0xFFE9EFE9),
    surfaceContainerHigh = Color(0xFFE3EAE3),
    surfaceContainerHighest = Color(0xFFDDE4DE)
)

private val EmeraldDarkScheme = darkColorScheme(
    primary = Color(0xFF89D7B0),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF005137),
    onPrimaryContainer = Color(0xFFA5F3CB),
    secondary = Color(0xFFB6CCBA),
    onSecondary = Color(0xFF213527),
    secondaryContainer = Color(0xFF374B3C),
    onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA6CCDE),
    onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5B),
    onTertiaryContainer = Color(0xFFC2E8FB),
    background = Color(0xFF0E1511),
    onBackground = Color(0xFFDFE4DE),
    surface = Color(0xFF0E1511),
    onSurface = Color(0xFFDFE4DE),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF8A938C),
    surfaceContainerLowest = Color(0xFF090F0C),
    surfaceContainerLow = Color(0xFF161E19),
    surfaceContainer = Color(0xFF1A221D),
    surfaceContainerHigh = Color(0xFF242D27),
    surfaceContainerHighest = Color(0xFF2F3832)
)

// 3. ROSE (Crimson & Mauve)
private val RoseLightScheme = lightColorScheme(
    primary = Color(0xFF984061),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCC1),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFFFF8F8),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFF8F8),
    onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514347),
    outline = Color(0xFF837377),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF1F3),
    surfaceContainer = Color(0xFFF7EBED),
    surfaceContainerHigh = Color(0xFFF1E5E7),
    surfaceContainerHighest = Color(0xFFECE0E1)
)

private val RoseDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB1C8),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF7B2949),
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = Color(0xFFE2BDC6),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5B3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFEFBD94),
    onTertiary = Color(0xFF472A0C),
    tertiaryContainer = Color(0xFF613F20),
    onTertiaryContainer = Color(0xFFFFDCC1),
    background = Color(0xFF191113),
    onBackground = Color(0xFFEFE0E1),
    surface = Color(0xFF191113),
    onSurface = Color(0xFFEFE0E1),
    surfaceVariant = Color(0xFF514347),
    onSurfaceVariant = Color(0xFFD5C2C6),
    outline = Color(0xFF9E8C90),
    surfaceContainerLowest = Color(0xFF130C0E),
    surfaceContainerLow = Color(0xFF21181B),
    surfaceContainer = Color(0xFF261C1F),
    surfaceContainerHigh = Color(0xFF312729),
    surfaceContainerHighest = Color(0xFF3C3134)
)

// 4. AMBER (Warm Autumn & Honey)
private val AmberLightScheme = lightColorScheme(
    primary = Color(0xFF835400),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB5),
    onPrimaryContainer = Color(0xFF2A1800),
    secondary = Color(0xFF705B40),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBDFBF),
    onSecondaryContainer = Color(0xFF271804),
    tertiary = Color(0xFF53643E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7EABF),
    onTertiaryContainer = Color(0xFF121F03),
    background = Color(0xFFFFF8F4),
    onBackground = Color(0xFF201B15),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF201B15),
    surfaceVariant = Color(0xFFF0E0CF),
    onSurfaceVariant = Color(0xFF4F4539),
    outline = Color(0xFF817567),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF2E9),
    surfaceContainer = Color(0xFFF6ECE3),
    surfaceContainerHigh = Color(0xFFF1E6DD),
    surfaceContainerHighest = Color(0xFFEBE0D7)
)

private val AmberDarkScheme = darkColorScheme(
    primary = Color(0xFFFFB958),
    onPrimary = Color(0xFF462A00),
    primaryContainer = Color(0xFF643E00),
    onPrimaryContainer = Color(0xFFFFDDB5),
    secondary = Color(0xFFDEC3A2),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF56432B),
    onSecondaryContainer = Color(0xFFFBDFBF),
    tertiary = Color(0xFFBBCDA4),
    onTertiary = Color(0xFF263514),
    tertiaryContainer = Color(0xFF3C4C28),
    onTertiaryContainer = Color(0xFFD7EABF),
    background = Color(0xFF18120C),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF18120C),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF4F4539),
    onSurfaceVariant = Color(0xFFD3C4B4),
    outline = Color(0xFF9C8F80),
    surfaceContainerLowest = Color(0xFF120D08),
    surfaceContainerLow = Color(0xFF201912),
    surfaceContainer = Color(0xFF251E16),
    surfaceContainerHigh = Color(0xFF302820),
    surfaceContainerHighest = Color(0xFF3B332B)
)

// 5. OBSIDIAN (Deep Amethyst & Violet)
private val ObsidianLightScheme = lightColorScheme(
    primary = Color(0xFF6E4FA3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDDCFF),
    onPrimaryContainer = Color(0xFF280057),
    secondary = Color(0xFF635B70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE9DEF8),
    onSecondaryContainer = Color(0xFF1F182A),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF31101D),
    background = Color(0xFFFFF7FE),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFF7FE),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    outline = Color(0xFF7A757F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F0FA),
    surfaceContainer = Color(0xFFF3EAF4),
    surfaceContainerHigh = Color(0xFFEDE5EE),
    surfaceContainerHighest = Color(0xFFE7DFE9)
)

private val ObsidianDarkScheme = darkColorScheme(
    primary = Color(0xFFD7BAFF),
    onPrimary = Color(0xFF3E1C71),
    primaryContainer = Color(0xFF553589),
    onPrimaryContainer = Color(0xFFEDDCFF),
    secondary = Color(0xFFCCC2DB),
    onSecondary = Color(0xFF342D40),
    secondaryContainer = Color(0xFF4B4358),
    onSecondaryContainer = Color(0xFFE9DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B49),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF16111C),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF16111C),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454E),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    surfaceContainerLowest = Color(0xFF100C16),
    surfaceContainerLow = Color(0xFF1E1824),
    surfaceContainer = Color(0xFF221C29),
    surfaceContainerHigh = Color(0xFF2D2634),
    surfaceContainerHighest = Color(0xFF38313F)
)

// 6. NORD (Glacier Blue & Arctic Teal)
private val NordLightScheme = lightColorScheme(
    primary = Color(0xFF2F637C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCEE7FF),
    onPrimaryContainer = Color(0xFF001D2D),
    secondary = Color(0xFF4F616D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E5F4),
    onSecondaryContainer = Color(0xFF0B1D28),
    tertiary = Color(0xFF62597C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE8DDFF),
    onTertiaryContainer = Color(0xFF1E1635),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF181C1F),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF181C1F),
    surfaceVariant = Color(0xFFDCE3E9),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF71787D),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F8),
    surfaceContainer = Color(0xFFEBF0F4),
    surfaceContainerHigh = Color(0xFFE5EAEE),
    surfaceContainerHighest = Color(0xFFDFE4E8)
)

private val NordDarkScheme = darkColorScheme(
    primary = Color(0xFF99CEEB),
    onPrimary = Color(0xFF00344A),
    primaryContainer = Color(0xFF114B63),
    onPrimaryContainer = Color(0xFFCEE7FF),
    secondary = Color(0xFFB6C9D8),
    onSecondary = Color(0xFF21323E),
    secondaryContainer = Color(0xFF374955),
    onSecondaryContainer = Color(0xFFD2E5F4),
    tertiary = Color(0xFFCCC1E9),
    onTertiary = Color(0xFF332B4C),
    tertiaryContainer = Color(0xFF4A4163),
    onTertiaryContainer = Color(0xFFE8DDFF),
    background = Color(0xFF0F1418),
    onBackground = Color(0xFFDEE3E7),
    surface = Color(0xFF0F1418),
    onSurface = Color(0xFFDEE3E7),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C7CD),
    outline = Color(0xFF8B9297),
    surfaceContainerLowest = Color(0xFF0B0F12),
    surfaceContainerLow = Color(0xFF171D22),
    surfaceContainer = Color(0xFF1B2126),
    surfaceContainerHigh = Color(0xFF252B31),
    surfaceContainerHighest = Color(0xFF30363C)
)

// 7. OCEAN (Vibrant Cobalt & Cyan)
private val OceanLightScheme = lightColorScheme(
    primary = Color(0xFF006494),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCAE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF50606E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E5F5),
    onSecondaryContainer = Color(0xFF0C1D29),
    tertiary = Color(0xFF65587B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDCFF),
    onTertiaryContainer = Color(0xFF201634),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF181C20),
    surface = Color(0xFFF8F9FF),
    onSurface = Color(0xFF181C20),
    surfaceVariant = Color(0xFFDDE3EA),
    onSurfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF71787E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F3FB),
    surfaceContainer = Color(0xFFEBF0F7),
    surfaceContainerHigh = Color(0xFFE5EAF1),
    surfaceContainerHighest = Color(0xFFDFE4EB)
)

private val OceanDarkScheme = darkColorScheme(
    primary = Color(0xFF8CCEFC),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004C73),
    onPrimaryContainer = Color(0xFFCAE6FF),
    secondary = Color(0xFFB7C8D9),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF384956),
    onSecondaryContainer = Color(0xFFD3E5F5),
    tertiary = Color(0xFFD0C0E8),
    onTertiary = Color(0xFF362B4A),
    tertiaryContainer = Color(0xFF4D4162),
    onTertiaryContainer = Color(0xFFEBDCFF),
    background = Color(0xFF0F1419),
    onBackground = Color(0xFFDEE3E9),
    surface = Color(0xFF0F1419),
    onSurface = Color(0xFFDEE3E9),
    surfaceVariant = Color(0xFF41474D),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9198),
    surfaceContainerLowest = Color(0xFF0A0F13),
    surfaceContainerLow = Color(0xFF171C22),
    surfaceContainer = Color(0xFF1B2026),
    surfaceContainerHigh = Color(0xFF252B31),
    surfaceContainerHighest = Color(0xFF30363C)
)
