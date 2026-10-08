package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColorLight: Color,
    val primaryColorDark: Color,
    val containerColorLight: Color,
    val containerColorDark: Color
) {
    COLOR_THEORY(
        id = "color_theory",
        displayName = "🎨 Color Theory Spectrum",
        description = "Harmonized Triadic Spectrum: Teal for growth, Indigo for trust, Amber for alerts with 60-30-10 balance",
        primaryColorLight = Color(0xFF0D9488),
        primaryColorDark = Color(0xFF2DD4BF),
        containerColorLight = Color(0xFFCCFBF1),
        containerColorDark = Color(0xFF115E59)
    ),
    DEFAULT(
        id = "default",
        displayName = "Purple Minimal",
        description = "Classic purple & clean neutral tone",
        primaryColorLight = Color(0xFF6750A4),
        primaryColorDark = Color(0xFFA5B4FC),
        containerColorLight = Color(0xFFEADDFF),
        containerColorDark = Color(0xFF312E81)
    ),
    EMERALD(
        id = "emerald",
        displayName = "Emerald Mint",
        description = "Fresh emerald green & financial growth vibes",
        primaryColorLight = Color(0xFF059669),
        primaryColorDark = Color(0xFF34D399),
        containerColorLight = Color(0xFFD1FAE5),
        containerColorDark = Color(0xFF065F46)
    ),
    OCEAN(
        id = "ocean",
        displayName = "Ocean Sapphire",
        description = "Calm sapphire blue & rich navy trust accents",
        primaryColorLight = Color(0xFF2563EB),
        primaryColorDark = Color(0xFF60A5FA),
        containerColorLight = Color(0xFFDBEAFE),
        containerColorDark = Color(0xFF1E40AF)
    ),
    AMBER(
        id = "amber",
        displayName = "Sunset Copper",
        description = "Warm amber gold & rich copper energy",
        primaryColorLight = Color(0xFFD97706),
        primaryColorDark = Color(0xFFFBBF24),
        containerColorLight = Color(0xFFFEF3C7),
        containerColorDark = Color(0xFF92400E)
    ),
    ROSE(
        id = "rose",
        displayName = "Rose Gold",
        description = "Elegant rose pink & subtle mauve luxury",
        primaryColorLight = Color(0xFFE11D48),
        primaryColorDark = Color(0xFFFB7185),
        containerColorLight = Color(0xFFFFE4E6),
        containerColorDark = Color(0xFF9F1239)
    ),
    MIDNIGHT(
        id = "midnight",
        displayName = "Midnight Cyber",
        description = "Electric neon cyan & OLED obsidian dark",
        primaryColorLight = Color(0xFF0891B2),
        primaryColorDark = Color(0xFF22D3EE),
        containerColorLight = Color(0xFFCFFAFE),
        containerColorDark = Color(0xFF0891B2)
    ),
    RETRO_AOL(
        id = "retro_aol",
        displayName = "90s AOL Retro",
        description = "Nostalgic dial-up blue, classic desktop gray & yellow running man accents",
        primaryColorLight = Color(0xFF003399),
        primaryColorDark = Color(0xFFFFC600),
        containerColorLight = Color(0xFFD6E4FF),
        containerColorDark = Color(0xFF001A4D)
    ),
    MAC_CLASSIC(
        id = "mac_classic",
        displayName = "90s Mac OS",
        description = "Classic System 7 / Platinum desktop grey with iconic blue window accents & pinstripes",
        primaryColorLight = Color(0xFF003399),
        primaryColorDark = Color(0xFF66A3FF),
        containerColorLight = Color(0xFFB4D5FE),
        containerColorDark = Color(0xFF002B66)
    ),
    RETRO_8BIT(
        id = "retro_8bit",
        displayName = "🕹️ 8-Bit Retro Arcade",
        description = "Vibrant retro pixel arcade vibes with 8-bit green, neon red & pixel grid accents",
        primaryColorLight = Color(0xFF00AA44),
        primaryColorDark = Color(0xFF00FF66),
        containerColorLight = Color(0xFF1E261E),
        containerColorDark = Color(0xFF003815)
    ),
    EREADER(
        id = "ereader",
        displayName = "📖 E-Reader Sepia",
        description = "Warm paper parchment & deep sepia ink contrast inspired by Kindle & e-ink displays",
        primaryColorLight = Color(0xFF5C4033),
        primaryColorDark = Color(0xFFE8D3BC),
        containerColorLight = Color(0xFFEFE6D5),
        containerColorDark = Color(0xFF382A22)
    );

    fun getColorScheme(darkTheme: Boolean): ColorScheme {
        return if (darkTheme) {
            darkColorScheme(
                primary = primaryColorDark,
                primaryContainer = containerColorDark,
                secondary = primaryColorDark,
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B)
            )
        } else {
            lightColorScheme(
                primary = primaryColorLight,
                primaryContainer = containerColorLight,
                secondary = primaryColorLight,
                background = Color(0xFFF8FAFC),
                surface = Color.White
            )
        }
    }

    companion object {
        fun fromId(id: String?): AppTheme {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: COLOR_THEORY
        }
    }
}
