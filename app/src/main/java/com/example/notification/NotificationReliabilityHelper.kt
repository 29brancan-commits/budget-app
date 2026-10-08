package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationReliabilityHelper {

    private const val TAG = "NotifReliabilityHelper"
    private const val PREFS_NAME = "budget_notification_prefs"
    const val KEY_KEEP_ALIVE_SERVICE = "pref_keep_alive_service"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if battery optimization is disabled for this app
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    /**
     * Opens battery optimization exemption prompt or system settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot launch direct request ignore battery, fallback to list", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Cannot launch battery settings", e2)
            }
        }
    }

    /**
     * Checks if exact alarms are allowed on Android 12+ (API 31+)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }
    }

    /**
     * Opens system exact alarm permission screen on Android 12+
     */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch exact alarm settings", e)
            }
        }
    }

    /**
     * Checks if notification permission is granted on Android 13+
     */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Opens system notification settings for this app
     */
    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:${context.packageName}")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app notification settings", e)
        }
    }

    /**
     * Checks whether the user has enabled the persistent foreground keep-alive service
     */
    fun isKeepAliveServiceEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_KEEP_ALIVE_SERVICE, false)
    }

    /**
     * Toggles the persistent foreground keep-alive service
     */
    fun setKeepAliveServiceEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_KEEP_ALIVE_SERVICE, enabled).apply()
        val intent = Intent(context, BillReminderForegroundService::class.java)
        if (enabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start keep alive service", e)
            }
        } else {
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop keep alive service", e)
            }
        }
    }

    /**
     * Starts the foreground service if user previously opted in
     */
    fun startKeepAliveServiceIfEnabled(context: Context) {
        if (isKeepAliveServiceEnabled(context)) {
            val intent = Intent(context, BillReminderForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting keep-alive service on app start", e)
            }
        }
    }

    /**
     * Schedules periodic background work using WorkManager to ensure reminders are checked
     * and delivered even if AlarmManager was cancelled or device rebooted.
     */
    fun schedulePeriodicWork(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(
                repeatInterval = 4,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BillReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Periodic BillReminderWorker scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule BillReminderWorker", e)
        }
    }

    /**
     * Returns OEM manufacturer instructions for ensuring background execution
     */
    fun getManufacturerGuide(): Pair<String, List<String>> {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") -> {
                "Samsung OneUI Optimization Guide" to listOf(
                    "1. Open Settings → Apps → PayCycle Budget.",
                    "2. Tap Battery → Select 'Unrestricted' (prevents Android from sleeping the app).",
                    "3. Open Settings → Device Care → Battery → Background usage limits.",
                    "4. Add PayCycle Budget to 'Never sleeping apps'.",
                    "5. Turn on 'Keep App Active' in the settings below for 100% guarantee."
                )
            }
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                "Xiaomi / MIUI / HyperOS Optimization Guide" to listOf(
                    "1. Open Settings → Apps → Manage Apps → PayCycle Budget.",
                    "2. Enable 'Autostart' (CRITICAL for reminders to fire).",
                    "3. Tap 'Battery saver' and select 'No restrictions'.",
                    "4. Tap 'Other permissions' and ensure 'Show on Lock screen' and 'Display pop-up windows' are enabled.",
                    "5. In Recent Apps overview, long-press the PayCycle Budget window and tap the Padlock icon to lock it in RAM."
                )
            }
            manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                "OnePlus / Oppo / Realme Optimization Guide" to listOf(
                    "1. Open Settings → Apps → App Management → PayCycle Budget.",
                    "2. Tap Battery Usage → Enable 'Allow background activity' and 'Allow auto-launch'.",
                    "3. Settings → Battery → Advanced settings → Optimize battery use → PayCycle Budget → 'Don't optimize'.",
                    "4. Enable 'Keep App Active' toggle in the settings below."
                )
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                "Vivo / iQOO Optimization Guide" to listOf(
                    "1. Open Settings → Battery → High background power consumption → Enable for PayCycle Budget.",
                    "2. Settings → Applications & Permissions → Autostart → Enable for PayCycle Budget.",
                    "3. In Recent Apps, swipe down on the app card and tap the Lock icon."
                )
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                "Huawei / Honor Optimization Guide" to listOf(
                    "1. Open Settings → Battery → App Launch → PayCycle Budget.",
                    "2. Disable 'Manage automatically' and enable all 3: Auto-launch, Secondary launch, and Run in background."
                )
            }
            else -> {
                "General Android Reliability Guide" to listOf(
                    "1. Ensure 'Battery Optimization' is set to Unrestricted / Don't optimize.",
                    "2. Ensure 'Exact Alarms' is allowed in system app settings.",
                    "3. Enable 'Keep App Active in Background' below to run a low-priority foreground service so the system never kills the process.",
                    "4. In the Recent Apps overview, tap the lock icon if available on your phone."
                )
            }
        }
    }
}
