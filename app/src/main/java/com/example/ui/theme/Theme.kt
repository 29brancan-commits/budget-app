package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

// Let's define Dark Clean Minimalism Palette
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMinimalDark,
    onPrimary = OnPrimaryMinimalDark,
    primaryContainer = PrimaryContainerMinimalDark,
    onPrimaryContainer = OnPrimaryContainerMinimalDark,
    secondary = SecondaryMinimalDark,
    secondaryContainer = SecondaryContainerMinimalDark,
    onSecondaryContainer = OnSecondaryContainerMinimalDark,
    background = BackgroundMinimalDark,
    onBackground = OnBackgroundMinimalDark,
    surface = SurfaceMinimalDark,
    onSurface = OnSurfaceMinimalDark,
    surfaceVariant = SurfaceVariantMinimalDark,
    onSurfaceVariant = OnSurfaceVariantMinimalDark,
    outline = OutlineMinimalDark,
    outlineVariant = OutlineVariantMinimalDark,
    error = ErrorMinimalDark,
    onError = OnErrorMinimalDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryMinimal,
    onPrimary = OnPrimaryMinimal,
    primaryContainer = PrimaryContainerMinimal,
    onPrimaryContainer = OnPrimaryContainerMinimal,
    secondary = SecondaryMinimal,
    secondaryContainer = SecondaryContainerMinimal,
    onSecondaryContainer = OnSecondaryContainerMinimal,
    background = BackgroundMinimal,
    onBackground = OnBackgroundMinimal,
    surface = SurfaceMinimal,
    onSurface = OnSurfaceMinimal,
    surfaceVariant = SurfaceVariantMinimal,
    onSurfaceVariant = OnSurfaceVariantMinimal,
    outline = OutlineMinimal,
    outlineVariant = OutlineVariantMinimal,
    error = ErrorMinimal,
    onError = OnErrorMinimal
)

// --- Color Schemas for AppThemes ---

// Color Theory Spectrum Theme (Triadic Chromatic Harmony)
// Built on core color theory principles for finance UI:
// 1. Triadic Color Harmony (120° Separation): Teal (#0D9488/#2DD4BF) for monetary growth & balance, Indigo (#4F46E5/#818CF8) for trust & structural clarity, Amber Gold (#D97706/#FBBF24) for alerts & pending actions.
// 2. 60-30-10 Visual Balance: 60% neutral surface canvas, 30% structural indigo/slate containers, 10% vibrant emerald & amber accents.
// 3. High Accessibility Contrast (WCAG AAA) & Chromatic Psychology.

private val ColorTheoryLightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),          // Teal Growth Primary
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFF4F46E5),        // Indigo Structural Trust
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = Color(0xFFD97706),         // Amber Gold Accent
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
)

private val ColorTheoryDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),          // Luminous Teal Growth Primary
    onPrimary = Color(0xFF0F3832),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF818CF8),        // Luminous Indigo Structural Accent
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFFFBBF24),         // Warm Amber Gold Accent
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0B0F17),       // Deep Velvet Obsidian Surface Canvas
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF111827),          // Rich Midnight Slate Card Surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1F2937),   // Slate Indigo Variant
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// Emerald Mint Theme
private val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    background = Color(0xFFF0FDF4),
    onBackground = Color(0xFF064E3B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFDCFCE7),
    onSurfaceVariant = Color(0xFF047857),
    outline = Color(0xFF6EE7B7),
    outlineVariant = Color(0xFFA7F3D0),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF115E59),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF000000),
    onBackground = Color(0xFFECFDF5),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF047857),
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF10B981),
    outlineVariant = Color(0xFF059669),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// Ocean Sapphire Theme
private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEFF6FF),
    onSurfaceVariant = Color(0xFF1E3A8A),
    outline = Color(0xFF93C5FD),
    outlineVariant = Color(0xFFBFDBFE),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E40AF),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF075985),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF93C5FD),
    outline = Color(0xFF3B82F6),
    outlineVariant = Color(0xFF1D4ED8),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// Sunset Copper Theme
private val AmberLightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFFEA580C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFF7C2D12),
    background = Color(0xFFFFFBEB),
    onBackground = Color(0xFF451A03),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF451A03),
    surfaceVariant = Color(0xFFFEF3C7),
    onSurfaceVariant = Color(0xFF92400E),
    outline = Color(0xFFFCD34D),
    outlineVariant = Color(0xFFFDE68A),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val AmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF78350F),
    primaryContainer = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFFB923C),
    onSecondary = Color(0xFF7C2D12),
    secondaryContainer = Color(0xFF9A3412),
    onSecondaryContainer = Color(0xFFFFEDD5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFEF3C7),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFFEF3C7),
    surfaceVariant = Color(0xFF3F3F46),
    onSurfaceVariant = Color(0xFFFCD34D),
    outline = Color(0xFFF59E0B),
    outlineVariant = Color(0xFFD97706),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// Rose Gold Theme
