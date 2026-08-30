package com.example.ui.theme

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.R

enum class AppIcon(
    val id: String,
    val title: String,
    val subtitle: String,
    val aliasName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val previewForegroundRes: Int,
    val previewBackgroundRes: Int,
    val tag: String
) {
    DEFAULT(
        id = "default",
        title = "Emerald Mint",
        subtitle = "Classic emerald & gold coin with financial growth vibes",
        aliasName = "com.example.MainActivityAliasDefault",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFFF59E0B),
        previewForegroundRes = R.drawable.ic_launcher_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_background,
        tag = "Classic"
    ),
    SAPPHIRE(
        id = "sapphire",
        title = "Sapphire Vault",
        subtitle = "Deep ocean navy & vibrant cyan electric glow",
        aliasName = "com.example.MainActivityAliasSapphire",
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF38BDF8),
        previewForegroundRes = R.drawable.ic_launcher_sapphire_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_sapphire_background,
        tag = "Modern"
    ),
    ONYX(
        id = "onyx",
        title = "Midnight Onyx",
        subtitle = "OLED pitch black stealth with razor neon green accents",
        aliasName = "com.example.MainActivityAliasOnyx",
        primaryColor = Color(0xFF22C55E),
        secondaryColor = Color(0xFF18181B),
        previewForegroundRes = R.drawable.ic_launcher_onyx_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_onyx_background,
        tag = "Stealth"
    ),
    SUNSET(
        id = "sunset",
        title = "Sunset Amber",
        subtitle = "Warm radiant amber gold & sunset coral energy",
        aliasName = "com.example.MainActivityAliasSunset",
        primaryColor = Color(0xFFEA580C),
        secondaryColor = Color(0xFFFBBF24),
        previewForegroundRes = R.drawable.ic_launcher_sunset_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_sunset_background,
        tag = "Warm"
    ),
    AMETHYST(
        id = "amethyst",
        title = "Amethyst Royal",
        subtitle = "Deep violet & electric purple crystalline spark",
        aliasName = "com.example.MainActivityAliasAmethyst",
        primaryColor = Color(0xFF9333EA),
        secondaryColor = Color(0xFFC084FC),
        previewForegroundRes = R.drawable.ic_launcher_amethyst_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_amethyst_background,
        tag = "Royal"
    ),
    RETRO(
        id = "retro",
        title = "Retro 8-Bit",
        subtitle = "90s arcade green CRT grid with pixelated coin",
        aliasName = "com.example.MainActivityAliasRetro",
        primaryColor = Color(0xFF00FF66),
        secondaryColor = Color(0xFFFFCC00),
        previewForegroundRes = R.drawable.ic_launcher_retro_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_retro_background,
        tag = "Arcade"
    );

    companion object {
        private const val PREFS_NAME = "app_icon_prefs"
        private const val KEY_SELECTED_ICON = "selected_app_icon_id"

        fun fromId(id: String?): AppIcon {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }

        fun getCurrentIcon(context: Context): AppIcon {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedId = prefs.getString(KEY_SELECTED_ICON, null)
            if (savedId != null) {
                return fromId(savedId)
            }
            // Otherwise check PackageManager state
            val pm = context.packageManager
            val packageName = context.packageName
            for (icon in values()) {
                val component = ComponentName(packageName, icon.aliasName)
                val state = try {
                    pm.getComponentEnabledSetting(component)
                } catch (e: Exception) {
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                }
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return icon
                }
            }
            return DEFAULT
        }

        fun setAppIcon(context: Context, targetIcon: AppIcon): Boolean {
            return try {
                val pm = context.packageManager
                val packageName = context.packageName

                // Enable target alias, disable all other aliases
                for (icon in values()) {
                    val component = ComponentName(packageName, icon.aliasName)
                    val newState = if (icon == targetIcon) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    }
                    pm.setComponentEnabledSetting(
                        component,
                        newState,
                        PackageManager.DONT_KILL_APP
                    )
                }

                // Save in SharedPreferences
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SELECTED_ICON, targetIcon.id)
                    .apply()

                true
            } catch (e: Exception) {
                Log.e("AppIconManager", "Failed to switch app icon to ${targetIcon.name}", e)
                false
            }
        }
    }
}
