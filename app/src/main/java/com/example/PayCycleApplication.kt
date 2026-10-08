package com.example

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

class PayCycleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        cleanupDuplicateLauncherAliases(this)
    }

    companion object {
        fun ensureAllLauncherComponentsEnabled(context: Context) {
            cleanupDuplicateLauncherAliases(context)
        }

        fun cleanupDuplicateLauncherAliases(context: Context) {
            try {
                val pm = context.packageManager
                val packageName = context.packageName

                // Ensure MainActivity is ALWAYS enabled
                val mainComp = ComponentName(packageName, "com.example.MainActivity")
                try {
                    pm.setComponentEnabledSetting(
                        mainComp,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (e: Exception) {
                    Log.d("PayCycleApp", "MainActivity enable note: ${e.message}")
                }

                // Explicitly disable any legacy activity aliases that caused duplicate icons
                val aliasNames = listOf(
                    "com.example.MainActivityAliasDefault",
                    "com.example.MainActivityAliasSapphire",
                    "com.example.MainActivityAliasOnyx",
                    "com.example.MainActivityAliasSunset",
                    "com.example.MainActivityAliasAmethyst",
                    "com.example.MainActivityAliasRetro"
                )
                for (alias in aliasNames) {
                    try {
                        val comp = ComponentName(packageName, alias)
                        pm.setComponentEnabledSetting(
                            comp,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                        )
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("PayCycleApp", "Error cleaning up launcher components", e)
            }
        }
    }
}