private val RoseLightColorScheme = lightColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = Color(0xFFC026D3),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFAE8FF),
    onSecondaryContainer = Color(0xFF701A75),
    background = Color(0xFFFFF1F2),
    onBackground = Color(0xFF4C0519),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF4C0519),
    surfaceVariant = Color(0xFFFFE4E6),
    onSurfaceVariant = Color(0xFF9F1239),
    outline = Color(0xFFFDA4AF),
    outlineVariant = Color(0xFFFECDD3),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val RoseDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF9F1239),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFE879F9),
    onSecondary = Color(0xFF701A75),
    secondaryContainer = Color(0xFF86198F),
    onSecondaryContainer = Color(0xFFFAE8FF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFF1F2),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFFFF1F2),
    surfaceVariant = Color(0xFF44403C),
    onSurfaceVariant = Color(0xFFFDA4AF),
    outline = Color(0xFFF43F5E),
    outlineVariant = Color(0xFFE11D48),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// Midnight Cyber Theme
private val MidnightLightColorScheme = lightColorScheme(
    primary = Color(0xFF0891B2),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCFFAFE),
    onPrimaryContainer = Color(0xFF155E75),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    background = Color(0xFFF0FDFA),
    onBackground = Color(0xFF164E63),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF164E63),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF0369A1),
    outline = Color(0xFF67E8F9),
    outlineVariant = Color(0xFFA5F3FC),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val MidnightDarkColorScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF003643),
    primaryContainer = Color(0xFF0891B2),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF4C1D95),
    secondaryContainer = Color(0xFF5B21B6),
    onSecondaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFECFEFF),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFECFEFF),
    surfaceVariant = Color(0xFF18181B),
    onSurfaceVariant = Color(0xFF67E8F9),
    outline = Color(0xFF06B6D4),
    outlineVariant = Color(0xFF0891B2),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// 90s AOL Retro Theme
private val RetroAolLightColorScheme = lightColorScheme(
    primary = Color(0xFF003399),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001A4D),
    secondary = Color(0xFFD97706),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    background = Color(0xFFF3F4F6),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF1F2937),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFD1D5DB),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val RetroAolDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFC600),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF002B80),
    onPrimaryContainer = Color(0xFFE0EAFF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = Color(0xFFFFD966),
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF4B5563),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// 90s Mac OS Theme (System 7 / Platinum Desktop)
private val MacClassicLightColorScheme = lightColorScheme(
    primary = Color(0xFF003399),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4D5FE),
    onPrimaryContainer = Color(0xFF001133),
    secondary = Color(0xFF0066CC),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    background = Color(0xFFD4D4D4),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFECECEC),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFD8D8D8),
    onSurfaceVariant = Color(0xFF1F2937),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFFB8B8B8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF)
)

private val MacClassicDarkColorScheme = darkColorScheme(
    primary = Color(0xFF66A3FF),
    onPrimary = Color(0xFF001133),
    primaryContainer = Color(0xFF002B66),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFFFBB00),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF332800),
    onSecondaryContainer = Color(0xFFFFE899),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF2D3039),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF374151),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

// 8-Bit Retro Arcade Theme
private val Retro8BitLightColorScheme = lightColorScheme(
    primary = Color(0xFF00AA44),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1E261E),
    onPrimaryContainer = Color(0xFF55FF88),
    secondary = Color(0xFFFF2255),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEE2E2),
    onSecondaryContainer = Color(0xFF990022),
    background = Color(0xFFE5E7EB),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF3F4F6),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFD1D5DB),
    onSurfaceVariant = Color(0xFF111827),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF4B5563),
    error = Color(0xFFE60012),
    onError = Color(0xFFFFFFFF)
)

private val Retro8BitDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003815),
    onPrimaryContainer = Color(0xFF88FFB4),
    secondary = Color(0xFFFF0055),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF55001A),
    onSecondaryContainer = Color(0xFFFF99BB),
    background = Color(0xFF000000),
    onBackground = Color(0xFF00FF66),
    surface = Color(0xFF09090B),
    onSurface = Color(0xFF00FF66),
    surfaceVariant = Color(0xFF222A3B),
    onSurfaceVariant = Color(0xFF76FFB1),
    outline = Color(0xFF00FF66),
    outlineVariant = Color(0xFF00AA44),
    error = Color(0xFFFF3344),
    onError = Color(0xFF000000)
)

