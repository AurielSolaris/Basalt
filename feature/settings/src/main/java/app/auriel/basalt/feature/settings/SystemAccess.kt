package app.auriel.basalt.feature.settings

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * What the system currently lets Basalt do.
 *
 * A clock app is only as reliable as these four answers. Every one of them
 * can be revoked by the user, by the OEM, or by the system itself deciding
 * the app is idle — so they are read fresh each time the screen resumes
 * rather than cached.
 */
data class SystemAccess(
    val notificationsEnabled: Boolean,
    val canScheduleExactAlarms: Boolean,
    val ignoringBatteryOptimizations: Boolean,
) {
    /** True when nothing stands between a scheduled alarm and the user. */
    val allClear: Boolean
        get() = notificationsEnabled && canScheduleExactAlarms && ignoringBatteryOptimizations
}

fun readSystemAccess(context: Context): SystemAccess {
    val notifications = context.getSystemService(NotificationManager::class.java)
    val alarms = context.getSystemService(AlarmManager::class.java)
    val power = context.getSystemService(PowerManager::class.java)
    return SystemAccess(
        notificationsEnabled = notifications?.areNotificationsEnabled() ?: false,
        // Below API 31 there is no permission to hold: exact alarms simply
        // work, so the honest answer is "yes".
        canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarms?.canScheduleExactAlarms() ?: false
        } else {
            true
        },
        ignoringBatteryOptimizations =
            power?.isIgnoringBatteryOptimizations(context.packageName) ?: false,
    )
}

/**
 * Asks to be exempted from battery optimisation.
 *
 * This is the single most important switch for a clock app on a modern
 * device. Without the exemption the app lands in a restricted App Standby
 * bucket, and while `setAlarmClock` itself still fires, everything around it
 * — rescheduling the next occurrence, restoring after a reboot, a timer's
 * foreground service surviving — is throttled or killed. On several OEM
 * skins the app is simply frozen overnight.
 *
 * The direct request dialog needs `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`,
 * which Google Play restricts to apps that genuinely need it; alarm clocks
 * are on the permitted list. If the dialog is unavailable for any reason,
 * this falls back to the system list, which needs no permission at all.
 */
fun requestIgnoreBatteryOptimizations(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:" + context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (!context.tryStart(direct)) {
        context.tryStart(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

fun requestExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:" + context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.tryStart(intent)) return
    }
    openAppSettings(context)
}

fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (!context.tryStart(intent)) openAppSettings(context)
}

/**
 * The app's system settings page.
 *
 * The escape hatch for everything OEM-specific that has no public intent:
 * Samsung's "Put app to sleep", Xiaomi's autostart, OnePlus's deep
 * optimisation. Basalt cannot turn those off itself, and pretending
 * otherwise would be worse than pointing at them.
 */
fun openAppSettings(context: Context) {
    context.tryStart(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:" + context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}

/**
 * Starts an activity, reporting failure instead of crashing.
 *
 * Every intent here is optional on some device somewhere: OEMs remove
 * settings screens, and a missing one must degrade to a fallback rather
 * than take the app down.
 */
private fun Context.tryStart(intent: Intent): Boolean = runCatching {
    startActivity(intent)
    true
}.getOrDefault(false)
