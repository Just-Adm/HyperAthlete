package com.hyperathlete.app.core.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hyperathlete.app.R

enum class PermissionGroup { ESSENTIAL, OPTIONAL }

/** How a permission is obtained. */
enum class GrantMethod {
    /** Standard runtime dialog. */
    RUNTIME,
    /** A system settings screen the user toggles. */
    SETTINGS,
    /** Manufacturer steps the app can't verify; the user confirms them. */
    GUIDE,
}

/** Every permission card shown in onboarding and Settings → Permissions. */
enum class AppPermission(
    @StringRes val title: Int,
    @StringRes val why: Int,
    @DrawableRes val icon: Int,
    val group: PermissionGroup,
    val method: GrantMethod,
    /** Feature that uses it ships in this build phase; until then the card shows the phase. */
    val readyInPhase: Int? = null,
) {
    NOTIFICATIONS(R.string.perm_notifications, R.string.perm_notifications_why, R.drawable.ic_notifications, PermissionGroup.ESSENTIAL, GrantMethod.RUNTIME),
    LIVE_UPDATES(R.string.perm_live_updates, R.string.perm_live_updates_why, R.drawable.ic_pill, PermissionGroup.ESSENTIAL, GrantMethod.SETTINGS),
    EXACT_ALARMS(R.string.perm_exact_alarm, R.string.perm_exact_alarm_why, R.drawable.ic_alarm, PermissionGroup.ESSENTIAL, GrantMethod.SETTINGS),
    BATTERY(R.string.perm_battery, R.string.perm_battery_why, R.drawable.ic_battery_charging_full, PermissionGroup.ESSENTIAL, GrantMethod.SETTINGS),
    AUTOSTART(R.string.perm_autostart, R.string.perm_autostart_why, R.drawable.ic_mobile_lock_portrait, PermissionGroup.ESSENTIAL, GrantMethod.GUIDE),
    CALENDAR(R.string.perm_calendar, R.string.perm_calendar_why, R.drawable.ic_calendar_month, PermissionGroup.ESSENTIAL, GrantMethod.RUNTIME),
    LOCATION(R.string.perm_location, R.string.perm_location_why, R.drawable.ic_location_on, PermissionGroup.ESSENTIAL, GrantMethod.RUNTIME);

    /** Android runtime permissions behind this card (empty for settings-based ones). */
    fun runtimePermissions(): Array<String> = when (this) {
        NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
        CALENDAR -> arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        LOCATION -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        else -> emptyArray()
    }
}

enum class PermissionState {
    GRANTED,
    DENIED,
    /** Not needed on this Android version (counts as granted). */
    NOT_NEEDED,
    /** Feature not built yet. */
    LATER,
    /**
     * Android blocks this permission for apps installed from an APK file ("restricted setting")
     * until the user allows it from the app info page.
     */
    RESTRICTED,
}

object PermissionChecker {

    fun state(context: Context, p: AppPermission, autostartAcknowledged: Boolean): PermissionState {
        if (p.readyInPhase != null) return PermissionState.LATER
        return when (p) {
            AppPermission.NOTIFICATIONS -> granted(NotificationManagerCompat.from(context).areNotificationsEnabled() && runtimeGranted(context, p))
            AppPermission.LIVE_UPDATES -> {
                if (Build.VERSION.SDK_INT < 36) PermissionState.NOT_NEEDED
                else granted(runCatching { context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications() }.getOrDefault(false))
            }
            AppPermission.EXACT_ALARMS -> {
                if (Build.VERSION.SDK_INT < 31) PermissionState.NOT_NEEDED
                else granted(context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms())
            }
            AppPermission.BATTERY -> granted(context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName))
            AppPermission.AUTOSTART -> granted(autostartAcknowledged)
            AppPermission.CALENDAR, AppPermission.LOCATION -> granted(runtimeGranted(context, p))
        }
    }

    private fun granted(b: Boolean) = if (b) PermissionState.GRANTED else PermissionState.DENIED

    fun runtimeGranted(context: Context, p: AppPermission): Boolean =
        p.runtimePermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }


    /** Settings screen that grants a SETTINGS-type permission. Returns false if nothing could be opened. */
    fun openSettingsFor(context: Context, p: AppPermission): Boolean {
        val pkg = Uri.parse("package:${context.packageName}")
        val intents = when (p) {
            AppPermission.LIVE_UPDATES -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= 36) Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) else null,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
            AppPermission.EXACT_ALARMS -> listOfNotNull(
                if (Build.VERSION.SDK_INT >= 31) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg) else null,
            )
            AppPermission.BATTERY -> listOf(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg),
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            )
            AppPermission.NOTIFICATIONS -> listOf(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
            else -> emptyList()
        } + appDetails(context)
        return launchFirst(context, intents)
    }

    /**
     * True when Android 13+ "restricted settings" still blocks special permissions for this sideloaded app.
     * Read from the app-op the system uses for it; unknown on other versions (false).
     */
    fun isRestricted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        return runCatching {
            val ops = context.getSystemService(android.app.AppOpsManager::class.java)
            val mode = ops.unsafeCheckOpNoThrow("android:access_restricted_settings", android.os.Process.myUid(), context.packageName)
            mode != android.app.AppOpsManager.MODE_ALLOWED && mode != android.app.AppOpsManager.MODE_DEFAULT
        }.getOrDefault(false)
    }

    fun appDetails(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    /**
     * Honor / MagicOS app-launch manager. Component names differ between MagicOS versions,
     * so try the known ones and fall back to the battery settings, then this app's details page.
     */
    fun openHonorAutostart(context: Context): Boolean {
        val components = listOf(
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.appcontrol.activity.StartupAppControlActivity",
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.power.ui.HwPowerManagerActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity",
        )
        val intents = components.map { (pkg, cls) -> Intent().setComponent(ComponentName(pkg, cls)) } +
            Intent(Intent.ACTION_POWER_USAGE_SUMMARY) +
            appDetails(context)
        return launchFirst(context, intents)
    }

    private fun launchFirst(context: Context, intents: List<Intent>): Boolean {
        for (intent in intents) {
            try {
                if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        return false
    }
}