// E-Reader Sepia Paper Theme
private val EReaderLightColorScheme = lightColorScheme(
    primary = Color(0xFF5C4033),            // Warm Sepia Brown Ink
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFE6D5),   // Soft Parchment Container
    onPrimaryContainer = Color(0xFF2A1C15),
    secondary = Color(0xFF8B5E3C),          // Warm Amber Ink Secondary
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5EDDE),
    onSecondaryContainer = Color(0xFF3D281C),
    tertiary = Color(0xFF705335),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF8F3EA),
    onTertiaryContainer = Color(0xFF332314),
    background = Color(0xFFF7F1E5),         // Warm Cream Parchment Canvas
    onBackground = Color(0xFF231F1D),       // Deep Ink Charcoal Text
    surface = Color(0xFFFCF9F2),            // Cream Paper Surface
    onSurface = Color(0xFF231F1D),          // Deep Ink Charcoal Text
    surfaceVariant = Color(0xFFEFE6D5),     // Parchment Variant
    onSurfaceVariant = Color(0xFF4A3B32),
    outline = Color(0xFFC7BBAA),
    outlineVariant = Color(0xFFDDD2C1),
    error = Color(0xFF991B1B),
    onError = Color(0xFFFFFFFF)
)

private val EReaderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE8D3BC),            // Warm Off-White E-Ink Primary
    onPrimary = Color(0xFF2A1C15),
    primaryContainer = Color(0xFF382A22),   // Warm Charcoal Container
    onPrimaryContainer = Color(0xFFF5EDDE),
    secondary = Color(0xFFD4C3AD),          // Sepia Warm Secondary Accent
    onSecondary = Color(0xFF231F1D),
    secondaryContainer = Color(0xFF2E2620),
    onSecondaryContainer = Color(0xFFEFE6D5),
    tertiary = Color(0xFFC9B6A0),
    onTertiary = Color(0xFF231F1D),
    tertiaryContainer = Color(0xFF3B3028),
    onTertiaryContainer = Color(0xFFF8F3EA),
    background = Color(0xFF141210),         // Dark Warm E-Ink Canvas
    onBackground = Color(0xFFECE5DA),       // Warm Parchment Text
    surface = Color(0xFF1A1715),            // Dark Warm Surface
    onSurface = Color(0xFFECE5DA),          // Warm Parchment Text
    surfaceVariant = Color(0xFF26221E),     // Dark Parchment Variant
    onSurfaceVariant = Color(0xFFC7BBAA),
    outline = Color(0xFF4A4139),
    outlineVariant = Color(0xFF332C26),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A)
)

fun getLightColorSchemeForTheme(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.COLOR_THEORY -> ColorTheoryLightColorScheme
    AppTheme.DEFAULT -> LightColorScheme
    AppTheme.EMERALD -> EmeraldLightColorScheme
    AppTheme.OCEAN -> OceanLightColorScheme
    AppTheme.AMBER -> AmberLightColorScheme
    AppTheme.ROSE -> RoseLightColorScheme
    AppTheme.MIDNIGHT -> MidnightLightColorScheme
    AppTheme.RETRO_AOL -> RetroAolLightColorScheme
    AppTheme.MAC_CLASSIC -> MacClassicLightColorScheme
    AppTheme.RETRO_8BIT -> Retro8BitLightColorScheme
    AppTheme.EREADER -> EReaderLightColorScheme
}

fun getDarkColorSchemeForTheme(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.COLOR_THEORY -> ColorTheoryDarkColorScheme
    AppTheme.DEFAULT -> DarkColorScheme
    AppTheme.EMERALD -> EmeraldDarkColorScheme
    AppTheme.OCEAN -> OceanDarkColorScheme
    AppTheme.AMBER -> AmberDarkColorScheme
    AppTheme.ROSE -> RoseDarkColorScheme
    AppTheme.MIDNIGHT -> MidnightDarkColorScheme
    AppTheme.RETRO_AOL -> RetroAolDarkColorScheme
    AppTheme.MAC_CLASSIC -> MacClassicDarkColorScheme
    AppTheme.RETRO_8BIT -> Retro8BitDarkColorScheme
    AppTheme.EREADER -> EReaderDarkColorScheme
}

fun getTypographyForTheme(theme: AppTheme): Typography = when (theme) {
    AppTheme.RETRO_8BIT -> Retro8BitTypography
    else -> Typography
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTheme: AppTheme = AppTheme.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> getDarkColorSchemeForTheme(appTheme)
        else -> getLightColorSchemeForTheme(appTheme)
    }

    val typography = getTypographyForTheme(appTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
